package com.example.voxelgame.game.inventory;

import java.util.Objects;

public final class ItemStack {
    private final Item item;
    private int count;

    public ItemStack(Item item, int count) {
        this.item = Objects.requireNonNull(item, "Item cannot be null.");
        if (count < 1 || count > item.getMaxStackSize()) {
            throw new IllegalArgumentException("Item stack count must be within the item's stack limits.");
        }
        this.count = count;
    }

    public Item getItem() {
        return item;
    }

    public int getCount() {
        return count;
    }

    public int getRemainingCapacity() {
        return item.getMaxStackSize() - count;
    }

    public boolean isFull() {
        return count >= item.getMaxStackSize();
    }

    public boolean canMerge(ItemStack other) {
        return other != null && item.isStackableWith(other.item);
    }

    public int add(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Cannot add a negative amount to an item stack.");
        }

        int accepted = Math.min(amount, getRemainingCapacity());
        count += accepted;
        return accepted;
    }

    public int remove(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Cannot remove a negative amount from an item stack.");
        }

        int removed = Math.min(amount, count);
        count -= removed;
        return removed;
    }

    public boolean isEmpty() {
        return count <= 0;
    }

    public ItemStack copy() {
        return new ItemStack(item, count);
    }
}
