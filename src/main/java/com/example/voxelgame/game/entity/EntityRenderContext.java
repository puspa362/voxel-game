package com.example.voxelgame.game.entity;

import org.joml.Vector3f;

public interface EntityRenderContext {
    void drawCube(Vector3f center, Vector3f halfExtents, float yawRadians, float red, float green, float blue, float alpha);
}
