package com.example.voxelgame.game.world;

import com.example.voxelgame.game.Player;
import com.example.voxelgame.game.entity.EntityManager;
import com.example.voxelgame.game.entity.ItemEntity;
import com.example.voxelgame.game.inventory.ItemStack;
import com.example.voxelgame.game.inventory.Item;
import com.example.voxelgame.game.mode.GameModeRules;
import org.joml.Vector3f;
import com.example.voxelgame.world.Block;
import com.example.voxelgame.world.BlockHitResult;
import com.example.voxelgame.world.BlockProperty;
import com.example.voxelgame.world.BlockRegistry;
import com.example.voxelgame.world.VoxelWorld;
import java.util.Objects;
import java.util.Optional;

public final class SurvivalWorldRules {
    private static final System.Logger LOGGER = System.getLogger(SurvivalWorldRules.class.getName());

    public boolean tryBreakBlock(Player player, VoxelWorld world, BlockHitResult hit, EntityManager entityManager) {
        Objects.requireNonNull(player, "Player cannot be null.");
        Objects.requireNonNull(world, "Voxel world cannot be null.");
        Objects.requireNonNull(hit, "Block hit result cannot be null.");
        Objects.requireNonNull(entityManager, "Entity manager cannot be null.");

        Block targetBlock = world.getBlockAtWorld(hit.blockX(), hit.blockY(), hit.blockZ());
        if (!targetBlock.isRenderable()) {
            return false;
        }

        if (!world.setBlockAtWorld(hit.blockX(), hit.blockY(), hit.blockZ(), BlockRegistry.AIR)) {
            return false;
        }

        GameModeRules rules = player.getGameModeRules();
        if (rules.shouldDropBlockItems()) {
            Optional<ItemStack> drop = determineDrop(player, targetBlock);
            drop.ifPresent(itemStack -> spawnDrop(entityManager, hit, itemStack));
        }
        return true;
    }

    public boolean tryPlaceSelectedBlock(Player player, VoxelWorld world, BlockHitResult hit) {
        Objects.requireNonNull(player, "Player cannot be null.");
        Objects.requireNonNull(world, "Voxel world cannot be null.");
        Objects.requireNonNull(hit, "Block hit result cannot be null.");

        GameModeRules rules = player.getGameModeRules();
        Optional<ItemStack> selectedStack = player.getSelectedItemStack();
        if (selectedStack.isEmpty()) {
            LOGGER.log(
                    System.Logger.Level.INFO,
                    "Placement skipped: inventory=#{0}, selectedSlot={1}, reason=empty-slot",
                    Integer.toHexString(System.identityHashCode(player.getInventory())),
                    player.getInventory().getSelectedHotbarSlot()
            );
            return false;
        }

        ItemStack selectedItemStack = selectedStack.get();
        Item selectedItem = selectedItemStack.getItem();
        LOGGER.log(
                System.Logger.Level.INFO,
                "Placement selected item: inventory=#{0}, selectedSlot={1}, item={2}, count={3}",
                Integer.toHexString(System.identityHashCode(player.getInventory())),
                player.getInventory().getSelectedHotbarSlot(),
                selectedItem.getId(),
                selectedItemStack.getCount()
        );

        Optional<Block> placementBlock = selectedItem.getPlaceableBlock();
        if (placementBlock.isEmpty()) {
            LOGGER.log(
                    System.Logger.Level.INFO,
                    "Placement skipped: inventory=#{0}, selectedSlot={1}, item={2}, reason=not-placeable",
                    Integer.toHexString(System.identityHashCode(player.getInventory())),
                    player.getInventory().getSelectedHotbarSlot(),
                    selectedItem.getId()
            );
            return false;
        }

        int blockX = hit.adjacentX();
        int blockY = hit.adjacentY();
        int blockZ = hit.adjacentZ();
        LOGGER.log(
                System.Logger.Level.INFO,
                "Placement attempt: inventory=#{0}, selectedSlot={1}, item={2}, block={3}, target=({4}, {5}, {6})",
                Integer.toHexString(System.identityHashCode(player.getInventory())),
                player.getInventory().getSelectedHotbarSlot(),
                selectedItem.getId(),
                placementBlock.get().getName(),
                blockX,
                blockY,
                blockZ
        );

        Block blockToPlace = orientPlacementBlock(placementBlock.get(), player);
        if (wouldIntersectPlayer(player, blockX, blockY, blockZ)) {
            LOGGER.log(System.Logger.Level.INFO, "Placement blocked: reason=player-intersection");
            return false;
        }

        Block existingBlock = world.getBlockAtWorld(blockX, blockY, blockZ);
        if (existingBlock.isRenderable() && !existingBlock.hasProperty(BlockProperty.REPLACEABLE)) {
            LOGGER.log(
                    System.Logger.Level.INFO,
                    "Placement blocked: reason=occupied, existingBlock={0}",
                    existingBlock.getName()
            );
            return false;
        }

        if (blockToPlace == BlockRegistry.OAK_DOOR_LOWER_CLOSED_NORTH || blockToPlace == BlockRegistry.OAK_DOOR_LOWER_CLOSED_EAST) {
            return tryPlaceDoor(player, world, rules, selectedItem, existingBlock, blockX, blockY, blockZ, blockToPlace);
        }

        if (!world.setBlockAtWorld(blockX, blockY, blockZ, blockToPlace)) {
            LOGGER.log(System.Logger.Level.INFO, "Placement blocked: reason=world-set-failed");
            return false;
        }

        if (!rules.consumesPlacementItems() || player.getInventory().removeFromSelectedSlot(1)) {
            LOGGER.log(
                    System.Logger.Level.INFO,
                    "Placement success: inventory=#{0}, selectedSlot={1}, item={2}",
                    Integer.toHexString(System.identityHashCode(player.getInventory())),
                    player.getInventory().getSelectedHotbarSlot(),
                    selectedItem.getId()
            );
            return true;
        }

        LOGGER.log(System.Logger.Level.WARNING, "Placement rollback: reason=selected-stack-remove-failed");
        world.setBlockAtWorld(blockX, blockY, blockZ, existingBlock);
        return false;
    }

