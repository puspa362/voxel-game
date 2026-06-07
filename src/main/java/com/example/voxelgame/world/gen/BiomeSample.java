package com.example.voxelgame.world.gen;

import java.util.Arrays;

public record BiomeSample(
        BiomeType primaryBiome,
        double temperature,
        double moisture,
        double heightOffset,
        double heightScale,
        double roughness,
        double vegetationDensity,
        double waterFrequency,
        double oreModifier,
        double[] weights
) {
    public BiomeSample {
        weights = Arrays.copyOf(weights, weights.length);
    }

    @Override
    public double[] weights() {
        return Arrays.copyOf(weights, weights.length);
    }

    public double weight(BiomeType type) {
        return weights[type.ordinal()];
    }

    public String debugText() {
        return "%s temp=%.2f moist=%.2f rough=%.2f trees=%.2f water=%.2f".formatted(
                primaryBiome.displayName(),
                temperature,
                moisture,
                roughness,
                vegetationDensity,
                waterFrequency
        );
    }
}
