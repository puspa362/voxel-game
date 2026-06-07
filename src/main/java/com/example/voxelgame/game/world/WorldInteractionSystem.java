package com.example.voxelgame.game.world;

import com.example.voxelgame.core.InputState;
import com.example.voxelgame.game.Player;
import com.example.voxelgame.game.entity.DamageableEntity;
import com.example.voxelgame.game.entity.EntityDamageSource;
import com.example.voxelgame.game.entity.EntityManager;
import com.example.voxelgame.game.entity.EntityRaycastHit;
import com.example.voxelgame.world.Block;
import com.example.voxelgame.world.BlockHitResult;
import com.example.voxelgame.world.VoxelWorld;
import java.util.Objects;
import java.util.Optional;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

public final class WorldInteractionSystem {
    private static final System.Logger LOGGER = System.getLogger(WorldInteractionSystem.class.getName());
    private static final boolean DEBUG_ENTITY_TARGETING = Boolean.getBoolean("voxelgame.debugEntityTargeting");
    private static final float ENTITY_ATTACK_DAMAGE = 4.0f;

    private final EntityManager entityManager;
    private final SurvivalWorldRules survivalRules = new SurvivalWorldRules();
    private Optional<BlockHitResult> currentTarget = Optional.empty();
    private MiningFeedbackListener miningFeedbackListener = MiningFeedbackListener.NO_OP;

    public WorldInteractionSystem(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "Entity manager cannot be null.");
    }

    public void update(InputState inputState, Player player, VoxelWorld world, boolean cursorCaptured) {
        Objects.requireNonNull(inputState, "Input state cannot be null.");
        Objects.requireNonNull(player, "Player cannot be null.");
        Objects.requireNonNull(world, "Voxel world cannot be null.");

        currentTarget = cursorCaptured
                ? world.raycast(player.getEyePosition(), player.getForward(), 8.0f)
                : Optional.empty();

        if (!cursorCaptured || currentTarget.isEmpty()) {
            return;
        }

        BlockHitResult hit = currentTarget.get();
        if (inputState.wasMouseButtonPressed(GLFW_MOUSE_BUTTON_RIGHT)) {
            if (player.consumeSelectedFood()) {
                return;
            }
            if (DecorativeBlockInteractions.tryInteract(world, hit)) {
                return;
            }
            LOGGER.log(
                    System.Logger.Level.INFO,
                    "Placement input: inventory=#{0}, selectedSlot={1}, selectedItem={2}",
                    Integer.toHexString(System.identityHashCode(player.getInventory())),
                    player.getInventory().getSelectedHotbarSlot(),
                    player.getSelectedItemStack()
                            .map(stack -> stack.getItem().getId() + "x" + stack.getCount())
                            .orElse("empty")
            );
            survivalRules.tryPlaceSelectedBlock(player, world, hit);
        }
    }

    public Optional<BlockHitResult> getCurrentTarget() {
        return currentTarget;
    }

    public Optional<Block> selectedPlacementBlock(Player player) {
        return survivalRules.selectedPlacementBlock(player);
    }

    public void setMiningFeedbackListener(MiningFeedbackListener miningFeedbackListener) {
        this.miningFeedbackListener = Objects.requireNonNull(miningFeedbackListener, "Mining feedback listener cannot be null.");
    }

    public void onMiningStarted(Block block, BlockHitResult hit) {
        miningFeedbackListener.onMiningStarted(block, hit);
    }

    public void onMiningProgress(Block block, BlockHitResult hit, float progress) {
        miningFeedbackListener.onMiningProgress(block, hit, progress);
    }

    public void onMiningStopped() {
        miningFeedbackListener.onMiningStopped();
    }

    public void onBlockBroken(Block block, BlockHitResult hit) {
        miningFeedbackListener.onBlockBroken(block, hit);
    }

    public boolean breakTarget(Player player, VoxelWorld world, BlockHitResult hit) {
        Objects.requireNonNull(player, "Player cannot be null.");
        Objects.requireNonNull(world, "Voxel world cannot be null.");
        Objects.requireNonNull(hit, "Block hit result cannot be null.");
        return survivalRules.tryBreakBlock(player, world, hit, entityManager);
    }

    public boolean attackTargetedEntity(Player player, VoxelWorld world, float reach) {
        Objects.requireNonNull(player, "Player cannot be null.");
        Objects.requireNonNull(world, "Voxel world cannot be null.");

        Vector3f origin = player.getEyePosition();
        Vector3f direction = player.getForward();
        Optional<EntityRaycastHit> entityHit = entityManager.raycastDamageable(origin, direction, reach);
        if (entityHit.isEmpty()) {
            if (DEBUG_ENTITY_TARGETING) {
                LOGGER.log(System.Logger.Level.INFO, "Attack ray: no entity target.");
            }
            return false;
        }

        float blockingDistance = currentTarget
                .map(hit -> distanceToBlock(origin, hit))
                .orElse(Float.POSITIVE_INFINITY);
        if (entityHit.get().distance() > blockingDistance + 0.05f) {
            if (DEBUG_ENTITY_TARGETING) {
                LOGGER.log(
                        System.Logger.Level.INFO,
                        "Attack ray blocked by block: entityDistance={0}, blockDistance={1}",
                        entityHit.get().distance(),
                        blockingDistance
                );
            }
            return false;
        }

        if (entityHit.get().entity() instanceof DamageableEntity damageable) {
            boolean damaged = damageable.damage(ENTITY_ATTACK_DAMAGE, EntityDamageSource.PLAYER_ATTACK, entityManager);
            if (DEBUG_ENTITY_TARGETING || damaged) {
                LOGGER.log(
                        System.Logger.Level.INFO,
                        "Attack entity: type={0}, distance={1}, damaged={2}, health={3}/{4}",
                        entityHit.get().entity().getClass().getSimpleName(),
                        entityHit.get().distance(),
                        damaged,
                        damageable.getCurrentHealth(),
                        damageable.getMaxHealth()
                );
            }
            return damaged;
        }
        return false;
    }

    private float distanceToBlock(Vector3f origin, BlockHitResult hit) {
        return new Vector3f(hit.blockX() + 0.5f, hit.blockY() + 0.5f, hit.blockZ() + 0.5f).sub(origin).length() - 0.9f;
    }

    public interface MiningFeedbackListener {
        MiningFeedbackListener NO_OP = new MiningFeedbackListener() {
        };

        default void onMiningStarted(Block block, BlockHitResult hit) {
        }

        default void onMiningProgress(Block block, BlockHitResult hit, float progress) {
        }

        default void onMiningStopped() {
        }

        default void onBlockBroken(Block block, BlockHitResult hit) {
        }
    }
}
