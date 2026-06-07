package com.example.voxelgame.camera;

public record CameraConfig(
        float moveSpeed,
        float mouseSensitivity,
        float pitchLimitDegrees,
        float jumpVelocity,
        float gravity,
        float playerHeight,
        float colliderRadius
) {
    public CameraConfig {
        if (moveSpeed <= 0.0f) {
            throw new IllegalArgumentException("Camera move speed must be greater than zero.");
        }
        if (mouseSensitivity <= 0.0f) {
            throw new IllegalArgumentException("Mouse sensitivity must be greater than zero.");
        }
        if (pitchLimitDegrees <= 0.0f || pitchLimitDegrees >= 90.0f) {
            throw new IllegalArgumentException("Pitch limit must be between 0 and 90 degrees.");
        }
        if (jumpVelocity <= 0.0f) {
            throw new IllegalArgumentException("Jump velocity must be greater than zero.");
        }
        if (gravity <= 0.0f) {
            throw new IllegalArgumentException("Gravity must be greater than zero.");
        }
        if (playerHeight <= 0.5f) {
            throw new IllegalArgumentException("Player height must be greater than 0.5.");
        }
        if (colliderRadius <= 0.0f || colliderRadius >= 1.0f) {
            throw new IllegalArgumentException("Collider radius must be between 0 and 1.");
        }
    }
}
