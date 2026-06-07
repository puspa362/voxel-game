package com.example.voxelgame.game.inventory;

public interface CraftingRecipe {
    boolean matches(CraftingGrid grid);

    boolean consumeIngredients(CraftingGrid grid);

    ItemStack getResult();

    int getIngredientCount();
}
