package com.example.voxelgame.world;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.joml.Vector3f;

import com.example.voxelgame.save.SaveManager;
import com.example.voxelgame.game.furnace.FurnaceBlockEntity;
import com.example.voxelgame.world.gen.TerrainGenerator;
import com.example.voxelgame.world.gen.BiomeSample;
import com.example.voxelgame.world.gen.village.VillageFeature;

public final class VoxelWorld {
    private final TerrainGenerator terrainGenerator;
    private final SaveManager saveManager;
    private final ChunkGenerationManager chunkGenerationManager;
    private final WaterSimulation waterSimulation = new WaterSimulation();
    private final LightPropagator lightPropagator;
    private final int renderDistanceChunks;
    private final int maxChunkLoadsPerUpdate;
    private final Map<Long, Chunk> chunks = new LinkedHashMap<>();
    private boolean meshDirty = true;
    private int centerChunkX = Integer.MIN_VALUE;
    private int centerChunkZ = Integer.MIN_VALUE;

    public VoxelWorld(TerrainGenerator terrainGenerator, SaveManager saveManager, int radiusInChunks) {
        this(terrainGenerator, saveManager, radiusInChunks, 2);
    }

    public VoxelWorld(TerrainGenerator terrainGenerator, SaveManager saveManager, int radiusInChunks, int maxChunkLoadsPerUpdate) {
        this.terrainGenerator = Objects.requireNonNull(terrainGenerator, "Terrain generator cannot be null.");
        this.saveManager = Objects.requireNonNull(saveManager, "Save manager cannot be null.");

        if (radiusInChunks < 0) {
            throw new IllegalArgumentException("Chunk radius cannot be negative.");
        }
        if (maxChunkLoadsPerUpdate < 1) {
            throw new IllegalArgumentException("Chunk loads per update must be at least one.");
        }

        this.renderDistanceChunks = radiusInChunks;
        this.maxChunkLoadsPerUpdate = maxChunkLoadsPerUpdate;
        this.chunkGenerationManager = new ChunkGenerationManager(terrainGenerator, saveManager, radiusInChunks, maxChunkLoadsPerUpdate);
        this.lightPropagator = new LightPropagator(this);
    }

    public synchronized void updateAround(Vector3f worldPosition) {
        Objects.requireNonNull(worldPosition, "World position cannot be null.");

        int playerChunkX = Math.floorDiv((int) Math.floor(worldPosition.x), Chunk.WIDTH);
        int playerChunkZ = Math.floorDiv((int) Math.floor(worldPosition.z), Chunk.DEPTH);
        boolean centerChanged = playerChunkX != centerChunkX || playerChunkZ != centerChunkZ;
        if (centerChanged) {
            centerChunkX = playerChunkX;
            centerChunkZ = playerChunkZ;
            rebuildPendingChunkLoads(playerChunkX, playerChunkZ);
        }

        boolean changed = false;
        changed |= unloadFarChunks(playerChunkX, playerChunkZ);
        changed |= processCompletedChunkFinalization(playerChunkX, playerChunkZ);

        chunkGenerationManager.pruneFarChunks(playerChunkX, playerChunkZ, renderDistanceChunks);
        chunkGenerationManager.schedulePendingWork();

        if (changed) {
            meshDirty = true;
        }
    }

    private void rebuildPendingChunkLoads(int playerChunkX, int playerChunkZ) {
        Set<Long> loadedChunkKeys = new HashSet<>(chunks.size());
        for (Chunk loadedChunk : chunks.values()) {
            loadedChunkKeys.add(key(loadedChunk.getChunkX(), loadedChunk.getChunkZ()));
        }

        chunkGenerationManager.updateDesiredChunks(playerChunkX, playerChunkZ, renderDistanceChunks, loadedChunkKeys);
    }

    private boolean processCompletedChunkFinalization(int playerChunkX, int playerChunkZ) {
        boolean changed = false;
        Set<Long> loadedChunkKeys = new HashSet<>(chunks.size());
        for (Chunk loadedChunk : chunks.values()) {
            loadedChunkKeys.add(key(loadedChunk.getChunkX(), loadedChunk.getChunkZ()));
        }

        List<ChunkGenerationResult> completed = chunkGenerationManager.pollCompletedChunks(playerChunkX, playerChunkZ, renderDistanceChunks, loadedChunkKeys);
        for (ChunkGenerationResult result : completed) {
            Chunk chunk = result.chunk();
            long chunkKey = result.key();
            if (chunks.containsKey(chunkKey)) {
                continue;
            }
            chunks.put(chunkKey, chunk);
            chunkGenerationManager.notifyChunkLoaded(chunkKey);
            markAdjacentChunksDirty(chunk.getChunkX(), chunk.getChunkZ());
            lightPropagator.propagateLight(chunk);
            changed = true;
        }
        return changed;
    }

