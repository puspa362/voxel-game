package com.example.voxelgame.world.gen.village;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.joml.Vector3i;

import com.example.voxelgame.world.Block;
import com.example.voxelgame.world.BlockRegistry;
import com.example.voxelgame.world.Chunk;
import com.example.voxelgame.world.gen.BiomeSample;
import com.example.voxelgame.world.gen.BiomeType;
import com.example.voxelgame.world.gen.TerrainGenerator;

public final class VillageGenerator {
    private static final int CELL_SIZE = 192;
    private static final int SEARCH_MARGIN = 72;
    private static final int MIN_RADIUS = 24;
    private static final int RADIUS_VARIATION = 10;
    private static final int MAX_SLOPE = 5;

    private final long seed;
    private final TerrainGenerator terrain;

    public VillageGenerator(long seed, TerrainGenerator terrain) {
        this.seed = seed ^ 0x4F1BBCDCBFA540A9L;
        this.terrain = terrain;
    }

    public void applyToChunk(Chunk chunk, Map<Long, Integer> heightCache, Map<Long, BiomeSample> biomeCache) {
        for (VillageFeature village : villagesNearChunk(chunk.getChunkX(), chunk.getChunkZ(), heightCache, biomeCache)) {
            flattenVillage(chunk, village);
            buildVillage(chunk, village);
        }
    }

