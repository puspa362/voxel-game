package com.example.voxelgame.game.inventory;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.example.voxelgame.world.BlockRegistry;

public final class Items {
    public static final Item DIRT_BLOCK = new Item("dirt_block", "Dirt", 64, BlockRegistry.DIRT);
    public static final Item GRASS_BLOCK = new Item("grass_block", "Grass", 64, BlockRegistry.GRASS);
    public static final Item STONE_BLOCK = new Item("stone_block", "Stone", 64, BlockRegistry.STONE);
    public static final Item OAK_LOG_BLOCK = new Item("oak_log_block", "Oak Log", 64, BlockRegistry.OAK_LOG);
    public static final Item SPRUCE_LOG_BLOCK = new Item("spruce_log_block", "Spruce Log", 64, BlockRegistry.SPRUCE_LOG);
    public static final Item BIRCH_LOG_BLOCK = new Item("birch_log_block", "Birch Log", 64, BlockRegistry.BIRCH_LOG);
    public static final Item DARK_OAK_LOG_BLOCK = new Item("dark_oak_log_block", "Dark Oak Log", 64, BlockRegistry.DARK_OAK_LOG);
    public static final Item OAK_LEAVES_BLOCK = new Item("oak_leaves_block", "Oak Leaves", 64, BlockRegistry.OAK_LEAVES);
    public static final Item SPRUCE_LEAVES_BLOCK = new Item("spruce_leaves_block", "Spruce Leaves", 64, BlockRegistry.SPRUCE_LEAVES);
    public static final Item BIRCH_LEAVES_BLOCK = new Item("birch_leaves_block", "Birch Leaves", 64, BlockRegistry.BIRCH_LEAVES);
    public static final Item DARK_OAK_LEAVES_BLOCK = new Item("dark_oak_leaves_block", "Dark Oak Leaves", 64, BlockRegistry.DARK_OAK_LEAVES);
    public static final Item OAK_PLANKS_BLOCK = new Item("oak_planks_block", "Oak Planks", 64, BlockRegistry.OAK_PLANKS);
    public static final Item SPRUCE_PLANKS_BLOCK = new Item("spruce_planks_block", "Spruce Planks", 64, BlockRegistry.SPRUCE_PLANKS);
    public static final Item BIRCH_PLANKS_BLOCK = new Item("birch_planks_block", "Birch Planks", 64, BlockRegistry.BIRCH_PLANKS);
    public static final Item ACACIA_PLANKS_BLOCK = new Item("acacia_planks_block", "Acacia Planks", 64, BlockRegistry.ACACIA_PLANKS);
    public static final Item DARK_OAK_PLANKS_BLOCK = new Item("dark_oak_planks_block", "Dark Oak Planks", 64, BlockRegistry.DARK_OAK_PLANKS);
    public static final Item CRAFTING_TABLE_BLOCK = new Item("crafting_table_block", "Crafting Table", 64, BlockRegistry.CRAFTING_TABLE);
    public static final Item FURNACE_BLOCK = new Item("furnace_block", "Furnace", 64, BlockRegistry.FURNACE);
    public static final Item TORCH_BLOCK = new Item("torch_block", "Torch", 64, BlockRegistry.TORCH);
    public static final Item SAND_BLOCK = new Item("sand_block", "Sand", 64, BlockRegistry.SAND);
    public static final Item SANDSTONE_BLOCK = new Item("sandstone_block", "Sandstone", 64, BlockRegistry.SANDSTONE);
    public static final Item SNOW_BLOCK = new Item("snow_block", "Snow", 64, BlockRegistry.SNOW);
    public static final Item ICE_BLOCK = new Item("ice_block", "Ice", 64, BlockRegistry.ICE);
    public static final Item COAL_ORE_BLOCK = new Item("coal_ore_block", "Coal Ore", 64, BlockRegistry.COAL_ORE);
    public static final Item IRON_ORE_BLOCK = new Item("iron_ore_block", "Iron Ore", 64, BlockRegistry.IRON_ORE);
    public static final Item COPPER_ORE_BLOCK = new Item("copper_ore_block", "Copper Ore", 64, BlockRegistry.COPPER_ORE);
    public static final Item GOLD_ORE_BLOCK = new Item("gold_ore_block", "Gold Ore", 64, BlockRegistry.GOLD_ORE);
    public static final Item DIAMOND_ORE_BLOCK = new Item("diamond_ore_block", "Diamond Ore", 64, BlockRegistry.DIAMOND_ORE);
    public static final Item MOSSY_STONE_BLOCK = new Item("mossy_stone_block", "Mossy Stone", 64, BlockRegistry.MOSSY_STONE);
    public static final Item GRAVEL_BLOCK = new Item("gravel_block", "Gravel", 64, BlockRegistry.GRAVEL);
    public static final Item WOODEN_PICKAXE = new Item("wooden_pickaxe", "Wooden Pickaxe", ToolType.PICKAXE, ToolTier.WOOD);
    public static final Item WOODEN_AXE = new Item("wooden_axe", "Wooden Axe", ToolType.AXE, ToolTier.WOOD);
    public static final Item WOODEN_SHOVEL = new Item("wooden_shovel", "Wooden Shovel", ToolType.SHOVEL, ToolTier.WOOD);
    public static final Item WOODEN_SWORD = new Item("wooden_sword", "Wooden Sword", ToolType.AXE, ToolTier.WOOD);
    public static final Item STONE_PICKAXE = new Item("stone_pickaxe", "Stone Pickaxe", ToolType.PICKAXE, ToolTier.STONE);
    public static final Item STONE_AXE = new Item("stone_axe", "Stone Axe", ToolType.AXE, ToolTier.STONE);
    public static final Item STONE_SHOVEL = new Item("stone_shovel", "Stone Shovel", ToolType.SHOVEL, ToolTier.STONE);
    public static final Item IRON_PICKAXE = new Item("iron_pickaxe", "Iron Pickaxe", ToolType.PICKAXE, ToolTier.IRON);
    public static final Item IRON_AXE = new Item("iron_axe", "Iron Axe", ToolType.AXE, ToolTier.IRON);
    public static final Item IRON_SHOVEL = new Item("iron_shovel", "Iron Shovel", ToolType.SHOVEL, ToolTier.IRON);
    public static final Item OAK_PLANKS = OAK_PLANKS_BLOCK;
    public static final Item SPRUCE_PLANKS = SPRUCE_PLANKS_BLOCK;
    public static final Item BIRCH_PLANKS = BIRCH_PLANKS_BLOCK;
    public static final Item ACACIA_PLANKS = ACACIA_PLANKS_BLOCK;
    public static final Item DARK_OAK_PLANKS = DARK_OAK_PLANKS_BLOCK;
    public static final Item CRAFTING_TABLE = CRAFTING_TABLE_BLOCK;
    public static final Item APPLE = new Item("apple", "Apple", 64, 4);
    public static final Item STICK = new Item("stick", "Stick", 64);
    public static final Item COAL = new Item("coal", "Coal", 64);
    public static final Item CHARCOAL = new Item("charcoal", "Charcoal", 64);
    public static final Item RAW_IRON = new Item("raw_iron", "Raw Iron", 64);
    public static final Item RAW_COPPER = new Item("raw_copper", "Raw Copper", 64);
    public static final Item RAW_GOLD = new Item("raw_gold", "Raw Gold", 64);
    public static final Item IRON_INGOT = new Item("iron_ingot", "Iron Ingot", 64);
    public static final Item GOLD_INGOT = new Item("gold_ingot", "Gold Ingot", 64);
    public static final Item DIAMOND = new Item("diamond", "Diamond", 64);
    public static final Item GLASS = new Item("glass", "Glass", 64);
    public static final Item OAK_SAPLING = new Item("oak_sapling", "Oak Sapling", 64, BlockRegistry.OAK_SAPLING);
    public static final Item SPRUCE_SAPLING = new Item("spruce_sapling", "Spruce Sapling", 64, BlockRegistry.SPRUCE_SAPLING);
    public static final Item BIRCH_SAPLING = new Item("birch_sapling", "Birch Sapling", 64, BlockRegistry.BIRCH_SAPLING);
    public static final Item DARK_OAK_SAPLING = new Item("dark_oak_sapling", "Dark Oak Sapling", 64, BlockRegistry.DARK_OAK_SAPLING);
    public static final Item DIRT_PATH_BLOCK = new Item("dirt_path_block", "Dirt Path", 64, BlockRegistry.DIRT_PATH);
    public static final Item FARMLAND_BLOCK = new Item("farmland_block", "Farmland", 64, BlockRegistry.FARMLAND);
    public static final Item BED_BLOCK = new Item("bed_block", "Bed", 64, BlockRegistry.BED);
    public static final Item WORKSTATION_BLOCK = new Item("workstation_block", "Workstation", 64, BlockRegistry.WORKSTATION);
    public static final Item ROSE_BUSH_BLOCK = new Item("rose_bush_block", "Rose Bush", 64, BlockRegistry.ROSE_BUSH);
    public static final Item LAVENDER_BLOCK = new Item("lavender_block", "Lavender", 64, BlockRegistry.LAVENDER);
    public static final Item DAISY_BLOCK = new Item("daisy_block", "Daisy", 64, BlockRegistry.DAISY);
    public static final Item LILAC_BLOCK = new Item("lilac_block", "Lilac", 64, BlockRegistry.LILAC);
    public static final Item TALL_GRASS_BLOCK = new Item("tall_grass_block", "Tall Grass", 64, BlockRegistry.TALL_GRASS);
    public static final Item SHORT_GRASS_BLOCK = new Item("short_grass_block", "Short Grass", 64, BlockRegistry.SHORT_GRASS);
    public static final Item OAK_STAIRS_BLOCK = new Item("oak_stairs_block", "Oak Stairs", 64, BlockRegistry.OAK_STAIRS_NORTH);
    public static final Item STONE_STAIRS_BLOCK = new Item("stone_stairs_block", "Stone Stairs", 64, BlockRegistry.STONE_STAIRS_NORTH);
    public static final Item OAK_FENCE_BLOCK = new Item("oak_fence_block", "Oak Fence", 64, BlockRegistry.OAK_FENCE);
    public static final Item OAK_FENCE_GATE_BLOCK = new Item("oak_fence_gate_block", "Oak Fence Gate", 64, BlockRegistry.OAK_FENCE_GATE_CLOSED_NORTH);
    public static final Item OAK_DOOR_BLOCK = new Item("oak_door_block", "Oak Door", 64, BlockRegistry.OAK_DOOR_LOWER_CLOSED_NORTH);
    public static final Item LAMP_BLOCK = new Item("lamp_block", "Lamp", 64, BlockRegistry.LAMP);
    public static final Item LANTERN_BLOCK = new Item("lantern_block", "Lantern", 64, BlockRegistry.LANTERN);
    public static final Item BELL_BLOCK = new Item("bell_block", "Bell", 64, BlockRegistry.BELL);
    public static final Item OAK_BOAT = new Item("oak_boat", "Oak Boat", 1);
    public static final Item SPRUCE_BOAT = new Item("spruce_boat", "Spruce Boat", 1);
    public static final Item WHEAT_SEEDS = new Item("wheat_seeds", "Wheat Seeds", 64);
    public static final Item WHEAT = new Item("wheat", "Wheat", 64);
    public static final Item BREAD = new Item("bread", "Bread", 64, 5);
    public static final Item FEATHER = new Item("feather", "Feather", 64);
    public static final Item LEATHER = new Item("leather", "Leather", 64);
    public static final Item WOOL = new Item("wool", "Wool", 64);
    public static final Item RAW_PORK = new Item("raw_pork", "Raw Pork", 64, 3);
    public static final Item COOKED_PORK = new Item("cooked_pork", "Cooked Pork", 64, 8);
    public static final Item RAW_MUTTON = new Item("raw_mutton", "Raw Mutton", 64, 2);
    public static final Item COOKED_MUTTON = new Item("cooked_mutton", "Cooked Mutton", 64, 6);
    public static final Item RAW_CHICKEN = new Item("raw_chicken", "Raw Chicken", 64, 2);
    public static final Item COOKED_CHICKEN = new Item("cooked_chicken", "Cooked Chicken", 64, 6);
    public static final Item EMERALD = new Item("emerald", "Emerald", 64);
    private static final Map<String, Item> ITEMS_BY_ID = new LinkedHashMap<>();

