package com.example.voxelgame.save;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.joml.Vector3f;

import com.example.voxelgame.game.Player;
import com.example.voxelgame.game.entity.Entity;
import com.example.voxelgame.game.entity.EntityPersistenceData;
import com.example.voxelgame.game.entity.animal.AnimalEntity;
import com.example.voxelgame.game.entity.villager.VillagerEntity;
import com.example.voxelgame.game.furnace.FurnaceBlockEntity;
import com.example.voxelgame.game.inventory.Inventory;
import com.example.voxelgame.game.inventory.Item;
import com.example.voxelgame.game.inventory.ItemStack;
import com.example.voxelgame.game.inventory.Items;
import com.example.voxelgame.game.mode.GameMode;
import com.example.voxelgame.world.Chunk;

public final class SaveManager {
    private static final int CHUNK_FILE_VERSION = 3;
    private static final int PLAYER_FILE_VERSION = 3;
    private static final int ENTITY_FILE_VERSION = 1;

    private final Path rootDirectory;
    private final Path worldDirectory;
    private final Path chunkDirectory;
    private final Path playerFile;
    private final Path entitiesFile;

    public SaveManager(Path rootDirectory) {
        this.rootDirectory = rootDirectory;
        this.worldDirectory = rootDirectory.resolve("world");
        this.chunkDirectory = worldDirectory.resolve("chunks");
        this.playerFile = rootDirectory.resolve("player.dat");
        this.entitiesFile = rootDirectory.resolve("entities.dat");
    }

    public Optional<Chunk> loadChunk(int chunkX, int chunkZ) {
        Path chunkFile = chunkFile(chunkX, chunkZ);
        if (!Files.isRegularFile(chunkFile)) {
            return Optional.empty();
        }

        try (DataInputStream input = new DataInputStream(new BufferedInputStream(Files.newInputStream(chunkFile)))) {
            int version = input.readInt();
            if (version < 1 || version > CHUNK_FILE_VERSION) {
                throw new IOException("Unsupported chunk file version: " + version);
            }

            int savedChunkX = input.readInt();
            int savedChunkZ = input.readInt();
            if (savedChunkX != chunkX || savedChunkZ != chunkZ) {
                throw new IOException("Chunk coordinate mismatch in save file.");
            }

            short[] blockIds = new short[Chunk.VOLUME];
            byte[] waterLevels = new byte[Chunk.VOLUME];
            byte[] lightLevels = new byte[Chunk.VOLUME];
            for (int i = 0; i < Chunk.VOLUME; i++) {
                blockIds[i] = input.readShort();
            }
            input.readFully(waterLevels);
            if (version >= 2) {
                input.readFully(lightLevels);
            }

            Chunk chunk = new Chunk(chunkX, chunkZ);
            chunk.loadSnapshot(blockIds, waterLevels, lightLevels);
            if (version >= 3) {
                int furnaceCount = input.readInt();
                for (int i = 0; i < furnaceCount; i++) {
                    int index = input.readInt();
                    FurnaceBlockEntity furnace = readFurnace(input);
                    chunk.loadFurnace(index, furnace);
                }
            }
            return Optional.of(chunk);
        } catch (IOException exception) {
            System.err.println("Failed to load chunk " + chunkX + "," + chunkZ + ": " + exception.getMessage());
            return Optional.empty();
        }
    }

