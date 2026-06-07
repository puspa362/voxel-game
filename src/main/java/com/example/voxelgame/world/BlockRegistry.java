package com.example.voxelgame.world;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import com.example.voxelgame.game.inventory.ToolType;

public final class BlockRegistry {
    public static final Block AIR = new Block(
            0,
            "Air",
            BlockType.AIR,
            EnumSet.of(BlockProperty.TRANSPARENT, BlockProperty.REPLACEABLE)
    );

    public static final Block STONE = new Block(
            1,
            "Stone",
            BlockType.TERRAIN,
            EnumSet.of(BlockProperty.SOLID, BlockProperty.COLLIDABLE),
            1.9f,
            ToolType.PICKAXE
    );

    public static final Block DIRT = new Block(
            2,
            "Dirt",
            BlockType.TERRAIN,
            EnumSet.of(BlockProperty.SOLID, BlockProperty.COLLIDABLE),
            0.7f,
            ToolType.SHOVEL
    );

    public static final Block GRASS = new Block(
            3,
            "Grass",
            BlockType.SURFACE,
            EnumSet.of(BlockProperty.SOLID, BlockProperty.COLLIDABLE),
            0.8f,
            ToolType.SHOVEL
    );

    public static final Block OAK_LOG = new Block(
            4,
            "Oak Log",
            BlockType.DECORATION,
            EnumSet.of(BlockProperty.SOLID, BlockProperty.COLLIDABLE),
            1.2f,
            ToolType.AXE
    );

    public static final Block OAK_LEAVES = new Block(
            5,
            "Oak Leaves",
            BlockType.DECORATION,
            EnumSet.of(BlockProperty.SOLID, BlockProperty.COLLIDABLE),
            0.25f,
            ToolType.AXE
    );

    public static final Block WATER = new Block(
            6,
            "Water",
            BlockType.FLUID,
            EnumSet.of(BlockProperty.TRANSPARENT, BlockProperty.REPLACEABLE),
            100.0f,
            null
    );

    public static final Block OAK_PLANKS = new Block(
            7,
            "Oak Planks",
            BlockType.DECORATION,
            EnumSet.of(BlockProperty.SOLID, BlockProperty.COLLIDABLE),
            1.0f,
            ToolType.AXE
    );

    public static final Block CRAFTING_TABLE = new Block(
            8,
            "Crafting Table",
            BlockType.DECORATION,
            EnumSet.of(BlockProperty.SOLID, BlockProperty.COLLIDABLE),
            1.6f,
            ToolType.AXE
    );

    public static final Block FURNACE = new Block(
            9,
            "Furnace",
            BlockType.DECORATION,
            EnumSet.of(BlockProperty.SOLID, BlockProperty.COLLIDABLE),
            2.6f,
            ToolType.PICKAXE
    );

    public static final Block BEDROCK = new Block(
            10,
            "Bedrock",
            BlockType.TERRAIN,
            EnumSet.of(BlockProperty.SOLID, BlockProperty.COLLIDABLE),
            999.0f,
            ToolType.PICKAXE
    );

    public static final Block TORCH = new Block(
            11,
            "Torch",
            BlockType.DECORATION,
            EnumSet.of(BlockProperty.TRANSPARENT, BlockProperty.COLLIDABLE),
            0.1f,
            null,
            14
    );

    public static final Block SAND = new Block(
            12,
            "Sand",
            BlockType.TERRAIN,
            EnumSet.of(BlockProperty.SOLID, BlockProperty.COLLIDABLE),
            0.55f,
            ToolType.SHOVEL
    );

    public static final Block SANDSTONE = new Block(
            13,
            "Sandstone",
            BlockType.TERRAIN,
            EnumSet.of(BlockProperty.SOLID, BlockProperty.COLLIDABLE),
            1.4f,
            ToolType.PICKAXE
    );

    public static final Block SNOW = new Block(
            14,
            "Snow",
            BlockType.SURFACE,
            EnumSet.of(BlockProperty.SOLID, BlockProperty.COLLIDABLE),
            0.35f,
            ToolType.SHOVEL
    );

    public static final Block ICE = new Block(
            15,
            "Ice",
            BlockType.TERRAIN,
            EnumSet.of(BlockProperty.SOLID, BlockProperty.COLLIDABLE, BlockProperty.TRANSPARENT),
            0.6f,
            ToolType.PICKAXE
    );

