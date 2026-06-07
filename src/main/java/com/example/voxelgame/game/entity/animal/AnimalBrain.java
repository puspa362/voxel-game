package com.example.voxelgame.game.entity.animal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.example.voxelgame.game.entity.EntityUpdateContext;

final class AnimalBrain {
    private final List<AnimalBehavior> behaviors;
    private AnimalBehavior activeBehavior;

    AnimalBrain(List<AnimalBehavior> behaviors) {
        this.behaviors = new ArrayList<>(Objects.requireNonNull(behaviors, "Animal behaviors cannot be null."));
        this.behaviors.sort(Comparator.comparingInt(AnimalBehavior::priority).reversed());
    }

    void tick(AnimalEntity animal, double deltaTimeSeconds, EntityUpdateContext context) {
        if (activeBehavior != null && activeBehavior.shouldContinue(animal, context)) {
            activeBehavior.tick(animal, deltaTimeSeconds, context);
            return;
        }

        activeBehavior = null;
        for (AnimalBehavior behavior : behaviors) {
            if (!behavior.canStart(animal, context)) {
                continue;
            }
            activeBehavior = behavior;
            activeBehavior.start(animal, context);
            activeBehavior.tick(animal, deltaTimeSeconds, context);
            return;
        }

        animal.enterIdle();
        animal.dampHorizontalVelocity();
    }
}
