package com.example.voxelgame.save;

import com.example.voxelgame.game.inventory.ItemStack;
import com.example.voxelgame.game.mode.GameMode;
import org.joml.Vector3f;

public record PlayerSaveData(
        Vector3f position,
        float health,
        float hunger,
        GameMode gameMode,
        int selectedHotbarSlot,
        ItemStack[] slots
) {
    public PlayerSaveData {
        position = new Vector3f(position);
    }

    @Override
    public Vector3f position() {
        return new Vector3f(position);
    }
}
