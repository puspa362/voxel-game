package com.example.voxelgame.core;

public record LaunchOptions(double maxRuntimeSeconds, int renderDistanceChunks, int chunkLoadsPerFrame, boolean spawnNearVillage) {
    private static final int DEFAULT_RENDER_DISTANCE_CHUNKS = 3;
    private static final int DEFAULT_CHUNK_LOADS_PER_FRAME = 2;

    public LaunchOptions(double maxRuntimeSeconds, int renderDistanceChunks, int chunkLoadsPerFrame) {
        this(maxRuntimeSeconds, renderDistanceChunks, chunkLoadsPerFrame, true);
    }

    public LaunchOptions {
        if (maxRuntimeSeconds < 0.0) {
            throw new IllegalArgumentException("Maximum runtime cannot be negative.");
        }
        if (renderDistanceChunks < 0) {
            throw new IllegalArgumentException("Render distance cannot be negative.");
        }
        if (chunkLoadsPerFrame < 1) {
            throw new IllegalArgumentException("Chunk loads per frame must be at least one.");
        }
    }

    public static LaunchOptions fromArgs(String[] args) {
        double maxRuntimeSeconds = 0.0;
        int renderDistanceChunks = DEFAULT_RENDER_DISTANCE_CHUNKS;
        int chunkLoadsPerFrame = DEFAULT_CHUNK_LOADS_PER_FRAME;
        boolean spawnNearVillage = true;

        for (String argument : args) {
            if (argument.startsWith("--max-runtime=")) {
                String rawValue = argument.substring("--max-runtime=".length());
                try {
                    maxRuntimeSeconds = Double.parseDouble(rawValue);
                } catch (NumberFormatException exception) {
                    throw new IllegalArgumentException("Invalid value for --max-runtime: " + rawValue, exception);
                }

                if (maxRuntimeSeconds <= 0.0) {
                    throw new IllegalArgumentException("Maximum runtime must be greater than zero.");
                }
                continue;
            }
            if (argument.startsWith("--render-distance=")) {
                String rawValue = argument.substring("--render-distance=".length());
                try {
                    renderDistanceChunks = Integer.parseInt(rawValue);
                } catch (NumberFormatException exception) {
                    throw new IllegalArgumentException("Invalid value for --render-distance: " + rawValue, exception);
                }

                if (renderDistanceChunks < 0) {
                    throw new IllegalArgumentException("Render distance cannot be negative.");
                }
                continue;
            }
            if (argument.startsWith("--chunk-loads-per-frame=")) {
                String rawValue = argument.substring("--chunk-loads-per-frame=".length());
                try {
                    chunkLoadsPerFrame = Integer.parseInt(rawValue);
                } catch (NumberFormatException exception) {
                    throw new IllegalArgumentException("Invalid value for --chunk-loads-per-frame: " + rawValue, exception);
                }

                if (chunkLoadsPerFrame < 1) {
                    throw new IllegalArgumentException("Chunk loads per frame must be at least one.");
                }
                continue;
            }
            if (argument.startsWith("--spawn-near-village=")) {
                spawnNearVillage = Boolean.parseBoolean(argument.substring("--spawn-near-village=".length()));
                continue;
            }

            throw new IllegalArgumentException("Unknown launch argument: " + argument);
        }

        return new LaunchOptions(maxRuntimeSeconds, renderDistanceChunks, chunkLoadsPerFrame, spawnNearVillage);
    }

    public boolean hasRuntimeLimit() {
        return maxRuntimeSeconds > 0.0;
    }
}
