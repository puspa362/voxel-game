package com.example.voxelgame.game.ui;

import com.example.voxelgame.game.inventory.ArmorSlot;
import com.example.voxelgame.game.inventory.Inventory;
import java.util.ArrayList;
import java.util.List;

public final class UiLayout {
    public static final float BASE_SLOT_SIZE = 44.0f;
    public static final float BASE_SLOT_GAP = 6.0f;
    public static final float BASE_PANEL_PADDING = 24.0f;
    public static final float BASE_SECTION_GAP = 32.0f;
    public static final float BASE_SECTION_HEADER_HEIGHT = 22.0f;
    public static final float BASE_SECTION_TOP_PADDING = 18.0f;
    public static final float BASE_HOTBAR_MARGIN = 18.0f;
    public static final float REFERENCE_WIDTH = 1920.0f;
    public static final float REFERENCE_HEIGHT = 1080.0f;

    private UiLayout() {
    }

    public static InventoryScreenLayout inventoryLayout(int screenWidth, int screenHeight, int inventoryColumns, int inventoryRows, int craftingWidth, int craftingHeight) {
        float scale = scale(screenWidth, screenHeight);
        float slotSize = slotSize(screenWidth, screenHeight);
        float slotGap = slotGap(screenWidth, screenHeight);
        float panelPadding = panelPadding(screenWidth, screenHeight);
        float sectionGap = sectionGap(screenWidth, screenHeight);
        float sectionHeaderHeight = sectionHeaderHeight(screenWidth, screenHeight);
        float sectionTopPadding = sectionTopPadding(screenWidth, screenHeight);

        float inventoryWidth = gridWidth(inventoryColumns, slotSize, slotGap);
        float inventoryHeight = gridHeight(inventoryRows, slotSize, slotGap);
        float armorWidth = slotSize;
        float armorHeight = gridHeight(ArmorSlot.values().length, slotSize, slotGap);
        float craftingGridWidth = gridWidth(craftingWidth, slotSize, slotGap);
        float craftingGridHeight = gridHeight(craftingHeight, slotSize, slotGap);
        float craftingWidthTotal = craftingGridWidth + 38.0f * scale + slotSize;
        float contentHeight = Math.max(inventoryHeight, Math.max(armorHeight, craftingGridHeight));

        float panelWidth = panelPadding * 2.0f
                + armorWidth
                + sectionGap
                + inventoryWidth
                + sectionGap
                + craftingWidthTotal;
        float panelHeight = panelPadding * 2.0f
                + sectionHeaderHeight
                + sectionTopPadding
                + contentHeight;

        UiRect panel = new UiRect(
                (screenWidth - panelWidth) * 0.5f,
                (screenHeight - panelHeight) * 0.5f,
                panelWidth,
                panelHeight
        );

        float contentTop = panel.y() + panelPadding + sectionHeaderHeight + sectionTopPadding;
        float sectionCenterY = contentTop + contentHeight * 0.5f;

        UiRect armorArea = new UiRect(
                panel.x() + panelPadding,
                sectionCenterY - armorHeight * 0.5f,
                armorWidth,
                armorHeight
        );
        UiRect inventoryArea = new UiRect(
                armorArea.x() + armorArea.width() + sectionGap,
                sectionCenterY - inventoryHeight * 0.5f,
                inventoryWidth,
                inventoryHeight
        );
        UiRect craftingArea = new UiRect(
                inventoryArea.x() + inventoryArea.width() + sectionGap,
                contentTop,
                craftingWidthTotal,
                craftingGridHeight
        );

        return new InventoryScreenLayout(panel, armorArea, inventoryArea, craftingArea);
    }

    public static UiRect hotbarSlot(int screenWidth, int screenHeight, int slot) {
        float slotSize = slotSize(screenWidth, screenHeight);
        float slotGap = slotGap(screenWidth, screenHeight);
        float totalWidth = inventoryColumns() * slotSize + (inventoryColumns() - 1) * slotGap;
        float startX = (screenWidth - totalWidth) * 0.5f;
        float y = screenHeight - slotSize - hotbarMargin(screenWidth, screenHeight);
        return new UiRect(startX + slot * (slotSize + slotGap), y, slotSize, slotSize);
    }

