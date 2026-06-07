package com.example.voxelgame.game.entity.villager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.joml.Vector3f;
import org.joml.Vector3i;

import com.example.voxelgame.game.Player;
import com.example.voxelgame.game.entity.BoundingBox;
import com.example.voxelgame.game.entity.Entity;
import com.example.voxelgame.game.entity.EntityPersistenceData;
import com.example.voxelgame.game.entity.EntityRenderContext;
import com.example.voxelgame.game.entity.EntityUpdateContext;
import com.example.voxelgame.game.entity.animal.GroundPathfinder;
import com.example.voxelgame.world.BlockRegistry;
import com.example.voxelgame.world.VoxelWorld;

public final class VillagerEntity extends Entity {
    public static final String PERSISTENCE_TYPE = "villager";
    private static final boolean DEBUG_MARKERS = Boolean.getBoolean("voxelgame.debugVillagers");
    private static final Vector3f HALF_EXTENTS = new Vector3f(0.28f, 0.90f, 0.28f);
    private static final float GRAVITY = 18.0f;
    private static final float WALK_SPEED = 1.05f;
    private static final float TARGET_REACHED_DISTANCE = 0.55f;
    private static final float MAX_HOME_DISTANCE = 28.0f;
    private static final double AI_INTERVAL_SECONDS = 0.25;
    private static final double PATH_INTERVAL_SECONDS = 1.25;

    private final GroundPathfinder pathfinder = new GroundPathfinder();
    private final long villagerId;
    private final Random random;
    private VillagerProfession profession;
    private Vector3i homePosition;
    private Vector3i workstationPosition;
    private Vector3i villageCenter;
    private VillagerBehaviorState behaviorState = VillagerBehaviorState.IDLE;
    private VillagerScheduleState scheduleState = VillagerScheduleState.DAY;
    private List<Vector3f> path = List.of();
    private final List<TradeOffer> tradeOffers = new ArrayList<>();
    private int pathIndex;
    private double stateTimerSeconds;
    private double aiTimerSeconds;
    private double pathTimerSeconds;
    private int stuckTicks;
    private Vector3f lastPosition;
    private float yawRadians;
    private double animationSeconds;

    public VillagerEntity(long villagerId, VillagerProfession profession, Vector3f position, Vector3i homePosition, Vector3i workstationPosition, Vector3i villageCenter) {
        this(villagerId, profession, position, new Vector3f(), homePosition, workstationPosition, villageCenter);
    }

    private VillagerEntity(long villagerId, VillagerProfession profession, Vector3f position, Vector3f velocity, Vector3i homePosition, Vector3i workstationPosition, Vector3i villageCenter) {
        super(position, velocity, HALF_EXTENTS);
        this.villagerId = villagerId;
        this.profession = profession;
        this.homePosition = new Vector3i(homePosition);
        this.workstationPosition = new Vector3i(workstationPosition);
        this.villageCenter = new Vector3i(villageCenter);
        this.random = new Random(villagerId ^ 0x7782C1F1E2C8F52DL);
        this.tradeOffers.addAll(VillagerTradeTable.initialOffers(profession, villagerId));
        this.lastPosition = new Vector3f(position);
    }

    public long getVillagerId() {
        return villagerId;
    }

    public VillagerProfession getProfession() {
        return profession;
    }

    public VillagerBehaviorState getBehaviorState() {
        return behaviorState;
    }

    public VillagerScheduleState getScheduleState() {
        return scheduleState;
    }

    public Vector3i getHomePosition() {
        return new Vector3i(homePosition);
    }

    public Vector3i getWorkstationPosition() {
        return new Vector3i(workstationPosition);
    }

    public List<TradeOffer> getTradeOffers() {
        return tradeOffers.stream().map(offer -> new TradeOffer(offer.id(), offer.cost(), offer.result(), offer.maxUses(), offer.uses())).toList();
    }

    public boolean tryTrade(Player player, int offerIndex) {
        if (offerIndex < 0 || offerIndex >= tradeOffers.size()) {
            return false;
        }
        TradeOffer offer = tradeOffers.get(offerIndex);
        if (!offer.canExecute(player.getInventory())) {
            return false;
        }
        tradeOffers.set(offerIndex, offer.execute(player.getInventory()));
        return true;
    }

