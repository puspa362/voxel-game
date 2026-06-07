package com.example.voxelgame.render;

import com.example.voxelgame.camera.FirstPersonCamera;
import org.joml.Vector3f;
import com.example.voxelgame.world.WorldTime;
import java.util.Objects;
import org.joml.Matrix4f;

import static org.lwjgl.opengl.GL33C.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL33C.GL_BLEND;
import static org.lwjgl.opengl.GL33C.GL_CULL_FACE;
import static org.lwjgl.opengl.GL33C.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL33C.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL33C.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL33C.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL33C.glBlendFunc;
import static org.lwjgl.opengl.GL33C.glClear;
import static org.lwjgl.opengl.GL33C.glClearColor;
import static org.lwjgl.opengl.GL33C.glDisable;
import static org.lwjgl.opengl.GL33C.glEnable;

public final class SkySystem implements AutoCloseable {
    private static final String SKY_VERTEX_SHADER = """
            #version 330 core
            layout (location = 0) in vec3 aPosition;

            uniform mat4 uProjection;
            uniform mat4 uView;
            uniform int uUseGradient;
            uniform float uGradientBaseY;

            out float vGradientMix;

            void main() {
                vGradientMix = clamp((aPosition.y - uGradientBaseY) / 220.0, 0.0, 1.0);
                gl_Position = uProjection * uView * vec4(aPosition, 1.0);
            }
            """;

    private static final String SKY_FRAGMENT_SHADER = """
            #version 330 core
            in float vGradientMix;
            uniform vec4 uColor;
            uniform vec4 uUpperColor;
            uniform vec4 uHorizonColor;
            uniform int uUseGradient;
            out vec4 fragColor;

            void main() {
                if (uUseGradient == 1) {
                    float t = smoothstep(0.0, 1.0, vGradientMix);
                    fragColor = mix(uHorizonColor, uUpperColor, t);
                } else {
                    fragColor = uColor;
                }
            }
            """;

    private static final int CLOUD_FIELD_RADIUS = 4;
    private static final int CLOUD_FIELD_DIAMETER = CLOUD_FIELD_RADIUS * 2 + 1;
    private static final int CLOUD_COUNT = CLOUD_FIELD_DIAMETER * CLOUD_FIELD_DIAMETER;
    private static final int MAX_CLOUD_PARTS = 10;
    private static final float SKY_RADIUS = 360.0f;
    private static final float CLOUD_CELL_SIZE = 172.0f;
    private static final float CLOUD_BASE_ALTITUDE = 214.0f;
    private static final float CLOUD_DRIFT_PER_DAY = 540.0f;
    private static final float CLOUD_WIND_X = 0.86f;
    private static final float CLOUD_WIND_Z = 0.31f;
    private static final float CLOUD_SPAWN_THRESHOLD = 0.26f;

    private final Cloud[] clouds = new Cloud[CLOUD_COUNT];
    private final float[] cloudTopVertices = new float[CLOUD_COUNT * MAX_CLOUD_PARTS * 6 * 3];
    private final float[] cloudSideVertices = new float[CLOUD_COUNT * MAX_CLOUD_PARTS * 4 * 6 * 3];
    private final float[] cloudBottomVertices = new float[CLOUD_COUNT * MAX_CLOUD_PARTS * 6 * 3];
    private final float[] celestialVertices = new float[6 * 3];
    private final float[] skyGradientVertices = new float[6 * 3];

    private ShaderProgram shaderProgram;
    private DynamicTriangleMesh3D triangleMesh;

    public SkySystem() {
        initializeClouds();
    }

    public void initialize() {
        shaderProgram = new ShaderProgram(SKY_VERTEX_SHADER, SKY_FRAGMENT_SHADER);
        triangleMesh = new DynamicTriangleMesh3D();
    }

