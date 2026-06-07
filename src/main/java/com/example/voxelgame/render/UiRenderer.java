package com.example.voxelgame.render;

import static org.lwjgl.opengl.GL11C.GL_BLEND;
import static org.lwjgl.opengl.GL11C.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11C.GL_FILL;
import static org.lwjgl.opengl.GL11C.GL_FRONT_AND_BACK;
import static org.lwjgl.opengl.GL11C.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11C.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11C.glBlendFunc;
import static org.lwjgl.opengl.GL11C.glDisable;
import static org.lwjgl.opengl.GL11C.glEnable;
import static org.lwjgl.opengl.GL11C.glPolygonMode;
import static org.lwjgl.opengl.GL13C.glActiveTexture;

import org.joml.Matrix4f;

import com.example.voxelgame.game.inventory.ArmorSlot;
import com.example.voxelgame.game.inventory.ItemStack;
import com.example.voxelgame.game.ui.InventoryScreenLayout;
import com.example.voxelgame.game.ui.UiLayout;
import com.example.voxelgame.game.ui.UiRect;
import com.example.voxelgame.game.ui.UiSlotRef;
import com.example.voxelgame.game.ui.UiSlotType;
import com.example.voxelgame.render.ItemIconAtlas.HudIcon;

public final class UiRenderer implements AutoCloseable {
    private static final boolean DEBUG_DRAW_FULL_ATLAS = Boolean.getBoolean("voxelgame.debugAtlas");
    private static final boolean DEBUG_FURNACE_UI = Boolean.getBoolean("voxelgame.debugFurnaceUi");
    private static final float HOTBAR_PANEL_SIDE_PADDING = 56.0f;
    private static final float HOTBAR_PANEL_TOP_PADDING = 14.0f;
    private static final float HOTBAR_PANEL_BOTTOM_PADDING = 16.0f;
    private static final float HOTBAR_BAR_GAP = 12.0f;
    private static final float HOTBAR_SELECTED_SCALE = 1.10f;
    private static final float CARRIED_ITEM_ALPHA = 0.82f;
    private static final float LOW_HUNGER_BLINK_THRESHOLD = 6.0f;
    private static final float SLOT_BORDER_THICKNESS = 2.0f;
    private static final float PANEL_BORDER_THICKNESS = 2.0f;
    private static final float SHADOW_OFFSET = 2.0f;
    private static final int PANEL_TILE_X = 2;
    private static final int PANEL_TILE_Y = 7;
    private static final int SLOT_TILE_X = 0;
    private static final int SLOT_TILE_Y = 7;
    private static final int SELECTED_TILE_X = 1;
    private static final int SELECTED_TILE_Y = 7;
    private static final int FURNACE_ARROW_EMPTY_TILE_X = 8;
    private static final int FURNACE_ARROW_EMPTY_TILE_Y = 7;
    private static final int FURNACE_ARROW_FULL_TILE_X = 9;
    private static final int FURNACE_ARROW_FULL_TILE_Y = 7;
    private static final int FURNACE_FLAME_TILE_X = 10;
    private static final int FURNACE_FLAME_TILE_Y = 7;

    private static final String UI_VERTEX_SHADER = """
            #version 330 core
            layout (location = 0) in vec2 aPosition;
            layout (location = 1) in vec2 aUv;

            uniform mat4 uProjection;

            out vec2 vUv;

            void main() {
                vUv = aUv;
                gl_Position = uProjection * vec4(aPosition, 0.0, 1.0);
            }
            """;

    private static final String UI_FRAGMENT_SHADER = """
            #version 330 core
            in vec2 vUv;

            uniform sampler2D uTexture;
            uniform vec4 uColor;
            uniform int uUseTexture;

            out vec4 fragColor;

            void main() {
                vec4 texel = uUseTexture == 1 ? texture(uTexture, vUv) : vec4(1.0);
                fragColor = vec4(texel.rgb * uColor.rgb, texel.a * uColor.a);
            }
            """;

    private static final String TEXT_VERTEX_SHADER = """
            #version 330 core
            layout (location = 0) in vec2 aPosition;
            layout (location = 1) in vec2 aUv;

            uniform mat4 uProjection;

            out vec2 vUv;

            void main() {
                vUv = aUv;
                gl_Position = uProjection * vec4(aPosition, 0.0, 1.0);
            }
            """;

    private static final String TEXT_FRAGMENT_SHADER = """
            #version 330 core
            in vec2 vUv;
            uniform sampler2D uFont;
            uniform vec4 uColor;
            out vec4 fragColor;

            void main() {
                float alpha = texture(uFont, vUv).r;
                fragColor = vec4(uColor.rgb, uColor.a * alpha);
            }
            """;

    private final DynamicTextMesh quadMesh = new DynamicTextMesh();
    private final DynamicTextMesh textMesh = new DynamicTextMesh();
    private final BitmapFont bitmapFont = new BitmapFont();
    private final ItemIconAtlas itemIconAtlas = new ItemIconAtlas();
    private final ShaderProgram uiShaderProgram = new ShaderProgram(UI_VERTEX_SHADER, UI_FRAGMENT_SHADER);
    private final ShaderProgram textShaderProgram = new ShaderProgram(TEXT_VERTEX_SHADER, TEXT_FRAGMENT_SHADER);
    private final Matrix4f uiProjection = new Matrix4f();
    private double lastFurnaceDebugLogSeconds = -1.0;

