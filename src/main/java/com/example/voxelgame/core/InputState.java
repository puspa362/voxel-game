package com.example.voxelgame.core;

import java.util.Arrays;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_LAST;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LAST;

public final class InputState {
    private final boolean[] keysDown = new boolean[GLFW_KEY_LAST + 1];
    private final boolean[] keysPressedThisFrame = new boolean[GLFW_KEY_LAST + 1];
    private final boolean[] mouseButtonsDown = new boolean[GLFW_MOUSE_BUTTON_LAST + 1];
    private final boolean[] mouseButtonsPressedThisFrame = new boolean[GLFW_MOUSE_BUTTON_LAST + 1];

    private double mouseDeltaX;
    private double mouseDeltaY;
    private double lastMouseX;
    private double lastMouseY;
    private double mouseX;
    private double mouseY;
    private double scrollY;
    private boolean firstMouseEvent = true;

    public void setKeyState(int key, boolean isPressed) {
        if (key >= 0 && key < keysDown.length) {
            if (isPressed && !keysDown[key]) {
                keysPressedThisFrame[key] = true;
            }
            keysDown[key] = isPressed;
        }
    }

    public boolean isKeyDown(int key) {
        return key >= 0 && key < keysDown.length && keysDown[key];
    }

    public boolean wasKeyPressed(int key) {
        return key >= 0 && key < keysPressedThisFrame.length && keysPressedThisFrame[key];
    }

    public void setMouseButtonState(int button, boolean isPressed) {
        if (button >= 0 && button < mouseButtonsDown.length) {
            if (isPressed && !mouseButtonsDown[button]) {
                mouseButtonsPressedThisFrame[button] = true;
            }
            mouseButtonsDown[button] = isPressed;
        }
    }

    public boolean isMouseButtonDown(int button) {
        return button >= 0 && button < mouseButtonsDown.length && mouseButtonsDown[button];
    }

    public boolean wasMouseButtonPressed(int button) {
        return button >= 0 && button < mouseButtonsPressedThisFrame.length && mouseButtonsPressedThisFrame[button];
    }

    public void handleMouseMove(double mouseX, double mouseY) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;

        if (firstMouseEvent) {
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            firstMouseEvent = false;
            return;
        }

        mouseDeltaX += mouseX - lastMouseX;
        mouseDeltaY += mouseY - lastMouseY;
        lastMouseX = mouseX;
        lastMouseY = mouseY;
    }

    public void handleScroll(double xOffset, double yOffset) {
        scrollY += yOffset;
    }

    public MouseDelta consumeMouseDelta() {
        MouseDelta delta = new MouseDelta(mouseDeltaX, mouseDeltaY);
        mouseDeltaX = 0.0;
        mouseDeltaY = 0.0;
        return delta;
    }

    public void resetMouseTracking() {
        mouseDeltaX = 0.0;
        mouseDeltaY = 0.0;
        scrollY = 0.0;
        firstMouseEvent = true;
    }

    public void reset() {
        Arrays.fill(keysDown, false);
        Arrays.fill(keysPressedThisFrame, false);
        Arrays.fill(mouseButtonsDown, false);
        Arrays.fill(mouseButtonsPressedThisFrame, false);
        mouseDeltaX = 0.0;
        mouseDeltaY = 0.0;
        scrollY = 0.0;
        firstMouseEvent = true;
        lastMouseX = 0.0;
        lastMouseY = 0.0;
    }

    public void finishFrame() {
        Arrays.fill(keysPressedThisFrame, false);
        Arrays.fill(mouseButtonsPressedThisFrame, false);
        scrollY = 0.0;
    }

    public double getMouseX() {
        return mouseX;
    }

    public double getMouseY() {
        return mouseY;
    }

    public double getScrollY() {
        return scrollY;
    }

    public record MouseDelta(double xOffset, double yOffset) {
    }
}
