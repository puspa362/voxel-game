package com.example.voxelgame.core;

import java.nio.IntBuffer;
import java.util.Objects;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_DISABLED;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_NORMAL;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_FORWARD_COMPAT;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.glfw.GLFW.glfwGetVideoMode;
import static org.lwjgl.glfw.GLFW.glfwGetWindowSize;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetFramebufferSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwSetInputMode;
import static org.lwjgl.glfw.GLFW.glfwSetWindowPos;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.glfw.GLFW.glfwSetWindowTitle;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class GameWindow implements AutoCloseable {
    private final WindowConfig config;
    private final InputState inputState = new InputState();

    private GLFWErrorCallback errorCallback;
    private long handle = NULL;
    private boolean glfwInitialized;
    private boolean cursorCaptured;
    private boolean autoCaptureEnabled = true;
    private int framebufferWidth;
    private int framebufferHeight;

    public GameWindow(WindowConfig config) {
        this.config = Objects.requireNonNull(config, "Window configuration cannot be null.");
    }

    public void initialize() {
        errorCallback = GLFWErrorCallback.createPrint(System.err);
        errorCallback.set();

        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW.");
        }

        glfwInitialized = true;
        configureWindowHints();

        handle = glfwCreateWindow(config.width(), config.height(), config.title(), NULL, NULL);
        if (handle == NULL) {
            throw new IllegalStateException("Unable to create the GLFW window.");
        }

        centerOnPrimaryMonitor();
        glfwMakeContextCurrent(handle);
        glfwSwapInterval(config.vSync() ? 1 : 0);
        registerCallbacks();
        updateFramebufferSize();
        glfwShowWindow(handle);
    }

    private void configureWindowHints() {
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
    }

    private void centerOnPrimaryMonitor() {
        try (MemoryStack stack = stackPush()) {
            IntBuffer windowWidth = stack.mallocInt(1);
            IntBuffer windowHeight = stack.mallocInt(1);
            glfwGetWindowSize(handle, windowWidth, windowHeight);

            long monitor = glfwGetPrimaryMonitor();
            GLFWVidMode videoMode = glfwGetVideoMode(monitor);
            if (videoMode == null) {
                return;
            }

            int centeredX = (videoMode.width() - windowWidth.get(0)) / 2;
            int centeredY = (videoMode.height() - windowHeight.get(0)) / 2;
            glfwSetWindowPos(handle, centeredX, centeredY);
        }
    }

    private void registerCallbacks() {
        org.lwjgl.glfw.GLFW.glfwSetKeyCallback(handle, (windowHandle, key, scanCode, action, modifiers) -> {
            inputState.setKeyState(key, action != org.lwjgl.glfw.GLFW.GLFW_RELEASE);
        });

        org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback(handle, (windowHandle, mouseX, mouseY) -> {
            inputState.handleMouseMove(mouseX, mouseY);
        });

        org.lwjgl.glfw.GLFW.glfwSetMouseButtonCallback(handle, (windowHandle, button, action, modifiers) -> {
            inputState.setMouseButtonState(button, action != org.lwjgl.glfw.GLFW.GLFW_RELEASE);
            if (autoCaptureEnabled
                    && button == GLFW_MOUSE_BUTTON_LEFT
                    && action == org.lwjgl.glfw.GLFW.GLFW_PRESS
                    && !cursorCaptured) {
                captureCursor();
            }
        });

        org.lwjgl.glfw.GLFW.glfwSetScrollCallback(handle, (windowHandle, xOffset, yOffset) -> {
            inputState.handleScroll(xOffset, yOffset);
        });

        glfwSetFramebufferSizeCallback(handle, (windowHandle, width, height) -> {
            if (width > 0 && height > 0) {
                framebufferWidth = width;
                framebufferHeight = height;
            }
        });

        releaseCursor();
    }

    private void updateFramebufferSize() {
        try (MemoryStack stack = stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            glfwGetFramebufferSize(handle, width, height);
            framebufferWidth = width.get(0);
            framebufferHeight = height.get(0);
        }
    }

    public void pollEvents() {
        glfwPollEvents();
    }

    public void swapBuffers() {
        glfwSwapBuffers(handle);
    }

    public boolean shouldClose() {
        return glfwWindowShouldClose(handle);
    }

    public long getHandle() {
        return handle;
    }

    public int getFramebufferWidth() {
        return framebufferWidth;
    }

    public int getFramebufferHeight() {
        return framebufferHeight;
    }

    public String getBaseTitle() {
        return config.title();
    }

    public InputState getInputState() {
        return inputState;
    }

    public void setTitle(String title) {
        glfwSetWindowTitle(handle, title);
    }

    public void finishFrame() {
        inputState.finishFrame();
    }

    public boolean isCursorCaptured() {
        return cursorCaptured;
    }

    public void captureCursor() {
        if (handle == NULL || cursorCaptured) {
            return;
        }
        glfwSetInputMode(handle, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        inputState.resetMouseTracking();
        cursorCaptured = true;
    }

    public void releaseCursor() {
        if (handle == NULL) {
            return;
        }
        glfwSetInputMode(handle, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
        inputState.resetMouseTracking();
        cursorCaptured = false;
    }

    public void requestClose() {
        if (handle != NULL) {
            glfwSetWindowShouldClose(handle, true);
        }
    }

    public void setAutoCaptureEnabled(boolean autoCaptureEnabled) {
        this.autoCaptureEnabled = autoCaptureEnabled;
    }

    @Override
    public void close() {
        if (handle != NULL) {
            inputState.reset();
            Callbacks.glfwFreeCallbacks(handle);
            glfwDestroyWindow(handle);
            handle = NULL;
        }

        if (glfwInitialized) {
            glfwTerminate();
            glfwInitialized = false;
        }

        if (errorCallback != null) {
            errorCallback.free();
            errorCallback = null;
        }
    }
}