    public void renderOverlay(int screenWidth, int screenHeight, HudState hudState, boolean inventoryOpen) {
        uiProjection.identity().ortho(0.0f, screenWidth, screenHeight, 0.0f, -1.0f, 1.0f);

        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        if (!inventoryOpen) {
            renderCrosshair(screenWidth * 0.5f, screenHeight * 0.5f, 8.0f, 2.0f, 2.0f, screenWidth, screenHeight);
        }
        renderModeLabel(screenWidth, screenHeight, hudState.gameModeText());

        if (!hudState.debugVisible()) {
            return;
        }

        String instructions = hudState.cursorCaptured()
                ? "WASD move  SPACE jump/fly  LMB break  RMB place  F4 mode  ESC release"
                : "Left click captures mouse  F4 mode  ESC exits  F10 quits immediately";
        String text = "Voxel Game\n"
                + "Clock: " + hudState.timeText() + "\n"
                + hudState.debugText()
                + "\n"
                + "Placement: " + hudState.placementBlock().getName() + "\n"
                + instructions;

        float textScale = 1.0f;
        float paddingX = 10.0f;
        float paddingY = 9.0f;
        float panelX = 16.0f;
        float panelY = 16.0f;
        float panelWidth = bitmapFont.measureTextWidth(text, textScale) + paddingX * 2.0f;
        float panelHeight = bitmapFont.measureTextHeight(text, textScale) + paddingY * 2.0f;

        drawColorQuad(screenWidth, screenHeight, new UiRect(panelX, panelY, panelWidth, panelHeight), 0.06f, 0.08f, 0.10f, 0.82f);
        drawText(screenWidth, screenHeight, text, panelX + paddingX, panelY + paddingY, textScale);
    }

    public void render(int screenWidth, int screenHeight, UiRenderState uiState) {
        uiProjection.identity().ortho(0.0f, screenWidth, screenHeight, 0.0f, -1.0f, 1.0f);

        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glActiveTexture(org.lwjgl.opengl.GL33C.GL_TEXTURE0);
        itemIconAtlas.bind(0);

        renderHotbar(screenWidth, screenHeight, uiState);
        renderHearts(screenWidth, screenHeight, uiState.hudState().currentHealth(), uiState.hudState().damageFlashSeconds(), uiState.animationTimeSeconds());
        renderHunger(screenWidth, screenHeight, uiState.hudState().currentHunger(), uiState.animationTimeSeconds());
        if (uiState.inventoryOpen()) {
            if (uiState.uiMode() == com.example.voxelgame.game.ui.UiMode.CRAFTING_TABLE) {
                drawCraftingMenu(screenWidth, screenHeight, uiState);
            } else if (uiState.uiMode() == com.example.voxelgame.game.ui.UiMode.FURNACE) {
                drawFurnaceMenu(screenWidth, screenHeight, uiState);
            } else if (uiState.uiMode() == com.example.voxelgame.game.ui.UiMode.TRADING) {
                drawTradingMenu(screenWidth, screenHeight, uiState);
            } else {
                renderInventoryScreen(screenWidth, screenHeight, uiState);
            }
        }
        if (DEBUG_DRAW_FULL_ATLAS) {
            renderAtlasDebug(screenWidth, screenHeight);
        }
    }

    private void renderCrosshair(float centerX, float centerY, float armLength, float gap, float thickness, int screenWidth, int screenHeight) {
        float halfThickness = thickness * 0.5f;
        drawColorQuad(screenWidth, screenHeight, new UiRect(centerX - armLength, centerY - halfThickness, armLength - gap, thickness), 1.0f, 1.0f, 1.0f, 0.95f);
        drawColorQuad(screenWidth, screenHeight, new UiRect(centerX + gap, centerY - halfThickness, armLength - gap, thickness), 1.0f, 1.0f, 1.0f, 0.95f);
        drawColorQuad(screenWidth, screenHeight, new UiRect(centerX - halfThickness, centerY - armLength, thickness, armLength - gap), 1.0f, 1.0f, 1.0f, 0.95f);
        drawColorQuad(screenWidth, screenHeight, new UiRect(centerX - halfThickness, centerY + gap, thickness, armLength - gap), 1.0f, 1.0f, 1.0f, 0.95f);
    }

    private void renderHotbar(int screenWidth, int screenHeight, UiRenderState uiState) {
        renderHotbarPanel(screenWidth, screenHeight);
        for (int i = 0; i < 9; i++) {
            UiRect rect = UiLayout.hotbarSlot(screenWidth, screenHeight, i);
            boolean selected = i == uiState.selectedHotbarSlot();
            UiRect slotRect = selected ? scaleRect(rect, HOTBAR_SELECTED_SCALE) : rect;
            renderHotbarSlot(screenWidth, screenHeight, slotRect, selected, uiState.animationTimeSeconds());
            renderSlotContents(screenWidth, screenHeight, slotRect, uiState.slotItems().get(new UiSlotRef(UiSlotType.INVENTORY, i)));
        }
    }

    private void renderHearts(int screenWidth, int screenHeight, float health, double damageFlashSeconds, double animationTimeSeconds) {
        renderVitalBar(
                screenWidth,
                screenHeight,
                hotbarPanelRect(screenWidth, screenHeight).x(),
                vitalBarY(screenWidth, screenHeight),
                health,
                HudIcon.HEART_EMPTY,
                HudIcon.HEART_HALF,
                HudIcon.HEART_FULL,
                health <= 6.0f,
                animationTimeSeconds,
                false,
                damageFlashSeconds > 0.0 ? (float) Math.min(1.0, damageFlashSeconds / 0.45) : 0.0f
        );
    }

    private void renderHunger(int screenWidth, int screenHeight, float hunger, double animationTimeSeconds) {
        float totalWidth = vitalBarWidth(screenWidth, screenHeight);
        float startX = hotbarPanelRect(screenWidth, screenHeight).x() + hotbarPanelRect(screenWidth, screenHeight).width() - totalWidth;
        renderVitalBar(
                screenWidth,
                screenHeight,
                startX,
                vitalBarY(screenWidth, screenHeight),
                hunger,
                HudIcon.HUNGER_EMPTY,
                HudIcon.HUNGER_HALF,
                HudIcon.HUNGER_FULL,
                false,
                animationTimeSeconds,
                hunger <= 0.0f,
                0.0f
        );
    }

