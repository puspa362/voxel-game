package com.example.voxelgame.core;

import java.util.Objects;

public final class FrameStatistics {
    private final String baseTitle;
    private int frames;
    private int updates;
    private int lastFramesPerSecond;
    private int lastUpdatesPerSecond;
    private double accumulatorSeconds;

    public FrameStatistics(String baseTitle) {
        this.baseTitle = Objects.requireNonNull(baseTitle, "Base title cannot be null.");
    }

    public void recordUpdate() {
        updates++;
    }

    public void recordFrame(double frameTimeSeconds) {
        frames++;
        accumulatorSeconds += frameTimeSeconds;

        if (accumulatorSeconds < 1.0) {
            return;
        }

        lastFramesPerSecond = frames;
        lastUpdatesPerSecond = updates;
        accumulatorSeconds -= 1.0;
        frames = 0;
        updates = 0;
    }

    public String formatDebugTitle(String suffix) {
        return "%s | %d FPS | %d UPS | %s".formatted(baseTitle, lastFramesPerSecond, lastUpdatesPerSecond, suffix);
    }
}
