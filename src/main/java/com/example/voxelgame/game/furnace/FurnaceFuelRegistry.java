package com.example.voxelgame.game.furnace;

import java.util.LinkedHashMap;
import java.util.Map;

import com.example.voxelgame.game.inventory.Item;
import com.example.voxelgame.game.inventory.Items;

public final class FurnaceFuelRegistry {
    private static final Map<String, Integer> FUELS = new LinkedHashMap<>();

    static {
        register(Items.COAL, 1600);
        register(Items.CHARCOAL, 1600);
        register(Items.OAK_LOG_BLOCK, 300);
        register(Items.SPRUCE_LOG_BLOCK, 300);
        register(Items.BIRCH_LOG_BLOCK, 300);
        register(Items.DARK_OAK_LOG_BLOCK, 300);
        register(Items.OAK_PLANKS, 300);
        register(Items.SPRUCE_PLANKS, 300);
        register(Items.BIRCH_PLANKS, 300);
        register(Items.ACACIA_PLANKS, 300);
        register(Items.DARK_OAK_PLANKS, 300);
        register(Items.STICK, 100);
    }

    private FurnaceFuelRegistry() {
    }

    public static int burnTicks(Item item) {
        return item == null ? 0 : FUELS.getOrDefault(item.getId(), 0);
    }

    public static boolean isFuel(Item item) {
        return burnTicks(item) > 0;
    }

    private static void register(Item item, int burnTicks) {
        FUELS.put(item.getId(), burnTicks);
    }
}