    private void renderVitalBar(
            int screenWidth,
            int screenHeight,
            float startX,
            float y,
            float value,
            HudIcon emptyIcon,
            HudIcon halfIcon,
            HudIcon fullIcon,
            boolean shakeLow,
            double animationTimeSeconds,
            boolean blinkEmpty,
            float damageFlash
    ) {
        float iconSize = vitalIconSize(screenWidth, screenHeight);
        float spacing = vitalIconSpacing(screenWidth, screenHeight);
        float lowShake = shakeLow ? (float) Math.sin(animationTimeSeconds * 9.0) * 1.5f : 0.0f;
        boolean blinkLowHunger = fullIcon == HudIcon.HUNGER_FULL && value <= LOW_HUNGER_BLINK_THRESHOLD;

        for (int i = 0; i < 10; i++) {
            float threshold = i * 2.0f;
            HudIcon icon = value >= threshold + 2.0f
                    ? fullIcon
                    : (value >= threshold + 1.0f ? halfIcon : emptyIcon);
            float alpha = 1.0f;
            if (blinkEmpty && icon == emptyIcon) {
                alpha = 0.58f;
            }
            if (blinkLowHunger && icon != emptyIcon) {
                alpha *= 0.72f + 0.28f * (0.5f + 0.5f * (float) Math.sin(animationTimeSeconds * 8.0f));
            }

            UiRect rect = new UiRect(startX + i * (iconSize + spacing), y + (icon != emptyIcon ? lowShake : 0.0f), iconSize, iconSize);
            drawShadowedTexturedQuad(screenWidth, screenHeight, rect, itemIconAtlas.getHudIconUv(icon), alpha);
            if (damageFlash > 0.0f && fullIcon == HudIcon.HEART_FULL && icon != emptyIcon) {
                drawColorQuad(screenWidth, screenHeight, rect, 1.0f, 1.0f, 1.0f, 0.18f * damageFlash);
            }
        }
    }

    private void renderInventoryScreen(int screenWidth, int screenHeight, UiRenderState uiState) {
        renderMenuScreen(screenWidth, screenHeight, uiState, uiState.creativeInventory() ? "Creative" : "Inventory");
    }

    private void drawCraftingMenu(int screenWidth, int screenHeight, UiRenderState uiState) {
        renderMenuScreen(screenWidth, screenHeight, uiState, uiState.menuTitle());
    }

    private void drawFurnaceMenu(int screenWidth, int screenHeight, UiRenderState uiState) {
        renderMenuScreen(screenWidth, screenHeight, uiState, uiState.menuTitle());
    }

    private void drawTradingMenu(int screenWidth, int screenHeight, UiRenderState uiState) {
        renderMenuScreen(screenWidth, screenHeight, uiState, uiState.menuTitle());
    }

    private void renderMenuScreen(int screenWidth, int screenHeight, UiRenderState uiState, String title) {
        InventoryScreenLayout layout = UiLayout.inventoryLayout(
                screenWidth,
                screenHeight,
                9,
                4,
                uiState.craftingWidth(),
                uiState.craftingHeight()
        );

        drawPanel(screenWidth, screenHeight, layout.panel(), 0.06f, 0.07f, 0.10f, 0.90f, 0.84f, 0.86f, 0.92f, 0.95f);
        drawText(screenWidth, screenHeight, title, layout.panel().x() + 24.0f, layout.panel().y() + 8.0f, 1.0f);
        if (uiState.creativeInventory()) {
            renderCreativeInventory(screenWidth, screenHeight, uiState, layout);
            renderTooltip(screenWidth, screenHeight, uiState);
            renderCarriedStack(screenWidth, screenHeight, uiState);
            return;
        }
        if (uiState.uiMode() == com.example.voxelgame.game.ui.UiMode.FURNACE) {
            renderInventory(screenWidth, screenHeight, uiState, layout);
            renderFurnace(screenWidth, screenHeight, uiState, layout);
            renderTooltip(screenWidth, screenHeight, uiState);
            renderCarriedStack(screenWidth, screenHeight, uiState);
            return;
        }
        if (uiState.uiMode() == com.example.voxelgame.game.ui.UiMode.TRADING) {
            renderInventory(screenWidth, screenHeight, uiState, layout);
            renderTrades(screenWidth, screenHeight, uiState, layout);
            renderTooltip(screenWidth, screenHeight, uiState);
            renderCarriedStack(screenWidth, screenHeight, uiState);
            return;
        }
        renderArmor(screenWidth, screenHeight, uiState, layout);
        renderInventory(screenWidth, screenHeight, uiState, layout);
        renderCrafting(screenWidth, screenHeight, uiState, layout);
        renderTooltip(screenWidth, screenHeight, uiState);

        renderCarriedStack(screenWidth, screenHeight, uiState);
    }

    private void renderTrades(int screenWidth, int screenHeight, UiRenderState uiState, InventoryScreenLayout layout) {
        drawText(screenWidth, screenHeight, "Offers", UiLayout.sectionTitleX(layout.craftingArea()), UiLayout.sectionTitleY(layout, screenWidth, screenHeight), 1.0f);
        float x = layout.craftingArea().x() + 8.0f;
        float y = layout.craftingArea().y() + 36.0f;
        for (int i = 0; i < uiState.tradeLines().size(); i++) {
            drawText(screenWidth, screenHeight, uiState.tradeLines().get(i), x, y + i * 28.0f, 0.76f);
        }
    }

