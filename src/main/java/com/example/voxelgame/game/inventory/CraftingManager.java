package com.example.voxelgame.game.inventory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public final class CraftingManager {
    private final List<CraftingRecipe> recipes;
    private final Map<Integer, List<CraftingRecipe>> recipesByIngredientCount;

    public CraftingManager(List<CraftingRecipe> recipes) {
        this.recipes = List.copyOf(Objects.requireNonNull(recipes, "Crafting recipes cannot be null."));
        this.recipesByIngredientCount = this.recipes.stream()
                .collect(Collectors.groupingBy(CraftingRecipe::getIngredientCount));
    }

    public Optional<CraftingRecipe> findMatch(CraftingGrid grid) {
        Objects.requireNonNull(grid, "Crafting grid cannot be null.");
        List<CraftingRecipe> candidates = recipesByIngredientCount.get(grid.countOccupiedSlots());
        if (candidates == null || candidates.isEmpty()) {
            return Optional.empty();
        }
        return candidates.stream().filter(recipe -> recipe.matches(grid)).findFirst();
    }
}
