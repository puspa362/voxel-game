package com.example.voxelgame.game;

import com.example.voxelgame.camera.FirstPersonCamera;
import com.example.voxelgame.core.InputState;
import com.example.voxelgame.game.entity.EntityManager;
import com.example.voxelgame.game.entity.ItemEntity;
import com.example.voxelgame.game.inventory.ArmorSlot;
import com.example.voxelgame.game.inventory.Inventory;
import com.example.voxelgame.game.inventory.Item;
import com.example.voxelgame.game.inventory.ItemStack;
import com.example.voxelgame.game.mode.GameMode;
import com.example.voxelgame.game.mode.GameModeRules;
import com.example.voxelgame.game.world.MiningCalculator;
import com.example.voxelgame.game.world.WorldInteractionSystem;
import org.joml.Vector3f;
import com.example.voxelgame.world.Block;
import com.example.voxelgame.world.BlockHitResult;
import com.example.voxelgame.world.VoxelWorld;
import com.example.voxelgame.world.WaterContact;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_1;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_2;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_3;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_4;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_5;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_6;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_7;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_8;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_9;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_CONTROL;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_W;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;

public final class Player {
    private static final float SAFE_FALL_SPEED = 10.0f;
    private static final float FALL_DAMAGE_SCALE = 0.5f;
    private static final float HUNGER_MAX = 20.0f;
    private static final int HUNGER_DRAIN_INTERVAL_TICKS = 120;
    private static final int HUNGER_SPRINT_DRAIN_INTERVAL_TICKS = 80;
    private static final float HUNGER_BASE_DRAIN_AMOUNT = 0.18f;
    private static final float HUNGER_SPRINT_DRAIN_AMOUNT = 0.20f;
    private static final float HUNGER_JUMP_DRAIN_AMOUNT = 0.10f;
    private static final float REGEN_HUNGER_THRESHOLD = 18.0f;
    private static final int REGEN_INTERVAL_TICKS = 60;
    private static final float REGEN_HEALTH_AMOUNT = 1.0f;
    private static final int STARVATION_DELAY_TICKS = 100;
    private static final int STARVATION_INTERVAL_TICKS = 100;
    private static final float STARVATION_DAMAGE_AMOUNT = 1.0f;
    private static final float STARVATION_MIN_HEALTH = 1.0f;
    private static final float SPRINT_SPEED_MULTIPLIER = 1.45f;
    private static final float VOID_RESPAWN_Y = -32.0f;
    private static final double DAMAGE_FLASH_DURATION_SECONDS = 0.45;
    private static final double ATTACK_COOLDOWN_SECONDS = 0.45;
    private static final float ENTITY_ATTACK_REACH = 4.2f;

    private final PlayerTransform transform;
    private final PlayerComponents components;
    private final PlayerState state = new PlayerState();
    private final PlayerVitals vitals = new PlayerVitals();
    private final List<StatusEffect> statusEffects = new ArrayList<>();
    private final Vector3f spawnPoint;

    public Player(FirstPersonCamera camera, Inventory inventory) {
        Objects.requireNonNull(camera, "Player camera cannot be null.");
        Objects.requireNonNull(inventory, "Player inventory cannot be null.");

        this.transform = new PlayerTransform(camera.getPosition(), new Vector3f());
        this.components = new PlayerComponents(camera, inventory, new PlayerInputHandler());
        this.spawnPoint = camera.getPosition();
    }

    public void update(
            InputState inputState,
            VoxelWorld world,
            WorldInteractionSystem interactionSystem,
            EntityManager entityManager,
            boolean movementInputActive,
            boolean interactionInputActive,
            double deltaTimeSeconds
    ) {
        Objects.requireNonNull(inputState, "Input state cannot be null.");
        Objects.requireNonNull(world, "Voxel world cannot be null.");
        Objects.requireNonNull(interactionSystem, "World interaction system cannot be null.");
        Objects.requireNonNull(entityManager, "Entity manager cannot be null.");

        components.inputHandler().handleHotbarSelection(inputState, components.inventory());

        if (movementInputActive) {
            unstickFromSolid(world);
        }
        Vector3f previousPosition = transform.position();
        if (interactionInputActive) {
            components.camera().updateLook(inputState);
        }
        if (movementInputActive) {
            updateMovement(inputState, world, deltaTimeSeconds);
        }

        syncCameraPosition();
        updateVelocity(previousPosition, deltaTimeSeconds);
        resolveVoidRespawnIfNeeded();

        interactionSystem.update(inputState, this, world, interactionInputActive);
        state.attackCooldownSeconds = Math.max(0.0, state.attackCooldownSeconds - deltaTimeSeconds);
        if (tryAttackEntity(inputState, world, interactionSystem, interactionInputActive)) {
            resetMining(interactionSystem, "Mining: entity targeted");
        } else {
            updateMining(inputState, world, interactionSystem, interactionInputActive, deltaTimeSeconds);
        }
        resolveDeathIfNeeded(entityManager);
    }

