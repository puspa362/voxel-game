package com.example.voxelgame.world;

public record BlockHitResult(
        int blockX,
        int blockY,
        int blockZ,
        int adjacentX,
        int adjacentY,
        int adjacentZ
) {
}
