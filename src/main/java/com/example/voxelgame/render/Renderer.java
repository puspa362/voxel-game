package com.example.voxelgame.render;

import static org.lwjgl.opengl.GL11C.GL_BLEND;
import static org.lwjgl.opengl.GL11C.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11C.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11C.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11C.GL_LEQUAL;
import static org.lwjgl.opengl.GL11C.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11C.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11C.glBlendFunc;
import static org.lwjgl.opengl.GL11C.glClear;
import static org.lwjgl.opengl.GL11C.glDepthFunc;
import static org.lwjgl.opengl.GL11C.glDisable;
import static org.lwjgl.opengl.GL11C.glEnable;
import static org.lwjgl.opengl.GL11C.glViewport;

import com.example.voxelgame.camera.FirstPersonCamera;
import com.example.voxelgame.core.GameWindow;
import com.example.voxelgame.game.entity.EntityManager;
import com.example.voxelgame.world.VoxelWorld;
import com.example.voxelgame.world.WorldTime;
import java.util.Objects;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL;

public final class Renderer {
    private final VoxelWorld world;
    private final Matrix4f projectionMatrix = new Matrix4f();
    private final Matrix4f viewMatrix = new Matrix4f();

    private SkyRenderer skyRenderer;
    private WorldRenderer worldRenderer;
    private EntityRenderer entityRenderer;
    private UiRenderer uiRenderer;

    public Renderer(VoxelWorld world) {
        this.world = Objects.requireNonNull(world, "Voxel world cannot be null.");
    }

    public void initialize(GameWindow window) {
        GL.createCapabilities();

        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
        glEnable(GL_CULL_FACE);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glViewport(0, 0, window.getFramebufferWidth(), window.getFramebufferHeight());

        skyRenderer = new SkyRenderer();
        skyRenderer.initialize();
        worldRenderer = new WorldRenderer(world);
        worldRenderer.initialize();
        entityRenderer = new EntityRenderer();
        uiRenderer = new UiRenderer();
    }

    public void render(GameWindow window, FirstPersonCamera camera, UiRenderState uiState, WorldTime worldTime, EntityManager entityManager) {
        Objects.requireNonNull(camera, "Camera cannot be null.");
        Objects.requireNonNull(uiState, "UI state cannot be null.");
        Objects.requireNonNull(worldTime, "World time cannot be null.");
        Objects.requireNonNull(entityManager, "Entity manager cannot be null.");
        glViewport(0, 0, window.getFramebufferWidth(), window.getFramebufferHeight());

        updateCameraMatrices(window, camera);

        skyRenderer.render(projectionMatrix, viewMatrix, camera, worldTime);
        glClear(GL_DEPTH_BUFFER_BIT);
        worldRenderer.render(projectionMatrix, viewMatrix, camera, worldTime, uiState.hudState());
        entityRenderer.render(entityManager, projectionMatrix, viewMatrix);

        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        uiRenderer.renderOverlay(window.getFramebufferWidth(), window.getFramebufferHeight(), uiState.hudState(), uiState.inventoryOpen());
        uiRenderer.render(window.getFramebufferWidth(), window.getFramebufferHeight(), uiState);
        glDisable(GL_BLEND);
        glEnable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);
    }

    private void updateCameraMatrices(GameWindow window, FirstPersonCamera camera) {
        float aspectRatio = (float) window.getFramebufferWidth() / (float) window.getFramebufferHeight();
        var eyePosition = camera.getEyePosition();
        projectionMatrix.identity().perspective((float) Math.toRadians(70.0), aspectRatio, 0.1f, 1000.0f);
        viewMatrix.identity().lookAt(
                eyePosition.x,
                eyePosition.y,
                eyePosition.z,
                eyePosition.x + camera.getForward().x,
                eyePosition.y + camera.getForward().y,
                eyePosition.z + camera.getForward().z,
                camera.getUp().x,
                camera.getUp().y,
                camera.getUp().z
        );
    }

    public void close() {
        if (worldRenderer != null) {
            worldRenderer.close();
            worldRenderer = null;
        }
        if (skyRenderer != null) {
            skyRenderer.close();
            skyRenderer = null;
        }
        if (uiRenderer != null) {
            uiRenderer.close();
            uiRenderer = null;
        }
        if (entityRenderer != null) {
            entityRenderer.close();
            entityRenderer = null;
        }
    }
}
