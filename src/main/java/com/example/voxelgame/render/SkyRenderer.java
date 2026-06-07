package com.example.voxelgame.render;

import com.example.voxelgame.camera.FirstPersonCamera;
import com.example.voxelgame.world.WorldTime;
import org.joml.Matrix4f;

public final class SkyRenderer implements AutoCloseable {
    private final SkySystem skySystem = new SkySystem();

    public void initialize() {
        skySystem.initialize();
    }

    public void render(Matrix4f projectionMatrix, Matrix4f viewMatrix, FirstPersonCamera camera, WorldTime worldTime) {
        skySystem.render(projectionMatrix, viewMatrix, camera, worldTime);
    }

    @Override
    public void close() {
        skySystem.close();
    }
}