    private boolean unloadFarChunks(int playerChunkX, int playerChunkZ) {
        boolean changed = false;
        Iterator<Map.Entry<Long, Chunk>> iterator = chunks.entrySet().iterator();
        while (iterator.hasNext()) {
            Chunk chunk = iterator.next().getValue();
            if (!isWithinRenderDistance(chunk.getChunkX(), chunk.getChunkZ(), playerChunkX, playerChunkZ)) {
                saveManager.saveChunk(chunk);
                markAdjacentChunksDirty(chunk.getChunkX(), chunk.getChunkZ());
                iterator.remove();
                changed = true;
            }
        }
        return changed;
    }

    private boolean isWithinRenderDistance(int chunkX, int chunkZ, int playerChunkX, int playerChunkZ) {
        return Math.abs(chunkX - playerChunkX) <= renderDistanceChunks
                && Math.abs(chunkZ - playerChunkZ) <= renderDistanceChunks;
    }

    public synchronized int getRenderDistanceChunks() {
        return renderDistanceChunks;
    }

    public synchronized int getPendingChunkLoadCount() {
        return chunkGenerationManager.getQueuedCount() + chunkGenerationManager.getGeneratingCount();
    }

    public synchronized double getChunkGenerationAverageMillis() {
        return chunkGenerationManager.getAverageGenerationTimeMillis();
    }

    public synchronized int getChunkGenerationCompletedCount() {
        return chunkGenerationManager.getCompletedCount();
    }

    public synchronized Collection<Chunk> getChunks() {
        return new ArrayList<>(chunks.values());
    }

    public synchronized List<VillageFeature> getLoadedVillageFeatures() {
        Map<Long, VillageFeature> villages = new LinkedHashMap<>();
        for (Chunk chunk : chunks.values()) {
            for (VillageFeature village : terrainGenerator.villagesNearChunk(chunk.getChunkX(), chunk.getChunkZ())) {
                villages.putIfAbsent(village.id(), village);
            }
        }
        return List.copyOf(villages.values());
    }

    public synchronized Chunk getChunk(int chunkX, int chunkZ) {
        return chunks.get(key(chunkX, chunkZ));
    }

    public synchronized int getLoadedChunkCount() {
        return chunks.size();
    }

    public synchronized Block getBlockAtWorld(int worldX, int worldY, int worldZ) {
        if (worldY < 0 || worldY >= Chunk.HEIGHT) {
            return BlockRegistry.AIR;
        }

        int chunkX = Math.floorDiv(worldX, Chunk.WIDTH);
        int chunkZ = Math.floorDiv(worldZ, Chunk.DEPTH);
        Chunk chunk = getChunk(chunkX, chunkZ);
        if (chunk == null) {
            return BlockRegistry.AIR;
        }

        int localX = Math.floorMod(worldX, Chunk.WIDTH);
        int localZ = Math.floorMod(worldZ, Chunk.DEPTH);
        return chunk.getBlock(localX, worldY, localZ);
    }

    public synchronized int getLightLevelAtWorld(int worldX, int worldY, int worldZ) {
        if (worldY < 0 || worldY >= Chunk.HEIGHT) {
            return 0;
        }

        int chunkX = Math.floorDiv(worldX, Chunk.WIDTH);
        int chunkZ = Math.floorDiv(worldZ, Chunk.DEPTH);
        Chunk chunk = getChunk(chunkX, chunkZ);
        if (chunk == null) {
            return 0;
        }

        int localX = Math.floorMod(worldX, Chunk.WIDTH);
        int localZ = Math.floorMod(worldZ, Chunk.DEPTH);
        return chunk.getLightLevel(localX, worldY, localZ);
    }

    public synchronized boolean isSolidBlockAtWorld(int worldX, int worldY, int worldZ) {
        return getBlockAtWorld(worldX, worldY, worldZ).isCollidable();
    }