    public void saveChunk(Chunk chunk) {
        Path chunkFile = chunkFile(chunk.getChunkX(), chunk.getChunkZ());

        try {
            Files.createDirectories(chunkDirectory);
            try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(chunkFile)))) {
                output.writeInt(CHUNK_FILE_VERSION);
                output.writeInt(chunk.getChunkX());
                output.writeInt(chunk.getChunkZ());

                short[] blockIds = chunk.copyBlockIds();
                byte[] waterLevels = chunk.copyWaterLevels();
                byte[] lightLevels = chunk.copyLightLevels();
                for (short blockId : blockIds) {
                    output.writeShort(blockId);
                }
                output.write(waterLevels);
                output.write(lightLevels);
                output.writeInt(chunk.copyFurnaces().size());
                for (Chunk.FurnaceSnapshot snapshot : chunk.copyFurnaces()) {
                    output.writeInt(snapshot.index());
                    writeFurnace(output, snapshot.furnace());
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to save chunk %d,%d".formatted(chunk.getChunkX(), chunk.getChunkZ()),
                    exception
            );
        }
    }

    public Optional<PlayerSaveData> loadPlayer() {
        if (!Files.isRegularFile(playerFile)) {
            return Optional.empty();
        }

        try (DataInputStream input = new DataInputStream(new BufferedInputStream(Files.newInputStream(playerFile)))) {
            int version = input.readInt();
            if (version < 1 || version > PLAYER_FILE_VERSION) {
                throw new IOException("Unsupported player file version: " + version);
            }

            Vector3f position = new Vector3f(input.readFloat(), input.readFloat(), input.readFloat());
            float health = version >= 2 ? input.readFloat() : 20.0f;
            float hunger = version >= 2 ? input.readFloat() : 20.0f;
            GameMode gameMode = version >= 3
                    ? GameMode.byId(input.readUTF()).orElse(GameMode.SURVIVAL)
                    : GameMode.SURVIVAL;
            int selectedHotbarSlot = input.readInt();
            int slotCount = input.readInt();
            ItemStack[] slots = new ItemStack[slotCount];
            for (int i = 0; i < slotCount; i++) {
                slots[i] = readItemStack(input).orElse(null);
            }

            return Optional.of(new PlayerSaveData(position, health, hunger, gameMode, selectedHotbarSlot, slots));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load player data.", exception);
        }
    }

    public void savePlayer(Player player) {
        try {
            Files.createDirectories(rootDirectory);
            try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(playerFile)))) {
                output.writeInt(PLAYER_FILE_VERSION);
                output.writeFloat(player.getPosition().x);
                output.writeFloat(player.getPosition().y);
                output.writeFloat(player.getPosition().z);
                output.writeFloat(player.getCurrentHealth());
                output.writeFloat(player.getHunger());
                output.writeUTF(player.getGameMode().id());
                output.writeInt(player.getInventory().getSelectedHotbarSlot());
                output.writeInt(player.getInventory().getSlotCount());
                for (int i = 0; i < player.getInventory().getSlotCount(); i++) {
                    writeItemStack(output, player.getInventory().getSlot(i).orElse(null));
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to save player data.", exception);
        }
    }

    public List<Entity> loadEntities() {
        if (!Files.isRegularFile(entitiesFile)) {
            return List.of();
        }

        try (DataInputStream input = new DataInputStream(new BufferedInputStream(Files.newInputStream(entitiesFile)))) {
            int version = input.readInt();
            if (version < 1 || version > ENTITY_FILE_VERSION) {
                throw new IOException("Unsupported entity file version: " + version);
            }
            int count = input.readInt();
            List<Entity> entities = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                EntityPersistenceData data = readEntity(input);
                if (AnimalEntity.PERSISTENCE_TYPE.equals(data.typeId())) {
                    entities.add(AnimalEntity.fromSave(data));
                } else if (VillagerEntity.PERSISTENCE_TYPE.equals(data.typeId())) {
                    entities.add(VillagerEntity.fromSave(data));
                }
            }
            return List.copyOf(entities);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load entity data.", exception);
        }
    }

    public void saveEntities(List<EntityPersistenceData> entities) {
        try {
            Files.createDirectories(rootDirectory);
            try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(entitiesFile)))) {
                output.writeInt(ENTITY_FILE_VERSION);
                output.writeInt(entities.size());
                for (EntityPersistenceData entity : entities) {
                    writeEntity(output, entity);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to save entity data.", exception);
        }
    }

    public void applyPlayerData(Inventory inventory, PlayerSaveData playerData) {
        inventory.clear();
        for (int i = 0; i < Math.min(inventory.getSlotCount(), playerData.slots().length); i++) {
            inventory.setSlot(i, playerData.slots()[i]);
        }
        inventory.setSelectedHotbarSlot(Math.clamp(playerData.selectedHotbarSlot(), 0, inventory.getHotbarSize() - 1));
    }

    private void writeItemStack(DataOutputStream output, ItemStack stack) throws IOException {
        if (stack == null) {
            output.writeBoolean(false);
            return;
        }

        output.writeBoolean(true);
        output.writeUTF(stack.getItem().getId());
        output.writeInt(stack.getCount());
    }

    private void writeEntity(DataOutputStream output, EntityPersistenceData entity) throws IOException {
        output.writeUTF(entity.typeId());
        Vector3f position = entity.position();
        Vector3f velocity = entity.velocity();
        output.writeFloat(position.x);
        output.writeFloat(position.y);
        output.writeFloat(position.z);
        output.writeFloat(velocity.x);
        output.writeFloat(velocity.y);
        output.writeFloat(velocity.z);
        output.writeInt(entity.data().size());
        for (Map.Entry<String, String> entry : entity.data().entrySet()) {
            output.writeUTF(entry.getKey());
            output.writeUTF(entry.getValue());
        }
    }

    private EntityPersistenceData readEntity(DataInputStream input) throws IOException {
        String typeId = input.readUTF();
        Vector3f position = new Vector3f(input.readFloat(), input.readFloat(), input.readFloat());
        Vector3f velocity = new Vector3f(input.readFloat(), input.readFloat(), input.readFloat());
        int dataCount = input.readInt();
        Map<String, String> data = new LinkedHashMap<>();
        for (int i = 0; i < dataCount; i++) {
            data.put(input.readUTF(), input.readUTF());
        }
        return new EntityPersistenceData(typeId, position, velocity, data);
    }

    private void writeFurnace(DataOutputStream output, FurnaceBlockEntity furnace) throws IOException {
        writeItemStack(output, furnace.getInput());
        writeItemStack(output, furnace.getFuel());
        writeItemStack(output, furnace.getOutput());
        output.writeInt(furnace.getBurnTicksRemaining());
        output.writeInt(furnace.getBurnTicksTotal());
        output.writeInt(furnace.getCookTicks());
    }

    private FurnaceBlockEntity readFurnace(DataInputStream input) throws IOException {
        FurnaceBlockEntity furnace = new FurnaceBlockEntity();
        furnace.setInput(readItemStack(input).orElse(null));
        furnace.setFuel(readItemStack(input).orElse(null));
        furnace.setOutput(readItemStack(input).orElse(null));
        furnace.setBurnTicksRemaining(input.readInt());
        furnace.setBurnTicksTotal(input.readInt());
        furnace.setCookTicks(input.readInt());
        return furnace;
    }

    private Optional<ItemStack> readItemStack(DataInputStream input) throws IOException {
        if (!input.readBoolean()) {
            return Optional.empty();
        }

        String itemId = input.readUTF();
        int count = input.readInt();
        Item item = Items.byId(itemId)
                .orElseThrow(() -> new IOException("Unknown saved item id: " + itemId));
        return Optional.of(new ItemStack(item, count));
    }

    private Path chunkFile(int chunkX, int chunkZ) {
        return chunkDirectory.resolve("chunk_%d_%d.bin".formatted(chunkX, chunkZ));
    }
}
