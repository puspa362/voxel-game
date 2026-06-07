package com.example.voxelgame.game.entity.animal;

public final class AnimalBreedingHooks {
    private AnimalBreedingHooks() {
    }

    public static boolean canBreed(AnimalEntity left, AnimalEntity right) {
        return left != null
                && right != null
                && left != right
                && left.getSpecies() == right.getSpecies()
                && left.getBreedingCooldownSeconds() <= 0.0
                && right.getBreedingCooldownSeconds() <= 0.0;
    }

    public static double defaultCooldownSeconds() {
        return 300.0;
    }
}
