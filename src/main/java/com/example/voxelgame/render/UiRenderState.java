package com.example.voxelgame.render;

import com.example.voxelgame.game.inventory.ItemStack;
import com.example.voxelgame.game.ui.UiMode;
import com.example.voxelgame.game.ui.UiSlotRef;
import java.util.Map;

public record UiRenderState(
        HudState hudState,
        boolean inventoryOpen,
        boolean creativeInventory,
        UiMode uiMode,
        String menuTitle,
        int selectedHotbarSlot,
        int creativeItemCount,
        int creativeScrollOffset,
        int creativeVisibleSlotCount,
        float furnaceBurnProgress,
        float furnaceCookProgress,
        int craftingWidth,
        int craftingHeight,
        double animationTimeSeconds,
        double mouseX,
        double mouseY,
        ItemStack carriedStack,
        UiSlotRef hoveredSlot,
        String hoveredItemName,
        java.util.List<String> tradeLines,
        Map<UiSlotRef, ItemStack> slotItems
) {
}
