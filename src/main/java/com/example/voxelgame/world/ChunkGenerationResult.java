package com.example.voxelgame.world;

public record ChunkGenerationResult(long key, Chunk chunk, long generationTimeNanos) {
}
