package com.example.voxelgame.game.mode;

import com.example.voxelgame.game.inventory.Item;
import com.example.voxelgame.game.inventory.Items;
import java.util.List;

public final class CreativeInventoryCatalog {
    private final List<Item> items = Items.all().stream()
            .toList();

    public int size() {
        return items.size();
    }

    public Item get(int index) {
        return items.get(index);
    }
}
