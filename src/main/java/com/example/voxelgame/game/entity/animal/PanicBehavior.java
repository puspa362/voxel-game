package com.example.voxelgame.game.entity.animal;

import com.example.voxelgame.game.entity.EntityUpdateContext;

final class PanicBehavior implements AnimalBehavior {
    @Override
    public int priority() {
        return 100;
    }

    @Override
    public boolean canStart(AnimalEntity animal, EntityUpdateContext context) {
        return animal.distanceToPlayer(context.player()) < animal.panicDistance();
    }

    @Override
    public void start(AnimalEntity animal, EntityUpdateContext context) {
        animal.enterPanic(context.world(), context.player().getPosition());
    }

    @Override
    public void tick(AnimalEntity animal, double deltaTimeSeconds, EntityUpdateContext context) {
        animal.followCurrentPath(deltaTimeSeconds);
    }

    @Override
    public boolean shouldContinue(AnimalEntity animal, EntityUpdateContext context) {
        return animal.getAiState() == AnimalAiState.PANIC
                && animal.getStateTimerSeconds() > 0.0
                && !animal.currentPathFinished();
    }
}