    @Override
    public void update(double deltaTimeSeconds, EntityUpdateContext context) {
        if (!isLoadedForUpdate(context.world())) {
            setVelocity(new Vector3f());
            return;
        }

        animationSeconds += deltaTimeSeconds;
        stateTimerSeconds -= deltaTimeSeconds;
        aiTimerSeconds -= deltaTimeSeconds;
        pathTimerSeconds -= deltaTimeSeconds;
        scheduleState = VillagerScheduleState.from(context.worldTime());

        if (aiTimerSeconds <= 0.0) {
            aiTimerSeconds = AI_INTERVAL_SECONDS;
            updateBehavior(context);
            updateStuckDetection();
        }

        followPath(deltaTimeSeconds, context.world());
        applyMovement(deltaTimeSeconds, context.world());
    }

    private boolean isLoadedForUpdate(VoxelWorld world) {
        Vector3f position = getPosition();
        return world.isBlockLoadedAtWorld((int) Math.floor(position.x), Math.max(0, (int) Math.floor(position.y)), (int) Math.floor(position.z));
    }

    private void updateBehavior(EntityUpdateContext context) {
        if (scheduleState == VillagerScheduleState.NIGHT) {
            if (isNear(homePosition, 1.6f)) {
                enterState(VillagerBehaviorState.SLEEP, 3.0);
                setPath(List.of());
                dampHorizontalVelocity();
            } else {
                moveToward(context.world(), standNear(homePosition), VillagerBehaviorState.GO_HOME);
            }
            return;
        }

        if (scheduleState == VillagerScheduleState.EVENING) {
            moveToward(context.world(), standNear(homePosition), VillagerBehaviorState.GO_HOME);
            return;
        }

        if (scheduleState == VillagerScheduleState.DAY && profession != VillagerProfession.UNEMPLOYED && !isNear(workstationPosition, 2.2f)) {
            moveToward(context.world(), standNear(workstationPosition), VillagerBehaviorState.WORK);
            return;
        }

        if (scheduleState == VillagerScheduleState.MORNING && !isNear(villageCenter, 5.5f)) {
            moveToward(context.world(), villageCenter, VillagerBehaviorState.GATHER);
            return;
        }

        if (stateTimerSeconds <= 0.0 || pathIndex >= path.size()) {
            chooseVillageWander(context.world());
        }
    }

    private void chooseVillageWander(VoxelWorld world) {
        int dx = random.nextInt(23) - 11;
        int dz = random.nextInt(23) - 11;
        Vector3i target = new Vector3i(villageCenter.x + dx, villageCenter.y + 1, villageCenter.z + dz);
        moveToward(world, target, random.nextInt(100) < 28 ? VillagerBehaviorState.IDLE : VillagerBehaviorState.WANDER);
        stateTimerSeconds = 2.5 + random.nextDouble() * 4.0;
    }

    private void moveToward(VoxelWorld world, Vector3i target, VillagerBehaviorState state) {
        enterState(state, 4.0);
        if (pathTimerSeconds > 0.0 && !path.isEmpty()) {
            return;
        }
        pathTimerSeconds = PATH_INTERVAL_SECONDS;
        Vector3f targetPosition = new Vector3f(target.x + 0.5f, target.y, target.z + 0.5f);
        if (distanceToVillage(targetPosition) > MAX_HOME_DISTANCE + 10.0f) {
            targetPosition.set(villageCenter.x + 0.5f, villageCenter.y + 1.0f, villageCenter.z + 0.5f);
        }
        setPath(pathfinder.findPath(world, getPosition(), targetPosition));
    }

    private void enterState(VillagerBehaviorState state, double timerSeconds) {
        if (behaviorState != state) {
            behaviorState = state;
            stateTimerSeconds = timerSeconds;
        }
    }

    private void setPath(List<Vector3f> newPath) {
        path = newPath;
        pathIndex = 0;
    }

