package com.example.voxelgame.game.entity.villager;

import java.util.Objects;

import com.example.voxelgame.game.inventory.Inventory;
import com.example.voxelgame.game.inventory.ItemStack;

public record TradeOffer(String id, ItemStack cost, ItemStack result, int maxUses, int uses) {
    public TradeOffer {
        Objects.requireNonNull(id, "Trade id cannot be null.");
        cost = Objects.requireNonNull(cost, "Trade cost cannot be null.").copy();
        result = Objects.requireNonNull(result, "Trade result cannot be null.").copy();
        if (maxUses < 1) {
            throw new IllegalArgumentException("Trade max uses must be positive.");
        }
        if (uses < 0 || uses > maxUses) {
            throw new IllegalArgumentException("Trade uses must be within max uses.");
        }
    }

    public boolean canExecute(Inventory inventory) {
        return uses < maxUses
                && inventory.countItem(cost.getItem()) >= cost.getCount()
                && inventory.canAccept(result);
    }

    public TradeOffer execute(Inventory inventory) {
        if (!canExecute(inventory)) {
            return this;
        }
        inventory.removeItem(cost.getItem(), cost.getCount());
        inventory.addItem(result);
        return new TradeOffer(id, cost, result, maxUses, uses + 1);
    }
}
