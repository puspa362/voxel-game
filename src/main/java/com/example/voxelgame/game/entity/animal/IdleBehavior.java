package com.example.voxelgame.game.entity.animal;

import com.example.voxelgame.game.entity.EntityUpdateContext;

final class IdleBehavior implements AnimalBehavior {
    @Override
    public int priority() {
        return 10;
    }

    @Override
    public boolean canStart(AnimalEntity animal, EntityUpdateContext context) {
        return animal.isReadyForCalmDecision();
    }

    @Override
    public void start(AnimalEntity animal, EntityUpdateContext context) {
        animal.enterIdle();
    }

    @Override
    public void tick(AnimalEntity animal, double deltaTimeSeconds, EntityUpdateContext context) {
        animal.dampHorizontalVelocity();
    }

    @Override
    public boolean shouldContinue(AnimalEntity animal, EntityUpdateContext context) {
        return animal.getAiState() == AnimalAiState.IDLE && animal.getStateTimerSeconds() > 0.0;
    }
}
