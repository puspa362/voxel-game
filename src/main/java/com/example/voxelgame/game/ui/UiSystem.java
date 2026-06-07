package com.example.voxelgame.game.ui;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_DOWN;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_UP;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.example.voxelgame.core.InputState;
import com.example.voxelgame.game.Player;
import com.example.voxelgame.game.furnace.FurnaceBlockEntity;
import com.example.voxelgame.game.furnace.FurnaceRecipes;
import com.example.voxelgame.game.entity.villager.TradeOffer;
import com.example.voxelgame.game.entity.villager.VillagerEntity;
import com.example.voxelgame.game.inventory.ArmorSlot;
import com.example.voxelgame.game.inventory.CraftingGrid;
import com.example.voxelgame.game.inventory.Inventory;
import com.example.voxelgame.game.inventory.Item;
import com.example.voxelgame.game.inventory.ItemStack;
import com.example.voxelgame.game.mode.CreativeInventoryCatalog;
import com.example.voxelgame.game.world.WorldInteractionSystem;
import com.example.voxelgame.render.HudState;
import com.example.voxelgame.render.UiRenderState;
import com.example.voxelgame.world.Block;
import com.example.voxelgame.world.BlockRegistry;
import com.example.voxelgame.world.VoxelWorld;
import com.example.voxelgame.world.WorldTime;

public final class UiSystem {
    private static final System.Logger LOGGER = System.getLogger(UiSystem.class.getName());
    private static final int CREATIVE_VISIBLE_ROWS = 4;

    private final InventoryUI inventoryUI = new InventoryUI();
    private final CraftingUI craftingUI = new CraftingUI();
    private final CreativeInventoryCatalog creativeCatalog = new CreativeInventoryCatalog();

    private UiMode uiMode = UiMode.CLOSED;
    private boolean showDebug;
    private ItemStack carriedStack;
    private UiSlotRef hoveredSlot;
    private String lastRenderedInventorySignature = "";
    private int creativeScrollOffset;
    private FurnaceBlockEntity activeFurnace;
    private VillagerEntity activeVillager;

    public void openInventory() {
        uiMode = UiMode.INVENTORY;
    }

    public void openCraftingTable() {
        uiMode = UiMode.CRAFTING_TABLE;
        activeFurnace = null;
    }

    public void openFurnace(FurnaceBlockEntity furnace) {
        activeFurnace = Objects.requireNonNull(furnace, "Furnace cannot be null.");
        activeVillager = null;
        uiMode = UiMode.FURNACE;
    }

    public void openTrading(VillagerEntity villager) {
        activeVillager = Objects.requireNonNull(villager, "Villager cannot be null.");
        activeFurnace = null;
        uiMode = UiMode.TRADING;
    }

    public void closeMenu() {
        uiMode = UiMode.CLOSED;
        activeFurnace = null;
        activeVillager = null;
    }

    public void toggleDebug() {
        showDebug = !showDebug;
    }

    public boolean isInventoryOpen() {
        return uiMode != UiMode.CLOSED;
    }

    public boolean isCraftingTableOpen() {
        return uiMode == UiMode.CRAFTING_TABLE;
    }

    public UiMode getUiMode() {
        return uiMode;
    }