    private void followPath(double deltaTimeSeconds, VoxelWorld world) {
        if (behaviorState == VillagerBehaviorState.SLEEP || behaviorState == VillagerBehaviorState.IDLE || pathIndex >= path.size()) {
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

        if (isWaterAhead(world, toTarget)) {
            setPath(List.of());
            return;
        }

        float speed = isOnPath(world) ? WALK_SPEED * 1.18f : WALK_SPEED;
        Vector3f desired = toTarget.normalize().mul(speed);
        yawRadians = (float) Math.atan2(desired.x, desired.z);
        Vector3f velocity = getVelocity();
        float alpha = Math.clamp((float) deltaTimeSeconds * 5.0f, 0.0f, 1.0f);
        velocity.x += (desired.x - velocity.x) * alpha;
        velocity.z += (desired.z - velocity.z) * alpha;
        setVelocity(velocity);
    }

    private boolean isWaterAhead(VoxelWorld world, Vector3f direction) {
        if (direction.lengthSquared() <= 0.001f) {
            return false;
        }
        Vector3f ahead = new Vector3f(getPosition()).add(direction.normalize().mul(0.85f));
        return world.isWaterAtWorld((int) Math.floor(ahead.x), (int) Math.floor(ahead.y), (int) Math.floor(ahead.z));
    }

    private boolean isOnPath(VoxelWorld world) {
        Vector3f position = getPosition();
        return world.getBlockAtWorld((int) Math.floor(position.x), Math.max(0, (int) Math.floor(position.y) - 1), (int) Math.floor(position.z)) == BlockRegistry.DIRT_PATH;
    }

    private void updateStuckDetection() {
        if (new Vector3f(getPosition()).sub(lastPosition).lengthSquared() < 0.01f && behaviorState != VillagerBehaviorState.SLEEP) {
            stuckTicks++;
        } else {
            stuckTicks = 0;
        }
        lastPosition = getPosition();
        if (stuckTicks >= 8) {
            setPath(List.of());
            pathTimerSeconds = 0.0;
            stuckTicks = 0;
        }
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
        BoundingBox bounds = BoundingBox.fromCenter(nextPosition, getHalfExtents());
        if (!isBoundsLoaded(world, bounds)) {
            setVelocity(new Vector3f());
            setPath(List.of());
            return;
        }
        if (!intersectsSolid(world, bounds)) {
            setPosition(nextPosition);
            return;
        }
        Vector3f velocity = getVelocity();
        if (verticalMove) {
            velocity.y = 0.0f;
        } else {
            velocity.x = 0.0f;
            velocity.z = 0.0f;
            setPath(List.of());
        }
        setVelocity(velocity);
    }

    private boolean isBoundsLoaded(VoxelWorld world, BoundingBox bounds) {
        Vector3f min = bounds.min();
        Vector3f max = bounds.max();
        for (int x = (int) Math.floor(min.x); x <= (int) Math.floor(max.x); x++) {
            for (int y = Math.max(0, (int) Math.floor(min.y)); y <= (int) Math.floor(max.y); y++) {
                for (int z = (int) Math.floor(min.z); z <= (int) Math.floor(max.z); z++) {
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

    private void dampHorizontalVelocity() {
        Vector3f velocity = getVelocity();
        velocity.x *= 0.70f;
        velocity.z *= 0.70f;
        setVelocity(velocity);
    }

    private boolean isNear(Vector3i target, float distance) {
        return new Vector3f(target.x + 0.5f, target.y, target.z + 0.5f).distance(getPosition()) <= distance;
    }

    private float distanceToVillage(Vector3f target) {
        float dx = target.x - villageCenter.x;
        float dz = target.z - villageCenter.z;
        return (float) Math.sqrt(dx * dx + dz * dz);
    }

    @Override
    public void render(EntityRenderContext context) {
        Vector3f feet = getPosition();
        float walk = (float) Math.sin(animationSeconds * 7.0) * Math.min(0.16f, Math.abs(getVelocity().x) + Math.abs(getVelocity().z));
        float coatR = profession.red();
        float coatG = profession.green();
        float coatB = profession.blue();
        if (behaviorState == VillagerBehaviorState.SLEEP) {
            yawRadians += 0.0f;
        }
        context.drawCube(new Vector3f(feet).add(0.0f, 0.58f, 0.0f), new Vector3f(0.24f, 0.36f, 0.18f), yawRadians, coatR, coatG, coatB, 1.0f);
        context.drawCube(new Vector3f(feet).add(0.0f, 1.10f, 0.0f), new Vector3f(0.22f, 0.22f, 0.22f), yawRadians, 0.70f, 0.52f, 0.36f, 1.0f);
        context.drawCube(new Vector3f(feet).add(-0.16f, 0.17f + walk, 0.0f), new Vector3f(0.08f, 0.18f, 0.08f), yawRadians, 0.18f, 0.16f, 0.14f, 1.0f);
        context.drawCube(new Vector3f(feet).add(0.16f, 0.17f - walk, 0.0f), new Vector3f(0.08f, 0.18f, 0.08f), yawRadians, 0.18f, 0.16f, 0.14f, 1.0f);
        if (DEBUG_MARKERS) {
            context.drawCube(new Vector3f(homePosition.x + 0.5f, homePosition.y + 1.25f, homePosition.z + 0.5f), new Vector3f(0.12f), 0.0f, 0.24f, 0.52f, 1.0f, 0.85f);
            context.drawCube(new Vector3f(workstationPosition.x + 0.5f, workstationPosition.y + 1.25f, workstationPosition.z + 0.5f), new Vector3f(0.12f), 0.0f, 1.0f, 0.72f, 0.18f, 0.85f);
            for (Vector3f node : path) {
                context.drawCube(new Vector3f(node).add(0.0f, 0.15f, 0.0f), new Vector3f(0.08f), 0.0f, 0.30f, 0.95f, 0.62f, 0.65f);
            }
        }
    }

    @Override
    public java.util.Optional<EntityPersistenceData> saveData() {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("id", Long.toString(villagerId));
        data.put("profession", profession.id());
        data.put("home", encode(homePosition));
        data.put("workstation", encode(workstationPosition));
        data.put("village", encode(villageCenter));
        data.put("behavior", behaviorState.name());
        data.put("schedule", scheduleState.name());
        for (int i = 0; i < tradeOffers.size(); i++) {
            data.put("trade." + i + ".uses", Integer.toString(tradeOffers.get(i).uses()));
        }
        return java.util.Optional.of(new EntityPersistenceData(PERSISTENCE_TYPE, getPosition(), getVelocity(), data));
    }

    public static VillagerEntity fromSave(EntityPersistenceData data) {
        Map<String, String> values = data.data();
        long id = parseLong(values.get("id"), mix64(Double.doubleToLongBits(data.position().x * 31.0 + data.position().z * 17.0)));
        VillagerProfession profession = VillagerProfession.byId(values.getOrDefault("profession", "unemployed"));
        VillagerEntity villager = new VillagerEntity(
                id,
                profession,
                data.position(),
                data.velocity(),
                decode(values.get("home"), data.position()),
                decode(values.get("workstation"), data.position()),
                decode(values.get("village"), data.position())
        );
        villager.behaviorState = parseBehavior(values.get("behavior"));
        villager.scheduleState = parseSchedule(values.get("schedule"));
        for (int i = 0; i < villager.tradeOffers.size(); i++) {
            TradeOffer offer = villager.tradeOffers.get(i);
            int uses = (int) parseLong(values.get("trade." + i + ".uses"), offer.uses());
            villager.tradeOffers.set(i, new TradeOffer(offer.id(), offer.cost(), offer.result(), offer.maxUses(), Math.clamp(uses, 0, offer.maxUses())));
        }
        return villager;
    }

    private static String encode(Vector3i position) {
        return position.x + "," + position.y + "," + position.z;
    }

    private static Vector3i standNear(Vector3i blockPosition) {
        return new Vector3i(blockPosition.x + 1, blockPosition.y, blockPosition.z + 1);
    }

    private static Vector3i decode(String raw, Vector3f fallback) {
        if (raw == null) {
            return new Vector3i((int) Math.floor(fallback.x), (int) Math.floor(fallback.y), (int) Math.floor(fallback.z));
        }
        String[] parts = raw.split(",");
        if (parts.length != 3) {
            return new Vector3i((int) Math.floor(fallback.x), (int) Math.floor(fallback.y), (int) Math.floor(fallback.z));
        }
        try {
            return new Vector3i(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        } catch (NumberFormatException exception) {
            return new Vector3i((int) Math.floor(fallback.x), (int) Math.floor(fallback.y), (int) Math.floor(fallback.z));
        }
    }

    private static VillagerBehaviorState parseBehavior(String raw) {
        try {
            return raw == null ? VillagerBehaviorState.IDLE : VillagerBehaviorState.valueOf(raw);
        } catch (IllegalArgumentException exception) {
            return VillagerBehaviorState.IDLE;
        }
    }

    private static VillagerScheduleState parseSchedule(String raw) {
        try {
            return raw == null ? VillagerScheduleState.DAY : VillagerScheduleState.valueOf(raw);
        } catch (IllegalArgumentException exception) {
            return VillagerScheduleState.DAY;
        }
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