    public static final Block COAL_ORE = new Block(
            16,
            "Coal Ore",
            BlockType.TERRAIN,
            EnumSet.of(BlockProperty.SOLID, BlockProperty.COLLIDABLE),
            2.2f,
            ToolType.PICKAXE
    );

    public static final Block IRON_ORE = new Block(
            17,
            "Iron Ore",
            BlockType.TERRAIN,
            EnumSet.of(BlockProperty.SOLID, BlockProperty.COLLIDABLE),
            2.7f,
            ToolType.PICKAXE
    );

    public static final Block COPPER_ORE = new Block(
            18,
            "Copper Ore",
            BlockType.TERRAIN,
            EnumSet.of(BlockProperty.SOLID, BlockProperty.COLLIDABLE),
            2.4f,
            ToolType.PICKAXE
    );

    public static final Block GOLD_ORE = new Block(
            19,
            "Gold Ore",
            BlockType.TERRAIN,
            EnumSet.of(BlockProperty.SOLID, BlockProperty.COLLIDABLE),
            3.0f,
            ToolType.PICKAXE
    );

    public static final Block DIAMOND_ORE = new Block(
            20,
            "Diamond Ore",
            BlockType.TERRAIN,
            EnumSet.of(BlockProperty.SOLID, BlockProperty.COLLIDABLE),
            3.4f,
            ToolType.PICKAXE
    );

    public static final Block MOSSY_STONE = new Block(
            21,
            "Mossy Stone",
            BlockType.TERRAIN,
            EnumSet.of(BlockProperty.SOLID, BlockProperty.COLLIDABLE),
            1.9f,
            ToolType.PICKAXE
    );

    public static final Block GRAVEL = new Block(
            22,
            "Gravel",
            BlockType.TERRAIN,
            EnumSet.of(BlockProperty.SOLID, BlockProperty.COLLIDABLE),
            0.65f,
            ToolType.SHOVEL
    );