    public void handleInput(InputState inputState, Player player, int screenWidth, int screenHeight) {
        Objects.requireNonNull(inputState, "Input state cannot be null.");
        Objects.requireNonNull(player, "Player cannot be null.");
        Inventory playerInventory = player.getInventory();
        Objects.requireNonNull(playerInventory, "Inventory cannot be null.");

        hoveredSlot = null;
        if (!isInventoryOpen()) {
            return;
        }
        if (uiMode == UiMode.TRADING) {
            handleTradingInput(inputState, player);
            return;
        }

        Inventory activeCraftingInventory = activeCraftingInventory(playerInventory);
        boolean creativeInventory = isCreativeInventory(player);
        if (creativeInventory) {
            updateCreativePaging(inputState);
        }
        InventoryScreenLayout layout = UiLayout.inventoryLayout(
                screenWidth,
                screenHeight,
                playerInventory.getColumns(),
                playerInventory.getRows(),
                activeCraftingInventory.getCraftingGrid().getWidth(),
                activeCraftingInventory.getCraftingGrid().getHeight()
        );
        hoveredSlot = hitTest(layout, playerInventory, activeCraftingInventory, creativeInventory, inputState.getMouseX(), inputState.getMouseY(), screenWidth, screenHeight);

        if (inputState.wasMouseButtonPressed(GLFW_MOUSE_BUTTON_LEFT)) {
            handleLeftClick(playerInventory, activeCraftingInventory, creativeInventory, isShiftDown(inputState));
        }
        if (inputState.wasMouseButtonPressed(GLFW_MOUSE_BUTTON_RIGHT)) {
            handleRightClick(playerInventory, activeCraftingInventory, creativeInventory);
        }
    }

    public UiRenderState createRenderState(
            Player player,
            WorldInteractionSystem interactionSystem,
            VoxelWorld world,
            WorldTime worldTime,
            boolean cursorCaptured,
            InputState inputState
    ) {
        Inventory playerInventory = player.getInventory();
        Inventory activeCraftingInventory = activeCraftingInventory(playerInventory);
        CraftingGrid activeCraftingGrid = activeCraftingInventory.getCraftingGrid();

        return new UiRenderState(
                createHudState(player, interactionSystem, world, worldTime, cursorCaptured),
                isInventoryOpen(),
                isCreativeInventory(player),
                uiMode,
                menuTitle(),
                playerInventory.getSelectedHotbarSlot(),
                creativeCatalog.size(),
                creativeScrollOffset,
                creativeVisibleSlotCount(),
                activeFurnace == null ? 0.0f : activeFurnace.burnProgress(),
                activeFurnace == null ? 0.0f : activeFurnace.cookProgress(FurnaceRecipes.DEFAULT_COOK_TICKS),
                activeCraftingGrid.getWidth(),
                activeCraftingGrid.getHeight(),
                System.nanoTime() / 1_000_000_000.0,
                inputState.getMouseX(),
                inputState.getMouseY(),
                carriedStack == null ? null : carriedStack.copy(),
                hoveredSlot,
                hoveredTooltipText(playerInventory, activeCraftingInventory),
                tradeLines(),
                collectSlotItems(player, activeCraftingInventory)
        );
    }

    private void handleTradingInput(InputState inputState, Player player) {
        if (activeVillager == null || !inputState.wasMouseButtonPressed(GLFW_MOUSE_BUTTON_LEFT)) {
            return;
        }
        int offerCount = activeVillager.getTradeOffers().size();
        if (offerCount == 0) {
            return;
        }
        int index = (int) Math.floor(Math.max(0.0, inputState.getMouseY() - 230.0) / 28.0);
        if (index >= 0 && index < offerCount) {
            activeVillager.tryTrade(player, index);
        }
    }

