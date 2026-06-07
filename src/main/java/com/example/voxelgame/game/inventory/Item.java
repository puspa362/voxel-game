package com.example.voxelgame.game.inventory;

import com.example.voxelgame.world.Block;
import java.util.Objects;
import java.util.Optional;

public final class Item {
    private final String id;
    private final String displayName;
    private final int maxStackSize;
    private final Block placeableBlock;
    private final ToolType toolType;
    private final ToolTier toolTier;
    private final int foodRestored;

    public Item(String id, String displayName, int maxStackSize, Block placeableBlock) {
        this(id, displayName, maxStackSize, placeableBlock, null, null, 0);
    }

    public Item(String id, String displayName, int maxStackSize) {
        this(id, displayName, maxStackSize, null, null, null, 0);
    }

    public Item(String id, String displayName, int maxStackSize, int foodRestored) {
        this(id, displayName, maxStackSize, null, null, null, foodRestored);
    }

    public Item(String id, String displayName, ToolType toolType, ToolTier toolTier) {
        this(id, displayName, 1, null, toolType, toolTier, 0);
    }

    private Item(
            String id,
            String displayName,
            int maxStackSize,
            Block placeableBlock,
            ToolType toolType,
            ToolTier toolTier,
            int foodRestored
    ) {
        this.id = requireText(id, "Item id cannot be blank.");
        this.displayName = requireText(displayName, "Item display name cannot be blank.");
        if (maxStackSize < 1 || maxStackSize > 64) {
            throw new IllegalArgumentException("Item max stack size must be between 1 and 64.");
        }
        if (foodRestored < 0 || foodRestored > 20) {
            throw new IllegalArgumentException("Food restoration must be between 0 and 20.");
        }

        this.maxStackSize = maxStackSize;
        this.placeableBlock = placeableBlock;
        this.toolType = toolType;
        this.toolTier = toolTier;
        this.foodRestored = foodRestored;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getName() {
        return displayName;
    }

    public int getMaxStackSize() {
        return maxStackSize;
    }

    public Optional<Block> getPlaceableBlock() {
        return Optional.ofNullable(placeableBlock);
    }

    public boolean isTool() {
        return toolType != null && toolTier != null;
    }

    public Optional<ToolType> getToolType() {
        return Optional.ofNullable(toolType);
    }

    public Optional<ToolTier> getToolTier() {
        return Optional.ofNullable(toolTier);
    }

    public boolean isFood() {
        return foodRestored > 0;
    }

    public int getFoodRestored() {
        return foodRestored;
    }

    public boolean isStackableWith(Item other) {
        return other != null && id.equals(other.id);
    }

    private static String requireText(String value, String message) {
        String normalized = Objects.requireNonNull(value, message).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }
}
