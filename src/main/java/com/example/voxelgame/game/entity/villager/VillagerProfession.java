package com.example.voxelgame.game.entity.villager;

public enum VillagerProfession {
    UNEMPLOYED("unemployed", 0.42f, 0.34f, 0.24f),
    FARMER("farmer", 0.74f, 0.58f, 0.22f),
    BLACKSMITH("blacksmith", 0.32f, 0.34f, 0.38f),
    LIBRARIAN("librarian", 0.42f, 0.22f, 0.62f);

    private final String id;
    private final float red;
    private final float green;
    private final float blue;

    VillagerProfession(String id, float red, float green, float blue) {
        this.id = id;
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    public String id() {
        return id;
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

    public static VillagerProfession byId(String id) {
        for (VillagerProfession profession : values()) {
            if (profession.id.equals(id)) {
                return profession;
            }
        }
        return UNEMPLOYED;
    }
}