    public void render(Matrix4f projectionMatrix, Matrix4f viewMatrix, FirstPersonCamera camera, WorldTime worldTime) {
        Objects.requireNonNull(projectionMatrix, "Projection matrix cannot be null.");
        Objects.requireNonNull(viewMatrix, "View matrix cannot be null.");
        Objects.requireNonNull(camera, "Camera cannot be null.");
        Objects.requireNonNull(worldTime, "World time cannot be null.");

        EnvironmentPalette palette = EnvironmentPalette.from(worldTime);
        float daylight = worldTime.getDaylight();
        float sunHeight = worldTime.getSunHeight();
        glClearColor(
                palette.upperR(),
                palette.upperG(),
                palette.upperB(),
                1.0f
        );
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        shaderProgram.bind();
        shaderProgram.setMatrix4("uProjection", projectionMatrix);
        shaderProgram.setMatrix4("uView", viewMatrix);
        shaderProgram.setFloat("uGradientBaseY", camera.getPosition().y - 70.0f);

        renderSkyGradient(camera, palette);
        renderClouds(camera, worldTime, palette, daylight);
        renderSun(camera, worldTime, daylight);
        renderMoon(camera, worldTime, 1.0f - daylight);

        shaderProgram.unbind();
        glDisable(GL_BLEND);
        glEnable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);
    }

    private void renderSkyGradient(FirstPersonCamera camera, EnvironmentPalette palette) {
        Vector3f center = new Vector3f(camera.getPosition()).add(new Vector3f(camera.getForward()).mul(320.0f));
        Vector3f right = camera.getRight().mul(500.0f);
        Vector3f up = camera.getUp().mul(300.0f);
        Vector3f bottomLeft = new Vector3f(center).sub(right).sub(up);
        Vector3f bottomRight = new Vector3f(center).add(right).sub(up);
        Vector3f topRight = new Vector3f(center).add(right).add(up);
        Vector3f topLeft = new Vector3f(center).sub(right).add(up);

        int index = 0;
        index = appendVertex(skyGradientVertices, index, bottomLeft);
        index = appendVertex(skyGradientVertices, index, bottomRight);
        index = appendVertex(skyGradientVertices, index, topRight);
        index = appendVertex(skyGradientVertices, index, topRight);
        index = appendVertex(skyGradientVertices, index, topLeft);
        appendVertex(skyGradientVertices, index, bottomLeft);

        shaderProgram.setInt("uUseGradient", 1);
        shaderProgram.setVector4("uUpperColor", palette.upperR(), palette.upperG(), palette.upperB(), 1.0f);
        shaderProgram.setVector4("uHorizonColor", palette.horizonR(), palette.horizonG(), palette.horizonB(), 1.0f);
        triangleMesh.render(skyGradientVertices, skyGradientVertices.length / 3);
    }

    private void renderClouds(FirstPersonCamera camera, WorldTime worldTime, EnvironmentPalette palette, float daylight) {
        float cloudOffset = (float) (worldTime.getDayProgress() * CLOUD_DRIFT_PER_DAY);
        Vector3f cameraPosition = camera.getPosition();
        int topIndex = 0;
        int sideIndex = 0;
        int bottomIndex = 0;

        float windX = CLOUD_WIND_X * cloudOffset;
        float windZ = CLOUD_WIND_Z * cloudOffset;
        int centerCellX = floorDiv((int) Math.floor(cameraPosition.x - windX), (int) CLOUD_CELL_SIZE);
        int centerCellZ = floorDiv((int) Math.floor(cameraPosition.z - windZ), (int) CLOUD_CELL_SIZE);
        int cloudIndex = 0;

        for (int cellZ = centerCellZ - CLOUD_FIELD_RADIUS; cellZ <= centerCellZ + CLOUD_FIELD_RADIUS; cellZ++) {
            for (int cellX = centerCellX - CLOUD_FIELD_RADIUS; cellX <= centerCellX + CLOUD_FIELD_RADIUS; cellX++) {
                Cloud cloud = cloudForCell(cellX, cellZ, windX, windZ);
                clouds[cloudIndex++] = cloud;
                if (cloud == null) {
                    continue;
                }

                for (CloudPart part : cloud.parts()) {
                    float minX = cloud.x() + part.offsetX() - part.halfWidth();
                    float maxX = cloud.x() + part.offsetX() + part.halfWidth();
                    float minY = cloud.altitude() + part.offsetY();
                    float maxY = minY + part.height();
                    float minZ = cloud.z() + part.offsetZ() - part.halfDepth();
                    float maxZ = cloud.z() + part.offsetZ() + part.halfDepth();
                    topIndex = appendTopFace(cloudTopVertices, topIndex, minX, maxY, minZ, maxX, maxZ);
                    sideIndex = appendSideFaces(cloudSideVertices, sideIndex, minX, minY, minZ, maxX, maxY, maxZ);
                    bottomIndex = appendBottomFace(cloudBottomVertices, bottomIndex, minX, minY, minZ, maxX, maxZ);
                }
            }
        }

        shaderProgram.setInt("uUseGradient", 0);
        float alpha = lerp(0.22f, 0.70f, daylight) + palette.sunset() * 0.10f;
        float tintR = palette.cloudR();
        float tintG = palette.cloudG();
        float tintB = palette.cloudB();
        shaderProgram.setVector4(
                "uColor",
                tintR * (0.76f + palette.sunset() * 0.10f),
                tintG * (0.77f + palette.sunset() * 0.03f),
                tintB * 0.82f,
                alpha * 0.76f
        );
        triangleMesh.render(cloudBottomVertices, bottomIndex / 3);

        shaderProgram.setVector4(
                "uColor",
                tintR * 0.90f,
                tintG * 0.91f,
                tintB * 0.94f,
                alpha * 0.88f
        );
        triangleMesh.render(cloudSideVertices, sideIndex / 3);

        shaderProgram.setVector4(
                "uColor",
                tintR,
                tintG,
                tintB,
                alpha
        );
        triangleMesh.render(cloudTopVertices, topIndex / 3);
    }

    private void renderSun(FirstPersonCamera camera, WorldTime worldTime, float daylight) {
        float visibility = saturate((worldTime.getSunHeight() + 0.08f) * 1.25f) * daylight;
        if (visibility <= 0.01f) {
            return;
        }

        Vector3f position = celestialPosition(camera, worldTime.getCelestialAngleRadians(), SKY_RADIUS, 24.0f);
        appendBillboard(celestialVertices, camera, position, 18.0f);
        shaderProgram.setInt("uUseGradient", 0);
        shaderProgram.setVector4("uColor", 1.0f, 0.84f, 0.30f, 0.65f + visibility * 0.30f);
        triangleMesh.render(celestialVertices, celestialVertices.length / 3);
    }

    private void renderMoon(FirstPersonCamera camera, WorldTime worldTime, float moonlight) {
        float visibility = saturate((-worldTime.getSunHeight() + 0.12f) * 1.20f) * moonlight;
        if (visibility <= 0.01f) {
            return;
        }

        Vector3f position = celestialPosition(camera, worldTime.getCelestialAngleRadians() + (float) Math.PI, SKY_RADIUS, -28.0f);
        appendBillboard(celestialVertices, camera, position, 13.0f);
        shaderProgram.setInt("uUseGradient", 0);
        shaderProgram.setVector4("uColor", 0.86f, 0.90f, 1.0f, 0.55f + visibility * 0.28f);
        triangleMesh.render(celestialVertices, celestialVertices.length / 3);
    }

    private Vector3f celestialPosition(FirstPersonCamera camera, float angle, float radius, float zBias) {
        float horizontalRadius = radius * 0.78f;
        return new Vector3f(
                camera.getPosition().x + (float) Math.cos(angle) * horizontalRadius,
                CLOUD_BASE_ALTITUDE + 62.0f + (float) Math.sin(angle) * radius,
                camera.getPosition().z + zBias
        );
    }

    private void appendBillboard(float[] target, FirstPersonCamera camera, Vector3f center, float halfSize) {
        Vector3f right = camera.getRight().mul(halfSize);
        Vector3f up = camera.getUp().mul(halfSize);

        Vector3f bottomLeft = new Vector3f(center).sub(right).sub(up);
        Vector3f bottomRight = new Vector3f(center).add(right).sub(up);
        Vector3f topRight = new Vector3f(center).add(right).add(up);
        Vector3f topLeft = new Vector3f(center).sub(right).add(up);

        int index = 0;
        index = appendVertex(target, index, bottomLeft);
        index = appendVertex(target, index, bottomRight);
        index = appendVertex(target, index, topRight);
        index = appendVertex(target, index, topRight);
        index = appendVertex(target, index, topLeft);
        appendVertex(target, index, bottomLeft);
    }

    private int appendTopFace(float[] target, int index, float minX, float y, float minZ, float maxX, float maxZ) {
        target[index++] = minX;
        target[index++] = y;
        target[index++] = minZ;
        target[index++] = maxX;
        target[index++] = y;
        target[index++] = minZ;
        target[index++] = maxX;
        target[index++] = y;
        target[index++] = maxZ;

        target[index++] = maxX;
        target[index++] = y;
        target[index++] = maxZ;
        target[index++] = minX;
        target[index++] = y;
        target[index++] = maxZ;
        target[index++] = minX;
        target[index++] = y;
        target[index++] = minZ;
        return index;
    }

    private int appendBottomFace(float[] target, int index, float minX, float y, float minZ, float maxX, float maxZ) {
        target[index++] = minX;
        target[index++] = y;
        target[index++] = maxZ;
        target[index++] = maxX;
        target[index++] = y;
        target[index++] = maxZ;
        target[index++] = maxX;
        target[index++] = y;
        target[index++] = minZ;

        target[index++] = maxX;
        target[index++] = y;
        target[index++] = minZ;
        target[index++] = minX;
        target[index++] = y;
        target[index++] = minZ;
        target[index++] = minX;
        target[index++] = y;
        target[index++] = maxZ;
        return index;
    }

    private int appendSideFaces(float[] target, int index, float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        index = appendVerticalFace(target, index, minX, minY, minZ, minX, maxY, minZ, maxX, minY, minZ, maxX, maxY, minZ);
        index = appendVerticalFace(target, index, maxX, minY, maxZ, maxX, maxY, maxZ, minX, minY, maxZ, minX, maxY, maxZ);
        index = appendVerticalFace(target, index, maxX, minY, minZ, maxX, maxY, minZ, maxX, minY, maxZ, maxX, maxY, maxZ);
        return appendVerticalFace(target, index, minX, minY, maxZ, minX, maxY, maxZ, minX, minY, minZ, minX, maxY, minZ);
    }

    private int appendVerticalFace(
            float[] target,
            int index,
            float bottomLeftX,
            float bottomLeftY,
            float bottomLeftZ,
            float topLeftX,
            float topLeftY,
            float topLeftZ,
            float bottomRightX,
            float bottomRightY,
            float bottomRightZ,
            float topRightX,
            float topRightY,
            float topRightZ
    ) {
        target[index++] = bottomLeftX;
        target[index++] = bottomLeftY;
        target[index++] = bottomLeftZ;
        target[index++] = bottomRightX;
        target[index++] = bottomRightY;
        target[index++] = bottomRightZ;
        target[index++] = topRightX;
        target[index++] = topRightY;
        target[index++] = topRightZ;

        target[index++] = topRightX;
        target[index++] = topRightY;
        target[index++] = topRightZ;
        target[index++] = topLeftX;
        target[index++] = topLeftY;
        target[index++] = topLeftZ;
        target[index++] = bottomLeftX;
        target[index++] = bottomLeftY;
        target[index++] = bottomLeftZ;
        return index;
    }

    private void initializeClouds() {
        for (int i = 0; i < CLOUD_COUNT; i++) {
            clouds[i] = null;
        }
    }

    private Cloud cloudForCell(int cellX, int cellZ, float windX, float windZ) {
        long seed = mix64((((long) cellX) << 32) ^ (cellZ & 0xFFFFFFFFL) ^ 0xC6BC279692B5CC83L);
        float spawnRoll = unit(seed);
        if (spawnRoll < CLOUD_SPAWN_THRESHOLD) {
            return null;
        }

        float density = smoothstep(CLOUD_SPAWN_THRESHOLD, 1.0f, spawnRoll);
        float centerX = cellX * CLOUD_CELL_SIZE + unit(seed >>> 8) * CLOUD_CELL_SIZE + windX;
        float centerZ = cellZ * CLOUD_CELL_SIZE + unit(seed >>> 24) * CLOUD_CELL_SIZE + windZ;
        float width = 42.0f + unit(seed >>> 40) * 92.0f;
        float depth = 22.0f + unit(seed >>> 48) * 56.0f;
        float height = 7.0f + unit(seed >>> 56) * 16.0f;
        float altitude = CLOUD_BASE_ALTITUDE + discreteHeight(seed) + density * 24.0f;
        CloudPart[] parts = buildCloudParts(seed, width, depth, height, density);
        return new Cloud(centerX, centerZ, altitude, width, depth, height, density, parts);
    }

    private CloudPart[] buildCloudParts(long seed, float width, float depth, float height, float density) {
        int partCount = 4 + (int) Math.floor(density * 6.0f);
        CloudPart[] parts = new CloudPart[partCount];
        parts[0] = new CloudPart(0.0f, 0.0f, 0.0f, width * 0.28f, depth * 0.34f, height * 0.72f);
        for (int i = 1; i < partCount; i++) {
            long partSeed = mix64(seed + i * 0x9E3779B97F4A7C15L);
            float angle = unit(partSeed) * (float) Math.PI * 2.0f;
            float radius = 0.18f + unit(partSeed >>> 10) * 0.38f;
            float offsetX = (float) Math.cos(angle) * width * radius;
            float offsetZ = (float) Math.sin(angle) * depth * radius;
            float offsetY = (unit(partSeed >>> 20) - 0.35f) * height * 0.52f;
            float halfWidth = width * (0.12f + unit(partSeed >>> 30) * 0.20f);
            float halfDepth = depth * (0.15f + unit(partSeed >>> 40) * 0.23f);
            float partHeight = height * (0.38f + unit(partSeed >>> 50) * 0.52f);
            parts[i] = new CloudPart(offsetX, offsetZ, offsetY, halfWidth, halfDepth, partHeight);
        }
        return parts;
    }

    private float discreteHeight(long seed) {
        int tier = (int) Math.floor(unit(seed >>> 32) * 3.0f);
        return switch (tier) {
            case 0 -> 0.0f;
            case 1 -> 34.0f;
            default -> 70.0f;
        };
    }

    private int appendVertex(float[] target, int index, Vector3f position) {
        target[index++] = position.x;
        target[index++] = position.y;
        target[index++] = position.z;
        return index;
    }

    private long mix64(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return value;
    }

    private float lerp(float start, float end, float alpha) {
        return start + (end - start) * alpha;
    }

    private float saturate(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private float smoothstep(float edge0, float edge1, float value) {
        float x = saturate((value - edge0) / (edge1 - edge0));
        return x * x * (3.0f - 2.0f * x);
    }

    private float unit(long value) {
        return (value & 0xFFFFL) / 65535.0f;
    }

    private int floorDiv(int value, int divisor) {
        return Math.floorDiv(value, divisor);
    }

    @Override
    public void close() {
        if (triangleMesh != null) {
            triangleMesh.close();
            triangleMesh = null;
        }
        if (shaderProgram != null) {
            shaderProgram.close();
            shaderProgram = null;
        }
    }
}
