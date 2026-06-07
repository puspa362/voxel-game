package com.example.voxelgame.render;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;
import org.lwjgl.BufferUtils;

import static org.lwjgl.opengl.GL33C.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL33C.GL_NEAREST;
import static org.lwjgl.opengl.GL33C.GL_RGBA;
import static org.lwjgl.opengl.GL33C.GL_TEXTURE0;
import static org.lwjgl.opengl.GL33C.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL33C.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL33C.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL33C.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL33C.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL33C.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL33C.glActiveTexture;
import static org.lwjgl.opengl.GL33C.glBindTexture;
import static org.lwjgl.opengl.GL33C.glDeleteTextures;
import static org.lwjgl.opengl.GL33C.glGenTextures;
import static org.lwjgl.opengl.GL33C.glTexImage2D;
import static org.lwjgl.opengl.GL33C.glTexParameteri;

public final class PngTextureAtlas implements AutoCloseable {
    private static final boolean DEBUG_PRINT_TEXTURE_ID = true;

    private final int textureId;
    private final int columns;
    private final int rows;

    public PngTextureAtlas(String resourcePath, int tileSize, int columns, int rows) {
        this.columns = columns;
        this.rows = rows;
        BufferedImage image = loadImage(resourcePath, tileSize * columns, tileSize * rows);
        ByteBuffer pixelBuffer = toRgbaBuffer(image);

        textureId = glGenTextures();
        if (textureId == 0) {
            throw new IllegalStateException("Failed to create OpenGL texture for atlas: " + resourcePath);
        }
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, image.getWidth(), image.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, pixelBuffer);
        glBindTexture(GL_TEXTURE_2D, 0);

        if (DEBUG_PRINT_TEXTURE_ID) {
            System.out.println("Atlas Texture ID: " + textureId + " (" + resourcePath + ")");
        }
    }

    public void bind(int textureUnit) {
        glActiveTexture(GL_TEXTURE0 + textureUnit);
        glBindTexture(GL_TEXTURE_2D, textureId);
    }

    public float[] uvBounds(int tileX, int tileY) {
        float u0 = tileX / (float) columns;
        float v0 = tileY / (float) rows;
        float u1 = (tileX + 1) / (float) columns;
        float v1 = (tileY + 1) / (float) rows;
        return new float[]{u0, v0, u1, v1};
    }

    @Override
    public void close() {
        glDeleteTextures(textureId);
    }

    private static BufferedImage loadImage(String resourcePath, int expectedWidth, int expectedHeight) {
        try (InputStream inputStream = PngTextureAtlas.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing texture atlas resource: " + resourcePath);
            }

            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                throw new IllegalStateException("Unable to decode texture atlas resource: " + resourcePath);
            }
            if (image.getWidth() != expectedWidth || image.getHeight() != expectedHeight) {
                throw new IllegalStateException(
                        "Texture atlas " + resourcePath
                                + " has size " + image.getWidth() + "x" + image.getHeight()
                                + " but expected " + expectedWidth + "x" + expectedHeight + '.'
                );
            }
            return image;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load texture atlas resource: " + resourcePath, exception);
        }
    }

    private static ByteBuffer toRgbaBuffer(BufferedImage image) {
        ByteBuffer buffer = BufferUtils.createByteBuffer(image.getWidth() * image.getHeight() * 4);
        // Keep the atlas row order aligned with the tile grid conventions used by the
        // renderer, where tile row 0 refers to the first row in the PNG.
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                buffer.put((byte) ((argb >> 16) & 0xFF));
                buffer.put((byte) ((argb >> 8) & 0xFF));
                buffer.put((byte) (argb & 0xFF));
                buffer.put((byte) ((argb >> 24) & 0xFF));
            }
        }
        buffer.flip();
        return buffer;
    }
}