    private void renderCarriedStack(int screenWidth, int screenHeight, UiRenderState uiState) {
        if (uiState.carriedStack() != null) {
            float slotSize = UiLayout.slotSize(screenWidth, screenHeight);
            UiRect mouseRect = new UiRect((float) uiState.mouseX() + 12.0f, (float) uiState.mouseY() + 12.0f, slotSize, slotSize);
            drawSlot(screenWidth, screenHeight, mouseRect, false, true);
            renderSlotContents(screenWidth, screenHeight, mouseRect, uiState.carriedStack(), CARRIED_ITEM_ALPHA);
        }
    }

    private void renderCreativeInventory(int screenWidth, int screenHeight, UiRenderState uiState, InventoryScreenLayout layout) {
        drawText(screenWidth, screenHeight, "Creative Items", UiLayout.sectionTitleX(layout.inventoryArea()), UiLayout.sectionTitleY(layout, screenWidth, screenHeight), 1.0f);

        for (int i = 0; i < uiState.creativeVisibleSlotCount(); i++) {
            UiSlotRef ref = new UiSlotRef(UiSlotType.CREATIVE, i);
            UiRect rect = UiLayout.inventorySlot(layout, i, screenWidth, screenHeight);
            drawSlot(screenWidth, screenHeight, rect, false, ref.equals(uiState.hoveredSlot()));
            renderSlotContents(screenWidth, screenHeight, rect, uiState.slotItems().get(ref), 1.0f, false);
        }
        renderCreativeScrollbar(screenWidth, screenHeight, uiState, layout);
    }

    private void renderCreativeScrollbar(int screenWidth, int screenHeight, UiRenderState uiState, InventoryScreenLayout layout) {
        if (uiState.creativeItemCount() <= uiState.creativeVisibleSlotCount()) {
            return;
        }

        float scale = UiLayout.slotSize(screenWidth, screenHeight) / UiLayout.BASE_SLOT_SIZE;
        float trackWidth = Math.max(5.0f, 7.0f * scale);
        float trackX = layout.inventoryArea().x() + layout.inventoryArea().width() + Math.max(8.0f, UiLayout.slotGap(screenWidth, screenHeight));
        UiRect track = new UiRect(trackX, layout.inventoryArea().y(), trackWidth, layout.inventoryArea().height());
        drawColorQuad(screenWidth, screenHeight, track, 0.02f, 0.025f, 0.03f, 0.70f);

        int columns = UiLayout.inventoryColumns();
        int totalRows = (uiState.creativeItemCount() + columns - 1) / columns;
        int visibleRows = Math.max(1, uiState.creativeVisibleSlotCount() / columns);
        int firstVisibleRow = uiState.creativeScrollOffset() / columns;
        float visibleRatio = Math.min(1.0f, visibleRows / (float) totalRows);
        float thumbHeight = Math.max(UiLayout.slotSize(screenWidth, screenHeight) * 0.55f, track.height() * visibleRatio);
        float scrollRange = Math.max(1.0f, totalRows - visibleRows);
        float thumbY = track.y() + (track.height() - thumbHeight) * (firstVisibleRow / scrollRange);
        UiRect thumb = new UiRect(track.x(), thumbY, track.width(), thumbHeight);
        drawColorQuad(screenWidth, screenHeight, thumb, 0.76f, 0.80f, 0.88f, 0.88f);
    }

    private void renderInventory(int screenWidth, int screenHeight, UiRenderState uiState, InventoryScreenLayout layout) {
        drawText(screenWidth, screenHeight, "Inventory", UiLayout.sectionTitleX(layout.inventoryArea()), UiLayout.sectionTitleY(layout, screenWidth, screenHeight), 1.0f);

        for (int i = 0; i < 36; i++) {
            UiSlotRef ref = new UiSlotRef(UiSlotType.INVENTORY, i);
            UiRect rect = UiLayout.inventorySlot(layout, i, screenWidth, screenHeight);
            boolean selected = i == uiState.selectedHotbarSlot() && i < 9;
            drawSlot(screenWidth, screenHeight, rect, selected, ref.equals(uiState.hoveredSlot()));
            renderSlotContents(screenWidth, screenHeight, rect, uiState.slotItems().get(ref));
        }
    }

    private void renderCrafting(int screenWidth, int screenHeight, UiRenderState uiState, InventoryScreenLayout layout) {
        drawText(screenWidth, screenHeight, "Crafting", UiLayout.sectionTitleX(layout.craftingArea()), UiLayout.sectionTitleY(layout, screenWidth, screenHeight), 1.0f);

        int craftingSlots = uiState.craftingWidth() * uiState.craftingHeight();
        for (int i = 0; i < craftingSlots; i++) {
            UiSlotRef ref = new UiSlotRef(UiSlotType.CRAFTING, i);
            UiRect rect = UiLayout.craftingSlot(layout, i, uiState.craftingWidth(), screenWidth, screenHeight);
            drawSlot(screenWidth, screenHeight, rect, false, ref.equals(uiState.hoveredSlot()));
            renderSlotContents(screenWidth, screenHeight, rect, uiState.slotItems().get(ref));
        }

        UiSlotRef resultRef = new UiSlotRef(UiSlotType.CRAFTING_RESULT, 0);
        UiRect resultRect = UiLayout.craftingResultSlot(layout, uiState.craftingWidth(), uiState.craftingHeight(), screenWidth, screenHeight);
        drawText(
                screenWidth,
                screenHeight,
                "=>",
                UiLayout.craftingArrowX(layout, uiState.craftingWidth(), screenWidth, screenHeight),
                UiLayout.craftingArrowY(layout, uiState.craftingHeight(), screenWidth, screenHeight),
                1.0f
        );
        drawSlot(screenWidth, screenHeight, resultRect, false, resultRef.equals(uiState.hoveredSlot()));
        renderSlotContents(screenWidth, screenHeight, resultRect, uiState.slotItems().get(resultRef));
    }

