package com.example.voxelgame.game.entity.animal;

import com.example.voxelgame.game.entity.EntityUpdateContext;

final class GrazeBehavior implements AnimalBehavior {
    @Override
    public int priority() {
        return 20;
    }

    @Override
    public boolean canStart(AnimalEntity animal, EntityUpdateContext context) {
        return animal.isReadyForCalmDecision() && animal.nextCalmDecisionRoll() < 34;
    }

    @Override
    public void start(AnimalEntity animal, EntityUpdateContext context) {
        animal.enterGraze();
    }

    @Override
    public void tick(AnimalEntity animal, double deltaTimeSeconds, EntityUpdateContext context) {
        animal.dampHorizontalVelocity();
    }

    @Override
    public boolean shouldContinue(AnimalEntity animal, EntityUpdateContext context) {
        return animal.getAiState() == AnimalAiState.GRAZE && animal.getStateTimerSeconds() > 0.0;
    }
}
