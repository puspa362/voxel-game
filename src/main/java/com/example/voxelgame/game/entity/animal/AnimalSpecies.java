package com.example.voxelgame.game.entity.animal;

import java.util.List;

import org.joml.Vector3f;

import com.example.voxelgame.game.inventory.ItemStack;
import com.example.voxelgame.game.inventory.Items;

public enum AnimalSpecies {
    COW("cow", new Vector3f(0.42f, 0.62f, 0.70f), 0.45f, 0.36f, 0.26f, List.of(new ItemStack(Items.LEATHER, 1))),
    PIG("pig", new Vector3f(0.42f, 0.46f, 0.58f), 0.86f, 0.48f, 0.52f, List.of(new ItemStack(Items.RAW_PORK, 1))),
    SHEEP("sheep", new Vector3f(0.42f, 0.58f, 0.64f), 0.86f, 0.84f, 0.72f, List.of(new ItemStack(Items.WOOL, 1), new ItemStack(Items.RAW_MUTTON, 1))),
    CHICKEN("chicken", new Vector3f(0.24f, 0.34f, 0.28f), 0.95f, 0.92f, 0.78f, List.of(new ItemStack(Items.RAW_CHICKEN, 1), new ItemStack(Items.FEATHER, 1)));

    private final String id;
    private final Vector3f halfExtents;
    private final float red;
    private final float green;
    private final float blue;
    private final List<ItemStack> drops;

    AnimalSpecies(String id, Vector3f halfExtents, float red, float green, float blue, List<ItemStack> drops) {
        this.id = id;
        this.halfExtents = new Vector3f(halfExtents);
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.drops = drops.stream().map(ItemStack::copy).toList();
    }

    public String id() {
        return id;
    }

    public Vector3f halfExtents() {
        return new Vector3f(halfExtents);
    }

    public float red() {
        return red;
    }

    public float green() {
        return green;
    }

    public float blue() {
        return blue;
    }

    public List<ItemStack> drops() {
        return drops.stream().map(ItemStack::copy).toList();
    }

    public static AnimalSpecies byId(String id) {
        for (AnimalSpecies species : values()) {
            if (species.id.equals(id)) {
                return species;
            }
        }
        return COW;
    }
}
