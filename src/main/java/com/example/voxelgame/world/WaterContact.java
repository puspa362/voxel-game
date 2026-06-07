package com.example.voxelgame.world;

public record WaterContact(
        boolean touchingWater,
        boolean partiallySubmerged,
        boolean headUnderwater,
        float deepestSubmersion,
        int depthBelowFeet
) {
    public static WaterContact dry() {
        return new WaterContact(false, false, false, 0.0f, 0);
    }
}
