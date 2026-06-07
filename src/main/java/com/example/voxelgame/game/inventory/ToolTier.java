package com.example.voxelgame.game.inventory;

public enum ToolTier {
    WOOD(2.0f),
    STONE(4.0f),
    IRON(6.0f);

    private final float miningSpeed;

    ToolTier(float miningSpeed) {
        this.miningSpeed = miningSpeed;
    }

    public float getMiningSpeed() {
        return miningSpeed;
    }
}