    private HudState createHudState(
            Player player,
            WorldInteractionSystem interactionSystem,
            VoxelWorld world,
            WorldTime worldTime,
            boolean cursorCaptured
    ) {
        Objects.requireNonNull(player, "Player cannot be null.");
        Objects.requireNonNull(interactionSystem, "Interaction system cannot be null.");
        Objects.requireNonNull(world, "Voxel world cannot be null.");
        Objects.requireNonNull(worldTime, "World time cannot be null.");

        Optional<ItemStack> selectedStack = player.getSelectedItemStack();
        Block placementBlock = interactionSystem.selectedPlacementBlock(player).orElse(BlockRegistry.AIR);
        String selectedText = selectedStack
                .map(stack -> "%s x%d".formatted(stack.getItem().getDisplayName(), stack.getCount()))
                .orElse("empty");
        String toolText = selectedStack
                .map(ItemStack::getItem)
                .filter(Item::isTool)
                .map(item -> "%s %s".formatted(item.getToolTier().orElseThrow().name(), item.getToolType().orElseThrow().name()))
                .orElse("hand");
        String targetText = interactionSystem.getCurrentTarget()
                .map(hit -> "Target: %d, %d, %d".formatted(hit.blockX(), hit.blockY(), hit.blockZ()))
                .orElse("Target: none");

        String debugText = """
                Pos: %.1f %.1f %.1f
                Vel: %.1f %.1f %.1f
                Time: %s
                Biome: %s
                Chunks: %d
                Villages: %d
                Pending: %d
                Generating: %d
                Avg gen: %.2f ms
                Grounded: %s
                Hotbar: %d
                Selected: %s
                Tool: %s
                Mode: %s
                %s
                %s
                %s
                Inventory: %s
                %s
                """.formatted(
                player.getPosition().x,
                player.getPosition().y,
                player.getPosition().z,
                player.getVelocity().x,
                player.getVelocity().y,
                player.getVelocity().z,
                worldTime.formatTimeOfDay(),
                world.getBiomeDebugText((int) Math.floor(player.getPosition().x), (int) Math.floor(player.getPosition().z)),
                world.getLoadedChunkCount(),
                world.getLoadedVillageFeatures().size(),
                world.getPendingChunkLoadCount(),
                world.getChunkGenerationCompletedCount(),
                world.getChunkGenerationAverageMillis(),
                player.isOnGround() ? "yes" : "no",
                player.getInventory().getSelectedHotbarSlot() + 1,
                selectedText,
                toolText,
                player.getGameMode().displayName(),
                player.getMiningDebugText(),
                player.getWaterDebugText(),
                world.getWaterDebugText(),
                isInventoryOpen() ? "open" : "closed",
                targetText
        );

        return new HudState(
                interactionSystem.getCurrentTarget(),
                player.getMiningTarget(),
                player.getMiningProgress(),
                placementBlock,
                world.getLoadedChunkCount(),
                player.isOnGround(),
                cursorCaptured,
                showDebug,
                player.getCurrentHealth(),
                player.getMaxHealth(),
                player.getHunger(),
                player.getMaxHunger(),
                player.getDamageFlashRemainingSeconds(),
                worldTime.formatTimeOfDay(),
                player.getGameMode().displayName(),
                debugText
        );
    }

    private Map<UiSlotRef, ItemStack> collectSlotItems(Player player, Inventory activeCraftingInventory) {
        Inventory playerInventory = player.getInventory();
        Map<UiSlotRef, ItemStack> slotItems = new LinkedHashMap<>();
        for (int i = 0; i < playerInventory.getSlotCount(); i++) {
            ItemStack stack = playerInventory.getSlot(i).orElse(null);
            if (stack != null) {
                slotItems.put(new UiSlotRef(UiSlotType.INVENTORY, i), stack);
            }
        }
        for (ArmorSlot slot : ArmorSlot.values()) {
            ItemStack stack = playerInventory.getArmor(slot).orElse(null);
            if (stack != null) {
                slotItems.put(new UiSlotRef(UiSlotType.ARMOR, slot.ordinal()), stack);
            }
        }

        if (isCreativeInventory(player)) {
            normalizeCreativeScrollOffset();
            int visibleItems = Math.min(creativeVisibleSlotCount(), creativeCatalog.size() - creativeScrollOffset);
            for (int i = 0; i < visibleItems; i++) {
                Item item = creativeCatalog.get(creativeScrollOffset + i);
                slotItems.put(new UiSlotRef(UiSlotType.CREATIVE, i), new ItemStack(item, item.getMaxStackSize()));
            }
        }

        if (uiMode == UiMode.FURNACE && activeFurnace != null) {
            putIfNotNull(slotItems, new UiSlotRef(UiSlotType.FURNACE_INPUT, 0), activeFurnace.getInput());
            putIfNotNull(slotItems, new UiSlotRef(UiSlotType.FURNACE_FUEL, 0), activeFurnace.getFuel());
            putIfNotNull(slotItems, new UiSlotRef(UiSlotType.FURNACE_OUTPUT, 0), activeFurnace.getOutput());
        }

        if (!isCreativeInventory(player)) {
            CraftingGrid craftingGrid = activeCraftingInventory.getCraftingGrid();
            int craftingSlots = craftingGrid.getWidth() * craftingGrid.getHeight();
            for (int i = 0; i < craftingSlots; i++) {
                int x = i % craftingGrid.getWidth();
                int y = i / craftingGrid.getWidth();
                ItemStack stack = craftingGrid.getSlot(x, y).orElse(null);
                if (stack != null) {
                    slotItems.put(new UiSlotRef(UiSlotType.CRAFTING, i), stack);
                }
            }
            ItemStack craftingResult = activeCraftingInventory.getCraftingResult().orElse(null);
            if (craftingResult != null) {
                slotItems.put(new UiSlotRef(UiSlotType.CRAFTING_RESULT, 0), craftingResult);
            }
        }

        String signature = describeInventorySlots(slotItems);
        if (!signature.equals(lastRenderedInventorySignature)) {
            LOGGER.log(
                    System.Logger.Level.INFO,
                    "UI render inventory snapshot: inventory=#{0}, crafting=#{1}, inventoryOpen={2}, slots={3}",
                    Integer.toHexString(System.identityHashCode(playerInventory)),
                    Integer.toHexString(System.identityHashCode(activeCraftingInventory)),
                    isInventoryOpen(),
                    signature
            );
            lastRenderedInventorySignature = signature;
        }
        return slotItems;
    }