    public synchronized boolean isBlockLoadedAtWorld(int worldX, int worldY, int worldZ) {
        if (worldY < 0 || worldY >= Chunk.HEIGHT) {
            return false;
        }

        int chunkX = Math.floorDiv(worldX, Chunk.WIDTH);
        int chunkZ = Math.floorDiv(worldZ, Chunk.DEPTH);
        return getChunk(chunkX, chunkZ) != null;
    }

    public synchronized int getWaterLevelAtWorld(int worldX, int worldY, int worldZ) {
        if (worldY < 0 || worldY >= Chunk.HEIGHT) {
            return -1;
        }

        int chunkX = Math.floorDiv(worldX, Chunk.WIDTH);
        int chunkZ = Math.floorDiv(worldZ, Chunk.DEPTH);
        Chunk chunk = getChunk(chunkX, chunkZ);
        if (chunk == null) {
            return -1;
        }

        int localX = Math.floorMod(worldX, Chunk.WIDTH);
        int localZ = Math.floorMod(worldZ, Chunk.DEPTH);
        return chunk.getWaterLevel(localX, worldY, localZ);
    }

    public synchronized boolean isWaterAtWorld(int worldX, int worldY, int worldZ) {
        return getBlockAtWorld(worldX, worldY, worldZ) == BlockRegistry.WATER;
    }

    public synchronized float getWaterSurfaceYAtWorld(int worldX, int worldY, int worldZ) {
        int level = getWaterLevelAtWorld(worldX, worldY, worldZ);
        if (level < 0) {
            return worldY;
        }
        if (getWaterLevelAtWorld(worldX, worldY + 1, worldZ) >= 0) {
            return worldY + 1.0f;
        }
        return worldY + waterHeightForLevel(level);
    }

    public synchronized int getWaterDepthBelow(int worldX, int footY, int worldZ) {
        int depth = 0;
        for (int y = footY; y >= 0 && isWaterAtWorld(worldX, y, worldZ); y--) {
            depth++;
        }
        return depth;
    }

    public synchronized WaterContact sampleWaterContact(
            float minX,
            float minY,
            float minZ,
            float maxX,
            float maxY,
            float maxZ,
            float headY
    ) {
        int blockMinX = (int) Math.floor(minX);
        int blockMaxX = (int) Math.floor(maxX);
        int blockMinY = (int) Math.floor(minY);
        int blockMaxY = (int) Math.floor(maxY);
        int blockMinZ = (int) Math.floor(minZ);
        int blockMaxZ = (int) Math.floor(maxZ);

        boolean touchingWater = false;
        float deepestSubmersion = 0.0f;
        for (int x = blockMinX; x <= blockMaxX; x++) {
            for (int y = blockMinY; y <= blockMaxY; y++) {
                for (int z = blockMinZ; z <= blockMaxZ; z++) {
                    if (!isWaterAtWorld(x, y, z)) {
                        continue;
                    }
                    float surfaceY = getWaterSurfaceYAtWorld(x, y, z);
                    float overlap = Math.min(maxY, surfaceY) - Math.max(minY, y);
                    if (overlap > 0.0f) {
                        touchingWater = true;
                        deepestSubmersion = Math.max(deepestSubmersion, overlap);
                    }
                }
            }
        }

        int headBlockX = (int) Math.floor((minX + maxX) * 0.5f);
        int headBlockY = (int) Math.floor(headY);
        int headBlockZ = (int) Math.floor((minZ + maxZ) * 0.5f);
        boolean headUnderwater = isWaterAtWorld(headBlockX, headBlockY, headBlockZ)
                && headY < getWaterSurfaceYAtWorld(headBlockX, headBlockY, headBlockZ);
        int footX = (int) Math.floor((minX + maxX) * 0.5f);
        int footY = (int) Math.floor(minY);
        int footZ = (int) Math.floor((minZ + maxZ) * 0.5f);
        return new WaterContact(
                touchingWater,
                touchingWater && deepestSubmersion > 0.05f,
                headUnderwater,
                deepestSubmersion,
                getWaterDepthBelow(footX, footY, footZ)
        );
    }

