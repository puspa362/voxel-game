package com.example.voxelgame.game.inventory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ShapelessCraftingRecipe implements CraftingRecipe {
    private final Map<Item, Integer> requiredCounts;
    private final ItemStack result;
    private final int ingredientCount;

    public ShapelessCraftingRecipe(List<Item> ingredients, ItemStack result) {
        Objects.requireNonNull(ingredients, "Recipe ingredients cannot be null.");
        if (ingredients.isEmpty()) {
            throw new IllegalArgumentException("Recipe must have at least one ingredient.");
        }

        this.requiredCounts = new HashMap<>();
        int count = 0;
        for (Item ingredient : ingredients) {
            Item item = Objects.requireNonNull(ingredient, "Shapeless ingredients cannot contain null.");
            requiredCounts.merge(item, 1, Integer::sum);
            count++;
        }
        this.ingredientCount = count;
        this.result = Objects.requireNonNull(result, "Recipe result cannot be null.").copy();
    }

    @Override
    public boolean matches(CraftingGrid grid) {
        Objects.requireNonNull(grid, "Crafting grid cannot be null.");
        if (grid.countOccupiedSlots() != ingredientCount) {
            return false;
        }
        return grid.countItems().equals(requiredCounts);
    }

    @Override
    public boolean consumeIngredients(CraftingGrid grid) {
        if (!matches(grid)) {
            return false;
        }

        Map<Item, Integer> remaining = new HashMap<>(requiredCounts);
        for (int y = 0; y < grid.getHeight(); y++) {
            for (int x = 0; x < grid.getWidth(); x++) {
                ItemStack stack = grid.getSlot(x, y).orElse(null);
                if (stack == null) {
                    continue;
                }
                Item item = stack.getItem();
                int needed = remaining.getOrDefault(item, 0);
                if (needed <= 0) {
                    return false;
                }
                if (!grid.removeOne(x, y, item)) {
                    return false;
                }
                if (needed == 1) {
                    remaining.remove(item);
                } else {
                    remaining.put(item, needed - 1);
                }
            }
        }
        return remaining.isEmpty();
    }

    @Override
    public ItemStack getResult() {
        return result.copy();
    }

    @Override
    public int getIngredientCount() {
        return ingredientCount;
    }
}
