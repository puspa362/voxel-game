package com.example.voxelgame.game.entity;

public interface DamageableEntity {
    boolean damage(float amount, EntityDamageSource source, EntityManager entityManager);

    float getCurrentHealth();

    float getMaxHealth();
}
