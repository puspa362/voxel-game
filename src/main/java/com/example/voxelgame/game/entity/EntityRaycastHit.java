package com.example.voxelgame.game.entity;

public record EntityRaycastHit(Entity entity, float distance) {
    public EntityRaycastHit {
        if (distance < 0.0f) {
            throw new IllegalArgumentException("Raycast hit distance cannot be negative.");
        }
    }
}
