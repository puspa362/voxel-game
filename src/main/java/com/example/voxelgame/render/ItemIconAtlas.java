package com.example.voxelgame.render;

import com.example.voxelgame.game.inventory.ItemStack;

public final class ItemIconAtlas implements AutoCloseable {
    private final TextureManager textureManager = new TextureManager();

    public void bind(int textureUnit) {
        textureManager.bind(textureUnit);
    }

    public float[] getUvBounds(ItemStack stack) {
        Tile tile = tileFor(stack);
        return textureManager.getUV(tile.tileX, tile.tileY);
    }

    public float[] getHudIconUv(HudIcon icon) {
        return textureManager.getUV(icon.tileX, icon.tileY);
    }

    public float[] getTileUv(int tileX, int tileY) {
        return textureManager.getUV(tileX, tileY);
    }

    @Override
    public void close() {
        textureManager.close();
    }

    private Tile tileFor(ItemStack stack) {
        String itemId = stack.getItem().getId();
        return switch (itemId) {
            case "dirt_block" -> Tile.DIRT;
            case "grass_block" -> Tile.GRASS;
            case "stone_block" -> Tile.STONE;
            case "oak_log_block" -> Tile.OAK_LOG;
            case "spruce_log_block" -> Tile.SPRUCE_LOG;
            case "birch_log_block" -> Tile.BIRCH_LOG;
            case "dark_oak_log_block" -> Tile.DARK_OAK_LOG;
            case "oak_leaves_block" -> Tile.OAK_LEAVES;
            case "spruce_leaves_block" -> Tile.SPRUCE_LEAVES;
            case "birch_leaves_block" -> Tile.BIRCH_LEAVES;
            case "dark_oak_leaves_block" -> Tile.DARK_OAK_LEAVES;
            case "oak_planks_block" -> Tile.OAK_PLANKS;
            case "spruce_planks_block", "spruce_planks" -> Tile.SPRUCE_PLANKS;
            case "birch_planks_block", "birch_planks" -> Tile.BIRCH_PLANKS;
            case "acacia_planks_block", "acacia_planks" -> Tile.ACACIA_PLANKS;
            case "dark_oak_planks_block", "dark_oak_planks" -> Tile.DARK_OAK_PLANKS;
            case "crafting_table_block" -> Tile.CRAFTING_TABLE;
            case "furnace_block" -> Tile.FURNACE;
            case "sand_block" -> Tile.SAND;
            case "sandstone_block" -> Tile.SANDSTONE;
            case "snow_block" -> Tile.SNOW;
            case "ice_block" -> Tile.ICE;
            case "coal_ore_block" -> Tile.COAL_ORE;
            case "iron_ore_block" -> Tile.IRON_ORE;
            case "copper_ore_block" -> Tile.COPPER_ORE;
            case "gold_ore_block" -> Tile.GOLD_ORE;
            case "diamond_ore_block" -> Tile.DIAMOND_ORE;
            case "mossy_stone_block" -> Tile.MOSSY_STONE;
            case "gravel_block" -> Tile.GRAVEL;
            case "wooden_pickaxe" -> Tile.WOODEN_PICKAXE;
            case "wooden_axe" -> Tile.WOODEN_AXE;
            case "wooden_shovel" -> Tile.WOODEN_SHOVEL;
            case "wooden_sword" -> Tile.WOODEN_SWORD;
            case "stone_pickaxe" -> Tile.STONE_PICKAXE;
            case "stone_axe" -> Tile.STONE_AXE;
            case "stone_shovel" -> Tile.STONE_SHOVEL;
            case "iron_pickaxe" -> Tile.IRON_PICKAXE;
            case "iron_axe" -> Tile.IRON_AXE;
            case "iron_shovel" -> Tile.IRON_SHOVEL;
            case "oak_planks" -> Tile.OAK_PLANKS;
            case "crafting_table" -> Tile.CRAFTING_TABLE;
            case "apple" -> Tile.APPLE;
            case "stick" -> Tile.STICK;
            case "coal" -> Tile.COAL;
            case "charcoal" -> Tile.CHARCOAL;
            case "raw_iron" -> Tile.RAW_IRON;
            case "raw_copper" -> Tile.RAW_COPPER;
            case "raw_gold" -> Tile.RAW_GOLD;
            case "iron_ingot" -> Tile.IRON_INGOT;
            case "gold_ingot" -> Tile.GOLD_INGOT;
            case "diamond" -> Tile.DIAMOND;
            case "glass" -> Tile.GLASS;
            case "oak_sapling" -> Tile.OAK_SAPLING;
            case "spruce_sapling" -> Tile.SPRUCE_SAPLING;
            case "birch_sapling" -> Tile.BIRCH_SAPLING;
            case "dark_oak_sapling" -> Tile.DARK_OAK_SAPLING;
            case "oak_boat" -> Tile.OAK_BOAT;
            case "spruce_boat" -> Tile.SPRUCE_BOAT;
            case "wheat_seeds" -> Tile.WHEAT_SEEDS;
            case "wheat" -> Tile.WHEAT;
            case "bread" -> Tile.BREAD;
            case "feather" -> Tile.FEATHER;
            case "leather" -> Tile.LEATHER;
            case "raw_pork" -> Tile.RAW_PORK;
            case "cooked_pork" -> Tile.COOKED_PORK;
            case "raw_mutton" -> Tile.RAW_MUTTON;
            case "cooked_mutton" -> Tile.COOKED_MUTTON;
            case "raw_chicken" -> Tile.RAW_CHICKEN;
            case "cooked_chicken" -> Tile.COOKED_CHICKEN;
            default -> Tile.GENERIC;
        };
    }