    private void renderArmor(int screenWidth, int screenHeight, UiRenderState uiState, InventoryScreenLayout layout) {
        drawText(screenWidth, screenHeight, "Armor", UiLayout.sectionTitleX(layout.armorArea()), UiLayout.sectionTitleY(layout, screenWidth, screenHeight), 1.0f);

        for (ArmorSlot armorSlot : ArmorSlot.values()) {
            UiSlotRef ref = new UiSlotRef(UiSlotType.ARMOR, armorSlot.ordinal());
            UiRect rect = UiLayout.armorSlot(layout, armorSlot, screenWidth, screenHeight);
            drawSlot(screenWidth, screenHeight, rect, false, ref.equals(uiState.hoveredSlot()));
            drawText(screenWidth, screenHeight, armorLabel(armorSlot), rect.x() + 8.0f, rect.y() + 10.0f, 0.7f);
            renderSlotContents(screenWidth, screenHeight, rect, uiState.slotItems().get(ref));
        }
    }

    private void renderSlotContents(int screenWidth, int screenHeight, UiRect rect, ItemStack stack) {
        renderSlotContents(screenWidth, screenHeight, rect, stack, 1.0f);
    }

    private void renderFurnace(int screenWidth, int screenHeight, UiRenderState uiState, InventoryScreenLayout layout) {
        drawText(screenWidth, screenHeight, "Smelting", UiLayout.sectionTitleX(layout.craftingArea()), UiLayout.sectionTitleY(layout, screenWidth, screenHeight), 1.0f);

        UiSlotRef inputRef = new UiSlotRef(UiSlotType.FURNACE_INPUT, 0);
        UiSlotRef fuelRef = new UiSlotRef(UiSlotType.FURNACE_FUEL, 0);
        UiSlotRef outputRef = new UiSlotRef(UiSlotType.FURNACE_OUTPUT, 0);
        UiRect inputRect = UiLayout.furnaceInputSlot(layout, screenWidth, screenHeight);
        UiRect fuelRect = UiLayout.furnaceFuelSlot(layout, screenWidth, screenHeight);
        UiRect outputRect = UiLayout.furnaceOutputSlot(layout, screenWidth, screenHeight);

        drawSlot(screenWidth, screenHeight, inputRect, false, inputRef.equals(uiState.hoveredSlot()));
        renderSlotContents(screenWidth, screenHeight, inputRect, uiState.slotItems().get(inputRef));
        drawSlot(screenWidth, screenHeight, fuelRect, false, fuelRef.equals(uiState.hoveredSlot()));
        renderSlotContents(screenWidth, screenHeight, fuelRect, uiState.slotItems().get(fuelRef));
        drawSlot(screenWidth, screenHeight, outputRect, false, outputRef.equals(uiState.hoveredSlot()));
        renderSlotContents(screenWidth, screenHeight, outputRect, uiState.slotItems().get(outputRef));

        float scale = UiLayout.slotSize(screenWidth, screenHeight) / UiLayout.BASE_SLOT_SIZE;
        renderFurnaceIndicators(screenWidth, screenHeight, uiState, inputRect, fuelRect, outputRect, scale);
    }

    private void renderFurnaceIndicators(int screenWidth, int screenHeight, UiRenderState uiState, UiRect inputRect, UiRect fuelRect, UiRect outputRect, float scale) {
        float arrowWidth = 32.0f * scale;
        float arrowHeight = 16.0f * scale;
        float arrowX = inputRect.x() + inputRect.width() + 20.0f * scale;
        float arrowY = outputRect.y() + (outputRect.height() - arrowHeight) * 0.5f;
        UiRect arrowRect = new UiRect(arrowX, arrowY, arrowWidth, arrowHeight);
        drawTexturedQuad(
                screenWidth,
                screenHeight,
                arrowRect,
                itemIconAtlas.getTileUv(FURNACE_ARROW_EMPTY_TILE_X, FURNACE_ARROW_EMPTY_TILE_Y),
                0.82f,
                0.86f,
                0.92f,
                0.86f
        );
        float cookProgress = saturate(uiState.furnaceCookProgress());
        if (cookProgress > 0.0f) {
            drawTexturedQuadSection(
                    screenWidth,
                    screenHeight,
                    arrowRect,
                    itemIconAtlas.getTileUv(FURNACE_ARROW_FULL_TILE_X, FURNACE_ARROW_FULL_TILE_Y),
                    0.0f,
                    0.0f,
                    cookProgress,
                    1.0f,
                    1.0f,
                    0.92f,
                    0.52f,
                    0.96f
            );
        }

        float flameSize = 18.0f * scale;
        UiRect flameRect = new UiRect(
                fuelRect.x() + (fuelRect.width() - flameSize) * 0.5f,
                inputRect.y() + inputRect.height() + 2.0f * scale,
                flameSize,
                flameSize
        );
        drawColorQuad(screenWidth, screenHeight, inflate(flameRect, 2.0f * scale), 0.03f, 0.035f, 0.04f, 0.48f);
        float fuelProgress = saturate(uiState.furnaceBurnProgress());
        if (fuelProgress > 0.0f) {
            drawTexturedQuadSection(
                    screenWidth,
                    screenHeight,
                    flameRect,
                    itemIconAtlas.getTileUv(FURNACE_FLAME_TILE_X, FURNACE_FLAME_TILE_Y),
                    0.0f,
                    1.0f - fuelProgress,
                    1.0f,
                    1.0f,
                    1.0f,
                    1.0f,
                    1.0f,
                    0.96f
            );
        }
        logFurnaceUiDebug(uiState, cookProgress, fuelProgress);
    }

