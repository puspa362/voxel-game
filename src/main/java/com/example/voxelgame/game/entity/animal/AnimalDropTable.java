package com.example.voxelgame.game.entity.animal;

import java.util.List;

import com.example.voxelgame.game.inventory.ItemStack;

public final class AnimalDropTable {
    private AnimalDropTable() {
    }

    public static List<ItemStack> dropsFor(AnimalSpecies species) {
        return species.drops();
    }
}
