package com.example.voxelgame.game.furnace;

import com.example.voxelgame.game.inventory.ItemStack;

public final class FurnaceBlockEntity {
    private static final boolean DEBUG = Boolean.getBoolean("voxelgame.debugFurnace");
    private static final System.Logger LOGGER = System.getLogger(FurnaceBlockEntity.class.getName());
    private static final int DEBUG_LOG_INTERVAL_TICKS = 60;

    private ItemStack input;
    private ItemStack fuel;
    private ItemStack output;
    private int burnTicksRemaining;
    private int burnTicksTotal;
    private int cookTicks;
    private long tickCount;
    private String lastDebugReason = "";

    public ItemStack getInput() {
        return input == null ? null : input.copy();
    }

    public void setInput(ItemStack input) {
        this.input = input == null ? null : input.copy();
    }

    public ItemStack getFuel() {
        return fuel == null ? null : fuel.copy();
    }

    public void setFuel(ItemStack fuel) {
        this.fuel = fuel == null ? null : fuel.copy();
    }

    public ItemStack getOutput() {
        return output == null ? null : output.copy();
    }

    public void setOutput(ItemStack output) {
        this.output = output == null ? null : output.copy();
    }

    public int getBurnTicksRemaining() {
        return burnTicksRemaining;
    }

    public void setBurnTicksRemaining(int burnTicksRemaining) {
        this.burnTicksRemaining = Math.max(0, burnTicksRemaining);
    }

    public int getBurnTicksTotal() {
        return burnTicksTotal;
    }

    public void setBurnTicksTotal(int burnTicksTotal) {
        this.burnTicksTotal = Math.max(0, burnTicksTotal);
    }

    public int getCookTicks() {
        return cookTicks;
    }

    public void setCookTicks(int cookTicks) {
        this.cookTicks = Math.max(0, cookTicks);
    }

    public float burnProgress() {
        return burnTicksTotal <= 0 ? 0.0f : burnTicksRemaining / (float) burnTicksTotal;
    }

    public float cookProgress(int cookTicksRequired) {
        return cookTicksRequired <= 0 ? 0.0f : Math.min(1.0f, cookTicks / (float) cookTicksRequired);
    }

    public boolean isBurning() {
        return burnTicksRemaining > 0;
    }

    public FurnaceState getState() {
        FurnaceRecipe recipe = input == null ? null : FurnaceRecipes.find(input.getItem()).orElse(null);
        if (recipe != null && !canAccept(recipe.result())) {
            return FurnaceState.OUTPUT_BLOCKED;
        }
        return isBurning() && recipe != null ? FurnaceState.BURNING : FurnaceState.IDLE;
    }

    public boolean tick() {
        tickCount++;
        boolean wasBurning = isBurning();

        if (burnTicksRemaining > 0) {
            burnTicksRemaining--;
        }

        FurnaceRecipe recipe = input == null ? null : FurnaceRecipes.find(input.getItem()).orElse(null);
        if (input == null) {
            cookTicks = 0;
            debug("idle: no input");
            return burningChanged(wasBurning);
        }
        if (recipe == null) {
            cookTicks = 0;
            debug("idle: no recipe for " + input.getItem().getId());
            return burningChanged(wasBurning);
        }
        if (!canAccept(recipe.result())) {
            debug("output blocked for " + recipe.result().getItem().getId());
            return burningChanged(wasBurning);
        }

        if (burnTicksRemaining <= 0) {
            int fuelTicks = fuel == null ? 0 : FurnaceFuelRegistry.burnTicks(fuel.getItem());
            if (fuelTicks <= 0) {
                debug(fuel == null ? "paused: no fuel" : "paused: invalid fuel " + fuel.getItem().getId());
                return burningChanged(wasBurning);
            }
            fuel.remove(1);
            if (fuel.isEmpty()) {
                fuel = null;
            }
            burnTicksRemaining = fuelTicks;
            burnTicksTotal = fuelTicks;
            debug("consumed fuel, burnTicks=" + fuelTicks);
        }

        cookTicks++;
        debug("smelting " + input.getItem().getId() + " cook=" + cookTicks + "/" + recipe.cookTicks() + " burn=" + burnTicksRemaining);
        if (cookTicks >= recipe.cookTicks()) {
            input.remove(1);
            if (input.isEmpty()) {
                input = null;
            }
            addOutput(recipe.result());
            cookTicks = 0;
            debug("completed recipe -> " + recipe.result().getItem().getId());
        }
        return burningChanged(wasBurning);
    }

    private boolean canAccept(ItemStack result) {
        if (output == null) {
            return true;
        }
        return output.canMerge(result) && output.getRemainingCapacity() >= result.getCount();
    }

    private void addOutput(ItemStack result) {
        if (output == null) {
            output = result.copy();
        } else {
            output.add(result.getCount());
        }
    }

    private boolean burningChanged(boolean wasBurning) {
        return wasBurning != isBurning();
    }

    private void debug(String reason) {
        if (!DEBUG) {
            return;
        }
        boolean reasonChanged = !reason.equals(lastDebugReason);
        if (reasonChanged || tickCount % DEBUG_LOG_INTERVAL_TICKS == 0) {
            LOGGER.log(
                    System.Logger.Level.INFO,
                    "Furnace tick={0}, state={1}, burn={2}/{3}, cook={4}, input={5}, fuel={6}, output={7}, reason={8}",
                    tickCount,
                    getState(),
                    burnTicksRemaining,
                    burnTicksTotal,
                    cookTicks,
                    describe(input),
                    describe(fuel),
                    describe(output),
                    reason
            );
            lastDebugReason = reason;
        }
    }

    private String describe(ItemStack stack) {
        return stack == null ? "empty" : stack.getItem().getId() + "x" + stack.getCount();
    }
}