    private void renderSlotContents(int screenWidth, int screenHeight, UiRect rect, ItemStack stack, float alpha) {
        renderSlotContents(screenWidth, screenHeight, rect, stack, alpha, true);
    }

    private void renderSlotContents(int screenWidth, int screenHeight, UiRect rect, ItemStack stack, float alpha, boolean showCount) {
        if (stack == null) {
            return;
        }

        drawItemIcon(screenWidth, screenHeight, rect, stack, alpha);
        if (showCount && stack.getCount() > 1) {
            drawText(screenWidth, screenHeight, Integer.toString(stack.getCount()), rect.x() + rect.width() - 18.0f, rect.y() + rect.height() - 18.0f, 0.8f);
        }
    }

    private void renderHotbarSlot(int screenWidth, int screenHeight, UiRect rect, boolean selected, double animationTimeSeconds) {
        if (selected) {
            float pulse = 0.5f + 0.5f * (float) Math.sin(animationTimeSeconds * 7.0);
            float glow = 4.0f + pulse * 2.5f;
            UiRect glowRect = inflate(rect, glow);
            drawColorQuad(screenWidth, screenHeight, glowRect, 1.0f, 0.92f, 0.45f, 0.15f + pulse * 0.10f);
            drawTexturedFramedRect(
                    screenWidth,
                    screenHeight,
                    rect,
                    itemIconAtlas.getTileUv(SELECTED_TILE_X, SELECTED_TILE_Y),
                    0.70f,
                    0.58f,
                    0.34f,
                    0.92f,
                    1.0f,
                    0.97f,
                    0.78f,
                    1.0f,
                    SLOT_BORDER_THICKNESS + 1.0f
            );
            return;
        }

        drawTexturedFramedRect(
                screenWidth,
                screenHeight,
                rect,
                itemIconAtlas.getTileUv(SLOT_TILE_X, SLOT_TILE_Y),
                0.26f,
                0.27f,
                0.30f,
                0.88f,
                0.78f,
                0.80f,
                0.86f,
                0.92f,
                SLOT_BORDER_THICKNESS
        );
    }

    private void drawSlot(int screenWidth, int screenHeight, UiRect rect, boolean selected, boolean hovered) {
        float background = hovered ? 0.20f : 0.12f;
        float alpha = hovered ? 0.95f : 0.86f;
        float border = selected ? 0.98f : (hovered ? 0.90f : 0.74f);
        float borderBlue = selected ? 0.72f : border;
        float borderGreen = selected ? 0.94f : border;
        drawTexturedFramedRect(
                screenWidth,
                screenHeight,
                rect,
                itemIconAtlas.getTileUv(selected ? SELECTED_TILE_X : SLOT_TILE_X, selected ? SELECTED_TILE_Y : SLOT_TILE_Y),
                background + 0.12f,
                background + 0.14f,
                background + 0.16f,
                alpha,
                border,
                borderGreen,
                borderBlue,
                0.96f,
                SLOT_BORDER_THICKNESS
        );
        UiRect inset = inset(rect, 3.0f);
        drawColorQuad(screenWidth, screenHeight, inset, 0.05f, 0.06f, 0.08f, hovered ? 0.52f : 0.36f);
    }

    private void drawPanel(int screenWidth, int screenHeight, UiRect rect, float r, float g, float b, float a, float br, float bg, float bb, float ba) {
        drawTexturedQuad(screenWidth, screenHeight, rect, itemIconAtlas.getTileUv(PANEL_TILE_X, PANEL_TILE_Y), r * 1.8f, g * 1.8f, b * 1.8f, a);
        drawColorQuad(screenWidth, screenHeight, rect, r, g, b, a * 0.72f);
        drawFrame(screenWidth, screenHeight, rect, br, bg, bb, ba, PANEL_BORDER_THICKNESS);
        drawFrame(screenWidth, screenHeight, inset(rect, 3.0f), 0.02f, 0.03f, 0.05f, 0.80f, 1.0f);
    }

    private void drawItemIcon(int screenWidth, int screenHeight, UiRect rect, ItemStack stack, float alpha) {
        float inset = Math.max(4.0f, rect.width() * 0.12f);
        UiRect iconRect = new UiRect(rect.x() + inset, rect.y() + inset, rect.width() - inset * 2.0f, rect.height() - inset * 2.0f);
        drawShadowedTexturedQuad(screenWidth, screenHeight, iconRect, itemIconAtlas.getUvBounds(stack), alpha);
    }

    private void drawShadowedTexturedQuad(int screenWidth, int screenHeight, UiRect rect, float[] uvBounds, float alpha) {
        UiRect shadowRect = new UiRect(rect.x() + SHADOW_OFFSET, rect.y() + SHADOW_OFFSET, rect.width(), rect.height());
        drawTexturedQuad(screenWidth, screenHeight, shadowRect, uvBounds, 0.0f, 0.0f, 0.0f, alpha * 0.35f);
        drawTexturedQuad(screenWidth, screenHeight, rect, uvBounds, 1.0f, 1.0f, 1.0f, alpha);
    }

    private void drawColorQuad(int screenWidth, int screenHeight, UiRect rect, float r, float g, float b, float a) {
        uiShaderProgram.bind();
        uiShaderProgram.setMatrix4("uProjection", uiProjection);
        uiShaderProgram.setInt("uUseTexture", 0);
        uiShaderProgram.setVector4("uColor", r, g, b, a);
        quadMesh.render(buildQuadVertices(rect.x(), rect.y(), rect.width(), rect.height(), 0.0f, 0.0f, 1.0f, 1.0f));
        uiShaderProgram.unbind();
    }

