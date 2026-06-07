package com.example.voxelgame.game.entity.animal;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import org.joml.Vector3f;

import com.example.voxelgame.game.Player;
import com.example.voxelgame.game.entity.BoundingBox;
import com.example.voxelgame.game.entity.DamageableEntity;
import com.example.voxelgame.game.entity.Entity;
import com.example.voxelgame.game.entity.EntityDamageSource;
import com.example.voxelgame.game.entity.EntityPersistenceData;
import com.example.voxelgame.game.entity.EntityRenderContext;
import com.example.voxelgame.game.entity.EntityUpdateContext;
import com.example.voxelgame.game.entity.EntityManager;
import com.example.voxelgame.game.entity.ItemEntity;
import com.example.voxelgame.world.VoxelWorld;

public class AnimalEntity extends Entity implements DamageableEntity {
    private static final System.Logger LOGGER = System.getLogger(AnimalEntity.class.getName());
    private static final boolean DEBUG_DAMAGE = Boolean.getBoolean("voxelgame.debugEntityDamage");
    public static final String PERSISTENCE_TYPE = "animal";
    private static final float GRAVITY = 18.0f;
    private static final float WALK_SPEED = 1.15f;
    private static final float PANIC_SPEED = 3.0f;
    private static final float TARGET_REACHED_DISTANCE = 0.55f;
    private static final float PLAYER_PANIC_DISTANCE = 2.2f;
    private static final double DROP_PICKUP_DELAY_SECONDS = 0.6;
    private static final double HURT_INVULNERABILITY_SECONDS = 0.35;
    private static final double HURT_FLASH_SECONDS = 0.18;

    private final AnimalSpecies species;
    private final GroundPathfinder pathfinder = new GroundPathfinder();
    private final AnimalBrain brain = new AnimalBrain(List.of(
            new PanicBehavior(),
            new WanderBehavior(),
            new GrazeBehavior(),
            new IdleBehavior()
    ));
    private final long randomSeed;
    private final Random random;
    private AnimalAiState aiState = AnimalAiState.IDLE;
    private List<Vector3f> path = List.of();
    private int pathIndex;
    private double stateTimerSeconds;
    private double breedingCooldownSeconds;
    private final float maxHealth;
    private float health;
    private double hurtCooldownSeconds;
    private double hurtFlashSeconds;
    private int pendingCalmDecisionRoll = -1;
    private float yawRadians;

    public AnimalEntity(AnimalSpecies species, Vector3f position) {
        this(species, position, new Vector3f());
    }

    public AnimalEntity(AnimalSpecies species, Vector3f position, Vector3f velocity) {
        this(species, position, velocity, mix64(Float.floatToIntBits(position.x) * 31L
                ^ Float.floatToIntBits(position.y) * 17L
                ^ Float.floatToIntBits(position.z) * 13L
                ^ species.id().hashCode()));
    }

    private AnimalEntity(AnimalSpecies species, Vector3f position, Vector3f velocity, long randomSeed) {
        super(position, velocity, species.halfExtents());
        this.species = species;
        this.randomSeed = randomSeed;
        this.random = new Random(randomSeed);
        this.stateTimerSeconds = 1.0;
        this.maxHealth = maxHealthFor(species);
        this.health = maxHealth;
    }

    public AnimalSpecies getSpecies() {
        return species;
    }

    public List<com.example.voxelgame.game.inventory.ItemStack> createDrops() {
        return AnimalDropTable.dropsFor(species);
    }

    public double getBreedingCooldownSeconds() {
        return breedingCooldownSeconds;
    }

    @Override
    public float getCurrentHealth() {
        return health;
    }

    @Override
    public float getMaxHealth() {
        return maxHealth;
    }

    public AnimalAiState getAiState() {
        return aiState;
    }

    double getStateTimerSeconds() {
        return stateTimerSeconds;
    }

    float panicDistance() {
        return PLAYER_PANIC_DISTANCE;
    }

    float distanceToPlayer(Player player) {
        return player.getPosition().distance(getPosition());
    }

