package com.example.voxelgame.world;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public final class Block {
    private final short id;
    private final String name;
    private final BlockType type;
    private final EnumSet<BlockProperty> properties;
    private final float hardness;
    private final com.example.voxelgame.game.inventory.ToolType requiredToolType;
    private final int lightEmission;

    public Block(int id, String name, BlockType type, Set<BlockProperty> properties) {
        this(id, name, type, properties, 0.0f, null, 0);
    }

    public Block(
            int id,
            String name,
            BlockType type,
            Set<BlockProperty> properties,
            float hardness,
            com.example.voxelgame.game.inventory.ToolType requiredToolType
    ) {
        this(id, name, type, properties, hardness, requiredToolType, 0);
    }

    public Block(
            int id,
            String name,
            BlockType type,
            Set<BlockProperty> properties,
            float hardness,
            com.example.voxelgame.game.inventory.ToolType requiredToolType,
            int lightEmission
    ) {
        if (id < 0 || id > 0xFFFF) {
            throw new IllegalArgumentException("Block id must fit in an unsigned 16-bit value.");
        }

        this.id = (short) id;
        this.name = Objects.requireNonNull(name, "Block name cannot be null.").trim();
        this.type = Objects.requireNonNull(type, "Block type cannot be null.");

        if (this.name.isEmpty()) {
            throw new IllegalArgumentException("Block name cannot be blank.");
        }

        if (properties == null || properties.isEmpty()) {
            this.properties = EnumSet.noneOf(BlockProperty.class);
        } else {
            this.properties = EnumSet.copyOf(properties);
        }
        if (hardness < 0.0f) {
            throw new IllegalArgumentException("Block hardness cannot be negative.");
        }
        this.hardness = hardness;
        this.requiredToolType = requiredToolType;
        if (lightEmission < 0 || lightEmission > 15) {
            throw new IllegalArgumentException("Light emission must be between 0 and 15.");
        }
        this.lightEmission = lightEmission;
    }

    public short getId() {
        return id;
    }

    public int getNumericId() {
        return Short.toUnsignedInt(id);
    }

    public String getName() {
        return name;
    }

    public BlockType getType() {
        return type;
    }

    public Set<BlockProperty> getProperties() {
        return Collections.unmodifiableSet(properties);
    }

    public boolean hasProperty(BlockProperty property) {
        return properties.contains(property);
    }

    public boolean isSolid() {
        return hasProperty(BlockProperty.SOLID);
    }

    public boolean isTransparent() {
        return hasProperty(BlockProperty.TRANSPARENT);
    }

    public boolean isCollidable() {
        return hasProperty(BlockProperty.COLLIDABLE);
    }

    public boolean isRenderable() {
        return type != BlockType.AIR;
    }

    public boolean isFluid() {
        return type == BlockType.FLUID;
    }

    public float getHardness() {
        return hardness;
    }

    public java.util.Optional<com.example.voxelgame.game.inventory.ToolType> getRequiredToolType() {
        return java.util.Optional.ofNullable(requiredToolType);
    }

    public int getLightEmission() {
        return lightEmission;
    }
}
