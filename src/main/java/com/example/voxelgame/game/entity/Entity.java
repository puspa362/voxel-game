package com.example.voxelgame.game.entity;

import org.joml.Vector3f;
import java.util.Objects;
import java.util.Optional;

public abstract class Entity {
    private Vector3f position;
    private Vector3f velocity;
    private final Vector3f halfExtents;
    private boolean removed;

    protected Entity(Vector3f position, Vector3f velocity, Vector3f halfExtents) {
        this.position = new Vector3f(Objects.requireNonNull(position, "Entity position cannot be null."));
        this.velocity = new Vector3f(Objects.requireNonNull(velocity, "Entity velocity cannot be null."));
        this.halfExtents = new Vector3f(Objects.requireNonNull(halfExtents, "Entity bounds cannot be null."));
    }

    public Vector3f getPosition() {
        return new Vector3f(position);
    }

    protected void setPosition(Vector3f position) {
        this.position = new Vector3f(Objects.requireNonNull(position, "Entity position cannot be null."));
    }

    public Vector3f getVelocity() {
        return new Vector3f(velocity);
    }

    protected void setVelocity(Vector3f velocity) {
        this.velocity = new Vector3f(Objects.requireNonNull(velocity, "Entity velocity cannot be null."));
    }

    public Vector3f getHalfExtents() {
        return new Vector3f(halfExtents);
    }

    public BoundingBox getBoundingBox() {
        return BoundingBox.fromCenter(position, halfExtents);
    }

    public boolean isRemoved() {
        return removed;
    }

    protected void markRemoved() {
        removed = true;
    }

    public abstract void update(double deltaTimeSeconds, EntityUpdateContext context);

    public abstract void render(EntityRenderContext context);

    public Optional<EntityPersistenceData> saveData() {
        return Optional.empty();
    }
}
