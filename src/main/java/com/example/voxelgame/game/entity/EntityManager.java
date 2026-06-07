package com.example.voxelgame.game.entity;

import com.example.voxelgame.game.Player;
import com.example.voxelgame.game.entity.animal.AnimalEntity;
import com.example.voxelgame.game.entity.villager.VillagerEntity;
import com.example.voxelgame.world.VoxelWorld;
import com.example.voxelgame.world.WorldTime;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class EntityManager {
    private static final System.Logger LOGGER = System.getLogger(EntityManager.class.getName());
    private static final boolean DEBUG_HITBOXES = Boolean.getBoolean("voxelgame.debugEntityHitboxes");
    private static final boolean DEBUG_TARGETING = Boolean.getBoolean("voxelgame.debugEntityTargeting");

    private final List<Entity> entities = new ArrayList<>();

    public void spawn(Entity entity) {
        entities.add(Objects.requireNonNull(entity, "Entity cannot be null."));
    }

    public void replacePersistentEntities(Collection<Entity> persistentEntities) {
        entities.removeIf(entity -> entity.saveData().isPresent());
        entities.addAll(Objects.requireNonNull(persistentEntities, "Persistent entities cannot be null."));
    }

    public void update(double deltaTimeSeconds, VoxelWorld world, Player player) {
        update(deltaTimeSeconds, world, player, null);
    }

    public void update(double deltaTimeSeconds, VoxelWorld world, Player player, WorldTime worldTime) {
        Objects.requireNonNull(world, "Voxel world cannot be null.");
        Objects.requireNonNull(player, "Player cannot be null.");

        EntityUpdateContext context = new EntityUpdateContext(world, player, worldTime);
        Iterator<Entity> iterator = entities.iterator();
        while (iterator.hasNext()) {
            Entity entity = iterator.next();
            entity.update(deltaTimeSeconds, context);
            if (entity.isRemoved()) {
                iterator.remove();
            }
        }
    }

    public void render(EntityRenderContext context) {
        Objects.requireNonNull(context, "Entity render context cannot be null.");
        for (Entity entity : entities) {
            entity.render(context);
            if (DEBUG_HITBOXES) {
                renderHitbox(context, entity);
            }
        }
    }

    private void renderHitbox(EntityRenderContext context, Entity entity) {
        BoundingBox bounds = entity.getBoundingBox();
        Vector3f min = bounds.min();
        Vector3f max = bounds.max();
        Vector3f center = new Vector3f(min).add(max).mul(0.5f);
        Vector3f halfExtents = new Vector3f(max).sub(min).mul(0.5f);
        context.drawCube(center, halfExtents, 0.0f, 1.0f, 0.18f, 0.12f, 0.28f);
    }

    public int getEntityCount() {
        return entities.size();
    }

    public int getAnimalCount() {
        int count = 0;
        for (Entity entity : entities) {
            if (entity instanceof AnimalEntity) {
                count++;
            }
        }
        return count;
    }

    public int getAnimalCountNear(float x, float z, float radius) {
        float radiusSquared = radius * radius;
        int count = 0;
        for (Entity entity : entities) {
            if (!(entity instanceof AnimalEntity)) {
                continue;
            }
            org.joml.Vector3f position = entity.getPosition();
            float dx = position.x - x;
            float dz = position.z - z;
            if (dx * dx + dz * dz <= radiusSquared) {
                count++;
            }
        }
        return count;
    }

    public boolean hasAnimalNear(float x, float z, float radius) {
        return getAnimalCountNear(x, z, radius) > 0;
    }

    public int getVillagerCountNear(float x, float z, float radius) {
        float radiusSquared = radius * radius;
        int count = 0;
        for (Entity entity : entities) {
            if (!(entity instanceof VillagerEntity)) {
                continue;
            }
            Vector3f position = entity.getPosition();
            float dx = position.x - x;
            float dz = position.z - z;
            if (dx * dx + dz * dz <= radiusSquared) {
                count++;
            }
        }
        return count;
    }

    public Optional<VillagerEntity> findVillagerInteraction(Player player, float maxDistance, float maxAngleDot) {
        Vector3f eye = player.getEyePosition();
        Vector3f forward = player.getForward().normalize();
        VillagerEntity best = null;
        float bestDistance = maxDistance;
        for (Entity entity : entities) {
            if (!(entity instanceof VillagerEntity villager) || villager.isRemoved()) {
                continue;
            }
            Vector3f target = villager.getPosition().add(0.0f, 0.9f, 0.0f);
            Vector3f toTarget = target.sub(eye);
            float distance = toTarget.length();
            if (distance > bestDistance || distance <= 0.001f) {
                continue;
            }
            float dot = toTarget.normalize().dot(forward);
            if (dot >= maxAngleDot) {
                best = villager;
                bestDistance = distance;
            }
        }
        return Optional.ofNullable(best);
    }

    public Optional<EntityRaycastHit> raycastDamageable(Vector3f origin, Vector3f direction, float maxDistance) {
        Objects.requireNonNull(origin, "Ray origin cannot be null.");
        Objects.requireNonNull(direction, "Ray direction cannot be null.");

        EntityRaycastHit bestHit = null;
        float maxDistanceSquared = maxDistance * maxDistance;
        for (Entity entity : entities) {
            if (entity.isRemoved() || !(entity instanceof DamageableEntity)) {
                continue;
            }
            Vector3f entityPosition = entity.getPosition();
            if (entityPosition.distanceSquared(origin) > maxDistanceSquared + 4.0f) {
                continue;
            }
            Optional<Float> hitDistance = entity.getBoundingBox().rayIntersection(origin, direction, maxDistance);
            if (hitDistance.isEmpty()) {
                if (DEBUG_TARGETING) {
                    LOGGER.log(System.Logger.Level.DEBUG, "Entity ray miss: {0}", entity.getClass().getSimpleName());
                }
                continue;
            }
            if (bestHit == null || hitDistance.get() < bestHit.distance()) {
                bestHit = new EntityRaycastHit(entity, hitDistance.get());
            }
        }

        if (DEBUG_TARGETING && bestHit != null) {
            LOGGER.log(
                    System.Logger.Level.INFO,
                    "Entity ray hit: type={0}, distance={1}",
                    bestHit.entity().getClass().getSimpleName(),
                    bestHit.distance()
            );
        }
        return Optional.ofNullable(bestHit);
    }

    public List<EntityPersistenceData> savePersistentEntities() {
        List<EntityPersistenceData> output = new ArrayList<>();
        for (Entity entity : entities) {
            Optional<EntityPersistenceData> data = entity.saveData();
            data.ifPresent(output::add);
        }
        return List.copyOf(output);
    }
}
