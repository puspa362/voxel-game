package com.example.voxelgame.render;

import com.example.voxelgame.game.entity.EntityManager;
import com.example.voxelgame.game.entity.EntityRenderContext;
import org.joml.Vector3f;
import java.util.Objects;
import org.joml.Matrix4f;

public final class EntityRenderer implements AutoCloseable, EntityRenderContext {
    private static final String ENTITY_VERTEX_SHADER = """
            #version 330 core
            layout (location = 0) in vec3 aPosition;

            uniform mat4 uProjection;
            uniform mat4 uView;

            void main() {
                gl_Position = uProjection * uView * vec4(aPosition, 1.0);
            }
            """;

    private static final String ENTITY_FRAGMENT_SHADER = """
            #version 330 core
            uniform vec4 uColor;
            out vec4 fragColor;

            void main() {
                fragColor = uColor;
            }
            """;

    private final DynamicTriangleMesh3D triangleMesh = new DynamicTriangleMesh3D();
    private final ShaderProgram shaderProgram = new ShaderProgram(ENTITY_VERTEX_SHADER, ENTITY_FRAGMENT_SHADER);
    private final Matrix4f projectionMatrix = new Matrix4f();
    private final Matrix4f viewMatrix = new Matrix4f();

    public void render(EntityManager entityManager, Matrix4f projectionMatrix, Matrix4f viewMatrix) {
        Objects.requireNonNull(entityManager, "Entity manager cannot be null.");
        this.projectionMatrix.set(projectionMatrix);
        this.viewMatrix.set(viewMatrix);

        shaderProgram.bind();
        shaderProgram.setMatrix4("uProjection", this.projectionMatrix);
        shaderProgram.setMatrix4("uView", this.viewMatrix);
        entityManager.render(this);
        shaderProgram.unbind();
    }

    @Override
    public void drawCube(Vector3f center, Vector3f halfExtents, float yawRadians, float red, float green, float blue, float alpha) {
        Objects.requireNonNull(center, "Cube center cannot be null.");
        Objects.requireNonNull(halfExtents, "Cube half extents cannot be null.");

        shaderProgram.setVector4("uColor", red, green, blue, alpha);
        float[] vertices = buildCubeVertices(center, halfExtents, yawRadians);
        triangleMesh.render(vertices, vertices.length / 3);
    }

    @Override
    public void close() {
        triangleMesh.close();
        shaderProgram.close();
    }

    private float[] buildCubeVertices(Vector3f center, Vector3f halfExtents, float yawRadians) {
        float[][] corners = {
                {-halfExtents.x, -halfExtents.y, -halfExtents.z},
                {halfExtents.x, -halfExtents.y, -halfExtents.z},
                {halfExtents.x, halfExtents.y, -halfExtents.z},
                {-halfExtents.x, halfExtents.y, -halfExtents.z},
                {-halfExtents.x, -halfExtents.y, halfExtents.z},
                {halfExtents.x, -halfExtents.y, halfExtents.z},
                {halfExtents.x, halfExtents.y, halfExtents.z},
                {-halfExtents.x, halfExtents.y, halfExtents.z}
        };

        float cosine = (float) Math.cos(yawRadians);
        float sine = (float) Math.sin(yawRadians);
        for (float[] corner : corners) {
            float rotatedX = corner[0] * cosine - corner[2] * sine;
            float rotatedZ = corner[0] * sine + corner[2] * cosine;
            corner[0] = center.x + rotatedX;
            corner[1] = center.y + corner[1];
            corner[2] = center.z + rotatedZ;
        }

        int[][] faces = {
                {4, 5, 6, 7},
                {1, 0, 3, 2},
                {0, 4, 7, 3},
                {5, 1, 2, 6},
                {3, 7, 6, 2},
                {0, 1, 5, 4}
        };

        float[] vertices = new float[faces.length * 6 * 3];
        int offset = 0;
        for (int[] face : faces) {
            offset = appendTriangle(vertices, offset, corners[face[0]], corners[face[1]], corners[face[2]]);
            offset = appendTriangle(vertices, offset, corners[face[2]], corners[face[3]], corners[face[0]]);
        }
        return vertices;
    }

    private int appendTriangle(float[] target, int offset, float[] a, float[] b, float[] c) {
        offset = appendVertex(target, offset, a);
        offset = appendVertex(target, offset, b);
        return appendVertex(target, offset, c);
    }

    private int appendVertex(float[] target, int offset, float[] vertex) {
        target[offset++] = vertex[0];
        target[offset++] = vertex[1];
        target[offset++] = vertex[2];
        return offset;
    }
}
