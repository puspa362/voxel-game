package com.example.voxelgame.world;

import java.util.ArrayDeque;
import java.util.Deque;

public final class LightPropagator {
    private static final int MAX_LIGHT = 15;
    private static final int[][] DIRECTIONS = {
        {0, 1, 0}, {0, -1, 0}, {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}
    };

    private final VoxelWorld world;

    public LightPropagator(VoxelWorld world) {
        this.world = world;
    }

    public void propagateLight(Chunk chunk) {
        chunk.clearLightLevels();
        propagateSunlight(chunk);
        propagateBlockLight(chunk);
    }

    private void propagateSunlight(Chunk chunk) {
        Deque<int[]> queue = new ArrayDeque<>();

        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                for (int y = Chunk.HEIGHT - 1; y >= 0; y--) {
                    if (!canLightPassThrough(chunk.getBlock(x, y, z))) {
                        break;
                    }
                    chunk.setLightLevel(x, y, z, MAX_LIGHT);
                    queue.add(new int[]{x, y, z});
                }
            }
        }

        floodFill(chunk, queue);
    }

    private void propagateBlockLight(Chunk chunk) {
        Deque<int[]> queue = new ArrayDeque<>();

        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int y = 0; y < Chunk.HEIGHT; y++) {
                for (int z = 0; z < Chunk.DEPTH; z++) {
                    Block block = chunk.getBlock(x, y, z);
                    int emission = block.getLightEmission();
                    if (emission > 0 && emission > chunk.getLightLevel(x, y, z)) {
                        chunk.setLightLevel(x, y, z, emission);
                        queue.add(new int[]{x, y, z});
                    }
                }
            }
        }

        floodFill(chunk, queue);
    }

    private void floodFill(Chunk chunk, Deque<int[]> queue) {
        while (!queue.isEmpty()) {
            int[] pos = queue.poll();
            int x = pos[0], y = pos[1], z = pos[2];
            int currentLight = chunk.getLightLevel(x, y, z);

            if (currentLight <= 1) {
                continue;
            }

            for (int[] dir : DIRECTIONS) {
                int nx = x + dir[0], ny = y + dir[1], nz = z + dir[2];
                if (nx >= 0 && nx < Chunk.WIDTH && ny >= 0 && ny < Chunk.HEIGHT && nz >= 0 && nz < Chunk.DEPTH) {
                    if (!canLightPassThrough(chunk.getBlock(nx, ny, nz))) {
                        continue;
                    }
                    int newLight = currentLight - 1;
                    if (newLight > chunk.getLightLevel(nx, ny, nz)) {
                        chunk.setLightLevel(nx, ny, nz, newLight);
                        queue.add(new int[]{nx, ny, nz});
                    }
                }
            }
        }
    }

    private boolean canLightPassThrough(Block block) {
        return block.isTransparent() || !block.isSolid();
    }
}
