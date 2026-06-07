package com.example.voxelgame.render;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import org.lwjgl.BufferUtils;

import static org.lwjgl.opengl.GL33C.GL_LINEAR;
import static org.lwjgl.opengl.GL33C.GL_RED;
import static org.lwjgl.opengl.GL33C.GL_TEXTURE0;
import static org.lwjgl.opengl.GL33C.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL33C.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL33C.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL33C.GL_UNPACK_ALIGNMENT;
import static org.lwjgl.opengl.GL33C.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL33C.glActiveTexture;
import static org.lwjgl.opengl.GL33C.glBindTexture;
import static org.lwjgl.opengl.GL33C.glDeleteTextures;
import static org.lwjgl.opengl.GL33C.glGenTextures;
import static org.lwjgl.opengl.GL33C.glPixelStorei;
import static org.lwjgl.opengl.GL33C.glTexImage2D;
import static org.lwjgl.opengl.GL33C.glTexParameteri;

public final class BitmapFont implements AutoCloseable {
    private static final int FIRST_CHAR = 32;
    private static final int LAST_CHAR = 126;
    private static final int COLUMNS = 16;

    private final int textureId;
    private final int cellWidth;
    private final int cellHeight;
    private final int atlasWidth;
    private final int atlasHeight;

    public BitmapFont() {
        Font font = new Font("Consolas", Font.PLAIN, 18);
        BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D probeGraphics = probe.createGraphics();
        probeGraphics.setFont(font);
        FontMetrics metrics = probeGraphics.getFontMetrics();
        cellWidth = metrics.charWidth('W') + 4;
        cellHeight = metrics.getHeight() + 4;
        probeGraphics.dispose();

        int rows = (LAST_CHAR - FIRST_CHAR + COLUMNS) / COLUMNS;
        atlasWidth = cellWidth * COLUMNS;
        atlasHeight = cellHeight * rows;

        BufferedImage atlas = new BufferedImage(atlasWidth, atlasHeight, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics = atlas.createGraphics();
        graphics.setFont(font);
        graphics.setColor(Color.BLACK);
        graphics.fillRect(0, 0, atlasWidth, atlasHeight);
        graphics.setColor(Color.WHITE);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        FontMetrics atlasMetrics = graphics.getFontMetrics();

        for (int codePoint = FIRST_CHAR; codePoint <= LAST_CHAR; codePoint++) {
            int index = codePoint - FIRST_CHAR;
            int column = index % COLUMNS;
            int row = index / COLUMNS;
            int x = column * cellWidth + 2;
            int y = row * cellHeight + atlasMetrics.getAscent() + 2;
            graphics.drawString(Character.toString((char) codePoint), x, y);
        }
        graphics.dispose();

        ByteBuffer pixels = BufferUtils.createByteBuffer(atlasWidth * atlasHeight);
        for (int y = 0; y < atlasHeight; y++) {
            for (int x = 0; x < atlasWidth; x++) {
                int value = atlas.getRaster().getSample(x, y, 0);
                pixels.put((byte) value);
            }
        }
        pixels.flip();

        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RED, atlasWidth, atlasHeight, 0, GL_RED, GL_UNSIGNED_BYTE, pixels);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void bind(int textureUnit) {
        glActiveTexture(GL_TEXTURE0 + textureUnit);
        glBindTexture(GL_TEXTURE_2D, textureId);
    }

    public float[] buildTextVertices(String text, float x, float y, float scale) {
        int capacity = text.length() * 6 * 4;
        float[] vertices = new float[capacity];
        int cursor = 0;
        float penX = x;
        float penY = y;

        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            if (character == '\n') {
                penX = x;
                penY += cellHeight * scale;
                continue;
            }

            if (character < FIRST_CHAR || character > LAST_CHAR) {
                penX += cellWidth * scale;
                continue;
            }

            int index = character - FIRST_CHAR;
            int column = index % COLUMNS;
            int row = index / COLUMNS;
            float u0 = column * cellWidth / (float) atlasWidth;
            float v0 = row * cellHeight / (float) atlasHeight;
            float u1 = (column + 1) * cellWidth / (float) atlasWidth;
            float v1 = (row + 1) * cellHeight / (float) atlasHeight;

            float x0 = penX;
            float y0 = penY;
            float x1 = penX + cellWidth * scale;
            float y1 = penY + cellHeight * scale;

            cursor = putVertex(vertices, cursor, x0, y0, u0, v0);
            cursor = putVertex(vertices, cursor, x1, y0, u1, v0);
            cursor = putVertex(vertices, cursor, x1, y1, u1, v1);
            cursor = putVertex(vertices, cursor, x1, y1, u1, v1);
            cursor = putVertex(vertices, cursor, x0, y1, u0, v1);
            cursor = putVertex(vertices, cursor, x0, y0, u0, v0);

            penX += cellWidth * scale;
        }

        float[] result = new float[cursor];
        System.arraycopy(vertices, 0, result, 0, cursor);
        return result;
    }

    public float getLineHeight(float scale) {
        return cellHeight * scale;
    }

    public float measureTextWidth(String text, float scale) {
        float maxWidth = 0.0f;
        float currentWidth = 0.0f;

        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            if (character == '\n') {
                maxWidth = Math.max(maxWidth, currentWidth);
                currentWidth = 0.0f;
                continue;
            }
            currentWidth += cellWidth * scale;
        }

        return Math.max(maxWidth, currentWidth);
    }

    public float measureTextHeight(String text, float scale) {
        int lineCount = 1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                lineCount++;
            }
        }
        return lineCount * getLineHeight(scale);
    }

    private int putVertex(float[] vertices, int cursor, float x, float y, float u, float v) {
        vertices[cursor++] = x;
        vertices[cursor++] = y;
        vertices[cursor++] = u;
        vertices[cursor++] = v;
        return cursor;
    }

    @Override
    public void close() {
        glDeleteTextures(textureId);
    }
}
