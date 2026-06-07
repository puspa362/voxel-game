package com.example.voxelgame.game.furnace;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import com.example.voxelgame.game.inventory.Item;
import com.example.voxelgame.game.inventory.ItemStack;
import com.example.voxelgame.game.inventory.Items;

public final class FurnaceRecipes {
    public static final int DEFAULT_COOK_TICKS = 200;
    private static final Map<String, FurnaceRecipe> RECIPES = new LinkedHashMap<>();

    static {
        register(Items.IRON_ORE_BLOCK, Items.IRON_INGOT);
        register(Items.GOLD_ORE_BLOCK, Items.GOLD_INGOT);
        register(Items.COAL_ORE_BLOCK, Items.COAL);
        register(Items.RAW_IRON, Items.IRON_INGOT);
        register(Items.RAW_GOLD, Items.GOLD_INGOT);
        register(Items.SAND_BLOCK, Items.GLASS);
        register(Items.OAK_LOG_BLOCK, Items.CHARCOAL);
        register(Items.SPRUCE_LOG_BLOCK, Items.CHARCOAL);
        register(Items.BIRCH_LOG_BLOCK, Items.CHARCOAL);
        register(Items.DARK_OAK_LOG_BLOCK, Items.CHARCOAL);
        register(Items.RAW_PORK, Items.COOKED_PORK);
        register(Items.RAW_MUTTON, Items.COOKED_MUTTON);
        register(Items.RAW_CHICKEN, Items.COOKED_CHICKEN);
    }

    private FurnaceRecipes() {
    }

    public static Optional<FurnaceRecipe> find(Item input) {
        return input == null ? Optional.empty() : Optional.ofNullable(RECIPES.get(input.getId()));
    }

    private static void register(Item input, Item output) {
        RECIPES.put(input.getId(), new FurnaceRecipe(input, new ItemStack(output, 1), DEFAULT_COOK_TICKS));
    }
}