    public static final Block SPRUCE_PLANKS = planks(23, "Spruce Planks");
    public static final Block BIRCH_PLANKS = planks(24, "Birch Planks");
    public static final Block ACACIA_PLANKS = planks(25, "Acacia Planks");
    public static final Block DARK_OAK_PLANKS = planks(26, "Dark Oak Planks");
    public static final Block FURNACE_ACTIVE = new Block(
            27,
            "Active Furnace",
            BlockType.DECORATION,
            EnumSet.of(BlockProperty.SOLID, BlockProperty.COLLIDABLE),
            2.6f,
            ToolType.PICKAXE
    );
    public static final Block SPRUCE_LOG = log(28, "Spruce Log");
    public static final Block BIRCH_LOG = log(29, "Birch Log");
    public static final Block DARK_OAK_LOG = log(30, "Dark Oak Log");
    public static final Block SPRUCE_LEAVES = leaves(31, "Spruce Leaves");
    public static final Block BIRCH_LEAVES = leaves(32, "Birch Leaves");
    public static final Block DARK_OAK_LEAVES = leaves(33, "Dark Oak Leaves");
    public static final Block OAK_SAPLING = sapling(34, "Oak Sapling");
    public static final Block SPRUCE_SAPLING = sapling(35, "Spruce Sapling");
    public static final Block BIRCH_SAPLING = sapling(36, "Birch Sapling");
    public static final Block DARK_OAK_SAPLING = sapling(37, "Dark Oak Sapling");
    public static final Block DIRT_PATH = new Block(
            38,
            "Dirt Path",
            BlockType.SURFACE,
            EnumSet.of(BlockProperty.SOLID, BlockProperty.COLLIDABLE),
            0.55f,
            ToolType.SHOVEL
    );
    public static final Block FARMLAND = new Block(
            39,
            "Farmland",
            BlockType.SURFACE,
            EnumSet.of(BlockProperty.SOLID, BlockProperty.COLLIDABLE),
            0.55f,
            ToolType.SHOVEL
    );
    public static final Block BED = new Block(
            40,
            "Bed",
            BlockType.DECORATION,
            EnumSet.of(BlockProperty.SOLID, BlockProperty.COLLIDABLE),
            0.35f,
            ToolType.AXE
    );
    public static final Block WORKSTATION = new Block(
            41,
            "Workstation",
            BlockType.DECORATION,
            EnumSet.of(BlockProperty.SOLID, BlockProperty.COLLIDABLE),
            1.1f,
            ToolType.AXE
    );
    public static final Block ROSE_BUSH = plant(42, "Rose Bush");
    public static final Block LAVENDER = plant(43, "Lavender");
    public static final Block DAISY = plant(44, "Daisy");
    public static final Block LILAC = plant(45, "Lilac");
    public static final Block TALL_GRASS = plant(46, "Tall Grass");
    public static final Block SHORT_GRASS = plant(47, "Short Grass");
    public static final Block OAK_STAIRS_NORTH = stair(48, "Oak Stairs North", ToolType.AXE);
    public static final Block OAK_STAIRS_EAST = stair(49, "Oak Stairs East", ToolType.AXE);
    public static final Block OAK_STAIRS_SOUTH = stair(50, "Oak Stairs South", ToolType.AXE);
    public static final Block OAK_STAIRS_WEST = stair(51, "Oak Stairs West", ToolType.AXE);
    public static final Block STONE_STAIRS_NORTH = stair(52, "Stone Stairs North", ToolType.PICKAXE);
    public static final Block STONE_STAIRS_EAST = stair(53, "Stone Stairs East", ToolType.PICKAXE);
    public static final Block STONE_STAIRS_SOUTH = stair(54, "Stone Stairs South", ToolType.PICKAXE);
    public static final Block STONE_STAIRS_WEST = stair(55, "Stone Stairs West", ToolType.PICKAXE);
    public static final Block OAK_FENCE = decorativeCollidable(56, "Oak Fence", 0.8f, ToolType.AXE);
    public static final Block OAK_FENCE_GATE_CLOSED_NORTH = decorativeCollidable(57, "Oak Fence Gate Closed North", 0.9f, ToolType.AXE);
    public static final Block OAK_FENCE_GATE_CLOSED_EAST = decorativeCollidable(58, "Oak Fence Gate Closed East", 0.9f, ToolType.AXE);
    public static final Block OAK_FENCE_GATE_OPEN_NORTH = decorativeOpen(59, "Oak Fence Gate Open North", 0.9f, ToolType.AXE);
    public static final Block OAK_FENCE_GATE_OPEN_EAST = decorativeOpen(60, "Oak Fence Gate Open East", 0.9f, ToolType.AXE);
    public static final Block OAK_DOOR_LOWER_CLOSED_NORTH = decorativeCollidable(61, "Oak Door Lower Closed North", 0.7f, ToolType.AXE);
    public static final Block OAK_DOOR_UPPER_CLOSED_NORTH = decorativeCollidable(62, "Oak Door Upper Closed North", 0.7f, ToolType.AXE);
    public static final Block OAK_DOOR_LOWER_CLOSED_EAST = decorativeCollidable(63, "Oak Door Lower Closed East", 0.7f, ToolType.AXE);
    public static final Block OAK_DOOR_UPPER_CLOSED_EAST = decorativeCollidable(64, "Oak Door Upper Closed East", 0.7f, ToolType.AXE);
    public static final Block OAK_DOOR_LOWER_OPEN_NORTH = decorativeOpen(65, "Oak Door Lower Open North", 0.7f, ToolType.AXE);
    public static final Block OAK_DOOR_UPPER_OPEN_NORTH = decorativeOpen(66, "Oak Door Upper Open North", 0.7f, ToolType.AXE);
    public static final Block OAK_DOOR_LOWER_OPEN_EAST = decorativeOpen(67, "Oak Door Lower Open East", 0.7f, ToolType.AXE);
    public static final Block OAK_DOOR_UPPER_OPEN_EAST = decorativeOpen(68, "Oak Door Upper Open East", 0.7f, ToolType.AXE);
    public static final Block LAMP = lightBlock(69, "Lamp", 15);
    public static final Block LANTERN = lightBlock(70, "Lantern", 13);
    public static final Block BELL = decorativeCollidable(71, "Bell", 0.7f, ToolType.PICKAXE);

    private static final Block[] BLOCKS_BY_ID = new Block[256];

