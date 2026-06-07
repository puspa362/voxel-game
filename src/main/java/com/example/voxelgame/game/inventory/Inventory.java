package com.example.voxelgame.game.inventory;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class Inventory {
    private static final System.Logger LOGGER = System.getLogger(Inventory.class.getName());

    private final int columns;
    private final int rows;
    private final ItemStack[] slots;
    private final Map<ArmorSlot, ItemStack> armorSlots = new EnumMap<>(ArmorSlot.class);
    private final Map<EquipmentSlot, ItemStack> equippedItems = new EnumMap<>(EquipmentSlot.class);
    private final CraftingGrid craftingGrid;
    private final CraftingManager craftingManager;
    private int selectedHotbarSlot;

    public Inventory(int columns, int rows) {
        this(columns, rows, 2, 2);
    }

    public Inventory(int columns, int rows, int craftingWidth, int craftingHeight) {
        if (columns < 1 || rows < 1) {
            throw new IllegalArgumentException("Inventory dimensions must be positive.");
        }

        this.columns = columns;
        this.rows = rows;
        this.slots = new ItemStack[columns * rows];
        this.craftingGrid = new CraftingGrid(craftingWidth, craftingHeight);
        this.craftingManager = CraftingRecipes.defaultManager();
    }

    public int getColumns() {
        return columns;
    }

    public int getRows() {
        return rows;
    }

    public int getSlotCount() {
        return slots.length;
    }

    public int getHotbarSize() {
        return columns;
    }

    public Optional<ItemStack> getSlot(int column, int row) {
        return getSlot(indexOf(column, row));
    }

    public Optional<ItemStack> getSlot(int index) {
        checkSlotIndex(index);
        ItemStack stack = slots[index];
        return stack == null ? Optional.empty() : Optional.of(stack.copy());
    }

    public void setSlot(int index, ItemStack stack) {
        checkSlotIndex(index);
        slots[index] = stack == null ? null : stack.copy();
    }

    public boolean addItem(Item item, int count) {
        Objects.requireNonNull(item, "Item cannot be null.");
        if (count < 1) {
            throw new IllegalArgumentException("Item count must be positive.");
        }

        LOGGER.log(
                System.Logger.Level.INFO,
                "Inventory addItem start: inventory=#{0}, item={1}, count={2}, slots={3}",
                Integer.toHexString(System.identityHashCode(this)),
                item.getId(),
                count,
                describeSlots()
        );

        int remaining = count;
        for (int i = 0; i < slots.length && remaining > 0; i++) {
            ItemStack stack = slots[i];
            if (stack != null && stack.getItem().isStackableWith(item) && !stack.isFull()) {
                remaining -= stack.add(remaining);
            }
        }

        for (int i = 0; i < slots.length && remaining > 0; i++) {
            if (slots[i] == null) {
                int stackSize = Math.min(remaining, item.getMaxStackSize());
                slots[i] = new ItemStack(item, stackSize);
                remaining -= stackSize;
            }
        }

        boolean inserted = remaining == 0;
        LOGGER.log(
                System.Logger.Level.INFO,
                "Inventory addItem end: inventory=#{0}, item={1}, inserted={2}, remaining={3}, slots={4}",
                Integer.toHexString(System.identityHashCode(this)),
                item.getId(),
                inserted,
                remaining,
                describeSlots()
        );
        return inserted;
    }

    public boolean addItem(ItemStack stack) {
        Objects.requireNonNull(stack, "Item stack cannot be null.");
        return addItem(stack.getItem(), stack.getCount());
    }

    public boolean removeItem(Item item, int count) {
        Objects.requireNonNull(item, "Item cannot be null.");
        if (count < 1) {
            throw new IllegalArgumentException("Item count must be positive.");
        }
        if (countItem(item) < count) {
            return false;
        }

        int remaining = count;
        for (int i = 0; i < slots.length && remaining > 0; i++) {
            ItemStack stack = slots[i];
            if (stack == null || !stack.getItem().isStackableWith(item)) {
                continue;
            }

            remaining -= stack.remove(remaining);
            if (stack.isEmpty()) {
                slots[i] = null;
            }
        }

        return true;
    }

    public boolean removeFromSelectedSlot(int amount) {
        if (amount < 1) {
            throw new IllegalArgumentException("Removal amount must be positive.");
        }

        ItemStack stack = slots[selectedHotbarSlot];
        if (stack == null) {
            return false;
        }

        stack.remove(amount);
        if (stack.isEmpty()) {
            slots[selectedHotbarSlot] = null;
        }
        return true;
    }

    public Optional<ItemStack> getSelectedHotbarStack() {
        ItemStack stack = slots[selectedHotbarSlot];
        return stack == null ? Optional.empty() : Optional.of(stack.copy());
    }

    public int countItem(Item item) {
        Objects.requireNonNull(item, "Item cannot be null.");

        int total = 0;
        for (ItemStack stack : slots) {
            if (stack != null && stack.getItem().isStackableWith(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    public int getSelectedHotbarSlot() {
        return selectedHotbarSlot;
    }

    public void setSelectedHotbarSlot(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= getHotbarSize()) {
            throw new IllegalArgumentException("Selected hotbar slot is out of bounds: " + slotIndex);
        }
        selectedHotbarSlot = slotIndex;
    }

    public void equip(EquipmentSlot slot, ItemStack stack) {
        Objects.requireNonNull(slot, "Equipment slot cannot be null.");
        equippedItems.put(slot, stack == null ? null : stack.copy());
    }

    public Optional<ItemStack> getEquipped(EquipmentSlot slot) {
        Objects.requireNonNull(slot, "Equipment slot cannot be null.");
        ItemStack stack = equippedItems.get(slot);
        return stack == null ? Optional.empty() : Optional.of(stack.copy());
    }

    public void equipArmor(ArmorSlot slot, ItemStack stack) {
        Objects.requireNonNull(slot, "Armor slot cannot be null.");
        armorSlots.put(slot, stack == null ? null : stack.copy());
    }

    public Optional<ItemStack> getArmor(ArmorSlot slot) {
        Objects.requireNonNull(slot, "Armor slot cannot be null.");
        ItemStack stack = armorSlots.get(slot);
        return stack == null ? Optional.empty() : Optional.of(stack.copy());
    }

    public CraftingGrid getCraftingGrid() {
        return craftingGrid;
    }

    public Optional<ItemStack> getCraftingResult() {
        return craftingManager.findMatch(craftingGrid).map(CraftingRecipe::getResult);
    }

    public Optional<ItemStack> takeCraftingResult() {
        Optional<CraftingRecipe> recipe = craftingManager.findMatch(craftingGrid);
        if (recipe.isEmpty()) {
            return Optional.empty();
        }

        ItemStack result = recipe.get().getResult();
        if (!craftingGrid.consumeIngredients(recipe.get())) {
            return Optional.empty();
        }
        return Optional.of(result);
    }

    public boolean craft() {
        Optional<ItemStack> craftingResult = getCraftingResult();
        if (craftingResult.isEmpty() || !canAccept(craftingResult.get())) {
            return false;
        }

        Optional<ItemStack> crafted = takeCraftingResult();
        if (crafted.isEmpty()) {
            return false;
        }
        ItemStack result = crafted.get();
        return addItem(result.getItem(), result.getCount());
    }

    public void clear() {
        Arrays.fill(slots, null);
        armorSlots.clear();
        equippedItems.clear();
        craftingGrid.clear();
        selectedHotbarSlot = 0;
    }

    public boolean canAccept(ItemStack stack) {
        int remaining = stack.getCount();

        for (ItemStack existing : slots) {
            if (existing != null && existing.getItem().isStackableWith(stack.getItem()) && !existing.isFull()) {
                remaining -= Math.min(remaining, existing.getRemainingCapacity());
                if (remaining == 0) {
                    return true;
                }
            }
        }

        for (ItemStack existing : slots) {
            if (existing == null) {
                remaining -= Math.min(remaining, stack.getItem().getMaxStackSize());
                if (remaining == 0) {
                    return true;
                }
            }
        }

        return remaining == 0;
    }

    private int indexOf(int column, int row) {
        if (column < 0 || column >= columns) {
            throw new IllegalArgumentException("Inventory column out of bounds: " + column);
        }
        if (row < 0 || row >= rows) {
            throw new IllegalArgumentException("Inventory row out of bounds: " + row);
        }
        return column + row * columns;
    }

    private void checkSlotIndex(int index) {
        if (index < 0 || index >= slots.length) {
            throw new IllegalArgumentException("Inventory slot index out of bounds: " + index);
        }
    }

    private String describeSlots() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < slots.length; i++) {
            ItemStack stack = slots[i];
            if (stack == null) {
                continue;
            }

            if (!builder.isEmpty()) {
                builder.append(", ");
            }
            builder.append(i)
                    .append('=')
                    .append(stack.getItem().getId())
                    .append('x')
                    .append(stack.getCount());
        }
        return builder.isEmpty() ? "empty" : builder.toString();
    }
}
