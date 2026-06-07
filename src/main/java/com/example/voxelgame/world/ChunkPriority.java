package com.example.voxelgame.world;

public record ChunkPriority(long key, int chunkX, int chunkZ, int distanceSquared) implements Comparable<ChunkPriority> {
    public ChunkPriority(int chunkX, int chunkZ, int distanceSquared) {
        this(chunkKey(chunkX, chunkZ), chunkX, chunkZ, distanceSquared);
    }

    @Override
    public int compareTo(ChunkPriority other) {
        int result = Integer.compare(distanceSquared, other.distanceSquared);
        if (result != 0) {
            return result;
        }
        return Long.compare(key, other.key);
    }

    public static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) ^ (chunkZ & 0xFFFFFFFFL);
    }
}
