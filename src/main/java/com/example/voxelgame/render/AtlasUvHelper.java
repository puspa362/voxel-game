package com.example.voxelgame.render;

public final class AtlasUvHelper {
    private final int columns;
    private final int rows;

    public AtlasUvHelper(int columns, int rows) {
        if (columns < 1 || rows < 1) {
            throw new IllegalArgumentException("Atlas grid dimensions must be positive.");
        }
        this.columns = columns;
        this.rows = rows;
    }

    public float[] getUV(int tileX, int tileY) {
        float halfTexelU = 0.5f / (columns * TextureManager.TILE_SIZE);
        float halfTexelV = 0.5f / (rows * TextureManager.TILE_SIZE);
        float u0 = tileX / (float) columns + halfTexelU;
        float v0 = tileY / (float) rows + halfTexelV;
        float u1 = (tileX + 1) / (float) columns - halfTexelU;
        float v1 = (tileY + 1) / (float) rows - halfTexelV;
        return new float[]{u0, v0, u1, v1};
    }

    public float[] uvBounds(int tileX, int tileY) {
        return getUV(tileX, tileY);
    }
}