    private enum Tile {
        DIRT(0, 3),
        GRASS(1, 3),
        STONE(2, 3),
        OAK_LOG(3, 3),
        OAK_LEAVES(4, 3),
        OAK_PLANKS(5, 3),
        CRAFTING_TABLE(6, 3),
        FURNACE(7, 3),
        APPLE(8, 3),
        SAND(9, 3),
        SANDSTONE(10, 3),
        SNOW(11, 3),
        ICE(12, 3),
        COAL_ORE(13, 3),
        IRON_ORE(14, 3),
        COPPER_ORE(15, 3),
        WOODEN_PICKAXE(0, 4),
        WOODEN_AXE(1, 4),
        WOODEN_SHOVEL(2, 4),
        WOODEN_SWORD(3, 4),
        GOLD_ORE(9, 4),
        DIAMOND_ORE(10, 4),
        MOSSY_STONE(11, 4),
        GRAVEL(12, 4),
        SPRUCE_PLANKS(13, 4),
        BIRCH_PLANKS(14, 4),
        ACACIA_PLANKS(15, 4),
        STONE_PICKAXE(0, 5),
        STONE_AXE(1, 5),
        STONE_SHOVEL(2, 5),
        STICK(3, 5),
        COAL(4, 5),
        CHARCOAL(5, 5),
        RAW_IRON(6, 5),
        RAW_COPPER(7, 5),
        RAW_GOLD(8, 5),
        IRON_INGOT(9, 5),
        GOLD_INGOT(10, 5),
        DIAMOND(11, 5),
        DARK_OAK_PLANKS(12, 5),
        IRON_PICKAXE(0, 6),
        IRON_AXE(1, 6),
        IRON_SHOVEL(2, 6),
        OAK_SAPLING(3, 6),
        SPRUCE_SAPLING(4, 6),
        BIRCH_SAPLING(5, 6),
        OAK_BOAT(6, 6),
        SPRUCE_BOAT(7, 6),
        WHEAT_SEEDS(8, 6),
        WHEAT(9, 6),
        BREAD(10, 6),
        FEATHER(11, 6),
        LEATHER(12, 6),
        RAW_PORK(13, 6),
        COOKED_PORK(14, 6),
        RAW_MUTTON(15, 6),
        RAW_CHICKEN(3, 7),
        COOKED_MUTTON(4, 7),
        COOKED_CHICKEN(5, 7),
        GLASS(6, 7),
        SPRUCE_LOG(11, 2),
        BIRCH_LOG(12, 2),
        DARK_OAK_LOG(13, 2),
        SPRUCE_LEAVES(14, 2),
        BIRCH_LEAVES(15, 2),
        DARK_OAK_LEAVES(11, 7),
        DARK_OAK_SAPLING(15, 7),
        GENERIC(2, 7);

        private final int tileX;
        private final int tileY;

        Tile(int tileX, int tileY) {
            this.tileX = tileX;
            this.tileY = tileY;
        }
    }

    public enum HudIcon {
        HEART_FULL(0, 1),
        HEART_HALF(1, 1),
        HEART_EMPTY(2, 1),
        HUNGER_FULL(0, 2),
        HUNGER_HALF(1, 2),
        HUNGER_EMPTY(2, 2);

        private final int tileX;
        private final int tileY;

        HudIcon(int tileX, int tileY) {
            this.tileX = tileX;
            this.tileY = tileY;
        }
    }
}
