package com.example.voxelgame.world.gen;

public enum BiomeType {
    PLAINS("Plains"),
    FOREST("Forest"),
    MOUNTAINS("Mountains"),
    DESERT("Desert"),
    SNOWY("Snowy");

    private final String displayName;

    BiomeType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
