package com.example.voxelgame.game.world;

import com.example.voxelgame.game.inventory.Item;
import com.example.voxelgame.world.Block;
import java.util.Optional;

public final class MiningCalculator {
    private static final float HAND_SPEED = 0.8f;
    private static final float WRONG_TOOL_SPEED = 0.45f;
    private static final float BASE_BREAK_TIME_SCALE = 1.6f;

    private MiningCalculator() {
    }

    public static float secondsToBreak(Block block, Optional<Item> heldItem) {
        float hardness = Math.max(0.05f, block.getHardness());
        float speed = miningSpeed(block, heldItem);
        return hardness * BASE_BREAK_TIME_SCALE / speed;
    }

    public static float progressPerSecond(Block block, Optional<Item> heldItem) {
        return 1.0f / secondsToBreak(block, heldItem);
    }

    public static float miningSpeed(Block block, Optional<Item> heldItem) {
        if (heldItem.isEmpty() || heldItem.get().isTool()) {
            if (heldItem.isPresent()) {
                Item tool = heldItem.get();
                if (block.getRequiredToolType().isPresent()
                        && tool.getToolType().isPresent()
                        && block.getRequiredToolType().get() == tool.getToolType().get()
                        && tool.getToolTier().isPresent()) {
                    return tool.getToolTier().get().getMiningSpeed();
                }
                return WRONG_TOOL_SPEED;
            }
            return HAND_SPEED;
        }
        return HAND_SPEED;
    }
}
