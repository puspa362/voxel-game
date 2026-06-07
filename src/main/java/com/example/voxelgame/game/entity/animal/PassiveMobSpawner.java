package com.example.voxelgame.game.entity.animal;

import java.util.Objects;
import java.util.Random;

import org.joml.Vector3f;

import com.example.voxelgame.game.Player;
import com.example.voxelgame.game.entity.EntityManager;
import com.example.voxelgame.world.BlockRegistry;
import com.example.voxelgame.world.Chunk;
import com.example.voxelgame.world.VoxelWorld;
import com.example.voxelgame.world.gen.BiomeType;

public final class PassiveMobSpawner {
    private static final int MAX_ANIMALS = 96;
    private static final int MAX_LOCAL_ANIMALS = 28;
    private static final int SPAWN_ATTEMPTS = 6;
    private static final int MIN_DISTANCE_FROM_PLAYER = 12;
    private static final int MAX_DISTANCE_FROM_PLAYER = 42;
    private static final int LOCAL_POPULATION_RADIUS = 64;
    private static final int CROWDING_RADIUS = 8;
    private static final double SPAWN_INTERVAL_SECONDS = 4.0;

    private final Random random;
    private double timerSeconds;

    public PassiveMobSpawner(long seed) {
        this.random = new Random(seed ^ 0x6C8E9CF570932BD5L);
    }

    public void update(double deltaTimeSeconds, VoxelWorld world, Player player, EntityManager entityManager) {
        Objects.requireNonNull(world, "Voxel world cannot be null.");
        Objects.requireNonNull(player, "Player cannot be null.");
        Objects.requireNonNull(entityManager, "Entity manager cannot be null.");

        timerSeconds -= deltaTimeSeconds;
        Vector3f playerPosition = player.getPosition();
        if (timerSeconds > 0.0
                || entityManager.getAnimalCount() >= MAX_ANIMALS
                || entityManager.getAnimalCountNear(playerPosition.x, playerPosition.z, LOCAL_POPULATION_RADIUS) >= MAX_LOCAL_ANIMALS
                || world.getLoadedChunkCount() == 0) {
            return;
        }
        timerSeconds = SPAWN_INTERVAL_SECONDS;

        for (int attempt = 0; attempt < SPAWN_ATTEMPTS
                && entityManager.getAnimalCount() < MAX_ANIMALS
                && entityManager.getAnimalCountNear(playerPosition.x, playerPosition.z, LOCAL_POPULATION_RADIUS) < MAX_LOCAL_ANIMALS; attempt++) {
            trySpawn(world, player, entityManager);
        }
    }

    private void trySpawn(VoxelWorld world, Player player, EntityManager entityManager) {
        Vector3f playerPosition = player.getPosition();
        double angle = random.nextDouble() * Math.PI * 2.0;
        int distance = MIN_DISTANCE_FROM_PLAYER + random.nextInt(MAX_DISTANCE_FROM_PLAYER - MIN_DISTANCE_FROM_PLAYER + 1);
        int worldX = (int) Math.floor(playerPosition.x + Math.cos(angle) * distance);
        int worldZ = (int) Math.floor(playerPosition.z + Math.sin(angle) * distance);
        if (!world.isBlockLoadedAtWorld(worldX, 1, worldZ)) {
            return;
        }
        if (entityManager.hasAnimalNear(worldX + 0.5f, worldZ + 0.5f, CROWDING_RADIUS)) {
            return;
        }

        int groundY = findGroundY(world, worldX, worldZ);
        if (groundY < 0 || groundY >= Chunk.HEIGHT - 3) {
            return;
        }
        if (world.getBlockAtWorld(worldX, groundY, worldZ) != BlockRegistry.GRASS
                && world.getBlockAtWorld(worldX, groundY, worldZ) != BlockRegistry.SNOW) {
            return;
        }
        if (world.isWaterAtWorld(worldX, groundY + 1, worldZ)) {
            return;
        }

        BiomeType biome = world.getBiomeAt(worldX, worldZ).primaryBiome();
        if (biome == BiomeType.DESERT) {
            return;
        }
        AnimalSpecies species = chooseSpecies(biome);
        entityManager.spawn(new AnimalEntity(species, new Vector3f(worldX + 0.5f, groundY + 1.05f, worldZ + 0.5f)));
    }

    private int findGroundY(VoxelWorld world, int worldX, int worldZ) {
        for (int y = Chunk.HEIGHT - 2; y >= 1; y--) {
            if (world.isSolidBlockAtWorld(worldX, y, worldZ) && !world.isSolidBlockAtWorld(worldX, y + 1, worldZ)) {
                return y;
            }
        }
        return -1;
    }

    private AnimalSpecies chooseSpecies(BiomeType biome) {
        int roll = random.nextInt(100);
        return switch (biome) {
            case SNOWY, MOUNTAINS -> roll < 55 ? AnimalSpecies.SHEEP : AnimalSpecies.COW;
            case FOREST -> roll < 35 ? AnimalSpecies.COW : (roll < 70 ? AnimalSpecies.PIG : AnimalSpecies.CHICKEN);
            case PLAINS -> roll < 34 ? AnimalSpecies.COW : (roll < 68 ? AnimalSpecies.SHEEP : AnimalSpecies.CHICKEN);
            case DESERT -> AnimalSpecies.CHICKEN;
        };
    }
}