    @Override
    public void update(double deltaTimeSeconds, EntityUpdateContext context) {
        if (!isLoadedForUpdate(context.world())) {
            setVelocity(new Vector3f());
            return;
        }

        breedingCooldownSeconds = Math.max(0.0, breedingCooldownSeconds - deltaTimeSeconds);
        hurtCooldownSeconds = Math.max(0.0, hurtCooldownSeconds - deltaTimeSeconds);
        hurtFlashSeconds = Math.max(0.0, hurtFlashSeconds - deltaTimeSeconds);
        stateTimerSeconds -= deltaTimeSeconds;
        brain.tick(this, deltaTimeSeconds, context);
        applyMovement(deltaTimeSeconds, context.world());
    }

    @Override
    public boolean damage(float amount, EntityDamageSource source, EntityManager entityManager) {
        if (amount <= 0.0f || health <= 0.0f || hurtCooldownSeconds > 0.0) {
            return false;
        }

        health = Math.max(0.0f, health - amount);
        hurtCooldownSeconds = HURT_INVULNERABILITY_SECONDS;
        hurtFlashSeconds = HURT_FLASH_SECONDS;
        aiState = AnimalAiState.PANIC;
        stateTimerSeconds = Math.max(stateTimerSeconds, 2.4);
        pathIndex = path.size();

        if (DEBUG_DAMAGE) {
            LOGGER.log(
                    System.Logger.Level.INFO,
                    "Animal damaged: species={0}, amount={1}, health={2}/{3}, source={4}",
                    species.id(),
                    amount,
                    health,
                    maxHealth,
                    source
            );
        }

        if (health <= 0.0f) {
            killAndDrop(entityManager);
        }
        return true;
    }

    private boolean isLoadedForUpdate(VoxelWorld world) {
        Vector3f position = getPosition();
        return world.isBlockLoadedAtWorld((int) Math.floor(position.x), Math.max(0, (int) Math.floor(position.y)), (int) Math.floor(position.z));
    }

    void enterPanic(VoxelWorld world, Vector3f playerPosition) {
        Vector3f away = new Vector3f(getPosition()).sub(playerPosition);
        if (away.lengthSquared() < 0.001f) {
            away.set(1.0f, 0.0f, 0.0f);
        }
        away.y = 0.0f;
        away.normalize().mul(7.0f).add(getPosition());
        aiState = AnimalAiState.PANIC;
        stateTimerSeconds = 3.5;
        path = pathfinder.findPath(world, getPosition(), away);
        pathIndex = 0;
        pendingCalmDecisionRoll = -1;
    }

    boolean isReadyForCalmDecision() {
        return stateTimerSeconds <= 0.0 || aiState == AnimalAiState.PANIC || currentPathFinished();
    }

    int nextCalmDecisionRoll() {
        if (pendingCalmDecisionRoll < 0) {
            pendingCalmDecisionRoll = random.nextInt(100);
        }
        return pendingCalmDecisionRoll;
    }

    void enterGraze() {
        int roll = consumeCalmDecisionRoll();
        aiState = AnimalAiState.GRAZE;
        path = List.of();
        pathIndex = 0;
        stateTimerSeconds = 2.5 + (roll % 20) * 0.08;
        dampHorizontalVelocity();
    }

    void enterIdle() {
        int roll = consumeCalmDecisionRoll();
        aiState = AnimalAiState.IDLE;
        path = List.of();
        pathIndex = 0;
        stateTimerSeconds = 1.2 + (roll % 16) * 0.1;
        dampHorizontalVelocity();
    }

    void enterWander(VoxelWorld world) {
        long hash = random.nextLong();
        aiState = AnimalAiState.WANDER;
        Vector3f target = randomNearbyTarget(hash);
        path = pathfinder.findPath(world, getPosition(), target);
        pathIndex = 0;
        stateTimerSeconds = 6.0;
        pendingCalmDecisionRoll = -1;
        if (path.isEmpty()) {
            enterIdle();
        }
    }

    private int consumeCalmDecisionRoll() {
        int roll = nextCalmDecisionRoll();
        pendingCalmDecisionRoll = -1;
        return roll;
    }

    private Vector3f randomNearbyTarget(long hash) {
        float dx = (Math.floorMod((int) hash, 13) - 6);
        float dz = (Math.floorMod((int) (hash >>> 32), 13) - 6);
        if (Math.abs(dx) + Math.abs(dz) < 2.0f) {
            dx += 3.0f;
        }
        return new Vector3f(getPosition()).add(dx, 0.0f, dz);
    }

