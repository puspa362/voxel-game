package com.example.voxelgame.game.ui;

import com.example.voxelgame.game.inventory.Inventory;

public final class CraftingUI {
    private final Inventory craftingInventory = new Inventory(1, 1, 3, 3);

    public String title() {
        return "Crafting Table";
    }

    public Inventory craftingInventory() {
        return craftingInventory;
    }
}
