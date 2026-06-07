package com.example.voxelgame.game.entity;

import com.example.voxelgame.game.Player;
import com.example.voxelgame.game.inventory.Inventory;
import com.example.voxelgame.game.inventory.ItemStack;
import org.joml.Vector3f;
import com.example.voxelgame.world.VoxelWorld;
import com.example.voxelgame.world.WaterContact;
import java.util.Objects;

public final class ItemEntity extends Entity {
    private static final System.Logger LOGGER = System.getLogger(ItemEntity.class.getName());

    private static final Vector3f HALF_EXTENTS = new Vector3f(0.18f, 0.18f, 0.18f);
    private static final float GRAVITY = 18.0f;
    private static final float HORIZONTAL_DAMPING = 0.84f;
    private static final float BOUNCE_DAMPING = 0.25f;
    private static final float RESTITUTION_THRESHOLD = 1.0f;
    private static final float PICKUP_RANGE = 1.35f;
    private static final float CLOSE_PICKUP_RANGE = 0.45f;
    private static final float ATTRACTION_RANGE = 4.0f;
    private static final float ATTRACTION_ACCELERATION = 22.0f;
    private static final float CLOSE_ATTRACTION_ACCELERATION = 42.0f;
    private static final float MAX_ATTRACTION_SPEED = 9.0f;
    private static final float FLOAT_AMPLITUDE = 0.08f;
    private static final float FLOAT_FREQUENCY = 3.2f;
    private static final float VISUAL_INTERPOLATION_SPEED = 14.0f;

    private final ItemStack stack;
    private double pickupDelaySeconds;
    private double ageSeconds;
    private Vector3f visualPosition;
    private float spinRadians;

    public ItemEntity(Vector3f position, Vector3f initialVelocity, ItemStack stack, double pickupDelaySeconds) {
        super(position, initialVelocity, HALF_EXTENTS);
        this.stack = Objects.requireNonNull(stack, "Item stack cannot be null.").copy();
        this.pickupDelaySeconds = Math.max(0.0, pickupDelaySeconds);
        this.visualPosition = new Vector3f(position);
    }

    public ItemStack getStack() {
        return stack.copy();
    }

    @Override
    public void update(double deltaTimeSeconds, EntityUpdateContext context) {
        pickupDelaySeconds = Math.max(0.0, pickupDelaySeconds - deltaTimeSeconds);
        ageSeconds += deltaTimeSeconds;
        spinRadians += (float) (deltaTimeSeconds * 2.25);

        WaterContact waterContact = sampleWaterContact(context.world());
        Vector3f velocity = getVelocity();
        if (waterContact.touchingWater()) {
            float dt = (float) deltaTimeSeconds;
            velocity.x *= Math.pow(0.42f, dt);
            velocity.z *= Math.pow(0.42f, dt);
            velocity.y += (5.4f - GRAVITY * 0.18f) * dt;
            velocity.y = Math.clamp(velocity.y, -1.8f, 2.4f);
        } else {
            velocity.add(0.0f, -GRAVITY * (float) deltaTimeSeconds, 0.0f);
            velocity.y = Math.max(velocity.y, -20.0f);
        }
        setVelocity(velocity);

        applyPlayerAttraction(context.player(), deltaTimeSeconds);
        move(context.world(), deltaTimeSeconds);
        interpolateVisualPosition(deltaTimeSeconds);
        tryPickup(context.player());
    }

    private WaterContact sampleWaterContact(VoxelWorld world) {
        BoundingBox bounds = getBoundingBox();
        Vector3f min = bounds.min();
        Vector3f max = bounds.max();
        return world.sampleWaterContact(min.x, min.y, min.z, max.x, max.y, max.z, max.y);
    }

    @Override
    public void render(EntityRenderContext context) {
        float[] color = colorForItem();
        float bobOffset = (float) Math.sin(ageSeconds * FLOAT_FREQUENCY) * FLOAT_AMPLITUDE;
        context.drawCube(new Vector3f(visualPosition).add(0.0f, bobOffset, 0.0f), getHalfExtents(), spinRadians, color[0], color[1], color[2], 1.0f);
    }

    private void applyPlayerAttraction(Player player, double deltaTimeSeconds) {
        if (pickupDelaySeconds > 0.0) {
            return;
        }

        Vector3f toPlayer = playerPickupTarget(player).sub(getPosition());
        float distance = toPlayer.length();
        if (distance <= 0.001f || distance > ATTRACTION_RANGE) {
            return;
        }

        float closeness = 1.0f - Math.clamp(distance / ATTRACTION_RANGE, 0.0f, 1.0f);
        float acceleration = distance <= CLOSE_PICKUP_RANGE ? CLOSE_ATTRACTION_ACCELERATION : ATTRACTION_ACCELERATION;
        Vector3f attraction = toPlayer.normalize().mul(acceleration * closeness * closeness * (float) deltaTimeSeconds);
        setVelocity(limitSpeed(new Vector3f(getVelocity()).add(attraction), MAX_ATTRACTION_SPEED));
    }

