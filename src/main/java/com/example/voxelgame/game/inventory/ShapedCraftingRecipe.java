package com.example.voxelgame.game.inventory;

import java.util.Objects;
import java.util.Optional;

public final class ShapedCraftingRecipe implements CraftingRecipe {
    private final int width;
    private final int height;
    private final Item[] ingredients;
    private final ItemStack result;
    private final RecipeBounds bounds;
    private final int ingredientCount;

    public ShapedCraftingRecipe(int width, int height, Item[] ingredients, ItemStack result) {
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException("Recipe dimensions must be positive.");
        }
        Objects.requireNonNull(ingredients, "Recipe ingredients cannot be null.");
        if (ingredients.length != width * height) {
            throw new IllegalArgumentException("Recipe ingredient count must match width * height.");
        }

        this.width = width;
        this.height = height;
        this.ingredients = ingredients.clone();
        this.result = Objects.requireNonNull(result, "Recipe result cannot be null.").copy();
        this.bounds = occupiedRecipeBounds();
        this.ingredientCount = countIngredients();
        if (ingredientCount == 0) {
            throw new IllegalArgumentException("Recipe must have at least one ingredient.");
        }
    }

    @Override
    public boolean matches(CraftingGrid grid) {
        return matchOrigin(grid).isPresent();
    }

    @Override
    public boolean consumeIngredients(CraftingGrid grid) {
        Objects.requireNonNull(grid, "Crafting grid cannot be null.");
        Optional<RecipeMatch> match = matchOrigin(grid);
        if (match.isEmpty()) {
            return false;
        }

        RecipeMatch recipeMatch = match.get();
        for (int y = recipeMatch.minIngredientY(); y <= recipeMatch.maxIngredientY(); y++) {
            for (int x = recipeMatch.minIngredientX(); x <= recipeMatch.maxIngredientX(); x++) {
                Item required = ingredients[indexOf(x, y)];
                if (required == null) {
                    continue;
                }

                int gridX = recipeMatch.originX() + (x - recipeMatch.minIngredientX());
                int gridY = recipeMatch.originY() + (y - recipeMatch.minIngredientY());
                if (!grid.removeOne(gridX, gridY, required)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public ItemStack getResult() {
        return result.copy();
    }

    @Override
    public int getIngredientCount() {
        return ingredientCount;
    }

    public Item getIngredient(int x, int y) {
        return ingredients[indexOf(x, y)];
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    private Optional<RecipeMatch> matchOrigin(CraftingGrid grid) {
        Objects.requireNonNull(grid, "Crafting grid cannot be null.");
        int trimmedWidth = bounds.maxX() - bounds.minX() + 1;
        int trimmedHeight = bounds.maxY() - bounds.minY() + 1;
        if (grid.getWidth() < trimmedWidth || grid.getHeight() < trimmedHeight) {
            return Optional.empty();
        }

        for (int originY = 0; originY <= grid.getHeight() - trimmedHeight; originY++) {
            for (int originX = 0; originX <= grid.getWidth() - trimmedWidth; originX++) {
                if (matchesAt(grid, bounds, originX, originY)) {
                    return Optional.of(new RecipeMatch(originX, originY, bounds.minX(), bounds.minY(), bounds.maxX(), bounds.maxY()));
                }
            }
        }
        return Optional.empty();
    }

    private boolean matchesAt(CraftingGrid grid, RecipeBounds bounds, int originX, int originY) {
        for (int gridY = 0; gridY < grid.getHeight(); gridY++) {
            for (int gridX = 0; gridX < grid.getWidth(); gridX++) {
                int recipeX = bounds.minX() + (gridX - originX);
                int recipeY = bounds.minY() + (gridY - originY);
                boolean insideTrimmedRecipe = recipeX >= bounds.minX()
                        && recipeX <= bounds.maxX()
                        && recipeY >= bounds.minY()
                        && recipeY <= bounds.maxY();

                Item required = insideTrimmedRecipe ? ingredients[indexOf(recipeX, recipeY)] : null;
                ItemStack actual = grid.getSlot(gridX, gridY).orElse(null);
                if (required == null && actual == null) {
                    continue;
                }
                if (required == null || actual == null) {
                    return false;
                }
                if (!actual.getItem().isStackableWith(required) || actual.getCount() < 1) {
                    return false;
                }
            }
        }
        return true;
    }

    private RecipeBounds occupiedRecipeBounds() {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (ingredients[indexOf(x, y)] == null) {
                    continue;
                }
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }

        if (minX == Integer.MAX_VALUE) {
            return new RecipeBounds(0, 0, 0, 0);
        }
        return new RecipeBounds(minX, minY, maxX, maxY);
    }

    private int countIngredients() {
        int count = 0;
        for (Item ingredient : ingredients) {
            if (ingredient != null) {
                count++;
            }
        }
        return count;
    }

    private int indexOf(int x, int y) {
        return x + y * width;
    }

    private record RecipeBounds(int minX, int minY, int maxX, int maxY) {
    }

    private record RecipeMatch(int originX, int originY, int minIngredientX, int minIngredientY, int maxIngredientX, int maxIngredientY) {
    }
}