    static {
        register(AIR);
        register(STONE);
        register(DIRT);
        register(GRASS);
        register(OAK_LOG);
        register(OAK_LEAVES);
        register(WATER);
        register(OAK_PLANKS);
        register(CRAFTING_TABLE);
        register(FURNACE);
        register(BEDROCK);
        register(TORCH);
        register(SAND);
        register(SANDSTONE);
        register(SNOW);
        register(ICE);
        register(COAL_ORE);
        register(IRON_ORE);
        register(COPPER_ORE);
        register(GOLD_ORE);
        register(DIAMOND_ORE);
        register(MOSSY_STONE);
        register(GRAVEL);
        register(SPRUCE_PLANKS);
        register(BIRCH_PLANKS);
        register(ACACIA_PLANKS);
        register(DARK_OAK_PLANKS);
        register(FURNACE_ACTIVE);
        register(SPRUCE_LOG);
        register(BIRCH_LOG);
        register(DARK_OAK_LOG);
        register(SPRUCE_LEAVES);
        register(BIRCH_LEAVES);
        register(DARK_OAK_LEAVES);
        register(OAK_SAPLING);
        register(SPRUCE_SAPLING);
        register(BIRCH_SAPLING);
        register(DARK_OAK_SAPLING);
        register(DIRT_PATH);
        register(FARMLAND);
        register(BED);
        register(WORKSTATION);
        register(ROSE_BUSH);
        register(LAVENDER);
        register(DAISY);
        register(LILAC);
        register(TALL_GRASS);
        register(SHORT_GRASS);
        register(OAK_STAIRS_NORTH);
        register(OAK_STAIRS_EAST);
        register(OAK_STAIRS_SOUTH);
        register(OAK_STAIRS_WEST);
        register(STONE_STAIRS_NORTH);
        register(STONE_STAIRS_EAST);
        register(STONE_STAIRS_SOUTH);
        register(STONE_STAIRS_WEST);
        register(OAK_FENCE);
        register(OAK_FENCE_GATE_CLOSED_NORTH);
        register(OAK_FENCE_GATE_CLOSED_EAST);
        register(OAK_FENCE_GATE_OPEN_NORTH);
        register(OAK_FENCE_GATE_OPEN_EAST);
        register(OAK_DOOR_LOWER_CLOSED_NORTH);
        register(OAK_DOOR_UPPER_CLOSED_NORTH);
        register(OAK_DOOR_LOWER_CLOSED_EAST);
        register(OAK_DOOR_UPPER_CLOSED_EAST);
        register(OAK_DOOR_LOWER_OPEN_NORTH);
        register(OAK_DOOR_UPPER_OPEN_NORTH);
        register(OAK_DOOR_LOWER_OPEN_EAST);
        register(OAK_DOOR_UPPER_OPEN_EAST);
        register(LAMP);
        register(LANTERN);
        register(BELL);
    }

    private BlockRegistry() {
    }

    public static Block get(short id) {
        int index = Short.toUnsignedInt(id);
        if (index >= BLOCKS_BY_ID.length || BLOCKS_BY_ID[index] == null) {
            throw new IllegalArgumentException("Unknown block id: " + index);
        }
        return BLOCKS_BY_ID[index];
    }

    public static Block get(int id) {
        if (id < 0 || id >= BLOCKS_BY_ID.length || BLOCKS_BY_ID[id] == null) {
            throw new IllegalArgumentException("Unknown block id: " + id);
        }
        return BLOCKS_BY_ID[id];
    }

    public static boolean isRegistered(short id) {
        int index = Short.toUnsignedInt(id);
        return index < BLOCKS_BY_ID.length && BLOCKS_BY_ID[index] != null;
    }

    public static List<Block> all() {
        return Arrays.stream(BLOCKS_BY_ID)
                .filter(block -> block != null)
                .toList();
    }

    public static boolean isFurnace(Block block) {
        return block == FURNACE || block == FURNACE_ACTIVE;
    }

    public static boolean isFurnaceId(short blockId) {
        return blockId == FURNACE.getId() || blockId == FURNACE_ACTIVE.getId();
    }

    public static boolean isDecorativePlant(Block block) {
        return block == ROSE_BUSH || block == LAVENDER || block == DAISY
                || block == LILAC || block == TALL_GRASS || block == SHORT_GRASS
                || block == OAK_SAPLING || block == SPRUCE_SAPLING
                || block == BIRCH_SAPLING || block == DARK_OAK_SAPLING;
    }

    public static boolean isStairs(Block block) {
        return block == OAK_STAIRS_NORTH || block == OAK_STAIRS_EAST
                || block == OAK_STAIRS_SOUTH || block == OAK_STAIRS_WEST
                || block == STONE_STAIRS_NORTH || block == STONE_STAIRS_EAST
                || block == STONE_STAIRS_SOUTH || block == STONE_STAIRS_WEST;
    }

