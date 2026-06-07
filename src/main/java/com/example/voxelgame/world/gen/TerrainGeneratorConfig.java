package com.example.voxelgame.world.gen;

public record TerrainGeneratorConfig(
        long seed,
        int seaLevel,
        int baseHeight,
        int heightVariation,
        int dirtDepth,
        double noiseScale,
        int treeCellSize,
        double treeNoiseScale,
        double treeThreshold,
        double biomeScale,
        double biomeBlendStrength,
        double terrainDetailScale
) {
    public TerrainGeneratorConfig(
            long seed,
            int seaLevel,
            int baseHeight,
            int heightVariation,
            int dirtDepth,
            double noiseScale,
            int treeCellSize,
            double treeNoiseScale,
            double treeThreshold
    ) {
        this(seed, seaLevel, baseHeight, heightVariation, dirtDepth, noiseScale, treeCellSize, treeNoiseScale, treeThreshold, 0.00115, 0.42, 0.022);
    }

    public TerrainGeneratorConfig {
        if (seaLevel < 1 || seaLevel >= 255) {
            throw new IllegalArgumentException("Sea level must be between 1 and 254.");
        }
        if (baseHeight < 1 || baseHeight >= 255) {
            throw new IllegalArgumentException("Base height must be between 1 and 254.");
        }
        if (heightVariation < 1 || heightVariation > 255) {
            throw new IllegalArgumentException("Height variation must be between 1 and 255.");
        }
        if (dirtDepth < 1 || dirtDepth > 32) {
            throw new IllegalArgumentException("Dirt depth must be between 1 and 32.");
        }
        if (noiseScale <= 0.0) {
            throw new IllegalArgumentException("Noise scale must be greater than zero.");
        }
        if (treeCellSize < 4 || treeCellSize > 32) {
            throw new IllegalArgumentException("Tree cell size must be between 4 and 32.");
        }
        if (treeNoiseScale <= 0.0) {
            throw new IllegalArgumentException("Tree noise scale must be greater than zero.");
        }
        if (treeThreshold < -1.0 || treeThreshold > 1.0) {
            throw new IllegalArgumentException("Tree threshold must be between -1.0 and 1.0.");
        }
        if (biomeScale <= 0.0) {
            throw new IllegalArgumentException("Biome scale must be greater than zero.");
        }
        if (biomeBlendStrength <= 0.0 || biomeBlendStrength > 2.0) {
            throw new IllegalArgumentException("Biome blend strength must be between 0.0 and 2.0.");
        }
        if (terrainDetailScale <= 0.0) {
            throw new IllegalArgumentException("Terrain detail scale must be greater than zero.");
        }
    }

    public static TerrainGeneratorConfig defaultConfig(long seed) {
        return new TerrainGeneratorConfig(seed, 62, 64, 30, 4, 0.0065, 8, 0.035, 0.04, 0.00115, 0.42, 0.022);
    }
}
