package com.example.voxelgame.game.mode;

import java.util.Locale;
import java.util.Optional;

public enum GameMode {
    SURVIVAL("survival", "Survival"),
    CREATIVE("creative", "Creative");

    private final String id;
    private final String displayName;

    GameMode(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public static Optional<GameMode> byId(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        for (GameMode mode : values()) {
            if (mode.id.equals(normalized) || mode.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                return Optional.of(mode);
            }
        }
        return Optional.empty();
    }
}
