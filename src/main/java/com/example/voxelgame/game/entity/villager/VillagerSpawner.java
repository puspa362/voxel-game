package com.example.voxelgame.game.entity.villager;

import java.util.List;
import java.util.Objects;

import org.joml.Vector3f;
import org.joml.Vector3i;

import com.example.voxelgame.game.Player;
import com.example.voxelgame.game.entity.EntityManager;
import com.example.voxelgame.world.VoxelWorld;
import com.example.voxelgame.world.gen.village.VillageFeature;

public final class VillagerSpawner {
    private static final double SPAWN_INTERVAL_SECONDS = 5.0;
    private static final int MAX_VILLAGERS_PER_VILLAGE = 5;
    private static final int VILLAGE_COUNT_RADIUS = 48;

    private double timerSeconds;

    public void update(double deltaTimeSeconds, VoxelWorld world, Player player, EntityManager entityManager) {
        Objects.requireNonNull(world, "Voxel world cannot be null.");
        Objects.requireNonNull(player, "Player cannot be null.");
        Objects.requireNonNull(entityManager, "Entity manager cannot be null.");

        timerSeconds -= deltaTimeSeconds;
        if (timerSeconds > 0.0) {
            return;
        }
        timerSeconds = SPAWN_INTERVAL_SECONDS;

        for (VillageFeature village : world.getLoadedVillageFeatures()) {
            if (entityManager.getVillagerCountNear(village.centerX(), village.centerZ(), VILLAGE_COUNT_RADIUS) >= MAX_VILLAGERS_PER_VILLAGE) {
                continue;
            }
            spawnMissingVillagers(village, world, entityManager);
        }
    }

    private void spawnMissingVillagers(VillageFeature village, VoxelWorld world, EntityManager entityManager) {
        List<Vector3i> beds = village.beds();
        List<Vector3i> workstations = village.workstations();
        int existing = entityManager.getVillagerCountNear(village.centerX(), village.centerZ(), VILLAGE_COUNT_RADIUS);
        for (int i = existing; i < Math.min(MAX_VILLAGERS_PER_VILLAGE, beds.size()); i++) {
            Vector3i home = beds.get(i);
            if (!world.isBlockLoadedAtWorld(home.x, home.y, home.z)) {
                continue;
            }
            Vector3i workstation = workstations.isEmpty() ? home : workstations.get(i % workstations.size());
            VillagerProfession profession = professionForIndex(i);
            long id = village.id() ^ (long) i * 0x9E3779B97F4A7C15L;
            entityManager.spawn(new VillagerEntity(
                    id,
                    profession,
                    new Vector3f(home.x + 2.5f, home.y + 0.05f, home.z + 2.5f),
                    home,
                    workstation,
                    new Vector3i(village.centerX(), village.centerY() + 1, village.centerZ())
            ));
        }
    }

    private VillagerProfession professionForIndex(int index) {
        return switch (Math.floorMod(index, 4)) {
            case 1 -> VillagerProfession.FARMER;
            case 2 -> VillagerProfession.BLACKSMITH;
            case 3 -> VillagerProfession.LIBRARIAN;
            default -> VillagerProfession.UNEMPLOYED;
        };
    }
}
