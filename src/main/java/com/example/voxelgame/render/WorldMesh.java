package com.example.voxelgame.render;

import static org.lwjgl.opengl.GL33C.GL_FLOAT;
import static org.lwjgl.opengl.GL33C.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL33C.glBindBuffer;
import static org.lwjgl.opengl.GL33C.glBindVertexArray;
import static org.lwjgl.opengl.GL33C.glBufferData;
import static org.lwjgl.opengl.GL33C.glDeleteBuffers;
import static org.lwjgl.opengl.GL33C.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL33C.glDrawArrays;
import static org.lwjgl.opengl.GL33C.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL33C.glGenBuffers;
import static org.lwjgl.opengl.GL33C.glGenVertexArrays;
import static org.lwjgl.opengl.GL33C.glVertexAttribPointer;
import static org.lwjgl.opengl.GL33C.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL33C.GL_TRIANGLES;

public final class WorldMesh implements AutoCloseable {
    private static final int FLOAT_SIZE_BYTES = Float.BYTES;
    private static final int STRIDE_FLOATS = 8;

    private final int vaoId;
    private final int vboId;
    private final int vertexCount;

    public WorldMesh(float[] vertices) {
        vertexCount = vertices.length / STRIDE_FLOATS;
        vaoId = glGenVertexArrays();
        vboId = glGenBuffers();

        glBindVertexArray(vaoId);
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, STRIDE_FLOATS * FLOAT_SIZE_BYTES, 0L);
        glEnableVertexAttribArray(0);

        glVertexAttribPointer(1, 2, GL_FLOAT, false, STRIDE_FLOATS * FLOAT_SIZE_BYTES, 3L * FLOAT_SIZE_BYTES);
        glEnableVertexAttribArray(1);

        glVertexAttribPointer(2, 1, GL_FLOAT, false, STRIDE_FLOATS * FLOAT_SIZE_BYTES, 5L * FLOAT_SIZE_BYTES);
        glEnableVertexAttribArray(2);

        glVertexAttribPointer(3, 2, GL_FLOAT, false, STRIDE_FLOATS * FLOAT_SIZE_BYTES, 6L * FLOAT_SIZE_BYTES);
        glEnableVertexAttribArray(3);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    public void render() {
        if (vertexCount == 0) {
            return;
        }
        glBindVertexArray(vaoId);
        glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        glBindVertexArray(0);
    }

    @Override
    public void close() {
        glDeleteBuffers(vboId);
        glDeleteVertexArrays(vaoId);
    }
}