    private boolean tryPlaceDoor(
            Player player,
            VoxelWorld world,
            GameModeRules rules,
            Item selectedItem,
            Block existingLower,
            int blockX,
            int blockY,
            int blockZ,
            Block lowerBlock
    ) {
        if (blockY + 1 >= com.example.voxelgame.world.Chunk.HEIGHT) {
            return false;
        }
        Block existingUpper = world.getBlockAtWorld(blockX, blockY + 1, blockZ);
        if (existingUpper.isRenderable() && !existingUpper.hasProperty(BlockProperty.REPLACEABLE)) {
            LOGGER.log(System.Logger.Level.INFO, "Placement blocked: reason=door-upper-occupied, existingBlock={0}", existingUpper.getName());
            return false;
        }
        Block upperBlock = lowerBlock == BlockRegistry.OAK_DOOR_LOWER_CLOSED_EAST
                ? BlockRegistry.OAK_DOOR_UPPER_CLOSED_EAST
                : BlockRegistry.OAK_DOOR_UPPER_CLOSED_NORTH;
        if (!world.setBlockAtWorld(blockX, blockY, blockZ, lowerBlock)) {
            return false;
        }
        if (!world.setBlockAtWorld(blockX, blockY + 1, blockZ, upperBlock)) {
            world.setBlockAtWorld(blockX, blockY, blockZ, existingLower);
            return false;
        }
        if (!rules.consumesPlacementItems() || player.getInventory().removeFromSelectedSlot(1)) {
            LOGGER.log(
                    System.Logger.Level.INFO,
                    "Placement success: inventory=#{0}, selectedSlot={1}, item={2}",
                    Integer.toHexString(System.identityHashCode(player.getInventory())),
                    player.getInventory().getSelectedHotbarSlot(),
                    selectedItem.getId()
            );
            return true;
        }
        world.setBlockAtWorld(blockX, blockY, blockZ, existingLower);
        world.setBlockAtWorld(blockX, blockY + 1, blockZ, existingUpper);
        return false;
    }

