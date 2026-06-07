package com.example.voxelgame.core;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_E;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_F4;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_F10;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_F9;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;
import static org.lwjgl.glfw.GLFW.glfwGetTime;

import java.util.Objects;

import com.example.voxelgame.game.Player;
import com.example.voxelgame.game.entity.EntityManager;
import com.example.voxelgame.game.entity.animal.PassiveMobSpawner;
import com.example.voxelgame.game.entity.villager.VillagerSpawner;
import com.example.voxelgame.game.ui.UiSystem;
import com.example.voxelgame.game.world.WorldInteractionSystem;
import com.example.voxelgame.render.Renderer;
import com.example.voxelgame.save.PlayerSaveData;
import com.example.voxelgame.save.SaveManager;
import com.example.voxelgame.world.BlockRegistry;
import com.example.voxelgame.world.VoxelWorld;
import com.example.voxelgame.world.WorldTime;

public final class GameApplication {
    private static final double FIXED_UPDATE_SECONDS = 1.0 / 60.0;
    private static final double MAX_FRAME_TIME_SECONDS = 0.25;

    private final GameWindow window;
    private final Renderer renderer;
    private final VoxelWorld world;
    private final Player player;
    private final EntityManager entityManager;
    private final UiSystem uiSystem;
    private final WorldInteractionSystem worldInteractionSystem;
    private final PassiveMobSpawner passiveMobSpawner = new PassiveMobSpawner(12345L);
    private final VillagerSpawner villagerSpawner = new VillagerSpawner();
    private final FrameStatistics frameStatistics;
    private final LaunchOptions launchOptions;
    private final WorldTime worldTime;
    private final SaveManager saveManager;
    private boolean recaptureCursorAfterInventory;

    public GameApplication(
            GameWindow window,
            Renderer renderer,
            VoxelWorld world,
            Player player,
            EntityManager entityManager,
            UiSystem uiSystem,
            WorldInteractionSystem worldInteractionSystem,
            LaunchOptions launchOptions,
            WorldTime worldTime,
            SaveManager saveManager
    ) {
        this.window = Objects.requireNonNull(window, "Window cannot be null.");
        this.renderer = Objects.requireNonNull(renderer, "Renderer cannot be null.");
        this.world = Objects.requireNonNull(world, "World cannot be null.");
        this.player = Objects.requireNonNull(player, "Player cannot be null.");
        this.entityManager = Objects.requireNonNull(entityManager, "Entity manager cannot be null.");
        this.uiSystem = Objects.requireNonNull(uiSystem, "UI system cannot be null.");
        this.worldInteractionSystem = Objects.requireNonNull(worldInteractionSystem, "World interaction system cannot be null.");
        this.launchOptions = Objects.requireNonNull(launchOptions, "Launch options cannot be null.");
        this.worldTime = Objects.requireNonNull(worldTime, "World time cannot be null.");
        this.saveManager = Objects.requireNonNull(saveManager, "Save manager cannot be null.");
        this.frameStatistics = new FrameStatistics(window.getBaseTitle());
    }

    public void run() {
        try {
            initialize();
            loop();
        } finally {
            saveManager.savePlayer(player);
            saveManager.saveEntities(entityManager.savePersistentEntities());
            world.close();
            world.saveAllChunks();
            renderer.close();
            window.close();
        }
    }

    private void initialize() {
        window.initialize();
        saveManager.loadPlayer().ifPresent(this::applyLoadedPlayerData);
        if (launchOptions.spawnNearVillage()) {
            player.setPosition(player.getSpawnPoint());
        }
        world.updateAround(player.getPosition());
        entityManager.replacePersistentEntities(saveManager.loadEntities());
        renderer.initialize(window);
    }

    private void applyLoadedPlayerData(PlayerSaveData playerSaveData) {
        saveManager.applyPlayerData(player.getInventory(), playerSaveData);
        player.setPosition(playerSaveData.position());
        player.setGameMode(playerSaveData.gameMode());
        player.restoreVitals(playerSaveData.health(), playerSaveData.hunger());
    }

    private void loop() {
        double startTime = glfwGetTime();
        double previousTime = startTime;
        double accumulator = 0.0;

        while (!window.shouldClose()) {
            double currentTime = glfwGetTime();
            double frameTime = Math.min(currentTime - previousTime, MAX_FRAME_TIME_SECONDS);
            previousTime = currentTime;
            accumulator += frameTime;

            if (launchOptions.hasRuntimeLimit() && currentTime - startTime >= launchOptions.maxRuntimeSeconds()) {
                break;
            }

            window.pollEvents();
            update(frameTime);

            while (accumulator >= FIXED_UPDATE_SECONDS) {
                fixedUpdate();
                accumulator -= FIXED_UPDATE_SECONDS;
            }

            renderer.render(
                    window,
                    player.getCamera(),
                    uiSystem.createRenderState(
                            player,
                            worldInteractionSystem,
                            world,
                            worldTime,
                            window.isCursorCaptured(),
                            window.getInputState()
                    ),
                    worldTime,
                    entityManager
            );
            window.swapBuffers();
            frameStatistics.recordFrame(frameTime);
            window.setTitle(window.getBaseTitle());
            window.finishFrame();
        }
    }

