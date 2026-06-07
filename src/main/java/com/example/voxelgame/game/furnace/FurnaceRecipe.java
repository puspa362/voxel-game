package com.example.voxelgame.game.furnace;

import com.example.voxelgame.game.inventory.Item;
import com.example.voxelgame.game.inventory.ItemStack;

public record FurnaceRecipe(Item input, ItemStack result, int cookTicks) {
    public FurnaceRecipe {
        if (cookTicks <= 0) {
            throw new IllegalArgumentException("Cook ticks must be positive.");
        }
    }
}