    private void drawTexturedQuad(int screenWidth, int screenHeight, UiRect rect, float[] uvBounds, float r, float g, float b, float a) {
        uiShaderProgram.bind();
        uiShaderProgram.setMatrix4("uProjection", uiProjection);
        uiShaderProgram.setInt("uTexture", 0);
        uiShaderProgram.setInt("uUseTexture", 1);
        uiShaderProgram.setVector4("uColor", r, g, b, a);
        glActiveTexture(org.lwjgl.opengl.GL33C.GL_TEXTURE0);
        itemIconAtlas.bind(0);
        quadMesh.render(buildQuadVertices(rect.x(), rect.y(), rect.width(), rect.height(), uvBounds[0], uvBounds[1], uvBounds[2], uvBounds[3]));
        uiShaderProgram.unbind();
    }

    private void drawTexturedQuadSection(
            int screenWidth,
            int screenHeight,
            UiRect rect,
            float[] uvBounds,
            float sourceX0,
            float sourceY0,
            float sourceX1,
            float sourceY1,
            float r,
            float g,
            float b,
            float a
    ) {
        float clampedX0 = saturate(sourceX0);
        float clampedY0 = saturate(sourceY0);
        float clampedX1 = saturate(sourceX1);
        float clampedY1 = saturate(sourceY1);
        if (clampedX1 <= clampedX0 || clampedY1 <= clampedY0) {
            return;
        }

        float rectX = rect.x() + rect.width() * clampedX0;
        float rectY = rect.y() + rect.height() * clampedY0;
        float rectWidth = rect.width() * (clampedX1 - clampedX0);
        float rectHeight = rect.height() * (clampedY1 - clampedY0);
        float uvWidth = uvBounds[2] - uvBounds[0];
        float uvHeight = uvBounds[3] - uvBounds[1];
        float u0 = uvBounds[0] + uvWidth * clampedX0;
        float v0 = uvBounds[1] + uvHeight * clampedY0;
        float u1 = uvBounds[0] + uvWidth * clampedX1;
        float v1 = uvBounds[1] + uvHeight * clampedY1;
        drawTexturedQuad(screenWidth, screenHeight, new UiRect(rectX, rectY, rectWidth, rectHeight), new float[]{u0, v0, u1, v1}, r, g, b, a);
    }

    private void drawFrame(int screenWidth, int screenHeight, UiRect rect, float r, float g, float b, float a, float thickness) {
        drawColorQuad(screenWidth, screenHeight, new UiRect(rect.x(), rect.y(), rect.width(), thickness), r, g, b, a);
        drawColorQuad(screenWidth, screenHeight, new UiRect(rect.x(), rect.y() + rect.height() - thickness, rect.width(), thickness), r, g, b, a);
        drawColorQuad(screenWidth, screenHeight, new UiRect(rect.x(), rect.y(), thickness, rect.height()), r, g, b, a);
        drawColorQuad(screenWidth, screenHeight, new UiRect(rect.x() + rect.width() - thickness, rect.y(), thickness, rect.height()), r, g, b, a);
    }

    private void drawFramedRect(
            int screenWidth,
            int screenHeight,
            UiRect rect,
            float fillR,
            float fillG,
            float fillB,
            float fillA,
            float borderR,
            float borderG,
            float borderB,
            float borderA,
            float borderThickness
    ) {
        drawColorQuad(screenWidth, screenHeight, rect, fillR, fillG, fillB, fillA);
        drawFrame(screenWidth, screenHeight, rect, borderR, borderG, borderB, borderA, borderThickness);
    }

    private void drawTexturedFramedRect(
            int screenWidth,
            int screenHeight,
            UiRect rect,
            float[] uvBounds,
            float fillR,
            float fillG,
            float fillB,
            float fillA,
            float borderR,
            float borderG,
            float borderB,
            float borderA,
            float borderThickness
    ) {
        drawTexturedQuad(screenWidth, screenHeight, rect, uvBounds, fillR, fillG, fillB, fillA);
        drawFrame(screenWidth, screenHeight, rect, borderR, borderG, borderB, borderA, borderThickness);
    }

    private void drawText(int screenWidth, int screenHeight, String text, float x, float y, float scale) {
        textShaderProgram.bind();
        textShaderProgram.setMatrix4("uProjection", uiProjection);
        textShaderProgram.setVector4("uColor", 0.96f, 0.97f, 1.0f, 1.0f);
        textShaderProgram.setInt("uFont", 1);
        bitmapFont.bind(1);
        textMesh.render(bitmapFont.buildTextVertices(text, x, y, scale));
        textShaderProgram.unbind();
    }

    private void renderAtlasDebug(int screenWidth, int screenHeight) {
        float size = Math.min(screenWidth, screenHeight) * 0.92f;
        UiRect rect = new UiRect((screenWidth - size) * 0.5f, (screenHeight - size) * 0.5f, size, size);
        drawPanel(screenWidth, screenHeight, inflate(rect, 4.0f), 0.0f, 0.0f, 0.0f, 0.75f, 0.84f, 0.84f, 0.84f, 1.0f);
        drawTexturedQuad(screenWidth, screenHeight, rect, new float[]{0.0f, 0.0f, 1.0f, 1.0f}, 1.0f, 1.0f, 1.0f, 1.0f);
    }