    private UiSlotRef hitTest(
            InventoryScreenLayout layout,
            Inventory playerInventory,
            Inventory activeCraftingInventory,
            boolean creativeInventory,
            double mouseX,
            double mouseY,
            int screenWidth,
            int screenHeight
    ) {
        if (creativeInventory) {
            normalizeCreativeScrollOffset();
            for (int i = 0; i < Math.min(creativeVisibleSlotCount(), creativeCatalog.size() - creativeScrollOffset); i++) {
                UiSlotRef slotRef = new UiSlotRef(UiSlotType.CREATIVE, i);
                if (UiLayout.inventorySlot(layout, i, screenWidth, screenHeight).contains(mouseX, mouseY)) {
                    return slotRef;
                }
            }
            return null;
        }

        if (uiMode == UiMode.FURNACE && activeFurnace != null) {
            for (UiSlotRef slotRef : new UiSlotRef[]{
                    new UiSlotRef(UiSlotType.FURNACE_INPUT, 0),
                    new UiSlotRef(UiSlotType.FURNACE_FUEL, 0),
                    new UiSlotRef(UiSlotType.FURNACE_OUTPUT, 0)
            }) {
                if (UiLayout.rectFor(layout, playerInventory, slotRef, screenWidth, screenHeight).contains(mouseX, mouseY)) {
                    return slotRef;
                }
            }
        }

        for (int i = 0; i < playerInventory.getSlotCount(); i++) {
            UiSlotRef slotRef = new UiSlotRef(UiSlotType.INVENTORY, i);
            if (UiLayout.inventorySlot(layout, i, screenWidth, screenHeight).contains(mouseX, mouseY)) {
                return slotRef;
            }
        }
        if (uiMode == UiMode.FURNACE) {
            return null;
        }
        for (ArmorSlot slot : ArmorSlot.values()) {
            UiSlotRef slotRef = new UiSlotRef(UiSlotType.ARMOR, slot.ordinal());
            if (UiLayout.armorSlot(layout, slot, screenWidth, screenHeight).contains(mouseX, mouseY)) {
                return slotRef;
            }
        }
        CraftingGrid craftingGrid = activeCraftingInventory.getCraftingGrid();
        int craftingSlots = craftingGrid.getWidth() * craftingGrid.getHeight();
        for (int i = 0; i < craftingSlots; i++) {
            UiSlotRef slotRef = new UiSlotRef(UiSlotType.CRAFTING, i);
            if (UiLayout.craftingSlot(layout, i, craftingGrid.getWidth(), screenWidth, screenHeight).contains(mouseX, mouseY)) {
                return slotRef;
            }
        }
        UiRect resultRect = UiLayout.craftingResultSlot(
                layout,
                craftingGrid.getWidth(),
                craftingGrid.getHeight(),
                screenWidth,
                screenHeight
        );
        return resultRect.contains(mouseX, mouseY) ? new UiSlotRef(UiSlotType.CRAFTING_RESULT, 0) : null;
    }

