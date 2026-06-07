package com.example.voxelgame.game.entity.animal;

import com.example.voxelgame.game.entity.EntityUpdateContext;

final class WanderBehavior implements AnimalBehavior {
    @Override
    public int priority() {
        return 30;
    }

    @Override
    public boolean canStart(AnimalEntity animal, EntityUpdateContext context) {
        return animal.isReadyForCalmDecision() && animal.nextCalmDecisionRoll() >= 62;
    }

    @Override
    public void start(AnimalEntity animal, EntityUpdateContext context) {
        animal.enterWander(context.world());
    }

    @Override
    public void tick(AnimalEntity animal, double deltaTimeSeconds, EntityUpdateContext context) {
        animal.followCurrentPath(deltaTimeSeconds);
    }

    @Override
    public boolean shouldContinue(AnimalEntity animal, EntityUpdateContext context) {
        return animal.getAiState() == AnimalAiState.WANDER
                && animal.getStateTimerSeconds() > 0.0
                && !animal.currentPathFinished();
    }
}