    private void update(double deltaTimeSeconds) {
        worldTime.advance(deltaTimeSeconds);
        handleWindowControls();
        handleBlockUiInteractions();

        if (uiSystem.isInventoryOpen()) {
            uiSystem.handleInput(
                    window.getInputState(),
                    player,
                    window.getFramebufferWidth(),
                    window.getFramebufferHeight()
            );
        }

        boolean movementInputActive = !uiSystem.isInventoryOpen();
        boolean interactionInputActive = window.isCursorCaptured() && !uiSystem.isInventoryOpen();
        player.update(
                window.getInputState(),
                world,
                worldInteractionSystem,
                entityManager,
                movementInputActive,
                interactionInputActive,
                deltaTimeSeconds
        );
        entityManager.update(deltaTimeSeconds, world, player, worldTime);
        passiveMobSpawner.update(deltaTimeSeconds, world, player, entityManager);
        villagerSpawner.update(deltaTimeSeconds, world, player, entityManager);
        world.updateAround(player.getPosition());
    }

    private void handleWindowControls() {
        var inputState = window.getInputState();

        if (inputState.wasKeyPressed(GLFW_KEY_E)) {
            if (uiSystem.isInventoryOpen()) {
                closeMenu();
            } else {
                openInventory();
            }
        }

        if (inputState.wasKeyPressed(GLFW_KEY_ESCAPE)) {
            if (uiSystem.isInventoryOpen()) {
                closeMenu();
            } else if (window.isCursorCaptured()) {
                window.releaseCursor();
            } else {
                window.requestClose();
            }
        }

        if (inputState.wasKeyPressed(GLFW_KEY_F10)) {
            window.requestClose();
        }

        if (inputState.wasKeyPressed(GLFW_KEY_F9)) {
            uiSystem.toggleDebug();
        }

        if (inputState.wasKeyPressed(GLFW_KEY_F4)) {
            player.toggleGameMode();
        }
    }

    private void fixedUpdate() {
        player.fixedUpdate(entityManager);
        world.tickWater();
        world.tickFurnaces();
        frameStatistics.recordUpdate();
    }

    private void openInventory() {
        recaptureCursorAfterInventory = window.isCursorCaptured();
        uiSystem.openInventory();
        window.setAutoCaptureEnabled(false);
        window.releaseCursor();
    }

    private void openCraftingTable() {
        recaptureCursorAfterInventory = window.isCursorCaptured();
        uiSystem.openCraftingTable();
        window.setAutoCaptureEnabled(false);
        window.releaseCursor();
    }

    private void openFurnace(com.example.voxelgame.game.furnace.FurnaceBlockEntity furnace) {
        recaptureCursorAfterInventory = window.isCursorCaptured();
        uiSystem.openFurnace(furnace);
        window.setAutoCaptureEnabled(false);
        window.releaseCursor();
    }

    private void closeMenu() {
        uiSystem.closeMenu();
        window.setAutoCaptureEnabled(true);
        if (recaptureCursorAfterInventory) {
            window.captureCursor();
        }
        recaptureCursorAfterInventory = false;
    }

    private void handleBlockUiInteractions() {
        if (uiSystem.isInventoryOpen() || !window.isCursorCaptured()) {
            return;
        }

        var inputState = window.getInputState();
        if (!inputState.wasMouseButtonPressed(GLFW_MOUSE_BUTTON_RIGHT)) {
            return;
        }

        if (entityManager.findVillagerInteraction(player, 4.0f, 0.86f).isPresent()) {
            entityManager.findVillagerInteraction(player, 4.0f, 0.86f).ifPresent(this::openTrading);
            return;
        }

        world.raycast(player.getEyePosition(), player.getForward(), 8.0f)
                .ifPresent(hit -> {
                    if (world.getBlockAtWorld(hit.blockX(), hit.blockY(), hit.blockZ()) == BlockRegistry.CRAFTING_TABLE) {
                        openCraftingTable();
                    } else if (BlockRegistry.isFurnace(world.getBlockAtWorld(hit.blockX(), hit.blockY(), hit.blockZ()))) {
                        world.getFurnaceAtWorld(hit.blockX(), hit.blockY(), hit.blockZ()).ifPresent(this::openFurnace);
                    }
                });
    }

    private void openTrading(com.example.voxelgame.game.entity.villager.VillagerEntity villager) {
        recaptureCursorAfterInventory = window.isCursorCaptured();
        uiSystem.openTrading(villager);
        window.setAutoCaptureEnabled(false);
        window.releaseCursor();
    }
}