    private void handleLeftClick(Inventory playerInventory, Inventory activeCraftingInventory, boolean creativeInventory, boolean shiftDown) {
        if (hoveredSlot == null) {
            return;
        }

        if (creativeInventory && hoveredSlot.type() == UiSlotType.CREATIVE) {
            Item item = creativeCatalog.get(creativeScrollOffset + hoveredSlot.index());
            playerInventory.setSlot(playerInventory.getSelectedHotbarSlot(), new ItemStack(item, item.getMaxStackSize()));
            carriedStack = null;
            return;
        }

        if (hoveredSlot.type() == UiSlotType.INVENTORY && hoveredSlot.index() < playerInventory.getHotbarSize()) {
            playerInventory.setSelectedHotbarSlot(hoveredSlot.index());
        }

        if (hoveredSlot.type() == UiSlotType.CRAFTING_RESULT) {
            handleCraftingResultClick(activeCraftingInventory);
            return;
        }

        if (shiftDown) {
            moveHoveredStack(playerInventory, activeCraftingInventory);
            return;
        }

        ItemStack slotStack = getSlotStack(playerInventory, activeCraftingInventory, hoveredSlot);
        if (carriedStack == null && slotStack == null) {
            return;
        }

        if (carriedStack == null) {
            setSlotStack(playerInventory, activeCraftingInventory, hoveredSlot, null);
            carriedStack = slotStack;
            return;
        }

        if (slotStack == null) {
            setSlotStack(playerInventory, activeCraftingInventory, hoveredSlot, carriedStack);
            carriedStack = null;
            return;
        }

        if (slotStack.canMerge(carriedStack) && !slotStack.isFull()) {
            int accepted = slotStack.add(carriedStack.getCount());
            carriedStack.remove(accepted);
            setSlotStack(playerInventory, activeCraftingInventory, hoveredSlot, slotStack);
            if (carriedStack.isEmpty()) {
                carriedStack = null;
            }
            return;
        }

        setSlotStack(playerInventory, activeCraftingInventory, hoveredSlot, carriedStack);
        carriedStack = slotStack;
    }

    private void handleRightClick(Inventory playerInventory, Inventory activeCraftingInventory, boolean creativeInventory) {
        if (hoveredSlot == null || hoveredSlot.type() == UiSlotType.CRAFTING_RESULT || hoveredSlot.type() == UiSlotType.FURNACE_OUTPUT) {
            return;
        }

        if (creativeInventory && hoveredSlot.type() == UiSlotType.CREATIVE) {
            Item item = creativeCatalog.get(creativeScrollOffset + hoveredSlot.index());
            playerInventory.setSlot(playerInventory.getSelectedHotbarSlot(), new ItemStack(item, 1));
            carriedStack = null;
            return;
        }

        if (hoveredSlot.type() == UiSlotType.INVENTORY && hoveredSlot.index() < playerInventory.getHotbarSize()) {
            playerInventory.setSelectedHotbarSlot(hoveredSlot.index());
        }

        ItemStack slotStack = getSlotStack(playerInventory, activeCraftingInventory, hoveredSlot);
        if (carriedStack == null) {
            if (slotStack == null) {
                return;
            }

            int takeCount = Math.max(1, (slotStack.getCount() + 1) / 2);
            carriedStack = new ItemStack(slotStack.getItem(), takeCount);
            slotStack.remove(takeCount);
            setSlotStack(playerInventory, activeCraftingInventory, hoveredSlot, slotStack.isEmpty() ? null : slotStack);
            return;
        }

        if (slotStack == null) {
            setSlotStack(playerInventory, activeCraftingInventory, hoveredSlot, new ItemStack(carriedStack.getItem(), 1));
            carriedStack.remove(1);
            if (carriedStack.isEmpty()) {
                carriedStack = null;
            }
            return;
        }

        if (slotStack.canMerge(carriedStack) && !slotStack.isFull()) {
            slotStack.add(1);
            carriedStack.remove(1);
            setSlotStack(playerInventory, activeCraftingInventory, hoveredSlot, slotStack);
            if (carriedStack.isEmpty()) {
                carriedStack = null;
            }
        }
    }

