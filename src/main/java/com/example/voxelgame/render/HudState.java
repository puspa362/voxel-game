package com.example.voxelgame.render;

import com.example.voxelgame.world.Block;
import com.example.voxelgame.world.BlockHitResult;
import java.util.Optional;

public record HudState(
        Optional<BlockHitResult> targetBlock,
        Optional<BlockHitResult> breakingBlock,
        float breakingProgress,
        Block placementBlock,
        int loadedChunkCount,
        boolean onGround,
        boolean cursorCaptured,
        boolean debugVisible,
        float currentHealth,
        float maxHealth,
        float currentHunger,
        float maxHunger,
        double damageFlashSeconds,
        String timeText,
        String gameModeText,
        String debugText
) {
}
