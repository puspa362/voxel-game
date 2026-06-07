package com.example.voxelgame.game.ui;

import com.example.voxelgame.game.inventory.Inventory;
import java.util.Objects;

public final class InventoryUI {
    public String title() {
        return "Inventory";
    }

    public Inventory craftingInventory(Inventory playerInventory) {
        return Objects.requireNonNull(playerInventory, "Player inventory cannot be null.");
    }
}
