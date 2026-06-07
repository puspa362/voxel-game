package com.example.voxelgame.game.entity.villager;

import java.util.List;

import com.example.voxelgame.game.inventory.ItemStack;
import com.example.voxelgame.game.inventory.Items;

public final class VillagerTradeTable {
    private VillagerTradeTable() {
    }

    public static List<TradeOffer> initialOffers(VillagerProfession profession, long seed) {
        return switch (profession) {
            case FARMER -> List.of(
                    offer("farmer_wheat_emerald", Items.WHEAT, 12, Items.EMERALD, 1),
                    offer("farmer_bread_apple", Items.BREAD, 2, Items.APPLE, 1)
            );
            case BLACKSMITH -> List.of(
                    offer("smith_coal_pickaxe", Items.COAL, 16, Items.STONE_PICKAXE, 1),
                    offer("smith_emerald_iron_shovel", Items.EMERALD, 3, Items.IRON_SHOVEL, 1)
            );
            case LIBRARIAN -> List.of(
                    offer("librarian_paper_placeholder", Items.WHEAT_SEEDS, 10, Items.EMERALD, 1),
                    offer("librarian_emerald_bread", Items.EMERALD, 1, Items.BREAD, 2)
            );
            case UNEMPLOYED -> List.of(
                    offer("unemployed_bread_apple", Items.BREAD, 1, Items.APPLE, 1)
            );
        };
    }

    private static TradeOffer offer(String id, com.example.voxelgame.game.inventory.Item cost, int costCount, com.example.voxelgame.game.inventory.Item result, int resultCount) {
        return new TradeOffer(id, new ItemStack(cost, costCount), new ItemStack(result, resultCount), 12, 0);
    }
}