    private Block orientPlacementBlock(Block block, Player player) {
        Vector3f forward = player.getForward();
        boolean eastWest = Math.abs(forward.x) > Math.abs(forward.z);
        if (block == BlockRegistry.OAK_STAIRS_NORTH) {
            if (eastWest) {
                return forward.x >= 0.0f ? BlockRegistry.OAK_STAIRS_EAST : BlockRegistry.OAK_STAIRS_WEST;
            }
            return forward.z >= 0.0f ? BlockRegistry.OAK_STAIRS_SOUTH : BlockRegistry.OAK_STAIRS_NORTH;
        }
        if (block == BlockRegistry.STONE_STAIRS_NORTH) {
            if (eastWest) {
                return forward.x >= 0.0f ? BlockRegistry.STONE_STAIRS_EAST : BlockRegistry.STONE_STAIRS_WEST;
            }
            return forward.z >= 0.0f ? BlockRegistry.STONE_STAIRS_SOUTH : BlockRegistry.STONE_STAIRS_NORTH;
        }
        if (block == BlockRegistry.OAK_FENCE_GATE_CLOSED_NORTH) {
            return eastWest ? BlockRegistry.OAK_FENCE_GATE_CLOSED_EAST : BlockRegistry.OAK_FENCE_GATE_CLOSED_NORTH;
        }
        if (block == BlockRegistry.OAK_DOOR_LOWER_CLOSED_NORTH) {
            return eastWest ? BlockRegistry.OAK_DOOR_LOWER_CLOSED_EAST : BlockRegistry.OAK_DOOR_LOWER_CLOSED_NORTH;
        }
        return block;
    }

    public Optional<Block> selectedPlacementBlock(Player player) {
        Objects.requireNonNull(player, "Player cannot be null.");
        return player.getSelectedItemStack().flatMap(stack -> stack.getItem().getPlaceableBlock());
    }

    private Optional<ItemStack> determineDrop(Player player, Block block) {
        Optional<Item> heldItem = player.getSelectedItemStack().map(ItemStack::getItem);
        return BlockLootTable.getDrop(block, heldItem);
    }

    private void spawnDrop(EntityManager entityManager, BlockHitResult hit, ItemStack stack) {
        entityManager.spawn(createDropEntity(hit, stack));
    }

    private ItemEntity createDropEntity(BlockHitResult hit, ItemStack stack) {
        Vector3f spawnPosition = new Vector3f(hit.blockX() + 0.5f, hit.blockY() + 0.35f, hit.blockZ() + 0.5f);
        float horizontalBiasX = ((hit.blockX() * 73 + hit.blockZ() * 17) % 11 - 5) * 0.025f;
        float horizontalBiasZ = ((hit.blockZ() * 53 + hit.blockY() * 29) % 11 - 5) * 0.025f;
        Vector3f initialVelocity = new Vector3f(horizontalBiasX, 2.6f, horizontalBiasZ);
        return new ItemEntity(spawnPosition, initialVelocity, stack, 0.45);
    }

    private boolean wouldIntersectPlayer(Player player, int blockX, int blockY, int blockZ) {
        float blockMinX = blockX;
        float blockMaxX = blockX + 1.0f;
        float blockMinY = blockY;
        float blockMaxY = blockY + 1.0f;
        float blockMinZ = blockZ;
        float blockMaxZ = blockZ + 1.0f;

        float radius = player.getColliderRadius();
        Vector3f playerPosition = player.getPosition();
        float playerMinX = playerPosition.x - radius;
        float playerMaxX = playerPosition.x + radius;
        float playerMinY = playerPosition.y;
        float playerMaxY = playerPosition.y + player.getPlayerHeight();
        float playerMinZ = playerPosition.z - radius;
        float playerMaxZ = playerPosition.z + radius;

        return playerMinX < blockMaxX && playerMaxX > blockMinX
                && playerMinY < blockMaxY && playerMaxY > blockMinY
                && playerMinZ < blockMaxZ && playerMaxZ > blockMinZ;
    }
}
