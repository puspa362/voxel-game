package com.example.voxelgame.game.entity.animal;

import com.example.voxelgame.game.entity.EntityUpdateContext;

interface AnimalBehavior {
    int priority();

    boolean canStart(AnimalEntity animal, EntityUpdateContext context);

    void start(AnimalEntity animal, EntityUpdateContext context);

    void tick(AnimalEntity animal, double deltaTimeSeconds, EntityUpdateContext context);

    boolean shouldContinue(AnimalEntity animal, EntityUpdateContext context);
}