    public List<VillageFeature> villagesNearChunk(int chunkX, int chunkZ, Map<Long, Integer> heightCache, Map<Long, BiomeSample> biomeCache) {
        int minX = chunkX * Chunk.WIDTH - SEARCH_MARGIN;
        int maxX = chunkX * Chunk.WIDTH + Chunk.WIDTH + SEARCH_MARGIN;
        int minZ = chunkZ * Chunk.DEPTH - SEARCH_MARGIN;
        int maxZ = chunkZ * Chunk.DEPTH + Chunk.DEPTH + SEARCH_MARGIN;
        int minCellX = Math.floorDiv(minX, CELL_SIZE);
        int maxCellX = Math.floorDiv(maxX, CELL_SIZE);
        int minCellZ = Math.floorDiv(minZ, CELL_SIZE);
        int maxCellZ = Math.floorDiv(maxZ, CELL_SIZE);
        List<VillageFeature> output = new ArrayList<>();

        for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
            for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
                candidateForCell(cellX, cellZ, heightCache, biomeCache).ifPresent(output::add);
            }
        }
        return output;
    }

    public Optional<VillageFeature> findNearestVillage(int worldX, int worldZ, int maxCellRadius) {
        Map<Long, Integer> heightCache = new HashMap<>();
        Map<Long, BiomeSample> biomeCache = new HashMap<>();
        int originCellX = Math.floorDiv(worldX, CELL_SIZE);
        int originCellZ = Math.floorDiv(worldZ, CELL_SIZE);
        VillageFeature best = null;
        long bestDistanceSquared = Long.MAX_VALUE;

        for (int radius = 0; radius <= maxCellRadius; radius++) {
            for (int cellZ = originCellZ - radius; cellZ <= originCellZ + radius; cellZ++) {
                for (int cellX = originCellX - radius; cellX <= originCellX + radius; cellX++) {
                    if (Math.abs(cellX - originCellX) != radius && Math.abs(cellZ - originCellZ) != radius) {
                        continue;
                    }
                    Optional<VillageFeature> candidate = candidateForCell(cellX, cellZ, heightCache, biomeCache);
                    if (candidate.isEmpty()) {
                        continue;
                    }
                    long dx = candidate.get().centerX() - worldX;
                    long dz = candidate.get().centerZ() - worldZ;
                    long distanceSquared = dx * dx + dz * dz;
                    if (distanceSquared < bestDistanceSquared) {
                        best = candidate.get();
                        bestDistanceSquared = distanceSquared;
                    }
                }
            }
            if (best != null && bestDistanceSquared <= (long) radius * CELL_SIZE * radius * CELL_SIZE) {
                return Optional.of(best);
            }
        }
        return Optional.ofNullable(best);
    }

    private java.util.Optional<VillageFeature> candidateForCell(int cellX, int cellZ, Map<Long, Integer> heightCache, Map<Long, BiomeSample> biomeCache) {
        long hash = mix64((((long) cellX) << 32) ^ (cellZ & 0xFFFFFFFFL) ^ seed);
        if (Math.floorMod((int) hash, 11) != 0) {
            return java.util.Optional.empty();
        }

        int centerX = cellX * CELL_SIZE + 48 + Math.floorMod((int) (hash >>> 8), CELL_SIZE - 96);
        int centerZ = cellZ * CELL_SIZE + 48 + Math.floorMod((int) (hash >>> 24), CELL_SIZE - 96);
        BiomeSample biome = sampleBiomeCached(centerX, centerZ, biomeCache);
        if (biome.primaryBiome() != BiomeType.PLAINS || biome.moisture() > 0.78) {
            return java.util.Optional.empty();
        }

        int centerY = sampleHeightCached(centerX, centerZ, heightCache);
        if (!isFlatEnough(centerX, centerZ, centerY, heightCache) || centerY <= 62 || centerY >= Chunk.HEIGHT - 12) {
            return java.util.Optional.empty();
        }

        int radius = MIN_RADIUS + Math.floorMod((int) (hash >>> 40), RADIUS_VARIATION);
        List<Vector3i> beds = new ArrayList<>();
        List<Vector3i> workstations = new ArrayList<>();
        for (HousePlan house : housePlans(centerX, centerY, centerZ, hash)) {
            beds.add(new Vector3i(house.x() + 1, centerY + 1, house.z() + 1));
            workstations.add(new Vector3i(house.x() + house.width() - 2, centerY + 1, house.z() + house.depth() - 2));
        }
        return java.util.Optional.of(new VillageFeature(hash, centerX, centerY, centerZ, radius, List.copyOf(beds), List.copyOf(workstations)));
    }

    private boolean isFlatEnough(int centerX, int centerZ, int centerY, Map<Long, Integer> heightCache) {
        for (int dz = -24; dz <= 24; dz += 8) {
            for (int dx = -24; dx <= 24; dx += 8) {
                int height = sampleHeightCached(centerX + dx, centerZ + dz, heightCache);
                if (Math.abs(height - centerY) > MAX_SLOPE) {
                    return false;
                }
            }
        }
        return true;
    }

    private void flattenVillage(Chunk chunk, VillageFeature village) {
        int baseX = chunk.getChunkX() * Chunk.WIDTH;
        int baseZ = chunk.getChunkZ() * Chunk.DEPTH;
        for (int localZ = 0; localZ < Chunk.DEPTH; localZ++) {
            for (int localX = 0; localX < Chunk.WIDTH; localX++) {
                int worldX = baseX + localX;
                int worldZ = baseZ + localZ;
                int dx = worldX - village.centerX();
                int dz = worldZ - village.centerZ();
                if (dx * dx + dz * dz > village.radius() * village.radius()) {
                    continue;
                }
                for (int y = village.centerY() + 1; y < Math.min(Chunk.HEIGHT, village.centerY() + 12); y++) {
                    chunk.setBlock(localX, y, localZ, BlockRegistry.AIR);
                }
                for (int y = Math.max(4, village.centerY() - 4); y < village.centerY(); y++) {
                    if (chunk.getBlock(localX, y, localZ) == BlockRegistry.AIR || chunk.getBlock(localX, y, localZ) == BlockRegistry.WATER) {
                        chunk.setBlock(localX, y, localZ, BlockRegistry.DIRT);
                    }
                }
                chunk.setBlock(localX, village.centerY(), localZ, BlockRegistry.GRASS);
            }
        }
    }

    private void buildVillage(Chunk chunk, VillageFeature village) {
        int y = village.centerY() + 1;
        fillRoad(chunk, village.centerX() - village.radius() + 4, village.centerZ(), village.centerX() + village.radius() - 4, village.centerZ(), village.centerY());
        fillRoad(chunk, village.centerX(), village.centerZ() - village.radius() + 4, village.centerX(), village.centerZ() + village.radius() - 4, village.centerY());
        placeLamp(chunk, village.centerX() - 3, y, village.centerZ() - 3);
        placeLamp(chunk, village.centerX() + 3, y, village.centerZ() + 3);
        buildFarm(chunk, village.centerX() + 9, village.centerY(), village.centerZ() - 7);
        for (HousePlan house : housePlans(village.centerX(), village.centerY(), village.centerZ(), village.id())) {
            buildHouse(chunk, house);
            fillRoad(chunk, village.centerX(), village.centerZ(), house.x() + house.width() / 2, house.z() + house.depth() / 2, village.centerY());
        }
    }

    private List<HousePlan> housePlans(int centerX, int centerY, int centerZ, long hash) {
        int count = 3 + Math.floorMod((int) (hash >>> 16), 3);
        List<HousePlan> plans = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2.0 * i / count) + ((hash >>> 32) & 15) * 0.025;
            int x = centerX + (int) Math.round(Math.cos(angle) * 15.0) - 3;
            int z = centerZ + (int) Math.round(Math.sin(angle) * 15.0) - 3;
            plans.add(new HousePlan(x, centerY, z, 7, 6));
        }
        return plans;
    }

    private void buildHouse(Chunk chunk, HousePlan house) {
        for (int dz = 0; dz < house.depth(); dz++) {
            for (int dx = 0; dx < house.width(); dx++) {
                int x = house.x() + dx;
                int z = house.z() + dz;
                place(chunk, x, house.y(), z, BlockRegistry.OAK_PLANKS);
                for (int dy = 1; dy <= 4; dy++) {
                    boolean wall = dx == 0 || dz == 0 || dx == house.width() - 1 || dz == house.depth() - 1;
                    boolean door = dz == 0 && dx == house.width() / 2 && dy <= 2;
                    place(chunk, x, house.y() + dy, z, wall && !door ? BlockRegistry.OAK_LOG : BlockRegistry.AIR);
                }
                if (dx >= -1 && dx <= house.width() && dz >= -1 && dz <= house.depth()) {
                    place(chunk, x, house.y() + 5, z, BlockRegistry.SPRUCE_PLANKS);
                }
            }
        }
        place(chunk, house.x() + 1, house.y() + 1, house.z() + 1, BlockRegistry.BED);
        place(chunk, house.x() + house.width() - 2, house.y() + 1, house.z() + house.depth() - 2, BlockRegistry.WORKSTATION);
        place(chunk, house.x() + house.width() / 2, house.y() + 3, house.z(), BlockRegistry.TORCH);
    }

    private void buildFarm(Chunk chunk, int x, int y, int z) {
        for (int dz = 0; dz < 7; dz++) {
            for (int dx = 0; dx < 9; dx++) {
                Block block = dx == 4 ? BlockRegistry.WATER : BlockRegistry.FARMLAND;
                place(chunk, x + dx, y, z + dz, block);
            }
        }
    }

    private void fillRoad(Chunk chunk, int x0, int z0, int x1, int z1, int y) {
        int steps = Math.max(Math.abs(x1 - x0), Math.abs(z1 - z0));
        for (int i = 0; i <= steps; i++) {
            float t = steps == 0 ? 0.0f : (float) i / steps;
            int x = Math.round(x0 + (x1 - x0) * t);
            int z = Math.round(z0 + (z1 - z0) * t);
            for (int dz = -1; dz <= 1; dz++) {
                for (int dx = -1; dx <= 1; dx++) {
                    place(chunk, x + dx, y, z + dz, BlockRegistry.DIRT_PATH);
                    place(chunk, x + dx, y + 1, z + dz, BlockRegistry.AIR);
                }
            }
        }
    }

    private void placeLamp(Chunk chunk, int x, int y, int z) {
        place(chunk, x, y, z, BlockRegistry.OAK_LOG);
        place(chunk, x, y + 1, z, BlockRegistry.OAK_LOG);
        place(chunk, x, y + 2, z, BlockRegistry.TORCH);
    }

    private void place(Chunk chunk, int worldX, int worldY, int worldZ, Block block) {
        if (worldY < 0 || worldY >= Chunk.HEIGHT) {
            return;
        }
        int chunkX = Math.floorDiv(worldX, Chunk.WIDTH);
        int chunkZ = Math.floorDiv(worldZ, Chunk.DEPTH);
        if (chunkX != chunk.getChunkX() || chunkZ != chunk.getChunkZ()) {
            return;
        }
        chunk.setBlock(Math.floorMod(worldX, Chunk.WIDTH), worldY, Math.floorMod(worldZ, Chunk.DEPTH), block);
    }

    private int sampleHeightCached(int worldX, int worldZ, Map<Long, Integer> heightCache) {
        long key = ((((long) worldX) << 32) ^ (worldZ & 0xFFFFFFFFL));
        return heightCache.computeIfAbsent(key, ignored -> terrain.sampleHeight(worldX, worldZ));
    }

    private BiomeSample sampleBiomeCached(int worldX, int worldZ, Map<Long, BiomeSample> biomeCache) {
        long key = ((((long) worldX) << 32) ^ (worldZ & 0xFFFFFFFFL));
        return biomeCache.computeIfAbsent(key, ignored -> terrain.sampleBiome(worldX, worldZ));
    }

    private static long mix64(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return value;
    }

    private record HousePlan(int x, int y, int z, int width, int depth) {
    }
}
