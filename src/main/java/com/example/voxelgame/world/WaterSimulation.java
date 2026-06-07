package com.example.voxelgame.world;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class WaterSimulation {
    private static final int MAX_WATER_LEVEL = 7;
    private static final int MAX_UPDATES_PER_TICK = 256;
    private static final int[][] SIDE_DIRECTIONS = {
            {1, 0},
            {-1, 0},
            {0, 1},
            {0, -1}
    };

    private final ArrayDeque<Long> activeQueue = new ArrayDeque<>();
    private final Set<Long> queuedPositions = new HashSet<>();
    private int lastProcessedUpdates;
    private int lastCreatedOrChangedBlocks;
    private int lastRemovedBlocks;
    private long totalProcessedUpdates;

    public void enqueue(int x, int y, int z) {
        if (y < 0 || y >= Chunk.HEIGHT) {
            return;
        }

        long key = positionKey(x, y, z);
        if (queuedPositions.add(key)) {
            activeQueue.addLast(key);
        }
    }

    public void enqueueNeighbors(int x, int y, int z) {
        enqueue(x, y, z);
        enqueue(x, y + 1, z);
        enqueue(x, y - 1, z);
        for (int[] direction : SIDE_DIRECTIONS) {
            enqueue(x + direction[0], y, z + direction[1]);
        }
    }

    public void onBlockChanged(int x, int y, int z) {
        enqueueNeighbors(x, y, z);
    }

    public void tick(VoxelWorld world) {
        Objects.requireNonNull(world, "Voxel world cannot be null.");

        int processed = 0;
        int changed = 0;
        int removed = 0;
        while (processed < MAX_UPDATES_PER_TICK && !activeQueue.isEmpty()) {
            long key = activeQueue.removeFirst();
            queuedPositions.remove(key);
            int x = unpackX(key);
            int y = unpackY(key);
            int z = unpackZ(key);
            UpdateResult result = updateBlock(world, x, y, z);
            changed += result.changedBlocks;
            removed += result.removedBlocks;
            processed++;
        }
        lastProcessedUpdates = processed;
        lastCreatedOrChangedBlocks = changed;
        lastRemovedBlocks = removed;
        totalProcessedUpdates += processed;
    }

    public String getDebugText() {
        return "Water: queue=%d processed=%d changed=%d removed=%d total=%d".formatted(
                activeQueue.size(),
                lastProcessedUpdates,
                lastCreatedOrChangedBlocks,
                lastRemovedBlocks,
                totalProcessedUpdates
        );
    }

    private UpdateResult updateBlock(VoxelWorld world, int x, int y, int z) {
        int changed = 0;
        int removed = 0;
        Block block = world.getBlockAtWorld(x, y, z);
        if (block != BlockRegistry.WATER) {
            for (int[] direction : SIDE_DIRECTIONS) {
                int neighborLevel = world.getWaterLevelAtWorld(x + direction[0], y, z + direction[1]);
                if (neighborLevel >= 0) {
                    changed += spreadFrom(world, x, y, z, neighborLevel);
                }
            }
            int aboveLevel = world.getWaterLevelAtWorld(x, y + 1, z);
            if (aboveLevel >= 0) {
                changed += spreadDown(world, x, y + 1, z, aboveLevel);
            }
            return new UpdateResult(changed, removed);
        }

        int currentLevel = world.getWaterLevelAtWorld(x, y, z);
        if (currentLevel < 0) {
            return new UpdateResult(changed, removed);
        }

        int supportedLevel = computeSupportedLevel(world, x, y, z, currentLevel);
        if (supportedLevel < 0) {
            if (world.setBlockAtWorld(x, y, z, BlockRegistry.AIR)) {
                enqueueNeighbors(x, y, z);
                removed++;
            }
            return new UpdateResult(changed, removed);
        }

        if (supportedLevel != currentLevel) {
            if (world.setWaterAtWorld(x, y, z, supportedLevel)) {
                enqueueNeighbors(x, y, z);
                changed++;
            }
            currentLevel = supportedLevel;
        }

        int downwardChange = spreadDown(world, x, y, z, currentLevel);
        changed += downwardChange;
        if (downwardChange > 0) {
            return new UpdateResult(changed, removed);
        }

        if (currentLevel >= MAX_WATER_LEVEL) {
            return new UpdateResult(changed, removed);
        }

        for (int[] direction : SIDE_DIRECTIONS) {
            changed += spreadFrom(world, x + direction[0], y, z + direction[1], currentLevel);
        }
        return new UpdateResult(changed, removed);
    }

    private int computeSupportedLevel(VoxelWorld world, int x, int y, int z, int currentLevel) {
        if (currentLevel == 0) {
            return 0;
        }

        int bestLevel = Integer.MAX_VALUE;
        int aboveLevel = world.getWaterLevelAtWorld(x, y + 1, z);
        if (aboveLevel >= 0 && aboveLevel < MAX_WATER_LEVEL) {
            bestLevel = Math.min(bestLevel, aboveLevel + 1);
        }

        for (int[] direction : SIDE_DIRECTIONS) {
            int neighborLevel = world.getWaterLevelAtWorld(x + direction[0], y, z + direction[1]);
            if (neighborLevel >= 0 && neighborLevel < MAX_WATER_LEVEL) {
                bestLevel = Math.min(bestLevel, neighborLevel + 1);
            }
        }

        return bestLevel == Integer.MAX_VALUE ? -1 : Math.min(bestLevel, MAX_WATER_LEVEL);
    }

    private int spreadDown(VoxelWorld world, int x, int y, int z, int currentLevel) {
        int targetY = y - 1;
        if (!canFlowInto(world, x, targetY, z)) {
            return 0;
        }

        // Falling water keeps a full column so pools and waterfalls remain
        // readable; sideways spreading is where the 0-7 dissipation happens.
        int nextLevel = currentLevel == 0 ? 0 : Math.min(currentLevel + 1, MAX_WATER_LEVEL);
        if (world.setWaterAtWorld(x, targetY, z, nextLevel)) {
            enqueueNeighbors(x, targetY, z);
            return 1;
        }
        return 0;
    }

    private int spreadFrom(VoxelWorld world, int x, int y, int z, int sourceLevel) {
        if (sourceLevel >= MAX_WATER_LEVEL || !canFlowInto(world, x, y, z)) {
            return 0;
        }

        int nextLevel = sourceLevel + 1;
        if (world.setWaterAtWorld(x, y, z, nextLevel)) {
            enqueueNeighbors(x, y, z);
            return 1;
        }
        return 0;
    }

    private boolean canFlowInto(VoxelWorld world, int x, int y, int z) {
        if (y < 0 || y >= Chunk.HEIGHT || !world.isBlockLoadedAtWorld(x, y, z)) {
            return false;
        }

        Block block = world.getBlockAtWorld(x, y, z);
        if (block == BlockRegistry.AIR) {
            return true;
        }
        if (block == BlockRegistry.WATER) {
            int existingLevel = world.getWaterLevelAtWorld(x, y, z);
            return existingLevel > 0;
        }
        return block.hasProperty(BlockProperty.REPLACEABLE);
    }

    private long positionKey(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38)
                | ((long) (z & 0x3FFFFFF) << 12)
                | (y & 0xFFFL);
    }

    private int unpackX(long key) {
        return signExtend26((int) (key >> 38));
    }

    private int unpackY(long key) {
        int value = (int) (key & 0xFFFL);
        return value >= 0x800 ? value - 0x1000 : value;
    }

    private int unpackZ(long key) {
        return signExtend26((int) ((key >> 12) & 0x3FFFFFF));
    }

    private int signExtend26(int value) {
        return (value & 0x2000000) != 0 ? value | ~0x3FFFFFF : value;
    }

    private record UpdateResult(int changedBlocks, int removedBlocks) {
    }
}