    void followCurrentPath(double deltaTimeSeconds) {
        if (pathIndex >= path.size()) {
            dampHorizontalVelocity();
            return;
        }
        Vector3f target = path.get(pathIndex);
        Vector3f toTarget = new Vector3f(target).sub(getPosition());
        toTarget.y = 0.0f;
        if (toTarget.length() <= TARGET_REACHED_DISTANCE) {
            pathIndex++;
            return;
        }
        float speed = aiState == AnimalAiState.PANIC ? PANIC_SPEED : WALK_SPEED;
        Vector3f desired = toTarget.normalize().mul(speed);
        yawRadians = (float) Math.atan2(desired.x, desired.z);
        Vector3f velocity = getVelocity();
        float alpha = Math.clamp((float) deltaTimeSeconds * 5.0f, 0.0f, 1.0f);
        velocity.x += (desired.x - velocity.x) * alpha;
        velocity.z += (desired.z - velocity.z) * alpha;
        setVelocity(velocity);
    }

    boolean currentPathFinished() {
        return pathIndex >= path.size();
    }

    private void applyMovement(double deltaTimeSeconds, VoxelWorld world) {
        Vector3f velocity = getVelocity();
        velocity.y = Math.max(-20.0f, velocity.y - GRAVITY * (float) deltaTimeSeconds);
        setVelocity(velocity);
        move(world, deltaTimeSeconds);
    }

    private void move(VoxelWorld world, double deltaTimeSeconds) {
        Vector3f velocity = getVelocity();
        attemptAxisMove(world, new Vector3f(velocity.x * (float) deltaTimeSeconds, 0.0f, 0.0f), false);
        attemptAxisMove(world, new Vector3f(0.0f, 0.0f, velocity.z * (float) deltaTimeSeconds), false);
        attemptAxisMove(world, new Vector3f(0.0f, velocity.y * (float) deltaTimeSeconds, 0.0f), true);
    }

    private void attemptAxisMove(VoxelWorld world, Vector3f delta, boolean verticalMove) {
        if (delta.lengthSquared() == 0.0f) {
            return;
        }
        Vector3f nextPosition = new Vector3f(getPosition()).add(delta);
        BoundingBox nextBounds = BoundingBox.fromCenter(nextPosition, getHalfExtents());
        if (!isBoundsLoaded(world, nextBounds)) {
            setVelocity(new Vector3f());
            pathIndex = path.size();
            return;
        }
        if (!intersectsSolid(world, nextBounds)) {
            setPosition(nextPosition);
            return;
        }
        Vector3f velocity = getVelocity();
        if (verticalMove) {
            velocity.y = 0.0f;
        } else {
            velocity.x *= -0.18f;
            velocity.z *= -0.18f;
            pathIndex = path.size();
        }
        setVelocity(velocity);
    }

