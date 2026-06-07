package com.example.voxelgame.world;

import java.util.Objects;

import com.example.voxelgame.save.SaveManager;
import com.example.voxelgame.world.gen.TerrainGenerator;

public final class ChunkGenerationTask {
    private final int chunkX;
    private final int chunkZ;
    private final TerrainGenerator terrainGenerator;
    private final SaveManager saveManager;

    public ChunkGenerationTask(int chunkX, int chunkZ, TerrainGenerator terrainGenerator, SaveManager saveManager) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.terrainGenerator = Objects.requireNonNull(terrainGenerator, "Terrain generator cannot be null.");
        this.saveManager = Objects.requireNonNull(saveManager, "Save manager cannot be null.");
    }

    public ChunkGenerationResult call() {
        long startNanos = System.nanoTime();
        Chunk chunk = saveManager.loadChunk(chunkX, chunkZ).orElseGet(() -> terrainGenerator.generateChunk(chunkX, chunkZ));
        long durationNanos = System.nanoTime() - startNanos;
        return new ChunkGenerationResult(ChunkPriority.chunkKey(chunkX, chunkZ), chunk, durationNanos);
    }
}