    private Vector3f limitSpeed(Vector3f velocity, float maxSpeed) {
        float speedSquared = velocity.lengthSquared();
        if (speedSquared <= maxSpeed * maxSpeed) {
            return velocity;
        }
        return new Vector3f(velocity).normalize().mul(maxSpeed);
    }

    private void interpolateVisualPosition(double deltaTimeSeconds) {
        float alpha = Math.clamp((float) deltaTimeSeconds * VISUAL_INTERPOLATION_SPEED, 0.0f, 1.0f);
        Vector3f delta = getPosition().sub(visualPosition);
        visualPosition = new Vector3f(visualPosition).add(delta.mul(alpha));
    }

    private void move(VoxelWorld world, double deltaTimeSeconds) {
        Vector3f velocity = getVelocity();
        float deltaX = velocity.x * (float) deltaTimeSeconds;
        float deltaY = velocity.y * (float) deltaTimeSeconds;
        float deltaZ = velocity.z * (float) deltaTimeSeconds;

        if (deltaX != 0.0f) {
            attemptAxisMove(world, new Vector3f(deltaX, 0.0f, 0.0f), false);
        }
        if (deltaZ != 0.0f) {
            attemptAxisMove(world, new Vector3f(0.0f, 0.0f, deltaZ), false);
        }
        if (deltaY != 0.0f) {
            attemptAxisMove(world, new Vector3f(0.0f, deltaY, 0.0f), true);
        }
    }

    private void attemptAxisMove(VoxelWorld world, Vector3f delta, boolean verticalMove) {
        Vector3f nextPosition = new Vector3f(getPosition()).add(delta);
        if (!intersectsSolid(world, BoundingBox.fromCenter(nextPosition, getHalfExtents()))) {
            setPosition(nextPosition);
            return;
        }

        if (verticalMove) {
            Vector3f velocity = getVelocity();
            float bouncedVelocity = -velocity.y * BOUNCE_DAMPING;
            if (Math.abs(bouncedVelocity) < RESTITUTION_THRESHOLD) {
                bouncedVelocity = 0.0f;
            }
            setVelocity(new Vector3f(velocity.x * HORIZONTAL_DAMPING, bouncedVelocity, velocity.z * HORIZONTAL_DAMPING));
            return;
        }

        setVelocity(new Vector3f(0.0f, getVelocity().y, 0.0f));
    }

    private boolean intersectsSolid(VoxelWorld world, BoundingBox bounds) {
        Vector3f min = bounds.min();
        Vector3f max = bounds.max();
        int minX = (int) Math.floor(min.x);
        int maxX = (int) Math.floor(max.x);
        int minY = (int) Math.floor(min.y);
        int maxY = (int) Math.floor(max.y);
        int minZ = (int) Math.floor(min.z);
        int maxZ = (int) Math.floor(max.z);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (world.isSolidBlockAtWorld(x, y, z)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private void tryPickup(Player player) {
        if (pickupDelaySeconds > 0.0) {
            return;
        }

        float distance = getPosition().sub(playerPickupTarget(player)).length();
        if (distance > PICKUP_RANGE) {
            return;
        }

        Inventory inventory = player.getInventory();
        LOGGER.log(
                System.Logger.Level.INFO,
                "Item pickup attempt: inventory=#{0}, item={1}, count={2}, entityPos=({3}, {4}, {5})",
                Integer.toHexString(System.identityHashCode(inventory)),
                stack.getItem().getId(),
                stack.getCount(),
                getPosition().x,
                getPosition().y,
                getPosition().z
        );

        if (inventory.addItem(stack)) {
            LOGGER.log(
                    System.Logger.Level.INFO,
                    "Item pickup success: inventory=#{0}, item={1}, count={2}",
                    Integer.toHexString(System.identityHashCode(inventory)),
                    stack.getItem().getId(),
                    stack.getCount()
            );
            markRemoved();
        } else {
            LOGGER.log(
                    System.Logger.Level.INFO,
                    "Item pickup blocked: inventory=#{0}, item={1}, count={2}",
                    Integer.toHexString(System.identityHashCode(inventory)),
                    stack.getItem().getId(),
                    stack.getCount()
            );
        }
    }

    private Vector3f playerPickupTarget(Player player) {
        return new Vector3f(player.getPosition()).add(0.0f, player.getPlayerHeight() * 0.45f, 0.0f);
    }

    private float[] colorForItem() {
        return switch (stack.getItem().getId()) {
            case "dirt_block" -> new float[]{0.56f, 0.36f, 0.20f};
            case "grass_block" -> new float[]{0.36f, 0.72f, 0.28f};
            case "stone_block" -> new float[]{0.66f, 0.68f, 0.72f};
            case "oak_log_block" -> new float[]{0.62f, 0.44f, 0.24f};
            case "oak_leaves_block" -> new float[]{0.30f, 0.58f, 0.22f};
            default -> new float[]{0.92f, 0.86f, 0.52f};
        };
    }
}