    private boolean isBoundsLoaded(VoxelWorld world, BoundingBox bounds) {
        Vector3f min = bounds.min();
        Vector3f max = bounds.max();
        int minX = (int) Math.floor(min.x);
        int maxX = (int) Math.floor(max.x);
        int minY = Math.max(0, (int) Math.floor(min.y));
        int maxY = (int) Math.floor(max.y);
        int minZ = (int) Math.floor(min.z);
        int maxZ = (int) Math.floor(max.z);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (!world.isBlockLoadedAtWorld(x, y, z)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean intersectsSolid(VoxelWorld world, BoundingBox bounds) {
        Vector3f min = bounds.min();
        Vector3f max = bounds.max();
        for (int x = (int) Math.floor(min.x); x <= (int) Math.floor(max.x); x++) {
            for (int y = (int) Math.floor(min.y); y <= (int) Math.floor(max.y); y++) {
                for (int z = (int) Math.floor(min.z); z <= (int) Math.floor(max.z); z++) {
                    if (world.isSolidBlockAtWorld(x, y, z)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    void dampHorizontalVelocity() {
        Vector3f velocity = getVelocity();
        velocity.x *= 0.72f;
        velocity.z *= 0.72f;
        setVelocity(velocity);
    }

    public Optional<AnimalEntity> createChildWith(AnimalEntity mate) {
        if (!AnimalBreedingHooks.canBreed(this, mate)) {
            return Optional.empty();
        }
        Vector3f childPosition = getPosition().add(mate.getPosition()).mul(0.5f);
        AnimalEntity child = new AnimalEntity(species, childPosition, new Vector3f());
        breedingCooldownSeconds = AnimalBreedingHooks.defaultCooldownSeconds();
        mate.breedingCooldownSeconds = AnimalBreedingHooks.defaultCooldownSeconds();
        child.breedingCooldownSeconds = AnimalBreedingHooks.defaultCooldownSeconds();
        return Optional.of(child);
    }

    public void killAndDrop(EntityManager entityManager) {
        for (com.example.voxelgame.game.inventory.ItemStack drop : createDrops()) {
            entityManager.spawn(new ItemEntity(
                    new Vector3f(getPosition()).add(0.0f, 0.3f, 0.0f),
                    new Vector3f((random.nextFloat() - 0.5f) * 0.8f, 1.8f, (random.nextFloat() - 0.5f) * 0.8f),
                    drop,
                    DROP_PICKUP_DELAY_SECONDS
            ));
        }
        markRemoved();
    }

    @Override
    public void render(EntityRenderContext context) {
        Vector3f body = new Vector3f(getPosition()).add(0.0f, getHalfExtents().y, 0.0f);
        float hurtMix = hurtFlashSeconds > 0.0 ? 0.55f : 0.0f;
        float red = species.red() + (1.0f - species.red()) * hurtMix;
        float green = species.green() * (1.0f - hurtMix);
        float blue = species.blue() * (1.0f - hurtMix);
        context.drawCube(body, getHalfExtents(), yawRadians, red, green, blue, 1.0f);
        Vector3f headOffset = new Vector3f(0.0f, getHalfExtents().y * 0.28f, getHalfExtents().z + 0.18f).rotateY(yawRadians);
        context.drawCube(new Vector3f(body).add(headOffset), new Vector3f(getHalfExtents()).mul(0.46f), yawRadians, red * 0.9f, green * 0.9f, blue * 0.9f, 1.0f);
    }

    @Override
    public java.util.Optional<EntityPersistenceData> saveData() {
        return java.util.Optional.of(new EntityPersistenceData(
                PERSISTENCE_TYPE,
                getPosition(),
                getVelocity(),
                Map.of(
                        "species", species.id(),
                        "state", aiState.name(),
                        "breedingCooldown", Double.toString(breedingCooldownSeconds),
                        "health", Float.toString(health),
                        "randomSeed", Long.toString(randomSeed)
                )
        ));
    }

    public static AnimalEntity fromSave(EntityPersistenceData data) {
        AnimalSpecies species = AnimalSpecies.byId(data.data().getOrDefault("species", "cow"));
        AnimalEntity entity = new AnimalEntity(species, data.position(), data.velocity(), parseLong(
                data.data().get("randomSeed"),
                mix64(Double.doubleToLongBits(data.position().x * 31.0 + data.position().z * 17.0))
        ));
        entity.aiState = parseAiState(data.data().get("state"));
        entity.breedingCooldownSeconds = parseDouble(data.data().get("breedingCooldown"));
        entity.health = Math.clamp((float) parseDouble(data.data().get("health"), entity.maxHealth), 0.0f, entity.maxHealth);
        return entity;
    }

    private static AnimalAiState parseAiState(String raw) {
        if (raw == null) {
            return AnimalAiState.IDLE;
        }
        try {
            return AnimalAiState.valueOf(raw);
        } catch (IllegalArgumentException exception) {
            return AnimalAiState.IDLE;
        }
    }

    private static double parseDouble(String raw) {
        return parseDouble(raw, 0.0);
    }

    private static double parseDouble(String raw, double fallback) {
        if (raw == null) {
            return fallback;
        }
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static float maxHealthFor(AnimalSpecies species) {
        return switch (species) {
            case CHICKEN -> 4.0f;
            case PIG, SHEEP -> 8.0f;
            case COW -> 10.0f;
        };
    }

    private static long parseLong(String raw, long fallback) {
        if (raw == null) {
            return fallback;
        }
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static long mix64(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return value;
    }
}