    public static boolean isFence(Block block) {
        return block == OAK_FENCE;
    }

    public static boolean isFenceGate(Block block) {
        return block == OAK_FENCE_GATE_CLOSED_NORTH || block == OAK_FENCE_GATE_CLOSED_EAST
                || block == OAK_FENCE_GATE_OPEN_NORTH || block == OAK_FENCE_GATE_OPEN_EAST;
    }

    public static boolean isDoor(Block block) {
        return isDoorLower(block) || isDoorUpper(block);
    }

    public static boolean isDoorLower(Block block) {
        return block == OAK_DOOR_LOWER_CLOSED_NORTH || block == OAK_DOOR_LOWER_CLOSED_EAST
                || block == OAK_DOOR_LOWER_OPEN_NORTH || block == OAK_DOOR_LOWER_OPEN_EAST;
    }

    public static boolean isDoorUpper(Block block) {
        return block == OAK_DOOR_UPPER_CLOSED_NORTH || block == OAK_DOOR_UPPER_CLOSED_EAST
                || block == OAK_DOOR_UPPER_OPEN_NORTH || block == OAK_DOOR_UPPER_OPEN_EAST;
    }

    public static boolean isLightDecoration(Block block) {
        return block == TORCH || block == LAMP || block == LANTERN;
    }

    public static boolean usesCustomMesh(Block block) {
        return isDecorativePlant(block) || isStairs(block) || isFence(block)
                || isFenceGate(block) || isDoor(block) || isLightDecoration(block)
                || block == BED || block == BELL;
    }

    private static void register(Block block) {
        int index = block.getNumericId();
        if (BLOCKS_BY_ID[index] != null) {
            throw new IllegalStateException("Duplicate block id registration: " + index);
        }
        BLOCKS_BY_ID[index] = block;
    }

    private static Block planks(int id, String name) {
        return new Block(
                id,
                name,
                BlockType.DECORATION,
                EnumSet.of(BlockProperty.SOLID, BlockProperty.COLLIDABLE),
                1.0f,
                ToolType.AXE
        );
    }

    private static Block log(int id, String name) {
        return new Block(
                id,
                name,
                BlockType.DECORATION,
                EnumSet.of(BlockProperty.SOLID, BlockProperty.COLLIDABLE),
                1.2f,
                ToolType.AXE
        );
    }

    private static Block leaves(int id, String name) {
        return new Block(
                id,
                name,
                BlockType.DECORATION,
                EnumSet.of(BlockProperty.SOLID, BlockProperty.COLLIDABLE),
                0.25f,
                ToolType.AXE
        );
    }

    private static Block sapling(int id, String name) {
        return new Block(
                id,
                name,
                BlockType.DECORATION,
                EnumSet.of(BlockProperty.TRANSPARENT, BlockProperty.REPLACEABLE),
                0.05f,
                null
        );
    }

    private static Block plant(int id, String name) {
        return new Block(
                id,
                name,
                BlockType.DECORATION,
                EnumSet.of(BlockProperty.TRANSPARENT, BlockProperty.REPLACEABLE),
                0.02f,
                null
        );
    }

    private static Block stair(int id, String name, ToolType toolType) {
        return new Block(
                id,
                name,
                BlockType.DECORATION,
                EnumSet.of(BlockProperty.TRANSPARENT, BlockProperty.COLLIDABLE),
                1.0f,
                toolType
        );
    }

    private static Block decorativeCollidable(int id, String name, float hardness, ToolType toolType) {
        return new Block(
                id,
                name,
                BlockType.DECORATION,
                EnumSet.of(BlockProperty.TRANSPARENT, BlockProperty.COLLIDABLE),
                hardness,
                toolType
        );
    }

    private static Block decorativeOpen(int id, String name, float hardness, ToolType toolType) {
        return new Block(
                id,
                name,
                BlockType.DECORATION,
                EnumSet.of(BlockProperty.TRANSPARENT),
                hardness,
                toolType
        );
    }

    private static Block lightBlock(int id, String name, int lightLevel) {
        return new Block(
                id,
                name,
                BlockType.DECORATION,
                EnumSet.of(BlockProperty.TRANSPARENT),
                0.25f,
                null,
                lightLevel
        );
    }
}