    public static UiRect inventorySlot(InventoryScreenLayout layout, int slot, int screenWidth, int screenHeight) {
        float slotSize = slotSize(screenWidth, screenHeight);
        float slotGap = slotGap(screenWidth, screenHeight);
        int columns = inventoryColumns();
        int row = slot / columns;
        int column = slot % columns;
        float x = layout.inventoryArea().x() + column * (slotSize + slotGap);
        float y = layout.inventoryArea().y() + row * (slotSize + slotGap);
        return new UiRect(x, y, slotSize, slotSize);
    }

    public static UiRect armorSlot(InventoryScreenLayout layout, ArmorSlot slot, int screenWidth, int screenHeight) {
        float slotSize = slotSize(screenWidth, screenHeight);
        float slotGap = slotGap(screenWidth, screenHeight);
        int index = switch (slot) {
            case HELMET -> 0;
            case CHESTPLATE -> 1;
            case LEGGINGS -> 2;
            case BOOTS -> 3;
        };
        float x = layout.armorArea().x();
        float y = layout.armorArea().y() + index * (slotSize + slotGap);
        return new UiRect(x, y, slotSize, slotSize);
    }

    public static UiRect craftingSlot(InventoryScreenLayout layout, int slot, int craftingWidth, int screenWidth, int screenHeight) {
        float slotSize = slotSize(screenWidth, screenHeight);
        float slotGap = slotGap(screenWidth, screenHeight);
        int row = slot / craftingWidth;
        int column = slot % craftingWidth;
        float x = layout.craftingArea().x() + column * (slotSize + slotGap);
        float y = layout.craftingArea().y() + row * (slotSize + slotGap);
        return new UiRect(x, y, slotSize, slotSize);
    }

    public static UiRect craftingResultSlot(InventoryScreenLayout layout, int craftingWidth, int craftingHeight, int screenWidth, int screenHeight) {
        float scale = scale(screenWidth, screenHeight);
        float slotSize = slotSize(screenWidth, screenHeight);
        float slotGap = slotGap(screenWidth, screenHeight);
        float gridWidth = gridWidth(craftingWidth, slotSize, slotGap);
        float gridHeight = gridHeight(craftingHeight, slotSize, slotGap);
        float x = layout.craftingArea().x() + gridWidth + 38.0f * scale;
        float y = layout.craftingArea().y() + (gridHeight - slotSize) * 0.5f;
        return new UiRect(x, y, slotSize, slotSize);
    }

    public static List<UiSlotRef> orderedInteractiveSlots(Inventory inventory) {
        List<UiSlotRef> slots = new ArrayList<>();
        for (int i = 0; i < inventory.getSlotCount(); i++) {
            slots.add(new UiSlotRef(UiSlotType.INVENTORY, i));
        }
        for (ArmorSlot slot : ArmorSlot.values()) {
            slots.add(new UiSlotRef(UiSlotType.ARMOR, slot.ordinal()));
        }
        int craftingSlots = inventory.getCraftingGrid().getWidth() * inventory.getCraftingGrid().getHeight();
        for (int i = 0; i < craftingSlots; i++) {
            slots.add(new UiSlotRef(UiSlotType.CRAFTING, i));
        }
        slots.add(new UiSlotRef(UiSlotType.CRAFTING_RESULT, 0));
        return slots;
    }

    public static UiRect rectFor(InventoryScreenLayout layout, Inventory inventory, UiSlotRef slotRef, int screenWidth, int screenHeight) {
        return switch (slotRef.type()) {
            case INVENTORY, CREATIVE -> inventorySlot(layout, slotRef.index(), screenWidth, screenHeight);
            case ARMOR -> armorSlot(layout, ArmorSlot.values()[slotRef.index()], screenWidth, screenHeight);
            case CRAFTING -> craftingSlot(layout, slotRef.index(), inventory.getCraftingGrid().getWidth(), screenWidth, screenHeight);
            case CRAFTING_RESULT -> craftingResultSlot(layout, inventory.getCraftingGrid().getWidth(), inventory.getCraftingGrid().getHeight(), screenWidth, screenHeight);
            case FURNACE_INPUT -> furnaceInputSlot(layout, screenWidth, screenHeight);
            case FURNACE_FUEL -> furnaceFuelSlot(layout, screenWidth, screenHeight);
            case FURNACE_OUTPUT -> furnaceOutputSlot(layout, screenWidth, screenHeight);
        };
    }