    private void handleCraftingResultClick(Inventory activeCraftingInventory) {
        Optional<ItemStack> crafted = activeCraftingInventory.getCraftingResult();
        if (crafted.isEmpty()) {
            return;
        }

        ItemStack resultStack = crafted.get();
        if (carriedStack == null) {
            Optional<ItemStack> taken = activeCraftingInventory.takeCraftingResult();
            if (taken.isPresent()) {
                carriedStack = taken.get();
            }
            return;
        }

        if (carriedStack.canMerge(resultStack) && carriedStack.getRemainingCapacity() >= resultStack.getCount()) {
            Optional<ItemStack> taken = activeCraftingInventory.takeCraftingResult();
            if (taken.isPresent()) {
                carriedStack.add(taken.get().getCount());
            }
        }
    }

    private ItemStack getSlotStack(Inventory playerInventory, Inventory activeCraftingInventory, UiSlotRef slotRef) {
        return switch (slotRef.type()) {
            case INVENTORY -> playerInventory.getSlot(slotRef.index()).map(ItemStack::copy).orElse(null);
            case CREATIVE -> {
                Item item = creativeCatalog.get(creativeScrollOffset + slotRef.index());
                yield new ItemStack(item, item.getMaxStackSize());
            }
            case ARMOR -> playerInventory.getArmor(ArmorSlot.values()[slotRef.index()]).map(ItemStack::copy).orElse(null);
            case CRAFTING -> {
                CraftingGrid craftingGrid = activeCraftingInventory.getCraftingGrid();
                int x = slotRef.index() % craftingGrid.getWidth();
                int y = slotRef.index() / craftingGrid.getWidth();
                yield craftingGrid.getSlot(x, y).map(ItemStack::copy).orElse(null);
            }
            case CRAFTING_RESULT -> activeCraftingInventory.getCraftingResult().map(ItemStack::copy).orElse(null);
            case FURNACE_INPUT -> activeFurnace == null ? null : activeFurnace.getInput();
            case FURNACE_FUEL -> activeFurnace == null ? null : activeFurnace.getFuel();
            case FURNACE_OUTPUT -> activeFurnace == null ? null : activeFurnace.getOutput();
        };
    }

    private void setSlotStack(Inventory playerInventory, Inventory activeCraftingInventory, UiSlotRef slotRef, ItemStack stack) {
        switch (slotRef.type()) {
            case INVENTORY -> playerInventory.setSlot(slotRef.index(), stack);
            case CREATIVE -> {
            }
            case ARMOR -> playerInventory.equipArmor(ArmorSlot.values()[slotRef.index()], stack);
            case CRAFTING -> {
                CraftingGrid craftingGrid = activeCraftingInventory.getCraftingGrid();
                int x = slotRef.index() % craftingGrid.getWidth();
                int y = slotRef.index() / craftingGrid.getWidth();
                craftingGrid.setSlot(x, y, stack);
            }
            case CRAFTING_RESULT -> {
            }
            case FURNACE_INPUT -> {
                if (activeFurnace != null) {
                    activeFurnace.setInput(stack);
                }
            }
            case FURNACE_FUEL -> {
                if (activeFurnace != null) {
                    activeFurnace.setFuel(stack);
                }
            }
            case FURNACE_OUTPUT -> {
                if (activeFurnace != null) {
                    activeFurnace.setOutput(stack);
                }
            }
        }
    }

