package com.example.voxelgame.camera;

import com.example.voxelgame.core.InputState;
import org.joml.Vector3f;
import java.util.Objects;

public final class FirstPersonCamera {
    private final CameraConfig config;

    private Vector3f position;
    private float yawDegrees;
    private float pitchDegrees;
    private Vector3f forward = new Vector3f(0.0f, 0.0f, -1.0f);
    private Vector3f right = new Vector3f(1.0f, 0.0f, 0.0f);
    private Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);

    public FirstPersonCamera(CameraConfig config, Vector3f startPosition, float yawDegrees, float pitchDegrees) {
        this.config = Objects.requireNonNull(config, "Camera config cannot be null.");
        this.position = new Vector3f(Objects.requireNonNull(startPosition, "Camera position cannot be null."));
        this.yawDegrees = yawDegrees;
        this.pitchDegrees = clampPitch(pitchDegrees);
        recalculateVectors();
    }

    public void updateLook(InputState inputState) {
        Objects.requireNonNull(inputState, "Input state cannot be null.");
        updateRotation(inputState.consumeMouseDelta());
    }

    public void setPosition(Vector3f position) {
        this.position = new Vector3f(Objects.requireNonNull(position, "Camera position cannot be null."));
    }

    private void updateRotation(InputState.MouseDelta mouseDelta) {
        float sensitivity = config.mouseSensitivity();
        yawDegrees += (float) mouseDelta.xOffset() * sensitivity;
        pitchDegrees -= (float) mouseDelta.yOffset() * sensitivity;
        pitchDegrees = clampPitch(pitchDegrees);
        recalculateVectors();
    }

    private void recalculateVectors() {
        double yawRadians = Math.toRadians(yawDegrees);
        double pitchRadians = Math.toRadians(pitchDegrees);

        float cosPitch = (float) Math.cos(pitchRadians);
        float sinPitch = (float) Math.sin(pitchRadians);
        float cosYaw = (float) Math.cos(yawRadians);
        float sinYaw = (float) Math.sin(yawRadians);

        forward = new Vector3f(
                cosYaw * cosPitch,
                sinPitch,
                sinYaw * cosPitch
        ).normalize();

        right = new Vector3f(forward).cross(new Vector3f(0.0f, 1.0f, 0.0f)).normalize();
        up = new Vector3f(right).cross(forward).normalize();
    }

    private float clampPitch(float pitch) {
        float limit = config.pitchLimitDegrees();
        return Math.clamp(pitch, -limit, limit);
    }

    public Vector3f getPosition() {
        return new Vector3f(position);
    }

    public Vector3f getEyePosition() {
        return new Vector3f(position).add(0.0f, config.playerHeight() * 0.9f, 0.0f);
    }

    public Vector3f getForward() {
        return new Vector3f(forward);
    }

    public Vector3f getRight() {
        return new Vector3f(right);
    }

    public Vector3f getUp() {
        return new Vector3f(up);
    }

    public float getYawDegrees() {
        return yawDegrees;
    }

    public float getPitchDegrees() {
        return pitchDegrees;
    }

    public float getMouseSensitivity() {
        return config.mouseSensitivity();
    }

    public float getMoveSpeed() {
        return config.moveSpeed();
    }

    public float getJumpVelocity() {
        return config.jumpVelocity();
    }

    public float getGravity() {
        return config.gravity();
    }

    public float getPlayerHeight() {
        return config.playerHeight();
    }

    public float getColliderRadius() {
        return config.colliderRadius();
    }
}
