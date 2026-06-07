package com.example.voxelgame.game.inventory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class CraftingRecipes {
    private static final CraftingManager DEFAULT_MANAGER = new CraftingManager(buildRecipes());

    private CraftingRecipes() {
    }

    public static CraftingManager defaultManager() {
        return DEFAULT_MANAGER;
    }

    private static List<CraftingRecipe> buildRecipes() {
        RecipeRegistry registry = new RecipeRegistry();

        registry.shapeless(output(Items.OAK_PLANKS, 4), Items.OAK_LOG_BLOCK);
        registry.shapeless(output(Items.SPRUCE_PLANKS, 4), Items.SPRUCE_LOG_BLOCK);
        registry.shapeless(output(Items.BIRCH_PLANKS, 4), Items.BIRCH_LOG_BLOCK);
        registry.shapeless(output(Items.DARK_OAK_PLANKS, 4), Items.DARK_OAK_LOG_BLOCK);
        registry.shaped(
                output(Items.STICK, 4),
                Map.of('P', Items.OAK_PLANKS),
                "P",
                "P"
        );
        registry.shaped(
                output(Items.CRAFTING_TABLE, 1),
                Map.of('P', Items.OAK_PLANKS),
                "PP",
                "PP"
        );
        registry.shaped(
                output(Items.FURNACE_BLOCK, 1),
                Map.of('S', Items.STONE_BLOCK),
                "SSS",
                "S S",
                "SSS"
        );
        registry.shaped(
                output(Items.TORCH_BLOCK, 4),
                Map.of('C', Items.COAL, 'S', Items.STICK),
                "C",
                "S"
        );
        registry.shaped(
                output(Items.TORCH_BLOCK, 4),
                Map.of('C', Items.CHARCOAL, 'S', Items.STICK),
                "C",
                "S"
        );

        registerToolSet(registry, Items.OAK_PLANKS, ToolMaterial.WOOD);
        registerToolSet(registry, Items.STONE_BLOCK, ToolMaterial.STONE);
        registerToolSet(registry, Items.IRON_INGOT, ToolMaterial.IRON);

        registry.shaped(
                output(Items.BREAD, 1),
                Map.of('W', Items.WHEAT),
                "WWW"
        );
        registry.shaped(
                output(Items.OAK_BOAT, 1),
                Map.of('P', Items.OAK_PLANKS),
                "P P",
                "PPP"
        );
        registry.shaped(
                output(Items.SPRUCE_BOAT, 1),
                Map.of('P', Items.SPRUCE_PLANKS),
                "P P",
                "PPP"
        );

        return registry.recipes();
    }

    private static void registerToolSet(RecipeRegistry registry, Item material, ToolMaterial toolMaterial) {
        registry.shaped(
                output(toolMaterial.pickaxe(), 1),
                Map.of('M', material, 'S', Items.STICK),
                "MMM",
                " S ",
                " S "
        );
        registry.shaped(
                output(toolMaterial.axe(), 1),
                Map.of('M', material, 'S', Items.STICK),
                "MM",
                "MS",
                " S"
        );
        registry.shaped(
                output(toolMaterial.shovel(), 1),
                Map.of('M', material, 'S', Items.STICK),
                "M",
                "S",
                "S"
        );
        if (toolMaterial.sword() != null) {
            registry.shaped(
                    output(toolMaterial.sword(), 1),
                    Map.of('M', material, 'S', Items.STICK),
                    "M",
                    "M",
                    "S"
            );
        }
    }

    private static ItemStack output(Item item, int count) {
        return new ItemStack(item, count);
    }

    private enum ToolMaterial {
        WOOD(Items.WOODEN_PICKAXE, Items.WOODEN_AXE, Items.WOODEN_SHOVEL, Items.WOODEN_SWORD),
        STONE(Items.STONE_PICKAXE, Items.STONE_AXE, Items.STONE_SHOVEL, null),
        IRON(Items.IRON_PICKAXE, Items.IRON_AXE, Items.IRON_SHOVEL, null);

        private final Item pickaxe;
        private final Item axe;
        private final Item shovel;
        private final Item sword;

        ToolMaterial(Item pickaxe, Item axe, Item shovel, Item sword) {
            this.pickaxe = pickaxe;
            this.axe = axe;
            this.shovel = shovel;
            this.sword = sword;
        }

        private Item pickaxe() {
            return pickaxe;
        }

        private Item axe() {
            return axe;
        }

        private Item shovel() {
            return shovel;
        }

        private Item sword() {
            return sword;
        }
    }

    private static final class RecipeRegistry {
        private final List<CraftingRecipe> recipes = new ArrayList<>();

        private void shaped(ItemStack result, Map<Character, Item> keys, String... pattern) {
            Objects.requireNonNull(result, "Recipe result cannot be null.");
            Objects.requireNonNull(keys, "Recipe keys cannot be null.");
            if (pattern.length == 0) {
                throw new IllegalArgumentException("Shaped recipe pattern cannot be empty.");
            }

            int width = Arrays.stream(pattern)
                    .mapToInt(String::length)
                    .max()
                    .orElseThrow();
            if (width == 0) {
                throw new IllegalArgumentException("Shaped recipe pattern rows cannot be empty.");
            }
            int height = pattern.length;
            Item[] ingredients = new Item[width * height];

            for (int y = 0; y < height; y++) {
                String row = pattern[y];
                for (int x = 0; x < width; x++) {
                    char symbol = x < row.length() ? row.charAt(x) : ' ';
                    if (symbol == ' ') {
                        continue;
                    }
                    Item item = keys.get(symbol);
                    if (item == null) {
                        throw new IllegalArgumentException("Missing recipe key for symbol: " + symbol);
                    }
                    ingredients[x + y * width] = item;
                }
            }

            recipes.add(new ShapedCraftingRecipe(width, height, ingredients, result));
        }

        private void shapeless(ItemStack result, Item... ingredients) {
            Objects.requireNonNull(result, "Recipe result cannot be null.");
            recipes.add(new ShapelessCraftingRecipe(List.of(ingredients), result));
        }

        private List<CraftingRecipe> recipes() {
            return List.copyOf(recipes);
        }
    }
}