    public synchronized boolean setBlockAtWorld(int worldX, int worldY, int worldZ, Block block) {
        Objects.requireNonNull(block, "Block cannot be null.");

        if (worldY < 0 || worldY >= Chunk.HEIGHT) {
            return false;
        }

        int chunkX = Math.floorDiv(worldX, Chunk.WIDTH);
        int chunkZ = Math.floorDiv(worldZ, Chunk.DEPTH);
        Chunk chunk = getChunk(chunkX, chunkZ);
        if (chunk == null) {
            return false;
        }

        int localX = Math.floorMod(worldX, Chunk.WIDTH);
        int localZ = Math.floorMod(worldZ, Chunk.DEPTH);
        short previousId = chunk.getBlockId(localX, worldY, localZ);
        if (previousId == block.getId()) {
            return false;
        }

        Block previousBlock = BlockRegistry.get(previousId);
        chunk.setBlock(localX, worldY, localZ, block);
        markNeighborChunksDirty(chunkX, chunkZ, localX, localZ);
        meshDirty = true;
        waterSimulation.onBlockChanged(worldX, worldY, worldZ);
        if (affectsLighting(previousBlock) || affectsLighting(block)) {
            lightPropagator.propagateLight(chunk);
        }
        return true;
    }

    public synchronized boolean setWaterAtWorld(int worldX, int worldY, int worldZ, int level) {
        if (level < 0 || level > 7) {
            throw new IllegalArgumentException("Water level must be between 0 and 7.");
        }
        if (worldY < 0 || worldY >= Chunk.HEIGHT) {
            return false;
        }

        int chunkX = Math.floorDiv(worldX, Chunk.WIDTH);
        int chunkZ = Math.floorDiv(worldZ, Chunk.DEPTH);
        Chunk chunk = getChunk(chunkX, chunkZ);
        if (chunk == null) {
            return false;
        }

        int localX = Math.floorMod(worldX, Chunk.WIDTH);
        int localZ = Math.floorMod(worldZ, Chunk.DEPTH);
        Block currentBlock = chunk.getBlock(localX, worldY, localZ);
        int currentLevel = chunk.getWaterLevel(localX, worldY, localZ);

        if (currentBlock != BlockRegistry.WATER) {
            chunk.setBlock(localX, worldY, localZ, BlockRegistry.WATER);
            chunk.setWaterLevel(localX, worldY, localZ, level);
            markNeighborChunksDirty(chunkX, chunkZ, localX, localZ);
            meshDirty = true;
            waterSimulation.onBlockChanged(worldX, worldY, worldZ);
            return true;
        }

        if (currentLevel == level) {
            return false;
        }

        chunk.setWaterLevel(localX, worldY, localZ, level);
        markNeighborChunksDirty(chunkX, chunkZ, localX, localZ);
        meshDirty = true;
        waterSimulation.onBlockChanged(worldX, worldY, worldZ);
        return true;
    }

    public synchronized Optional<FurnaceBlockEntity> getFurnaceAtWorld(int worldX, int worldY, int worldZ) {
        if (!BlockRegistry.isFurnace(getBlockAtWorld(worldX, worldY, worldZ))) {
            return Optional.empty();
        }
        int chunkX = Math.floorDiv(worldX, Chunk.WIDTH);
        int chunkZ = Math.floorDiv(worldZ, Chunk.DEPTH);
        Chunk chunk = getChunk(chunkX, chunkZ);
        if (chunk == null) {
            return Optional.empty();
        }
        int localX = Math.floorMod(worldX, Chunk.WIDTH);
        int localZ = Math.floorMod(worldZ, Chunk.DEPTH);
        return Optional.of(chunk.getOrCreateFurnace(localX, worldY, localZ));
    }

