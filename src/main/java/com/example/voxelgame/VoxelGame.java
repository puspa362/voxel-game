package com.example.voxelgame;

import java.nio.file.Path;

import org.joml.Vector3f;

import com.example.voxelgame.camera.CameraConfig;
import com.example.voxelgame.camera.FirstPersonCamera;
import com.example.voxelgame.core.GameApplication;
import com.example.voxelgame.core.GameWindow;
import com.example.voxelgame.core.LaunchOptions;
import com.example.voxelgame.core.WindowConfig;
import com.example.voxelgame.game.Player;
import com.example.voxelgame.game.entity.EntityManager;
import com.example.voxelgame.game.inventory.Inventory;
import com.example.voxelgame.game.inventory.Items;
import com.example.voxelgame.game.ui.UiSystem;
import com.example.voxelgame.game.world.WorldInteractionSystem;
import com.example.voxelgame.render.Renderer;
import com.example.voxelgame.save.SaveManager;
import com.example.voxelgame.world.VoxelWorld;
import com.example.voxelgame.world.WorldTime;
import com.example.voxelgame.world.gen.TerrainGenerator;
import com.example.voxelgame.world.gen.TerrainGeneratorConfig;
import com.example.voxelgame.world.gen.village.VillageFeature;

public final class VoxelGame {
    private static final System.Logger LOGGER = System.getLogger(VoxelGame.class.getName());
    private static final Vector3f FALLBACK_SPAWN = new Vector3f(8.0f, 80.0f, 24.0f);

    private VoxelGame() {
    }

    public static void main(String[] args) {
        var launchOptions = LaunchOptions.fromArgs(args);
        var windowConfig = new WindowConfig(1600, 900, "Voxel Game", true);
        var cameraConfig = new CameraConfig(5.5f, 0.12f, 89.0f, 8.5f, 24.0f, 1.8f, 0.32f);
        var terrainGenerator = new TerrainGenerator(TerrainGeneratorConfig.defaultConfig(12345L));
        var saveManager = new SaveManager(Path.of("saves", "default"));
        var world = new VoxelWorld(
                terrainGenerator,
                saveManager,
                launchOptions.renderDistanceChunks(),
                launchOptions.chunkLoadsPerFrame()
        );
        var worldTime = new WorldTime(WorldTime.DEFAULT_DAY_LENGTH_SECONDS, 0.20);
        var window = new GameWindow(windowConfig);
        var renderer = new Renderer(world);
        Vector3f spawnPosition = launchOptions.spawnNearVillage()
                ? villageTestSpawn(terrainGenerator)
                : new Vector3f(FALLBACK_SPAWN);
        var camera = new FirstPersonCamera(cameraConfig, spawnPosition, -90.0f, -25.0f);
        var inventory = createStarterInventory();
        var player = new Player(camera, inventory);
        var entityManager = new EntityManager();
        var uiSystem = new UiSystem();
        var worldInteractionSystem = new WorldInteractionSystem(entityManager);
        var application = new GameApplication(
                window,
                renderer,
                world,
                player,
                entityManager,
                uiSystem,
                worldInteractionSystem,
                launchOptions,
                worldTime,
                saveManager
        );

        try {
            application.run();
        } catch (RuntimeException exception) {
            LOGGER.log(System.Logger.Level.ERROR, "Voxel Game terminated due to a fatal error.", exception);
            throw exception;
        }
    }

    private static Inventory createStarterInventory() {
        Inventory inventory = new Inventory(9, 4, 2, 2);
        inventory.addItem(Items.DIRT_BLOCK, 64);
        inventory.addItem(Items.GRASS_BLOCK, 64);
        inventory.addItem(Items.STONE_BLOCK, 64);
        inventory.addItem(Items.OAK_LOG_BLOCK, 64);
        inventory.addItem(Items.OAK_LEAVES_BLOCK, 64);
        inventory.addItem(Items.OAK_PLANKS_BLOCK, 64);
        inventory.addItem(Items.CRAFTING_TABLE_BLOCK, 8);
        inventory.addItem(Items.FURNACE_BLOCK, 8);
        inventory.addItem(Items.TORCH_BLOCK, 64);
        inventory.addItem(Items.SAND_BLOCK, 64);
        inventory.addItem(Items.SANDSTONE_BLOCK, 64);
        inventory.addItem(Items.SNOW_BLOCK, 64);
        inventory.addItem(Items.ICE_BLOCK, 64);
        inventory.addItem(Items.COAL_ORE_BLOCK, 32);
        inventory.addItem(Items.IRON_ORE_BLOCK, 32);
        inventory.addItem(Items.COPPER_ORE_BLOCK, 32);
        inventory.addItem(Items.GOLD_ORE_BLOCK, 32);
        inventory.addItem(Items.DIAMOND_ORE_BLOCK, 16);
        inventory.addItem(Items.MOSSY_STONE_BLOCK, 64);
        inventory.addItem(Items.GRAVEL_BLOCK, 64);
        inventory.addItem(Items.WOODEN_PICKAXE, 1);
        inventory.addItem(Items.WOODEN_AXE, 1);
        inventory.addItem(Items.WOODEN_SHOVEL, 1);
        inventory.addItem(Items.WOODEN_SWORD, 1);
        inventory.addItem(Items.STONE_PICKAXE, 1);
        inventory.addItem(Items.STONE_AXE, 1);
        inventory.addItem(Items.STONE_SHOVEL, 1);
        inventory.addItem(Items.IRON_PICKAXE, 1);
        inventory.addItem(Items.IRON_AXE, 1);
        inventory.addItem(Items.IRON_SHOVEL, 1);
        inventory.addItem(Items.OAK_PLANKS, 64);
        inventory.addItem(Items.CRAFTING_TABLE, 16);
        inventory.addItem(Items.APPLE, 16);
        inventory.setSelectedHotbarSlot(0);
        return inventory;
    }

    private static Vector3f villageTestSpawn(TerrainGenerator terrainGenerator) {
        return terrainGenerator.findNearestVillage(0, 0, 24)
                .map(VoxelGame::spawnNearVillage)
                .orElseGet(() -> {
                    LOGGER.log(System.Logger.Level.WARNING, "No generated village found near origin; using fallback spawn.");
                    return new Vector3f(FALLBACK_SPAWN);
                });
    }

    private static Vector3f spawnNearVillage(VillageFeature village) {
        Vector3f spawn = new Vector3f(village.centerX() + 6.5f, village.centerY() + 2.0f, village.centerZ() + 6.5f);
        LOGGER.log(
                System.Logger.Level.INFO,
                "Development village spawn selected: village=({0}, {1}, {2}), player=({3}, {4}, {5})",
                village.centerX(),
                village.centerY(),
                village.centerZ(),
                spawn.x,
                spawn.y,
                spawn.z
        );
        return spawn;
    }
}