    private void moveHoveredStack(Inventory playerInventory, Inventory activeCraftingInventory) {
        if (hoveredSlot == null || hoveredSlot.type() == UiSlotType.CRAFTING_RESULT || hoveredSlot.type() == UiSlotType.FURNACE_OUTPUT) {
            return;
        }

        ItemStack original = getSlotStack(playerInventory, activeCraftingInventory, hoveredSlot);
        if (original == null) {
            return;
        }

        ItemStack remainder = switch (hoveredSlot.type()) {
            case INVENTORY -> moveInventoryStack(playerInventory, hoveredSlot.index(), original.copy());
            case CREATIVE -> null;
            case ARMOR, CRAFTING, FURNACE_INPUT, FURNACE_FUEL -> moveIntoInventory(playerInventory, original.copy(), true);
            case CRAFTING_RESULT, FURNACE_OUTPUT -> original.copy();
        };

        if (remainder == null) {
            setSlotStack(playerInventory, activeCraftingInventory, hoveredSlot, null);
        } else if (remainder.getCount() != original.getCount()) {
            setSlotStack(playerInventory, activeCraftingInventory, hoveredSlot, remainder);
        }
    }

    private ItemStack moveInventoryStack(Inventory inventory, int sourceIndex, ItemStack moving) {
        boolean sourceIsHotbar = sourceIndex < inventory.getHotbarSize();
        ItemStack remainder = sourceIsHotbar
                ? moveIntoInventoryRange(inventory, moving, inventory.getHotbarSize(), inventory.getSlotCount())
                : moveIntoInventoryRange(inventory, moving, 0, inventory.getHotbarSize());
        if (remainder != null) {
            remainder = sourceIsHotbar
                    ? moveIntoInventoryRange(inventory, remainder, 0, inventory.getHotbarSize())
                    : moveIntoInventoryRange(inventory, remainder, inventory.getHotbarSize(), inventory.getSlotCount());
        }
        return remainder;
    }

    private ItemStack moveIntoInventory(Inventory inventory, ItemStack moving, boolean preferMainInventory) {
        ItemStack remainder = moving;
        if (preferMainInventory) {
            remainder = moveIntoInventoryRange(inventory, remainder, inventory.getHotbarSize(), inventory.getSlotCount());
            if (remainder != null) {
                remainder = moveIntoInventoryRange(inventory, remainder, 0, inventory.getHotbarSize());
            }
            return remainder;
        }

        remainder = moveIntoInventoryRange(inventory, remainder, 0, inventory.getHotbarSize());
        if (remainder != null) {
            remainder = moveIntoInventoryRange(inventory, remainder, inventory.getHotbarSize(), inventory.getSlotCount());
        }
        return remainder;
    }

    private ItemStack moveIntoInventoryRange(Inventory inventory, ItemStack moving, int startInclusive, int endExclusive) {
        if (moving == null) {
            return null;
        }

        int remaining = moving.getCount();
        for (int i = startInclusive; i < endExclusive && remaining > 0; i++) {
            ItemStack existing = inventory.getSlot(i).orElse(null);
            if (existing != null && existing.canMerge(moving) && !existing.isFull()) {
                int accepted = existing.add(remaining);
                remaining -= accepted;
                inventory.setSlot(i, existing);
            }
        }

        for (int i = startInclusive; i < endExclusive && remaining > 0; i++) {
            ItemStack existing = inventory.getSlot(i).orElse(null);
            if (existing == null) {
                int placed = Math.min(remaining, moving.getItem().getMaxStackSize());
                inventory.setSlot(i, new ItemStack(moving.getItem(), placed));
                remaining -= placed;
            }
        }

        return remaining == 0 ? null : new ItemStack(moving.getItem(), remaining);
    }

    private String hoveredTooltipText(Inventory playerInventory, Inventory activeCraftingInventory) {
        if (hoveredSlot == null) {
            return null;
        }
        ItemStack hoveredStack = getSlotStack(playerInventory, activeCraftingInventory, hoveredSlot);
        return hoveredStack == null ? null : hoveredStack.getItem().getDisplayName();
    }

