package com.example.voxelgame.game.entity;

import java.util.Map;
import java.util.Objects;

import org.joml.Vector3f;

public record EntityPersistenceData(
        String typeId,
        Vector3f position,
        Vector3f velocity,
        Map<String, String> data
) {
    public EntityPersistenceData {
        typeId = Objects.requireNonNull(typeId, "Entity type id cannot be null.");
        position = new Vector3f(Objects.requireNonNull(position, "Entity position cannot be null."));
        velocity = new Vector3f(Objects.requireNonNull(velocity, "Entity velocity cannot be null."));
        data = Map.copyOf(Objects.requireNonNull(data, "Entity data cannot be null."));
    }

    @Override
    public Vector3f position() {
        return new Vector3f(position);
    }

    @Override
    public Vector3f velocity() {
        return new Vector3f(velocity);
    }
}