    public static UiRect furnaceInputSlot(InventoryScreenLayout layout, int screenWidth, int screenHeight) {
        float slotSize = slotSize(screenWidth, screenHeight);
        return new UiRect(layout.craftingArea().x(), layout.craftingArea().y(), slotSize, slotSize);
    }

    public static UiRect furnaceFuelSlot(InventoryScreenLayout layout, int screenWidth, int screenHeight) {
        float slotSize = slotSize(screenWidth, screenHeight);
        float slotGap = slotGap(screenWidth, screenHeight);
        return new UiRect(layout.craftingArea().x(), layout.craftingArea().y() + slotSize + slotGap, slotSize, slotSize);
    }

    public static UiRect furnaceOutputSlot(InventoryScreenLayout layout, int screenWidth, int screenHeight) {
        float slotSize = slotSize(screenWidth, screenHeight);
        float scale = scale(screenWidth, screenHeight);
        return new UiRect(layout.craftingArea().x() + slotSize + 76.0f * scale, layout.craftingArea().y() + (slotSize * 0.5f), slotSize, slotSize);
    }

    public static float sectionTitleX(UiRect sectionArea) {
        return sectionArea.x();
    }

    public static float sectionTitleY(InventoryScreenLayout layout, int screenWidth, int screenHeight) {
        return layout.panel().y() + panelPadding(screenWidth, screenHeight);
    }

    public static float craftingArrowX(InventoryScreenLayout layout, int craftingWidth, int screenWidth, int screenHeight) {
        return layout.craftingArea().x()
                + gridWidth(craftingWidth, slotSize(screenWidth, screenHeight), slotGap(screenWidth, screenHeight))
                + 8.0f * scale(screenWidth, screenHeight);
    }

    public static float craftingArrowY(InventoryScreenLayout layout, int craftingHeight, int screenWidth, int screenHeight) {
        return craftingResultSlot(layout, 1, craftingHeight, screenWidth, screenHeight).y() + 12.0f * scale(screenWidth, screenHeight);
    }

    public static float slotSize(int screenWidth, int screenHeight) {
        return BASE_SLOT_SIZE * scale(screenWidth, screenHeight);
    }

    public static float slotGap(int screenWidth, int screenHeight) {
        return BASE_SLOT_GAP * scale(screenWidth, screenHeight);
    }

    public static float panelPadding(int screenWidth, int screenHeight) {
        return BASE_PANEL_PADDING * scale(screenWidth, screenHeight);
    }

    public static float sectionGap(int screenWidth, int screenHeight) {
        return BASE_SECTION_GAP * scale(screenWidth, screenHeight);
    }

    public static float sectionHeaderHeight(int screenWidth, int screenHeight) {
        return BASE_SECTION_HEADER_HEIGHT * scale(screenWidth, screenHeight);
    }

    public static float sectionTopPadding(int screenWidth, int screenHeight) {
        return BASE_SECTION_TOP_PADDING * scale(screenWidth, screenHeight);
    }

    public static float hotbarMargin(int screenWidth, int screenHeight) {
        return BASE_HOTBAR_MARGIN * scale(screenWidth, screenHeight);
    }

    public static int inventoryColumns() {
        return 9;
    }

    private static float gridWidth(int columns, float slotSize, float slotGap) {
        return columns * slotSize + Math.max(0, columns - 1) * slotGap;
    }

    private static float gridHeight(int rows, float slotSize, float slotGap) {
        return rows * slotSize + Math.max(0, rows - 1) * slotGap;
    }

    private static float scale(int screenWidth, int screenHeight) {
        float widthScale = screenWidth / REFERENCE_WIDTH;
        float heightScale = screenHeight / REFERENCE_HEIGHT;
        return Math.clamp(Math.min(widthScale, heightScale), 0.75f, 1.35f);
    }
}
