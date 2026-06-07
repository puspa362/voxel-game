package com.example.voxelgame.world;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import com.example.voxelgame.game.furnace.FurnaceBlockEntity;

public final class Chunk {
    public static final int WIDTH = 16;
    public static final int DEPTH = 16;
    public static final int HEIGHT = 256;
    public static final int VOLUME = WIDTH * DEPTH * HEIGHT;

    private final int chunkX;
    private final int chunkZ;

    // Each cell stores only a compact block id. Block metadata lives in BlockRegistry,
    // which keeps chunk memory stable and cheap before meshing/rendering is added.
    private final short[] blockIds = new short[VOLUME];
    private final byte[] waterLevels = new byte[VOLUME];
    private final byte[] lightLevels = new byte[VOLUME];
    private final Map<Integer, FurnaceBlockEntity> furnaces = new LinkedHashMap<>();

    private int nonAirBlockCount;
    private boolean dirty = true;
    private long meshVersion;

    public Chunk(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        Arrays.fill(blockIds, BlockRegistry.AIR.getId());
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public synchronized short getBlockId(int x, int y, int z) {
        return blockIds[indexOf(x, y, z)];
    }

    public synchronized Block getBlock(int x, int y, int z) {
        return BlockRegistry.get(getBlockId(x, y, z));
    }

    public synchronized void setBlockId(int x, int y, int z, short blockId) {
        if (!BlockRegistry.isRegistered(blockId)) {
            throw new IllegalArgumentException("Cannot store unregistered block id: " + Short.toUnsignedInt(blockId));
        }

        int index = indexOf(x, y, z);
        short previousId = blockIds[index];
        if (previousId == blockId) {
            return;
        }

        if (previousId == BlockRegistry.AIR.getId() && blockId != BlockRegistry.AIR.getId()) {
            nonAirBlockCount++;
        } else if (previousId != BlockRegistry.AIR.getId() && blockId == BlockRegistry.AIR.getId()) {
            nonAirBlockCount--;
        }

        blockIds[index] = blockId;
        if (!BlockRegistry.isFurnaceId(blockId)) {
            furnaces.remove(index);
        }
        if (blockId == BlockRegistry.WATER.getId()) {
            waterLevels[index] = 0;
        } else {
            waterLevels[index] = 0;
        }
        dirty = true;
        meshVersion++;
    }

    public synchronized void setBlock(int x, int y, int z, Block block) {
        setBlockId(x, y, z, block.getId());
    }

    public synchronized void fill(Block block) {
        short newId = block.getId();
        Arrays.fill(blockIds, newId);
        nonAirBlockCount = newId == BlockRegistry.AIR.getId() ? 0 : VOLUME;
        dirty = true;
        meshVersion++;
    }

    public boolean isEmpty() {
        return nonAirBlockCount == 0;
    }

    public int getNonAirBlockCount() {
        return nonAirBlockCount;
    }

    public synchronized boolean isDirty() {
        return dirty;
    }

    public synchronized void clearDirty() {
        dirty = false;
    }

    public synchronized void markDirty() {
        dirty = true;
        meshVersion++;
    }

    public synchronized long getMeshVersion() {
        return meshVersion;
    }

    public int getHighestFilledY(int x, int z) {
        checkXZBounds(x, z);

        for (int y = HEIGHT - 1; y >= 0; y--) {
            if (getBlockId(x, y, z) != BlockRegistry.AIR.getId()) {
                return y;
            }
        }

        return -1;
    }

    public synchronized short[] copyBlockIds() {
        return Arrays.copyOf(blockIds, blockIds.length);
    }

    public synchronized byte[] copyWaterLevels() {
        return Arrays.copyOf(waterLevels, waterLevels.length);
    }

    public synchronized int getWaterLevel(int x, int y, int z) {
        int index = indexOf(x, y, z);
        if (blockIds[index] != BlockRegistry.WATER.getId()) {
            return -1;
        }
        return Byte.toUnsignedInt(waterLevels[index]);
    }

    public synchronized void setWaterLevel(int x, int y, int z, int level) {
        if (level < 0 || level > 7) {
            throw new IllegalArgumentException("Water level must be between 0 and 7.");
        }

        int index = indexOf(x, y, z);
        if (blockIds[index] != BlockRegistry.WATER.getId()) {
            throw new IllegalStateException("Water level can only be assigned to water blocks.");
        }

        waterLevels[index] = (byte) level;
        dirty = true;
        meshVersion++;
    }

    public synchronized int getLightLevel(int x, int y, int z) {
        return Byte.toUnsignedInt(lightLevels[indexOf(x, y, z)]);
    }

    public synchronized void setLightLevel(int x, int y, int z, int level) {
        if (level < 0 || level > 15) {
            throw new IllegalArgumentException("Light level must be between 0 and 15.");
        }

        lightLevels[indexOf(x, y, z)] = (byte) level;
    }

    public synchronized void clearLightLevels() {
        Arrays.fill(lightLevels, (byte) 0);
    }

    public synchronized byte[] copyLightLevels() {
        return Arrays.copyOf(lightLevels, lightLevels.length);
    }

    public synchronized FurnaceBlockEntity getOrCreateFurnace(int x, int y, int z) {
        if (!BlockRegistry.isFurnace(getBlock(x, y, z))) {
            throw new IllegalStateException("Furnace entity can only exist at furnace blocks.");
        }
        return furnaces.computeIfAbsent(indexOf(x, y, z), ignored -> new FurnaceBlockEntity());
    }

    public synchronized Collection<FurnaceSnapshot> copyFurnaces() {
        return furnaces.entrySet().stream()
                .map(entry -> new FurnaceSnapshot(entry.getKey(), entry.getValue()))
                .toList();
    }

    public synchronized void loadFurnace(int index, FurnaceBlockEntity furnace) {
        if (index < 0 || index >= VOLUME) {
            throw new IllegalArgumentException("Furnace index out of bounds: " + index);
        }
        if (BlockRegistry.isFurnaceId(blockIds[index])) {
            furnaces.put(index, furnace);
        }
    }

    public synchronized boolean tickFurnaces() {
        boolean changed = false;
        for (Map.Entry<Integer, FurnaceBlockEntity> entry : furnaces.entrySet()) {
            FurnaceBlockEntity furnace = entry.getValue();
            boolean activeStateChanged = furnace.tick();
            if (activeStateChanged || furnace.isBurning() != (blockIds[entry.getKey()] == BlockRegistry.FURNACE_ACTIVE.getId())) {
                blockIds[entry.getKey()] = furnace.isBurning()
                        ? BlockRegistry.FURNACE_ACTIVE.getId()
                        : BlockRegistry.FURNACE.getId();
                dirty = true;
                meshVersion++;
                changed = true;
            }
        }
        return changed;
    }

    public synchronized void loadSnapshot(short[] savedBlockIds, byte[] savedWaterLevels) {
        byte[] lightLevels = new byte[VOLUME]; // Default to 0
        loadSnapshot(savedBlockIds, savedWaterLevels, lightLevels);
    }

    public synchronized void loadSnapshot(short[] savedBlockIds, byte[] savedWaterLevels, byte[] savedLightLevels) {
        if (savedBlockIds.length != VOLUME || savedWaterLevels.length != VOLUME || savedLightLevels.length != VOLUME) {
            throw new IllegalArgumentException("Chunk snapshot arrays must match chunk volume.");
        }

        System.arraycopy(savedBlockIds, 0, blockIds, 0, VOLUME);
        System.arraycopy(savedWaterLevels, 0, waterLevels, 0, VOLUME);
        System.arraycopy(savedLightLevels, 0, lightLevels, 0, VOLUME);
        furnaces.clear();

        nonAirBlockCount = 0;
        for (short blockId : blockIds) {
            if (blockId != BlockRegistry.AIR.getId()) {
                nonAirBlockCount++;
            }
        }
        dirty = true;
        meshVersion++;
    }

    private static int indexOf(int x, int y, int z) {
        checkBounds(x, y, z);
        return x + (z * WIDTH) + (y * WIDTH * DEPTH);
    }

    public record FurnaceSnapshot(int index, FurnaceBlockEntity furnace) {
    }

    private static void checkBounds(int x, int y, int z) {
        checkXZBounds(x, z);

        if (y < 0 || y >= HEIGHT) {
            throw new IndexOutOfBoundsException("Chunk Y coordinate out of bounds: " + y);
        }
    }

    private static void checkXZBounds(int x, int z) {
        if (x < 0 || x >= WIDTH) {
            throw new IndexOutOfBoundsException("Chunk X coordinate out of bounds: " + x);
        }
        if (z < 0 || z >= DEPTH) {
            throw new IndexOutOfBoundsException("Chunk Z coordinate out of bounds: " + z);
        }
    }
}