    private void renderTooltip(int screenWidth, int screenHeight, UiRenderState uiState) {
        if (uiState.hoveredItemName() == null || uiState.hoveredItemName().isBlank() || uiState.carriedStack() != null) {
            return;
        }

        float textScale = 0.8f;
        float paddingX = 6.0f;
        float paddingY = 4.0f;
        float width = bitmapFont.measureTextWidth(uiState.hoveredItemName(), textScale) + paddingX * 2.0f;
        float height = bitmapFont.measureTextHeight(uiState.hoveredItemName(), textScale) + paddingY * 2.0f;
        float x = Math.min((float) uiState.mouseX() + 14.0f, screenWidth - width - 16.0f);
        float y = Math.min((float) uiState.mouseY() + 18.0f, screenHeight - height - 16.0f);
        UiRect tooltipRect = new UiRect(x, y, width, height);
        drawPanel(screenWidth, screenHeight, tooltipRect, 0.04f, 0.05f, 0.06f, 0.94f, 0.96f, 0.92f, 0.62f, 0.92f);
        drawText(screenWidth, screenHeight, uiState.hoveredItemName(), x + paddingX, y + paddingY, textScale);
    }

    private String armorLabel(ArmorSlot slot) {
        return switch (slot) {
            case HELMET -> "H";
            case CHESTPLATE -> "C";
            case LEGGINGS -> "L";
            case BOOTS -> "B";
        };
    }

    private float vitalBarY(int screenWidth, int screenHeight) {
        return hotbarPanelRect(screenWidth, screenHeight).y() - vitalIconSize(screenWidth, screenHeight) - HOTBAR_BAR_GAP;
    }

    private float vitalIconSize(int screenWidth, int screenHeight) {
        return UiLayout.slotSize(screenWidth, screenHeight) * 0.52f;
    }

    private float vitalIconSpacing(int screenWidth, int screenHeight) {
        return Math.max(2.0f, UiLayout.slotGap(screenWidth, screenHeight) * 0.30f);
    }

    private float vitalBarWidth(int screenWidth, int screenHeight) {
        float iconSize = vitalIconSize(screenWidth, screenHeight);
        float spacing = vitalIconSpacing(screenWidth, screenHeight);
        return iconSize * 10.0f + spacing * 9.0f;
    }

    private void renderHotbarPanel(int screenWidth, int screenHeight) {
        UiRect panel = hotbarPanelRect(screenWidth, screenHeight);
        drawPanel(screenWidth, screenHeight, panel, 0.03f, 0.04f, 0.06f, 0.72f, 0.82f, 0.84f, 0.90f, 0.90f);
    }

    private void renderModeLabel(int screenWidth, int screenHeight, String gameModeText) {
        String label = "Mode: " + gameModeText;
        float scale = 0.85f;
        float width = bitmapFont.measureTextWidth(label, scale) + 16.0f;
        float height = bitmapFont.measureTextHeight(label, scale) + 10.0f;
        UiRect rect = new UiRect(16.0f, screenHeight - height - 16.0f, width, height);
        drawColorQuad(screenWidth, screenHeight, rect, 0.04f, 0.05f, 0.07f, 0.58f);
        drawText(screenWidth, screenHeight, label, rect.x() + 8.0f, rect.y() + 5.0f, scale);
    }

    private UiRect hotbarPanelRect(int screenWidth, int screenHeight) {
        UiRect firstSlot = UiLayout.hotbarSlot(screenWidth, screenHeight, 0);
        UiRect lastSlot = UiLayout.hotbarSlot(screenWidth, screenHeight, 8);
        float width = (lastSlot.x() + lastSlot.width()) - firstSlot.x() + HOTBAR_PANEL_SIDE_PADDING * 2.0f;
        float height = firstSlot.height() + HOTBAR_PANEL_TOP_PADDING + HOTBAR_PANEL_BOTTOM_PADDING;
        return new UiRect(
                firstSlot.x() - HOTBAR_PANEL_SIDE_PADDING,
                firstSlot.y() - HOTBAR_PANEL_TOP_PADDING,
                width,
                height
        );
    }

    private UiRect scaleRect(UiRect rect, float scale) {
        float scaledWidth = rect.width() * scale;
        float scaledHeight = rect.height() * scale;
        return new UiRect(
                rect.x() - (scaledWidth - rect.width()) * 0.5f,
                rect.y() - (scaledHeight - rect.height()) * 0.5f,
                scaledWidth,
                scaledHeight
        );
    }

    private UiRect inset(UiRect rect, float amount) {
        return new UiRect(rect.x() + amount, rect.y() + amount, rect.width() - amount * 2.0f, rect.height() - amount * 2.0f);
    }

    private UiRect inflate(UiRect rect, float amount) {
        return new UiRect(rect.x() - amount, rect.y() - amount, rect.width() + amount * 2.0f, rect.height() + amount * 2.0f);
    }

    private float saturate(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private void logFurnaceUiDebug(UiRenderState uiState, float cookProgress, float fuelProgress) {
        if (!DEBUG_FURNACE_UI || uiState.animationTimeSeconds() - lastFurnaceDebugLogSeconds < 0.5) {
            return;
        }
        System.getLogger(UiRenderer.class.getName()).log(
                System.Logger.Level.INFO,
                "Furnace UI progress: cook={0}, fuel={1}, active={2}",
                cookProgress,
                fuelProgress,
                fuelProgress > 0.0f
        );
        lastFurnaceDebugLogSeconds = uiState.animationTimeSeconds();
    }

    private float[] buildQuadVertices(float x, float y, float width, float height, float u0, float v0, float u1, float v1) {
        return new float[]{
                x, y, u0, v0,
                x + width, y, u1, v0,
                x + width, y + height, u1, v1,
                x + width, y + height, u1, v1,
                x, y + height, u0, v1,
                x, y, u0, v0
        };
    }

    @Override
    public void close() {
        quadMesh.close();
        textMesh.close();
        bitmapFont.close();
        itemIconAtlas.close();
        uiShaderProgram.close();
        textShaderProgram.close();
    }
}
