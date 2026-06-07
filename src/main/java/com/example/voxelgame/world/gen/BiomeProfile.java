package com.example.voxelgame.world.gen;

public record BiomeProfile(
        BiomeType type,
        double idealTemperature,
        double idealMoisture,
        double heightOffset,
        double heightScale,
        double roughness,
        double vegetationDensity,
        double waterFrequency,
        double oreModifier
) {
}
