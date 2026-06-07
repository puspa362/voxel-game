package com.example.voxelgame.core;

import java.util.Objects;

public record WindowConfig(int width, int height, String title, boolean vSync) {
    public WindowConfig {
        if (width <= 0) {
            throw new IllegalArgumentException("Window width must be greater than zero.");
        }
        if (height <= 0) {
            throw new IllegalArgumentException("Window height must be greater than zero.");
        }

        title = Objects.requireNonNull(title, "Window title cannot be null.").trim();
        if (title.isEmpty()) {
            throw new IllegalArgumentException("Window title cannot be blank.");
        }
    }
}
