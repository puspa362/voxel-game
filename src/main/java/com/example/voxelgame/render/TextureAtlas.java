package com.example.voxelgame.render;

import com.example.voxelgame.world.Block;

public final class TextureAtlas implements AutoCloseable {
    private final TextureManager textureManager = new TextureManager();

    public void bind(int textureUnit) {
        textureManager.bind(textureUnit);
    }

    public float[] getUvBounds(Tile tile) {
        return textureManager.getUV(tile.tileX, tile.tileY);
    }

    public float[] getUvBounds(Block block, int faceIndex) {
        return getUvBounds(tileFor(block, faceIndex));
    }

    public Tile tileFor(Block block, int faceIndex) {
        return switch (block.getNumericId()) {
            case 1 -> Tile.STONE;
            case 2 -> Tile.DIRT;
            case 3 -> faceIndex == 0 ? Tile.GRASS_TOP : Tile.GRASS_SIDE;
            case 4 -> Tile.WOOD_LOG;
            case 5 -> Tile.LEAVES;
            case 6 -> Tile.WATER;
            case 7 -> Tile.WOOD_PLANK;
            case 8 -> switch (faceIndex) {
                case 0 -> Tile.WOOD_PLANK;
                case 1 -> Tile.WOOD_PLANK;
                default -> Tile.WOOD_LOG;
            };
            case 9 -> Tile.FURNACE;
            case 10 -> Tile.BEDROCK;
            case 12 -> Tile.SAND;
            case 13 -> Tile.SANDSTONE;
            case 14 -> Tile.SNOW;
            case 15 -> Tile.ICE;
            case 16 -> Tile.COAL_ORE;
            case 17 -> Tile.IRON_ORE;
            case 18 -> Tile.COPPER_ORE;
            case 19 -> Tile.GOLD_ORE;
            case 20 -> Tile.DIAMOND_ORE;
            case 21 -> Tile.MOSSY_STONE;
            case 22 -> Tile.GRAVEL;
            case 23 -> Tile.SPRUCE_PLANKS;
            case 24 -> Tile.BIRCH_PLANKS;
            case 25 -> Tile.ACACIA_PLANKS;
            case 26 -> Tile.DARK_OAK_PLANKS;
            case 27 -> Tile.FURNACE_ACTIVE;
            case 28 -> Tile.SPRUCE_LOG;
            case 29 -> Tile.BIRCH_LOG;
            case 30 -> Tile.DARK_OAK_LOG;
            case 31 -> Tile.SPRUCE_LEAVES;
            case 32 -> Tile.BIRCH_LEAVES;
            case 33 -> Tile.DARK_OAK_LEAVES;
            case 34 -> Tile.OAK_SAPLING;
            case 35 -> Tile.SPRUCE_SAPLING;
            case 36 -> Tile.BIRCH_SAPLING;
            case 37 -> Tile.DARK_OAK_SAPLING;
            case 38 -> Tile.DIRT_PATH;
            case 39 -> Tile.FARMLAND;
            case 40 -> Tile.BED;
            case 41 -> Tile.WORKSTATION;
            case 42 -> Tile.ROSE_BUSH;
            case 43 -> Tile.LAVENDER;
            case 44 -> Tile.DAISY;
            case 45 -> Tile.LILAC;
            case 46 -> Tile.TALL_GRASS;
            case 47 -> Tile.SHORT_GRASS;
            case 48, 49, 50, 51, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68 -> Tile.WOOD_PLANK;
            case 52, 53, 54, 55, 71 -> Tile.STONE;
            case 69 -> Tile.LAMP;
            case 70 -> Tile.LANTERN;
            default -> Tile.STONE;
        };
    }

    @Override
    public void close() {
        textureManager.close();
    }

    public enum Tile {
        GRASS_TOP(0, 0),
        GRASS_SIDE(1, 0),
        DIRT(2, 0),
        STONE(3, 0),
        WOOD_LOG(4, 0),
        WOOD_PLANK(5, 0),
        LEAVES(6, 0),
        WATER(7, 0),
        BEDROCK(8, 0),
        CRACK_1(9, 0),
        CRACK_2(10, 0),
        CRACK_3(11, 0),
        CRACK_4(12, 0),
        SAND(13, 0),
        SANDSTONE(14, 0),
        SNOW(15, 0),
        ICE(3, 1),
        COAL_ORE(4, 1),
        IRON_ORE(5, 1),
        COPPER_ORE(6, 1),
        GOLD_ORE(7, 1),
        DIAMOND_ORE(8, 1),
        MOSSY_STONE(9, 1),
        GRAVEL(10, 1),
        SPRUCE_PLANKS(11, 1),
        BIRCH_PLANKS(12, 1),
        ACACIA_PLANKS(13, 1),
        DARK_OAK_PLANKS(14, 1),
        FURNACE(15, 1),
        FURNACE_ACTIVE(7, 7),
        SPRUCE_LOG(11, 2),
        BIRCH_LOG(12, 2),
        DARK_OAK_LOG(13, 2),
        SPRUCE_LEAVES(14, 2),
        BIRCH_LEAVES(15, 2),
        DARK_OAK_LEAVES(11, 7),
        OAK_SAPLING(12, 7),
        SPRUCE_SAPLING(13, 7),
        BIRCH_SAPLING(14, 7),
        DARK_OAK_SAPLING(15, 7),
        DIRT_PATH(0, 8),
        FARMLAND(1, 8),
        BED(2, 8),
        WORKSTATION(3, 8),
        ROSE_BUSH(4, 8),
        LAVENDER(5, 8),
        DAISY(6, 8),
        LILAC(7, 8),
        TALL_GRASS(8, 8),
        SHORT_GRASS(9, 8),
        LAMP(10, 8),
        LANTERN(11, 8);

        private final int tileX;
        private final int tileY;

        Tile(int tileX, int tileY) {
            this.tileX = tileX;
            this.tileY = tileY;
        }

        public int tileX() {
            return tileX;
        }

        public int tileY() {
            return tileY;
        }
    }
}
