package com.example.voxelgame.game.world;

import com.example.voxelgame.game.inventory.ItemStack;
import com.example.voxelgame.game.inventory.Item;
import com.example.voxelgame.game.inventory.Items;
import com.example.voxelgame.world.Block;
import com.example.voxelgame.world.BlockRegistry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public final class BlockLootTable {
    private BlockLootTable() {
    }

    public static Optional<ItemStack> getDrop(Block block, Optional<Item> heldItem) {
        Objects.requireNonNull(block, "Block cannot be null.");
        Objects.requireNonNull(heldItem, "Held item cannot be null.");

        if (block == BlockRegistry.STONE) {
            return hasCorrectTool(block, heldItem)
                    ? Optional.of(new ItemStack(Items.STONE_BLOCK, 1))
                    : Optional.empty();
        }
        if (block == BlockRegistry.DIRT || block == BlockRegistry.GRASS) {
            return Optional.of(new ItemStack(Items.DIRT_BLOCK, 1));
        }
        if (block == BlockRegistry.OAK_LOG) {
            return Optional.of(new ItemStack(Items.OAK_LOG_BLOCK, 1));
        }
        if (block == BlockRegistry.SPRUCE_LOG) {
            return Optional.of(new ItemStack(Items.SPRUCE_LOG_BLOCK, 1));
        }
        if (block == BlockRegistry.BIRCH_LOG) {
            return Optional.of(new ItemStack(Items.BIRCH_LOG_BLOCK, 1));
        }
        if (block == BlockRegistry.DARK_OAK_LOG) {
            return Optional.of(new ItemStack(Items.DARK_OAK_LOG_BLOCK, 1));
        }
        if (block == BlockRegistry.OAK_LEAVES) {
            return leafDrop(Items.OAK_SAPLING, true);
        }
        if (block == BlockRegistry.SPRUCE_LEAVES) {
            return leafDrop(Items.SPRUCE_SAPLING, false);
        }
        if (block == BlockRegistry.BIRCH_LEAVES) {
            return leafDrop(Items.BIRCH_SAPLING, false);
        }
        if (block == BlockRegistry.DARK_OAK_LEAVES) {
            return leafDrop(Items.DARK_OAK_SAPLING, true);
        }
        if (block == BlockRegistry.OAK_PLANKS) {
            return Optional.of(new ItemStack(Items.OAK_PLANKS_BLOCK, 1));
        }
        if (block == BlockRegistry.SPRUCE_PLANKS) {
            return Optional.of(new ItemStack(Items.SPRUCE_PLANKS_BLOCK, 1));
        }
        if (block == BlockRegistry.BIRCH_PLANKS) {
            return Optional.of(new ItemStack(Items.BIRCH_PLANKS_BLOCK, 1));
        }
        if (block == BlockRegistry.ACACIA_PLANKS) {
            return Optional.of(new ItemStack(Items.ACACIA_PLANKS_BLOCK, 1));
        }
        if (block == BlockRegistry.DARK_OAK_PLANKS) {
            return Optional.of(new ItemStack(Items.DARK_OAK_PLANKS_BLOCK, 1));
        }
        if (block == BlockRegistry.CRAFTING_TABLE) {
            return Optional.of(new ItemStack(Items.CRAFTING_TABLE_BLOCK, 1));
        }
        if (BlockRegistry.isFurnace(block)) {
            return hasCorrectTool(block, heldItem)
                    ? Optional.of(new ItemStack(Items.FURNACE_BLOCK, 1))
                    : Optional.empty();
        }
        if (block == BlockRegistry.SAND) {
            return Optional.of(new ItemStack(Items.SAND_BLOCK, 1));
        }
        if (block == BlockRegistry.SANDSTONE) {
            return hasCorrectTool(block, heldItem)
                    ? Optional.of(new ItemStack(Items.SANDSTONE_BLOCK, 1))
                    : Optional.empty();
        }
        if (block == BlockRegistry.SNOW) {
            return Optional.of(new ItemStack(Items.SNOW_BLOCK, 1));
        }
        if (block == BlockRegistry.ICE) {
            return hasCorrectTool(block, heldItem)
                    ? Optional.of(new ItemStack(Items.ICE_BLOCK, 1))
                    : Optional.empty();
        }
        if (block == BlockRegistry.COAL_ORE) {
            return hasCorrectTool(block, heldItem)
                    ? Optional.of(new ItemStack(Items.COAL_ORE_BLOCK, 1))
                    : Optional.empty();
        }
        if (block == BlockRegistry.IRON_ORE) {
            return hasCorrectTool(block, heldItem)
                    ? Optional.of(new ItemStack(Items.IRON_ORE_BLOCK, 1))
                    : Optional.empty();
        }
        if (block == BlockRegistry.COPPER_ORE) {
            return hasCorrectTool(block, heldItem)
                    ? Optional.of(new ItemStack(Items.COPPER_ORE_BLOCK, 1))
                    : Optional.empty();
        }
        if (block == BlockRegistry.GOLD_ORE) {
            return hasCorrectTool(block, heldItem)
                    ? Optional.of(new ItemStack(Items.GOLD_ORE_BLOCK, 1))
                    : Optional.empty();
        }
        if (block == BlockRegistry.DIAMOND_ORE) {
            return hasCorrectTool(block, heldItem)
                    ? Optional.of(new ItemStack(Items.DIAMOND_ORE_BLOCK, 1))
                    : Optional.empty();
        }
        if (block == BlockRegistry.MOSSY_STONE) {
            return hasCorrectTool(block, heldItem)
                    ? Optional.of(new ItemStack(Items.MOSSY_STONE_BLOCK, 1))
                    : Optional.empty();
        }
        if (block == BlockRegistry.GRAVEL) {
            return Optional.of(new ItemStack(Items.GRAVEL_BLOCK, 1));
        }
        if (block == BlockRegistry.OAK_SAPLING) {
            return Optional.of(new ItemStack(Items.OAK_SAPLING, 1));
        }
        if (block == BlockRegistry.SPRUCE_SAPLING) {
            return Optional.of(new ItemStack(Items.SPRUCE_SAPLING, 1));
        }
        if (block == BlockRegistry.BIRCH_SAPLING) {
            return Optional.of(new ItemStack(Items.BIRCH_SAPLING, 1));
        }
        if (block == BlockRegistry.DARK_OAK_SAPLING) {
            return Optional.of(new ItemStack(Items.DARK_OAK_SAPLING, 1));
        }
        if (block == BlockRegistry.ROSE_BUSH) {
            return Optional.of(new ItemStack(Items.ROSE_BUSH_BLOCK, 1));
        }
        if (block == BlockRegistry.LAVENDER) {
            return Optional.of(new ItemStack(Items.LAVENDER_BLOCK, 1));
        }
        if (block == BlockRegistry.DAISY) {
            return Optional.of(new ItemStack(Items.DAISY_BLOCK, 1));
        }
        if (block == BlockRegistry.LILAC) {
            return Optional.of(new ItemStack(Items.LILAC_BLOCK, 1));
        }
        if (block == BlockRegistry.TALL_GRASS || block == BlockRegistry.SHORT_GRASS) {
            return ThreadLocalRandom.current().nextInt(100) < 24
                    ? Optional.of(new ItemStack(Items.WHEAT_SEEDS, 1))
                    : Optional.empty();
        }
        if (block == BlockRegistry.BED) {
            return Optional.of(new ItemStack(Items.BED_BLOCK, 1));
        }
        if (block == BlockRegistry.LAMP) {
            return Optional.of(new ItemStack(Items.LAMP_BLOCK, 1));
        }
        if (block == BlockRegistry.LANTERN) {
            return Optional.of(new ItemStack(Items.LANTERN_BLOCK, 1));
        }
        if (block == BlockRegistry.BELL) {
            return Optional.of(new ItemStack(Items.BELL_BLOCK, 1));
        }
        if (BlockRegistry.isStairs(block)) {
            return Optional.of(new ItemStack(block.getName().startsWith("Stone") ? Items.STONE_STAIRS_BLOCK : Items.OAK_STAIRS_BLOCK, 1));
        }
        if (BlockRegistry.isFence(block)) {
            return Optional.of(new ItemStack(Items.OAK_FENCE_BLOCK, 1));
        }
        if (BlockRegistry.isFenceGate(block)) {
            return Optional.of(new ItemStack(Items.OAK_FENCE_GATE_BLOCK, 1));
        }
        if (BlockRegistry.isDoor(block)) {
            return BlockRegistry.isDoorLower(block)
                    ? Optional.of(new ItemStack(Items.OAK_DOOR_BLOCK, 1))
                    : Optional.empty();
        }

        return Optional.empty();
    }

    private static Optional<ItemStack> leafDrop(Item sapling, boolean canDropApple) {
        int roll = ThreadLocalRandom.current().nextInt(100);
        if (canDropApple && roll < 4) {
            return Optional.of(new ItemStack(Items.APPLE, 1));
        }
        if (roll < 16) {
            return Optional.of(new ItemStack(sapling, 1));
        }
        return Optional.empty();
    }

    private static boolean hasCorrectTool(Block block, Optional<Item> heldItem) {
        return block.getRequiredToolType().isPresent()
                && heldItem.filter(Item::isTool)
                .flatMap(Item::getToolType)
                .filter(toolType -> toolType == block.getRequiredToolType().orElseThrow())
                .isPresent();
    }
}