    public Vector3f getPosition() {
        return transform.position();
    }

    public Vector3f getSpawnPoint() {
        return new Vector3f(spawnPoint);
    }

    public void setPosition(Vector3f position) {
        transform.setPosition(Objects.requireNonNull(position, "Player position cannot be null."));
        transform.setVelocity(new Vector3f());
        state.verticalVelocity = 0.0f;
        state.onGround = false;
        syncCameraPosition();
    }

    public Vector3f getVelocity() {
        return transform.velocity();
    }

    public FirstPersonCamera getCamera() {
        return components.camera();
    }

    public Inventory getInventory() {
        return components.inventory();
    }

    public Vector3f getEyePosition() {
        return components.camera().getEyePosition();
    }

    public Vector3f getForward() {
        return components.camera().getForward();
    }

    public boolean isOnGround() {
        return state.onGround;
    }

    public float getColliderRadius() {
        return components.camera().getColliderRadius();
    }

    public float getPlayerHeight() {
        return components.camera().getPlayerHeight();
    }

    public Optional<ItemStack> getSelectedItemStack() {
        return components.inventory().getSelectedHotbarStack();
    }

    public float getMiningProgress() {
        return state.miningProgress;
    }

    public Optional<BlockHitResult> getMiningTarget() {
        return state.miningTarget;
    }

    public String getMiningDebugText() {
        return state.miningDebugText;
    }

    public String getWaterDebugText() {
        return "Player water: in=%s head=%s depth=%d submerge=%.2f".formatted(
                state.inWater ? "yes" : "no",
                state.headUnderwater ? "yes" : "no",
                state.waterDepthBelowFeet,
                state.waterSubmersion
        );
    }

    public boolean isInWater() {
        return state.inWater;
    }

    public boolean isHeadUnderwater() {
        return state.headUnderwater;
    }

    public float getHealth() {
        return vitals.health;
    }

    public float getCurrentHealth() {
        return vitals.health;
    }

    public float getMaxHealth() {
        return vitals.maxHealth;
    }

    public float getHunger() {
        return vitals.hunger;
    }

    public GameMode getGameMode() {
        return state.gameMode;
    }

    public GameModeRules getGameModeRules() {
        return GameModeRules.forMode(state.gameMode);
    }

    public void setGameMode(GameMode gameMode) {
        GameMode previous = state.gameMode;
        state.gameMode = Objects.requireNonNull(gameMode, "Game mode cannot be null.");
        if (previous != state.gameMode) {
            resetMining(null, "Mining: idle");
            if (state.gameMode == GameMode.CREATIVE) {
                vitals.health = vitals.maxHealth;
                vitals.hunger = vitals.maxHunger;
                state.verticalVelocity = 0.0f;
            }
        }
    }

    public void toggleGameMode() {
        setGameMode(state.gameMode == GameMode.SURVIVAL ? GameMode.CREATIVE : GameMode.SURVIVAL);
    }

    public float getMaxHunger() {
        return vitals.maxHunger;
    }

    public double getDamageFlashRemainingSeconds() {
        double elapsedSeconds = (System.nanoTime() - state.lastDamageTimeNanos) / 1_000_000_000.0;
        return Math.max(0.0, DAMAGE_FLASH_DURATION_SECONDS - elapsedSeconds);
    }

    public boolean consumeSelectedFood() {
        Optional<ItemStack> selectedStack = getSelectedItemStack();
        if (selectedStack.isEmpty()) {
            return false;
        }

        Item item = selectedStack.get().getItem();
        if (!item.isFood() || vitals.hunger >= vitals.maxHunger) {
            return false;
        }

        vitals.hunger = Math.min(vitals.maxHunger, vitals.hunger + item.getFoodRestored());
        return !getGameModeRules().consumesFoodItems() || components.inventory().removeFromSelectedSlot(1);
    }