    private void putIfNotNull(Map<UiSlotRef, ItemStack> slotItems, UiSlotRef slotRef, ItemStack stack) {
        if (stack != null) {
            slotItems.put(slotRef, stack);
        }
    }

    private Inventory activeCraftingInventory(Inventory playerInventory) {
        return uiMode == UiMode.CRAFTING_TABLE
                ? craftingUI.craftingInventory()
                : inventoryUI.craftingInventory(playerInventory);
    }

    private String menuTitle() {
        if (uiMode == UiMode.CRAFTING_TABLE) {
            return craftingUI.title();
        }
        if (uiMode == UiMode.FURNACE) {
            return "Furnace";
        }
        if (uiMode == UiMode.TRADING && activeVillager != null) {
            return "Trading - " + activeVillager.getProfession().name();
        }
        return inventoryUI.title();
    }

    private java.util.List<String> tradeLines() {
        if (uiMode != UiMode.TRADING || activeVillager == null) {
            return java.util.List.of();
        }
        java.util.ArrayList<String> lines = new java.util.ArrayList<>();
        for (TradeOffer offer : activeVillager.getTradeOffers()) {
            lines.add("%s x%d -> %s x%d (%d/%d)".formatted(
                    offer.cost().getItem().getDisplayName(),
                    offer.cost().getCount(),
                    offer.result().getItem().getDisplayName(),
                    offer.result().getCount(),
                    offer.uses(),
                    offer.maxUses()
            ));
        }
        return java.util.List.copyOf(lines);
    }

    private boolean isCreativeInventory(Player player) {
        return uiMode == UiMode.INVENTORY && player.getGameModeRules().hasCreativeInventory();
    }

    private void updateCreativePaging(InputState inputState) {
        int columns = UiLayout.inventoryColumns();
        int wheelRows = (int) Math.signum(inputState.getScrollY());
        if (wheelRows != 0) {
            creativeScrollOffset -= wheelRows * columns;
        }
        if (inputState.wasKeyPressed(GLFW_KEY_PAGE_DOWN)) {
            creativeScrollOffset += creativeVisibleSlotCount();
        }
        if (inputState.wasKeyPressed(GLFW_KEY_PAGE_UP)) {
            creativeScrollOffset -= creativeVisibleSlotCount();
        }
        normalizeCreativeScrollOffset();
    }

    private void normalizeCreativeScrollOffset() {
        if (creativeCatalog.size() <= creativeVisibleSlotCount()) {
            creativeScrollOffset = 0;
            return;
        }
        int columns = UiLayout.inventoryColumns();
        int totalRows = (creativeCatalog.size() + columns - 1) / columns;
        int maxFirstVisibleRow = Math.max(0, totalRows - CREATIVE_VISIBLE_ROWS);
        creativeScrollOffset = Math.clamp(creativeScrollOffset, 0, maxFirstVisibleRow * columns);
        creativeScrollOffset = (creativeScrollOffset / columns) * columns;
    }

    private int creativeVisibleSlotCount() {
        return UiLayout.inventoryColumns() * CREATIVE_VISIBLE_ROWS;
    }

    private boolean isShiftDown(InputState inputState) {
        return inputState.isKeyDown(GLFW_KEY_LEFT_SHIFT) || inputState.isKeyDown(GLFW_KEY_RIGHT_SHIFT);
    }

    private String describeInventorySlots(Map<UiSlotRef, ItemStack> slotItems) {
        StringBuilder builder = new StringBuilder();
        slotItems.entrySet().stream()
                .filter(entry -> entry.getKey().type() == UiSlotType.INVENTORY)
                .sorted(Map.Entry.comparingByKey((left, right) -> Integer.compare(left.index(), right.index())))
                .forEach(entry -> {
                    if (!builder.isEmpty()) {
                        builder.append(", ");
                    }
                    builder.append(entry.getKey().index())
                            .append('=')
                            .append(entry.getValue().getItem().getId())
                            .append('x')
                            .append(entry.getValue().getCount());
                });
        return builder.isEmpty() ? "empty" : builder.toString();
    }
}
