package com.example.voxelgame.game.mode;

public final class GameModeRules {
    private final GameMode mode;

    private GameModeRules(GameMode mode) {
        this.mode = mode;
    }

    public static GameModeRules forMode(GameMode mode) {
        return new GameModeRules(mode);
    }

    public GameMode mode() {
        return mode;
    }

    public boolean instantBlockBreaking() {
        return mode == GameMode.CREATIVE;
    }

    public boolean shouldDropBlockItems() {
        return mode == GameMode.SURVIVAL;
    }

    public boolean consumesPlacementItems() {
        return mode == GameMode.SURVIVAL;
    }

    public boolean consumesFoodItems() {
        return mode == GameMode.SURVIVAL;
    }

    public boolean usesHunger() {
        return mode == GameMode.SURVIVAL;
    }

    public boolean canTakeDamage() {
        return mode == GameMode.SURVIVAL;
    }

    public boolean canFly() {
        return mode == GameMode.CREATIVE;
    }

    public boolean usesCraftingRequirements() {
        return mode == GameMode.SURVIVAL;
    }

    public boolean hasCreativeInventory() {
        return mode == GameMode.CREATIVE;
    }
}