    static {
        register(DIRT_BLOCK);
        register(GRASS_BLOCK);
        register(STONE_BLOCK);
        register(OAK_LOG_BLOCK);
        register(SPRUCE_LOG_BLOCK);
        register(BIRCH_LOG_BLOCK);
        register(DARK_OAK_LOG_BLOCK);
        register(OAK_LEAVES_BLOCK);
        register(SPRUCE_LEAVES_BLOCK);
        register(BIRCH_LEAVES_BLOCK);
        register(DARK_OAK_LEAVES_BLOCK);
        register(OAK_PLANKS_BLOCK);
        register(SPRUCE_PLANKS_BLOCK);
        register(BIRCH_PLANKS_BLOCK);
        register(ACACIA_PLANKS_BLOCK);
        register(DARK_OAK_PLANKS_BLOCK);
        register(CRAFTING_TABLE_BLOCK);
        register(FURNACE_BLOCK);
        register(TORCH_BLOCK);
        register(SAND_BLOCK);
        register(SANDSTONE_BLOCK);
        register(SNOW_BLOCK);
        register(ICE_BLOCK);
        register(COAL_ORE_BLOCK);
        register(IRON_ORE_BLOCK);
        register(COPPER_ORE_BLOCK);
        register(GOLD_ORE_BLOCK);
        register(DIAMOND_ORE_BLOCK);
        register(MOSSY_STONE_BLOCK);
        register(GRAVEL_BLOCK);
        register(WOODEN_PICKAXE);
        register(WOODEN_AXE);
        register(WOODEN_SHOVEL);
        register(WOODEN_SWORD);
        register(STONE_PICKAXE);
        register(STONE_AXE);
        register(STONE_SHOVEL);
        register(IRON_PICKAXE);
        register(IRON_AXE);
        register(IRON_SHOVEL);
        register(APPLE);
        register(STICK);
        register(COAL);
        register(CHARCOAL);
        register(RAW_IRON);
        register(RAW_COPPER);
        register(RAW_GOLD);
        register(IRON_INGOT);
        register(GOLD_INGOT);
        register(DIAMOND);
        register(GLASS);
        register(OAK_SAPLING);
        register(SPRUCE_SAPLING);
        register(BIRCH_SAPLING);
        register(DARK_OAK_SAPLING);
        register(DIRT_PATH_BLOCK);
        register(FARMLAND_BLOCK);
        register(BED_BLOCK);
        register(WORKSTATION_BLOCK);
        register(ROSE_BUSH_BLOCK);
        register(LAVENDER_BLOCK);
        register(DAISY_BLOCK);
        register(LILAC_BLOCK);
        register(TALL_GRASS_BLOCK);
        register(SHORT_GRASS_BLOCK);
        register(OAK_STAIRS_BLOCK);
        register(STONE_STAIRS_BLOCK);
        register(OAK_FENCE_BLOCK);
        register(OAK_FENCE_GATE_BLOCK);
        register(OAK_DOOR_BLOCK);
        register(LAMP_BLOCK);
        register(LANTERN_BLOCK);
        register(BELL_BLOCK);
        register(OAK_BOAT);
        register(SPRUCE_BOAT);
        register(WHEAT_SEEDS);
        register(WHEAT);
        register(BREAD);
        register(FEATHER);
        register(LEATHER);
        register(WOOL);
        register(RAW_PORK);
        register(COOKED_PORK);
        register(RAW_MUTTON);
        register(COOKED_MUTTON);
        register(RAW_CHICKEN);
        register(COOKED_CHICKEN);
        register(EMERALD);
        alias("oak_planks", OAK_PLANKS_BLOCK);
        alias("spruce_planks", SPRUCE_PLANKS_BLOCK);
        alias("birch_planks", BIRCH_PLANKS_BLOCK);
        alias("acacia_planks", ACACIA_PLANKS_BLOCK);
        alias("dark_oak_planks", DARK_OAK_PLANKS_BLOCK);
        alias("crafting_table", CRAFTING_TABLE_BLOCK);
    }

    private Items() {
    }

    public static Optional<Item> byId(String id) {
        return Optional.ofNullable(ITEMS_BY_ID.get(id));
    }

    public static List<Item> all() {
        return List.copyOf(new LinkedHashSet<>(ITEMS_BY_ID.values()));
    }

    private static void register(Item item) {
        if (ITEMS_BY_ID.put(item.getId(), item) != null) {
            throw new IllegalStateException("Duplicate item id registration: " + item.getId());
        }
    }

    private static void alias(String id, Item item) {
        if (ITEMS_BY_ID.put(id, item) != null) {
            throw new IllegalStateException("Duplicate item id alias: " + id);
        }
    }
}
