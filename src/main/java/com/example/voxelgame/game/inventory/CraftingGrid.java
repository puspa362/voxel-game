package com.example.voxelgame.game.inventory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class CraftingGrid {
    private final int width;
    private final int height;
    private final ItemStack[] slots;

    public CraftingGrid(int width, int height) {
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException("Crafting grid dimensions must be positive.");
        }

        this.width = width;
        this.height = height;
        this.slots = new ItemStack[width * height];
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Optional<ItemStack> getSlot(int x, int y) {
        int index = indexOf(x, y);
        ItemStack stack = slots[index];
        return stack == null ? Optional.empty() : Optional.of(stack.copy());
    }

    public void setSlot(int x, int y, ItemStack stack) {
        slots[indexOf(x, y)] = stack == null ? null : stack.copy();
    }

    public void clear() {
        Arrays.fill(slots, null);
    }

    public ItemStack[] copyContents() {
        ItemStack[] copy = new ItemStack[slots.length];
        for (int i = 0; i < slots.length; i++) {
            copy[i] = slots[i] == null ? null : slots[i].copy();
        }
        return copy;
    }

    public boolean consumeIngredients(CraftingRecipe recipe) {
        Objects.requireNonNull(recipe, "Crafting recipe cannot be null.");
        return recipe.consumeIngredients(this);
    }

    public boolean removeOne(int x, int y, Item expectedItem) {
        Objects.requireNonNull(expectedItem, "Expected item cannot be null.");
        int index = indexOf(x, y);
        ItemStack stack = slots[index];
        if (stack == null || !stack.getItem().isStackableWith(expectedItem)) {
            return false;
        }
        stack.remove(1);
        slots[index] = stack.isEmpty() ? null : stack;
        return true;
    }

    public int countOccupiedSlots() {
        int count = 0;
        for (ItemStack stack : slots) {
            if (stack != null && !stack.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    public Map<Item, Integer> countItems() {
        Map<Item, Integer> counts = new HashMap<>();
        for (ItemStack stack : slots) {
            if (stack != null && !stack.isEmpty()) {
                counts.merge(stack.getItem(), 1, Integer::sum);
            }
        }
        return counts;
    }

    private int indexOf(int x, int y) {
        if (x < 0 || x >= width) {
            throw new IllegalArgumentException("Crafting grid X out of bounds: " + x);
        }
        if (y < 0 || y >= height) {
            throw new IllegalArgumentException("Crafting grid Y out of bounds: " + y);
        }
        return x + y * width;
    }
}
