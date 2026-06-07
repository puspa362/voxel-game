package com.example.voxelgame.game.entity;

import com.example.voxelgame.game.Player;
import com.example.voxelgame.world.VoxelWorld;
import com.example.voxelgame.world.WorldTime;
import java.util.Objects;

public record EntityUpdateContext(VoxelWorld world, Player player, WorldTime worldTime) {
    public EntityUpdateContext(VoxelWorld world, Player player) {
        this(world, player, null);
    }

    public EntityUpdateContext {
        Objects.requireNonNull(world, "Voxel world cannot be null.");
        Objects.requireNonNull(player, "Player cannot be null.");
    }
}