    public synchronized Optional<BlockHitResult> raycast(Vector3f origin, Vector3f direction, float maxDistance) {
        Objects.requireNonNull(origin, "Ray origin cannot be null.");
        Objects.requireNonNull(direction, "Ray direction cannot be null.");

        Vector3f ray = new Vector3f(direction).normalize();
        if (ray.lengthSquared() == 0.0f) {
            return Optional.empty();
        }

        int x = (int) Math.floor(origin.x);
        int y = (int) Math.floor(origin.y);
        int z = (int) Math.floor(origin.z);

        int stepX = Integer.compare((int) Math.signum(ray.x), 0);
        int stepY = Integer.compare((int) Math.signum(ray.y), 0);
        int stepZ = Integer.compare((int) Math.signum(ray.z), 0);

        float tDeltaX = stepX == 0 ? Float.POSITIVE_INFINITY : Math.abs(1.0f / ray.x);
        float tDeltaY = stepY == 0 ? Float.POSITIVE_INFINITY : Math.abs(1.0f / ray.y);
        float tDeltaZ = stepZ == 0 ? Float.POSITIVE_INFINITY : Math.abs(1.0f / ray.z);

        float tMaxX = initialTMax(origin.x, x, stepX, ray.x);
        float tMaxY = initialTMax(origin.y, y, stepY, ray.y);
        float tMaxZ = initialTMax(origin.z, z, stepZ, ray.z);

        int previousX = x;
        int previousY = y;
        int previousZ = z;

        while (true) {
            Block raycastBlock = getBlockAtWorld(x, y, z);
            if (raycastBlock.isRenderable() && !raycastBlock.isFluid()) {
                return Optional.of(new BlockHitResult(x, y, z, previousX, previousY, previousZ));
            }

            previousX = x;
            previousY = y;
            previousZ = z;

            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    if (tMaxX > maxDistance) {
                        break;
                    }
                    x += stepX;
                    tMaxX += tDeltaX;
                } else {
                    if (tMaxZ > maxDistance) {
                        break;
                    }
                    z += stepZ;
                    tMaxZ += tDeltaZ;
                }
            } else {
                if (tMaxY < tMaxZ) {
                    if (tMaxY > maxDistance) {
                        break;
                    }
                    y += stepY;
                    tMaxY += tDeltaY;
                } else {
                    if (tMaxZ > maxDistance) {
                        break;
                    }
                    z += stepZ;
                    tMaxZ += tDeltaZ;
                }
            }
        }

        return Optional.empty();
    }

    public synchronized boolean consumeMeshDirty() {
        boolean dirty = meshDirty;
        meshDirty = false;
        return dirty;
    }

    public synchronized void tickWater() {
        waterSimulation.tick(this);
    }

    public synchronized void tickFurnaces() {
        boolean changed = false;
        for (Chunk chunk : chunks.values()) {
            changed |= chunk.tickFurnaces();
        }
        if (changed) {
            meshDirty = true;
        }
    }

    public synchronized String getWaterDebugText() {
        return waterSimulation.getDebugText();
    }

    public synchronized String getBiomeDebugText(int worldX, int worldZ) {
        return terrainGenerator.sampleBiomeDebugText(worldX, worldZ);
    }

    public synchronized BiomeSample getBiomeAt(int worldX, int worldZ) {
        return terrainGenerator.sampleBiome(worldX, worldZ);
    }

    public static float waterHeightForLevel(int level) {
        int clampedLevel = Math.clamp(level, 0, 7);
        return 1.0f - clampedLevel * 0.085f;
    }

    public synchronized void saveAllChunks() {
        for (Chunk chunk : chunks.values()) {
            saveManager.saveChunk(chunk);
        }
    }

    public synchronized void close() {
        chunkGenerationManager.close();
    }

    private void markNeighborChunksDirty(int chunkX, int chunkZ, int localX, int localZ) {
        if (localX == 0) {
            markChunkDirty(chunkX - 1, chunkZ);
        }
        if (localX == Chunk.WIDTH - 1) {
            markChunkDirty(chunkX + 1, chunkZ);
        }
        if (localZ == 0) {
            markChunkDirty(chunkX, chunkZ - 1);
        }
        if (localZ == Chunk.DEPTH - 1) {
            markChunkDirty(chunkX, chunkZ + 1);
        }
    }

    private void markAdjacentChunksDirty(int chunkX, int chunkZ) {
        markChunkDirty(chunkX - 1, chunkZ);
        markChunkDirty(chunkX + 1, chunkZ);
        markChunkDirty(chunkX, chunkZ - 1);
        markChunkDirty(chunkX, chunkZ + 1);
    }

    private boolean affectsLighting(Block block) {
        return block.isSolid() || !block.isTransparent() || block.getLightEmission() > 0;
    }

    private void markChunkDirty(int chunkX, int chunkZ) {
        Chunk chunk = getChunk(chunkX, chunkZ);
        if (chunk != null) {
            chunk.markDirty();
        }
    }

    private static float initialTMax(float origin, int blockCoord, int step, float directionComponent) {
        if (step == 0) {
            return Float.POSITIVE_INFINITY;
        }

        float nextBoundary = step > 0 ? blockCoord + 1.0f : blockCoord;
        return Math.abs((nextBoundary - origin) / directionComponent);
    }

    private static long key(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) ^ (chunkZ & 0xFFFFFFFFL);
    }

    private record ChunkCoordinate(int x, int z) {
        private int distanceSquaredTo(int centerX, int centerZ) {
            int dx = x - centerX;
            int dz = z - centerZ;
            return dx * dx + dz * dz;
        }
    }
}
