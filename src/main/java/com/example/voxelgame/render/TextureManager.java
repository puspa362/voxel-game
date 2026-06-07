package com.example.voxelgame.render;

public final class TextureManager implements AutoCloseable {
    public static final String ATLAS_RESOURCE = "/assets/textures/atlas.png";
    public static final int TILE_SIZE = 16;
    public static final int ATLAS_COLUMNS = 16;
    public static final int ATLAS_ROWS = 16;

    private static final Object LOCK = new Object();
    private static PngTextureAtlas sharedAtlas;
    private static int references;

    private final AtlasUvHelper uvHelper = new AtlasUvHelper(ATLAS_COLUMNS, ATLAS_ROWS);

    public TextureManager() {
        synchronized (LOCK) {
            if (sharedAtlas == null) {
                sharedAtlas = new PngTextureAtlas(ATLAS_RESOURCE, TILE_SIZE, ATLAS_COLUMNS, ATLAS_ROWS);
            }
            references++;
        }
    }

    public void bind(int textureUnit) {
        synchronized (LOCK) {
            if (sharedAtlas == null) {
                throw new IllegalStateException("Texture atlas was accessed after shutdown.");
            }
            sharedAtlas.bind(textureUnit);
        }
    }

    public float[] uvBounds(int tileX, int tileY) {
        return getUV(tileX, tileY);
    }

    public float[] getUV(int tileX, int tileY) {
        return uvHelper.getUV(tileX, tileY);
    }

    @Override
    public void close() {
        synchronized (LOCK) {
            references = Math.max(0, references - 1);
            if (references == 0 && sharedAtlas != null) {
                sharedAtlas.close();
                sharedAtlas = null;
            }
        }
    }
}
