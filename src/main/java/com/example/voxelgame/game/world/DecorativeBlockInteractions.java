package com.example.voxelgame.game.world;

import com.example.voxelgame.world.Block;
import com.example.voxelgame.world.BlockHitResult;
import com.example.voxelgame.world.BlockRegistry;
import com.example.voxelgame.world.VoxelWorld;
import java.util.Objects;

public final class DecorativeBlockInteractions {
    private static final System.Logger LOGGER = System.getLogger(DecorativeBlockInteractions.class.getName());
    private static final boolean DEBUG_DECOR = Boolean.getBoolean("voxelgame.debugDecorBlocks");

    private DecorativeBlockInteractions() {
    }

    public static boolean tryInteract(VoxelWorld world, BlockHitResult hit) {
        Objects.requireNonNull(world, "Voxel world cannot be null.");
        Objects.requireNonNull(hit, "Block hit result cannot be null.");

        Block block = world.getBlockAtWorld(hit.blockX(), hit.blockY(), hit.blockZ());
        if (BlockRegistry.isFenceGate(block)) {
            return toggleFenceGate(world, hit.blockX(), hit.blockY(), hit.blockZ(), block);
        }
        if (BlockRegistry.isDoor(block)) {
            return toggleDoor(world, hit.blockX(), hit.blockY(), hit.blockZ(), block);
        }
        if (block == BlockRegistry.BED) {
            if (DEBUG_DECOR) {
                LOGGER.log(System.Logger.Level.INFO, "Bed interaction: spawn/sleep hook at ({0}, {1}, {2})", hit.blockX(), hit.blockY(), hit.blockZ());
            }
            return true;
        }
        if (block == BlockRegistry.BELL) {
            if (DEBUG_DECOR) {
                LOGGER.log(System.Logger.Level.INFO, "Bell interaction: ring hook at ({0}, {1}, {2})", hit.blockX(), hit.blockY(), hit.blockZ());
            }
            return true;
        }
        return false;
    }

    private static boolean toggleFenceGate(VoxelWorld world, int x, int y, int z, Block block) {
        Block replacement;
        if (block == BlockRegistry.OAK_FENCE_GATE_CLOSED_NORTH) {
            replacement = BlockRegistry.OAK_FENCE_GATE_OPEN_NORTH;
        } else if (block == BlockRegistry.OAK_FENCE_GATE_CLOSED_EAST) {
            replacement = BlockRegistry.OAK_FENCE_GATE_OPEN_EAST;
        } else if (block == BlockRegistry.OAK_FENCE_GATE_OPEN_NORTH) {
            replacement = BlockRegistry.OAK_FENCE_GATE_CLOSED_NORTH;
        } else {
            replacement = BlockRegistry.OAK_FENCE_GATE_CLOSED_EAST;
        }
        return world.setBlockAtWorld(x, y, z, replacement);
    }

    private static boolean toggleDoor(VoxelWorld world, int x, int y, int z, Block clickedBlock) {
        int lowerY = BlockRegistry.isDoorUpper(clickedBlock) ? y - 1 : y;
        Block lower = world.getBlockAtWorld(x, lowerY, z);
        Block upper = world.getBlockAtWorld(x, lowerY + 1, z);
        if (!BlockRegistry.isDoorLower(lower) || !BlockRegistry.isDoorUpper(upper)) {
            return false;
        }

        Block nextLower;
        Block nextUpper;
        if (lower == BlockRegistry.OAK_DOOR_LOWER_CLOSED_NORTH) {
            nextLower = BlockRegistry.OAK_DOOR_LOWER_OPEN_NORTH;
            nextUpper = BlockRegistry.OAK_DOOR_UPPER_OPEN_NORTH;
        } else if (lower == BlockRegistry.OAK_DOOR_LOWER_OPEN_NORTH) {
            nextLower = BlockRegistry.OAK_DOOR_LOWER_CLOSED_NORTH;
            nextUpper = BlockRegistry.OAK_DOOR_UPPER_CLOSED_NORTH;
        } else if (lower == BlockRegistry.OAK_DOOR_LOWER_CLOSED_EAST) {
            nextLower = BlockRegistry.OAK_DOOR_LOWER_OPEN_EAST;
            nextUpper = BlockRegistry.OAK_DOOR_UPPER_OPEN_EAST;
        } else {
            nextLower = BlockRegistry.OAK_DOOR_LOWER_CLOSED_EAST;
            nextUpper = BlockRegistry.OAK_DOOR_UPPER_CLOSED_EAST;
        }

        boolean lowerSet = world.setBlockAtWorld(x, lowerY, z, nextLower);
        boolean upperSet = world.setBlockAtWorld(x, lowerY + 1, z, nextUpper);
        return lowerSet || upperSet;
    }
}
