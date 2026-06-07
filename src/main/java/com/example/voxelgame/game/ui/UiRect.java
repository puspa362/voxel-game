package com.example.voxelgame.game.ui;

public record UiRect(float x, float y, float width, float height) {
    public boolean contains(double px, double py) {
        return px >= x && px <= x + width && py >= y && py <= y + height;
    }
}