    public void fixedUpdate(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "Entity manager cannot be null.");

        if (!getGameModeRules().usesHunger()) {
            vitals.health = vitals.maxHealth;
            vitals.hunger = vitals.maxHunger;
            return;
        }
        applyHungerTick();
        resolveDeathIfNeeded(entityManager);
    }

    public void restoreVitals(float health, float hunger) {
        vitals.health = Math.clamp(health, 0.0f, vitals.maxHealth);
        vitals.hunger = Math.clamp(hunger, 0.0f, vitals.maxHunger);
    }

    public List<StatusEffect> getStatusEffects() {
        return List.copyOf(statusEffects);
    }

    public void damage(float amount) {
        if (amount <= 0.0f || vitals.health <= 0.0f || !getGameModeRules().canTakeDamage()) {
            return;
        }

        vitals.health = Math.max(0.0f, vitals.health - amount);
        state.lastDamageTimeNanos = System.nanoTime();
    }

    private void updateMovement(InputState inputState, VoxelWorld world, double deltaTimeSeconds) {
        updateWaterState(world);
        if (getGameModeRules().canFly()) {
            updateCreativeFlight(inputState, deltaTimeSeconds);
            updateWaterState(world);
            return;
        }

        Vector3f forward = getForward();
        Vector3f flatForward = new Vector3f(forward.x, 0.0f, forward.z).normalize();
        Vector3f movement = new Vector3f();

        if (inputState.isKeyDown(GLFW_KEY_W)) {
            movement.add(flatForward);
        }
        if (inputState.isKeyDown(GLFW_KEY_S)) {
            movement.sub(flatForward);
        }
        if (inputState.isKeyDown(GLFW_KEY_D)) {
            movement.add(components.camera().getRight());
        }
        if (inputState.isKeyDown(GLFW_KEY_A)) {
            movement.sub(components.camera().getRight());
        }

        state.movingThisFrame = movement.lengthSquared() > 0.0f;
        state.sprinting = state.movingThisFrame && isSprintInputDown(inputState);

        float waterSpeedMultiplier = state.inWater ? (state.headUnderwater ? 0.48f : 0.62f) : 1.0f;
        float movementSpeed = components.camera().getMoveSpeed() * (state.sprinting && !state.inWater ? SPRINT_SPEED_MULTIPLIER : 1.0f) * waterSpeedMultiplier;
        float movementStep = (float) (movementSpeed * deltaTimeSeconds);
        Vector3f horizontalStep = movement.lengthSquared() == 0.0f
                ? new Vector3f()
                : new Vector3f(movement).normalize().mul(movementStep);

        applyJump(inputState, state.inWater);
        applyGravity(inputState, deltaTimeSeconds);

        moveWithCollision(world, horizontalStep.x, 0.0f, 0.0f);
        moveWithCollision(world, 0.0f, 0.0f, horizontalStep.z);

        float verticalStep = state.verticalVelocity * (float) deltaTimeSeconds;
        boolean collidedVertically = moveWithCollision(world, 0.0f, verticalStep, 0.0f);
        if (collidedVertically) {
            float impactVelocity = state.verticalVelocity;
            if (state.verticalVelocity < 0.0f) {
                state.onGround = true;
                applyFallDamage(impactVelocity);
            }
            state.verticalVelocity = 0.0f;
        } else {
            state.onGround = false;
        }
        updateWaterState(world);
    }

    private void updateCreativeFlight(InputState inputState, double deltaTimeSeconds) {
        Vector3f forward = getForward();
        Vector3f flatForward = new Vector3f(forward.x, 0.0f, forward.z);
        if (flatForward.lengthSquared() > 0.0f) {
            flatForward.normalize();
        }
        Vector3f movement = new Vector3f();

        if (inputState.isKeyDown(GLFW_KEY_W)) {
            movement.add(flatForward);
        }
        if (inputState.isKeyDown(GLFW_KEY_S)) {
            movement.sub(flatForward);
        }
        if (inputState.isKeyDown(GLFW_KEY_D)) {
            movement.add(components.camera().getRight());
        }
        if (inputState.isKeyDown(GLFW_KEY_A)) {
            movement.sub(components.camera().getRight());
        }
        if (inputState.isKeyDown(GLFW_KEY_SPACE)) {
            movement.y += 1.0f;
        }
        if (inputState.isKeyDown(GLFW_KEY_LEFT_SHIFT) || inputState.isKeyDown(GLFW_KEY_RIGHT_SHIFT)) {
            movement.y -= 1.0f;
        }

        state.movingThisFrame = movement.lengthSquared() > 0.0f;
        state.sprinting = state.movingThisFrame && isSprintInputDown(inputState);
        float speed = components.camera().getMoveSpeed() * (state.sprinting ? SPRINT_SPEED_MULTIPLIER : 1.0f) * 1.55f;
        if (movement.lengthSquared() > 0.0f) {
            transform.setPosition(new Vector3f(transform.position()).add(movement.normalize().mul(speed * (float) deltaTimeSeconds)));
        }
        state.verticalVelocity = 0.0f;
        state.onGround = false;
    }

    private void updateWaterState(VoxelWorld world) {
        float radius = components.camera().getColliderRadius();
        Vector3f position = transform.position();
        WaterContact contact = world.sampleWaterContact(
                position.x - radius,
                position.y,
                position.z - radius,
                position.x + radius,
                position.y + components.camera().getPlayerHeight(),
                position.z + radius,
                position.y + components.camera().getPlayerHeight() * 0.92f
        );

        boolean wasInWater = state.inWater;
        state.inWater = contact.touchingWater();
        state.headUnderwater = contact.headUnderwater();
        state.waterSubmersion = contact.deepestSubmersion();
        state.waterDepthBelowFeet = contact.depthBelowFeet();
        if (!wasInWater && state.inWater) {
            onEnteredWater();
        } else if (wasInWater && !state.inWater) {
            onExitedWater();
        }
    }

    private void applyJump(InputState inputState, boolean inWater) {
        if (inWater) {
            if (inputState.isKeyDown(GLFW_KEY_SPACE)) {
                state.verticalVelocity = Math.max(state.verticalVelocity, components.camera().getJumpVelocity() * 0.34f);
            }
            return;
        }

        if (state.onGround && inputState.wasKeyPressed(GLFW_KEY_SPACE)) {
            state.verticalVelocity = components.camera().getJumpVelocity();
            state.onGround = false;
            state.jumpTriggeredSinceLastTick = true;
        }
    }

    private void applyGravity(InputState inputState, double deltaTimeSeconds) {
        float dt = (float) deltaTimeSeconds;
        if (state.inWater) {
            float gravity = components.camera().getGravity() * (state.headUnderwater ? 0.18f : 0.28f);
            state.verticalVelocity -= gravity * dt;
            float buoyancy = state.headUnderwater || state.waterSubmersion > 0.45f ? 4.2f : 1.6f;
            state.verticalVelocity += buoyancy * dt;
            if (!inputState.isKeyDown(GLFW_KEY_SPACE)) {
                state.verticalVelocity *= Math.pow(0.72f, dt * 8.0f);
            }
            state.verticalVelocity = Math.clamp(state.verticalVelocity, -3.0f, 4.5f);
            return;
        }

        state.verticalVelocity -= components.camera().getGravity() * dt;
        state.verticalVelocity = Math.max(state.verticalVelocity, -components.camera().getGravity() * 2.0f);
    }

    private boolean moveWithCollision(VoxelWorld world, float deltaX, float deltaY, float deltaZ) {
        if (deltaX == 0.0f && deltaY == 0.0f && deltaZ == 0.0f) {
            return false;
        }

        Vector3f candidate = new Vector3f(transform.position()).add(deltaX, deltaY, deltaZ);
        if (intersectsSolid(world, candidate)) {
            return true;
        }

        transform.setPosition(candidate);
        return false;
    }

    private void unstickFromSolid(VoxelWorld world) {
        if (!intersectsSolid(world, transform.position())) {
            return;
        }

        Vector3f position = transform.position();
        float startY = (float) Math.floor(position.y) + 0.02f;
        for (int offsetY = 1; offsetY <= 16; offsetY++) {
            Vector3f candidate = new Vector3f(position.x, startY + offsetY, position.z);
            if (!intersectsSolid(world, candidate)) {
                transform.setPosition(candidate);
                state.verticalVelocity = 0.0f;
                state.onGround = false;
                return;
            }
        }
    }

    private boolean intersectsSolid(VoxelWorld world, Vector3f candidatePosition) {
        float radius = components.camera().getColliderRadius();
        float minX = candidatePosition.x - radius;
        float maxX = candidatePosition.x + radius;
        float minY = candidatePosition.y;
        float maxY = candidatePosition.y + components.camera().getPlayerHeight();
        float minZ = candidatePosition.z - radius;
        float maxZ = candidatePosition.z + radius;

        int blockMinX = (int) Math.floor(minX);
        int blockMaxX = (int) Math.floor(maxX);
        int blockMinY = (int) Math.floor(minY);
        int blockMaxY = (int) Math.floor(maxY);
        int blockMinZ = (int) Math.floor(minZ);
        int blockMaxZ = (int) Math.floor(maxZ);

        for (int x = blockMinX; x <= blockMaxX; x++) {
            for (int y = blockMinY; y <= blockMaxY; y++) {
                for (int z = blockMinZ; z <= blockMaxZ; z++) {
                    if (world.isSolidBlockAtWorld(x, y, z)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private void updateMining(
            InputState inputState,
            VoxelWorld world,
            WorldInteractionSystem interactionSystem,
            boolean cursorCaptured,
            double deltaTimeSeconds
    ) {
        if (!cursorCaptured || !inputState.isMouseButtonDown(GLFW_MOUSE_BUTTON_LEFT)) {
            resetMining(interactionSystem, "Mining: idle");
            return;
        }

        Optional<BlockHitResult> currentTarget = interactionSystem.getCurrentTarget();
        if (currentTarget.isEmpty()) {
            resetMining(interactionSystem, "Mining: no target");
            return;
        }

        BlockHitResult hit = currentTarget.get();
        Block block = world.getBlockAtWorld(hit.blockX(), hit.blockY(), hit.blockZ());
        if (!block.isRenderable()) {
            resetMining(interactionSystem, "Mining: invalid target");
            return;
        }

        if (state.miningTarget.isEmpty() || !sameBlock(state.miningTarget.get(), hit)) {
            if (state.miningTarget.isPresent()) {
                interactionSystem.onMiningStopped();
            }
            state.miningTarget = Optional.of(hit);
            state.miningProgress = 0.0f;
            interactionSystem.onMiningStarted(block, hit);
        }

        Optional<Item> heldItem = getSelectedItemStack().map(ItemStack::getItem);
        float progressPerSecond = getGameModeRules().instantBlockBreaking()
                ? Float.POSITIVE_INFINITY
                : MiningCalculator.progressPerSecond(block, heldItem);
        state.miningProgress = getGameModeRules().instantBlockBreaking()
                ? 1.0f
                : Math.min(1.0f, state.miningProgress + progressPerSecond * (float) deltaTimeSeconds);
        state.miningDebugText = "Mining: %s %d%%".formatted(block.getName(), Math.round(state.miningProgress * 100.0f));
        interactionSystem.onMiningProgress(block, hit, state.miningProgress);

        if (state.miningProgress >= 1.0f && interactionSystem.breakTarget(this, world, hit)) {
            interactionSystem.onBlockBroken(block, hit);
            resetMining(interactionSystem, "Mining: idle");
        }
    }

    private void resetMining(WorldInteractionSystem interactionSystem, String debugText) {
        if (state.miningTarget.isPresent() && interactionSystem != null) {
            interactionSystem.onMiningStopped();
        }
        state.miningTarget = Optional.empty();
        state.miningProgress = 0.0f;
        state.miningDebugText = debugText;
    }

    private void applyFallDamage(float impactVelocity) {
        float downwardSpeed = -impactVelocity;
        if (downwardSpeed <= SAFE_FALL_SPEED || !getGameModeRules().canTakeDamage()) {
            return;
        }

        float damageAmount = (downwardSpeed - SAFE_FALL_SPEED) * FALL_DAMAGE_SCALE;
        damageAmount *= waterFallDamageMultiplier();
        damage(damageAmount);
    }

    private boolean tryAttackEntity(
            InputState inputState,
            VoxelWorld world,
            WorldInteractionSystem interactionSystem,
            boolean cursorCaptured
    ) {
        if (!cursorCaptured || !inputState.wasMouseButtonPressed(GLFW_MOUSE_BUTTON_LEFT) || state.attackCooldownSeconds > 0.0) {
            return false;
        }
        if (interactionSystem.attackTargetedEntity(this, world, ENTITY_ATTACK_REACH)) {
            state.attackCooldownSeconds = ATTACK_COOLDOWN_SECONDS;
            return true;
        }
        return false;
    }

    private float waterFallDamageMultiplier() {
        if (state.waterDepthBelowFeet >= 2 || state.headUnderwater) {
            return 0.0f;
        }
        if (state.waterDepthBelowFeet == 1 || state.inWater) {
            return 0.35f;
        }
        return 1.0f;
    }

    private void onEnteredWater() {
        state.waterTransitionDebugText = "entered";
        playWaterSplashSound();
    }

    private void onExitedWater() {
        state.waterTransitionDebugText = "exited";
        playWaterExitSound();
    }

    private void playWaterSplashSound() {
        // Hook for a future sound system.
    }

    private void playWaterExitSound() {
        // Hook for a future sound system.
    }

    private void applyHungerTick() {
        state.baseHungerTicks++;
        if (state.baseHungerTicks >= HUNGER_DRAIN_INTERVAL_TICKS) {
            drainHunger(HUNGER_BASE_DRAIN_AMOUNT);
            state.baseHungerTicks = 0;
        }

        if (state.sprinting) {
            state.sprintHungerTicks++;
            if (state.sprintHungerTicks >= HUNGER_SPRINT_DRAIN_INTERVAL_TICKS) {
                drainHunger(HUNGER_SPRINT_DRAIN_AMOUNT);
                state.sprintHungerTicks = 0;
            }
        } else {
            state.sprintHungerTicks = 0;
        }

        if (state.jumpTriggeredSinceLastTick) {
            drainHunger(HUNGER_JUMP_DRAIN_AMOUNT);
            state.jumpTriggeredSinceLastTick = false;
        }

        if (vitals.hunger >= REGEN_HUNGER_THRESHOLD && vitals.health < vitals.maxHealth) {
            state.regenerationTicks++;
            if (state.regenerationTicks >= REGEN_INTERVAL_TICKS) {
                vitals.health = Math.min(vitals.maxHealth, vitals.health + REGEN_HEALTH_AMOUNT);
                state.regenerationTicks = 0;
            }
        } else {
            state.regenerationTicks = 0;
        }

        if (vitals.hunger <= 0.0f && vitals.health > STARVATION_MIN_HEALTH) {
            state.starvationTicks++;
            if (state.starvationTicks >= STARVATION_DELAY_TICKS) {
                state.starvationDamageTicks++;
            }
            if (state.starvationDamageTicks >= STARVATION_INTERVAL_TICKS) {
                vitals.health = Math.max(STARVATION_MIN_HEALTH, vitals.health - STARVATION_DAMAGE_AMOUNT);
                state.starvationDamageTicks = 0;
            }
        } else {
            state.starvationTicks = 0;
            state.starvationDamageTicks = 0;
        }
    }

    private void drainHunger(float amount) {
        vitals.hunger = Math.max(0.0f, vitals.hunger - amount);
    }

    private void resolveDeathIfNeeded(EntityManager entityManager) {
        if (vitals.health > 0.0f) {
            return;
        }

        dropInventory(entityManager);
        components.inventory().clear();
        respawn();
    }

    private void resolveVoidRespawnIfNeeded() {
        if (transform.position().y >= VOID_RESPAWN_Y) {
            return;
        }

        respawnPreservingInventory();
    }

    private void dropInventory(EntityManager entityManager) {
        Vector3f basePosition = new Vector3f(transform.position()).add(0.0f, 0.5f, 0.0f);
        int dropIndex = 0;

        for (int i = 0; i < components.inventory().getSlotCount(); i++) {
            dropIndex = spawnInventoryStack(entityManager, components.inventory().getSlot(i).orElse(null), basePosition, dropIndex);
        }
        for (ArmorSlot armorSlot : ArmorSlot.values()) {
            dropIndex = spawnInventoryStack(entityManager, components.inventory().getArmor(armorSlot).orElse(null), basePosition, dropIndex);
        }
    }

    private int spawnInventoryStack(EntityManager entityManager, ItemStack stack, Vector3f basePosition, int dropIndex) {
        if (stack == null) {
            return dropIndex;
        }

        float offsetX = ((dropIndex % 3) - 1) * 0.22f;
        float offsetZ = (((dropIndex / 3) % 3) - 1) * 0.22f;
        float velocityX = ((dropIndex % 5) - 2) * 0.18f;
        float velocityZ = (((dropIndex / 5) % 5) - 2) * 0.18f;
        entityManager.spawn(new ItemEntity(
                new Vector3f(basePosition).add(offsetX, 0.0f, offsetZ),
                new Vector3f(velocityX, 2.2f, velocityZ),
                stack,
                1.0
        ));
        return dropIndex + 1;
    }

    private void respawn() {
        components.inventory().setSelectedHotbarSlot(0);
        respawnPreservingInventory();
    }

    private void respawnPreservingInventory() {
        statusEffects.clear();
        resetMining(null, "Mining: idle");
        vitals.health = vitals.maxHealth;
        vitals.hunger = vitals.maxHunger;
        state.verticalVelocity = 0.0f;
        state.onGround = false;
        setPosition(spawnPoint);
    }

    private boolean sameBlock(BlockHitResult left, BlockHitResult right) {
        return left.blockX() == right.blockX()
                && left.blockY() == right.blockY()
                && left.blockZ() == right.blockZ();
    }

    private void syncCameraPosition() {
        components.camera().setPosition(transform.position());
    }

    private boolean isSprintInputDown(InputState inputState) {
        return inputState.isKeyDown(GLFW_KEY_LEFT_CONTROL) || inputState.isKeyDown(GLFW_KEY_RIGHT_CONTROL);
    }

    private void updateVelocity(Vector3f previousPosition, double deltaTimeSeconds) {
        if (deltaTimeSeconds > 0.0) {
            transform.setVelocity(new Vector3f(transform.position()).sub(previousPosition).mul((float) (1.0 / deltaTimeSeconds)));
        } else {
            transform.setVelocity(new Vector3f());
        }
    }

    private static final class PlayerTransform {
        private Vector3f position;
        private Vector3f velocity;

        private PlayerTransform(Vector3f position, Vector3f velocity) {
            this.position = new Vector3f(position);
            this.velocity = new Vector3f(velocity);
        }

        private Vector3f position() {
            return new Vector3f(position);
        }

        private void setPosition(Vector3f position) {
            this.position = new Vector3f(position);
        }

        private Vector3f velocity() {
            return new Vector3f(velocity);
        }

        private void setVelocity(Vector3f velocity) {
            this.velocity = new Vector3f(velocity);
        }
    }

    private record PlayerComponents(
            FirstPersonCamera camera,
            Inventory inventory,
            PlayerInputHandler inputHandler
    ) {
    }

    private static final class PlayerState {
        private float verticalVelocity;
        private boolean onGround;
        private boolean movingThisFrame;
        private boolean sprinting;
        private boolean jumpTriggeredSinceLastTick;
        private int baseHungerTicks;
        private int sprintHungerTicks;
        private int regenerationTicks;
        private int starvationTicks;
        private int starvationDamageTicks;
        private long lastDamageTimeNanos = Long.MIN_VALUE;
        private Optional<BlockHitResult> miningTarget = Optional.empty();
        private float miningProgress;
        private String miningDebugText = "Mining: idle";
        private boolean inWater;
        private boolean headUnderwater;
        private float waterSubmersion;
        private int waterDepthBelowFeet;
        private String waterTransitionDebugText = "dry";
        private GameMode gameMode = GameMode.SURVIVAL;
        private double attackCooldownSeconds;
    }

    private static final class PlayerVitals {
        private final float maxHealth = 20.0f;
        private final float maxHunger = HUNGER_MAX;
        private float health = maxHealth;
        private float hunger = maxHunger;
    }

    public record StatusEffect(String id, float remainingSeconds, int amplifier) {
        public StatusEffect {
            Objects.requireNonNull(id, "Status effect id cannot be null.");
            if (remainingSeconds < 0.0f) {
                throw new IllegalArgumentException("Status effect duration cannot be negative.");
            }
            if (amplifier < 0) {
                throw new IllegalArgumentException("Status effect amplifier cannot be negative.");
            }
        }
    }

    private static final class PlayerInputHandler {
        private void handleHotbarSelection(InputState inputState, Inventory inventory) {
            int[] keys = {
                    GLFW_KEY_1, GLFW_KEY_2, GLFW_KEY_3, GLFW_KEY_4, GLFW_KEY_5,
                    GLFW_KEY_6, GLFW_KEY_7, GLFW_KEY_8, GLFW_KEY_9
            };

            int hotbarSize = Math.min(inventory.getHotbarSize(), keys.length);
            for (int i = 0; i < hotbarSize; i++) {
                if (inputState.wasKeyPressed(keys[i])) {
                    inventory.setSelectedHotbarSlot(i);
                    return;
                }
            }
        }
    }
}
