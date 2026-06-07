package com.example.voxelgame.render;

import static org.lwjgl.opengl.GL11C.GL_BLEND;
import static org.lwjgl.opengl.GL11C.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11C.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11C.GL_FILL;
import static org.lwjgl.opengl.GL11C.GL_FRONT_AND_BACK;
import static org.lwjgl.opengl.GL11C.GL_LESS;
import static org.lwjgl.opengl.GL11C.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11C.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11C.glBlendFunc;
import static org.lwjgl.opengl.GL11C.glDepthFunc;
import static org.lwjgl.opengl.GL11C.glDepthMask;
import static org.lwjgl.opengl.GL11C.glDisable;
import static org.lwjgl.opengl.GL11C.glEnable;
import static org.lwjgl.opengl.GL11C.glPolygonMode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.joml.Matrix4f;

import com.example.voxelgame.camera.FirstPersonCamera;
import com.example.voxelgame.render.WorldMeshBuilder.ChunkSnapshot;
import com.example.voxelgame.render.WorldMeshBuilder.MeshWorldSnapshot;
import com.example.voxelgame.world.BlockRegistry;
import com.example.voxelgame.world.Chunk;
import com.example.voxelgame.world.VoxelWorld;
import com.example.voxelgame.world.WorldTime;

public final class WorldRenderer implements AutoCloseable {
    private static final int MAX_MESH_UPLOADS_PER_FRAME = 2;

    private static final String WORLD_VERTEX_SHADER = """
            #version 330 core
            layout (location = 0) in vec3 aPosition;
            layout (location = 1) in vec2 aUv;
            layout (location = 2) in float aLight;
            layout (location = 3) in vec2 aTile;

            uniform mat4 uProjection;
            uniform mat4 uView;
            uniform float uTime;
            uniform vec2 uWaterTile;
            uniform int uUseTiledAtlas;

            out vec2 vUv;
            out vec2 vLocalUv;
            out vec2 vTile;
            out float vLight;
            out vec3 vWorldPosition;
            out float vIsWater;

            float insideWaterTile(vec2 uv) {
                vec2 tile = floor(clamp(uv, vec2(0.0), vec2(0.9999)) * 16.0);
                vec2 matchTile = 1.0 - step(vec2(0.5), abs(tile - uWaterTile));
                return matchTile.x * matchTile.y;
            }

            void main() {
                vec3 position = aPosition;
                vTile = aTile;
                vLocalUv = aUv;
                vIsWater = uUseTiledAtlas == 1
                        ? (1.0 - step(0.5, abs(aTile.x - uWaterTile.x))) * (1.0 - step(0.5, abs(aTile.y - uWaterTile.y)))
                        : insideWaterTile(aUv);
                if (vIsWater > 0.5) {
                    float wave = sin(position.x * 0.55 + uTime * 0.85) + sin(position.z * 0.43 + uTime * 0.62);
                    position.y += wave * 0.018;
                }
                vUv = aUv;
                vLight = aLight;
                vWorldPosition = position;
                gl_Position = uProjection * uView * vec4(position, 1.0);
            }
            """;

    private static final String WORLD_FRAGMENT_SHADER = """
            #version 330 core
            in vec2 vUv;
            in vec2 vLocalUv;
            in vec2 vTile;
            in float vLight;
            in vec3 vWorldPosition;
            in float vIsWater;
            out vec4 fragColor;
            uniform sampler2D uAtlas;
            uniform vec3 uTint;
            uniform float uAmbientLight;
            uniform float uSunLight;
            uniform float uTime;
            uniform vec3 uCameraPosition;
            uniform vec3 uFogColor;
            uniform int uUnderwater;
            uniform vec2 uWaterUvMin;
            uniform vec2 uWaterUvMax;
            uniform int uUseTiledAtlas;

            vec2 atlasUv(vec2 tile, vec2 localUv) {
                vec2 atlasTiles = vec2(16.0, 16.0);
                vec2 halfTexel = vec2(0.5) / (atlasTiles * 16.0);
                vec2 tileInset = halfTexel * atlasTiles;
                vec2 repeated = fract(localUv);
                return (tile + tileInset + repeated * (vec2(1.0) - tileInset * 2.0)) / atlasTiles;
            }

            void main() {
                vec2 uv = vUv;
                if (uUseTiledAtlas == 1) {
                    vec2 localUv = vLocalUv;
                    if (vIsWater > 0.5) {
                        localUv += vec2(uTime * 0.018, uTime * 0.011);
                    }
                    uv = atlasUv(vTile, localUv);
                } else if (vIsWater > 0.5) {
                    vec2 waterSize = uWaterUvMax - uWaterUvMin;
                    vec2 localUv = (uv - uWaterUvMin) / waterSize;
                    localUv = fract(localUv + vec2(uTime * 0.018, uTime * 0.011));
                    uv = uWaterUvMin + localUv * waterSize;
                }

                vec4 sampled = texture(uAtlas, uv);
                if (sampled.a <= 0.01) {
                    discard;
                }
                float faceLight = mix(0.35, 1.0, clamp(vLight, 0.0, 1.0));
                float light = clamp(uAmbientLight + (faceLight * uSunLight), 0.0, 1.35);
                vec3 color = sampled.rgb * uTint * light;
                float alpha = sampled.a;

                if (vIsWater > 0.5) {
                    float depthTint = clamp((70.0 - vWorldPosition.y) / 34.0, 0.0, 1.0);
                    vec3 shallow = vec3(0.40, 0.86, 0.92);
                    vec3 deep = vec3(0.08, 0.34, 0.56);
                    vec3 waterTint = mix(shallow, deep, depthTint);
                    float grazing = pow(1.0 - abs(normalize(uCameraPosition - vWorldPosition).y), 2.0);
                    color = mix(color, waterTint, 0.48);
                    color += vec3(0.18, 0.28, 0.25) * grazing * 0.30;
                    alpha = clamp(alpha * 0.78 + grazing * 0.08, 0.22, 0.62);
                }

                float distanceToCamera = length(uCameraPosition - vWorldPosition);
                float fogAmount = smoothstep(58.0, 190.0, distanceToCamera);
                if (uUnderwater == 1) {
                    fogAmount = max(fogAmount, smoothstep(5.0, 48.0, distanceToCamera));
                    color = mix(color, vec3(0.08, 0.34, 0.38), 0.28);
                }
                color = mix(color, uFogColor, fogAmount * (uUnderwater == 1 ? 0.78 : 0.46));
                fragColor = vec4(color, alpha);
            }
            """;

    private static final String LINE_VERTEX_SHADER = """
            #version 330 core
            layout (location = 0) in vec3 aPosition;

            uniform mat4 uProjection;
            uniform mat4 uView;

            void main() {
                gl_Position = uProjection * uView * vec4(aPosition, 1.0);
            }
            """;

    private static final String LINE_FRAGMENT_SHADER = """
            #version 330 core
            uniform vec4 uColor;
            out vec4 fragColor;

            void main() {
                fragColor = uColor;
            }
            """;

    private final VoxelWorld world;
    private final Map<Long, ChunkMeshSet> chunkMeshes = new LinkedHashMap<>();
    private final Map<Long, MeshBuildJob> pendingMeshBuilds = new HashMap<>();
    private final Queue<MeshBuildResult> completedMeshBuilds = new ConcurrentLinkedQueue<>();

    private ShaderProgram worldShaderProgram;
    private ShaderProgram lineShaderProgram;
    private TextureAtlas textureAtlas;
    private WorldMeshBuilder worldMeshBuilder;
    private DynamicLineMesh3D lineMesh3D;
    private DynamicTexturedTriangleMesh3D crackMesh3D;
    private ExecutorService meshExecutor;

    public WorldRenderer(VoxelWorld world) {
        this.world = Objects.requireNonNull(world, "Voxel world cannot be null.");
    }

    public void initialize() {
        worldShaderProgram = new ShaderProgram(WORLD_VERTEX_SHADER, WORLD_FRAGMENT_SHADER);
        lineShaderProgram = new ShaderProgram(LINE_VERTEX_SHADER, LINE_FRAGMENT_SHADER);
        textureAtlas = new TextureAtlas();
        worldMeshBuilder = new WorldMeshBuilder(textureAtlas);
        lineMesh3D = new DynamicLineMesh3D();
        crackMesh3D = new DynamicTexturedTriangleMesh3D();
        meshExecutor = Executors.newFixedThreadPool(meshThreadCount(), new MeshThreadFactory());
        syncChunkMeshes(null);
    }

    public void render(Matrix4f projectionMatrix, Matrix4f viewMatrix, FirstPersonCamera camera, WorldTime worldTime, HudState hudState) {
        uploadCompletedChunkMeshes();
        if (world.consumeMeshDirty()) {
            syncChunkMeshes(camera);
        }

        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LESS);
        glDisable(GL_BLEND);
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);

        renderChunks(projectionMatrix, viewMatrix, camera, worldTime);
        renderTargetHighlights(projectionMatrix, viewMatrix, hudState);
    }

    private void renderChunks(Matrix4f projectionMatrix, Matrix4f viewMatrix, FirstPersonCamera camera, WorldTime worldTime) {
        EnvironmentPalette palette = EnvironmentPalette.from(worldTime);
        float daylight = worldTime.getDaylight();
        float sunHeight = worldTime.getSunHeight();
        float twilight = 1.0f - saturate(Math.abs(sunHeight) * 3.2f);
        var waterUv = textureAtlas.getUvBounds(TextureAtlas.Tile.WATER);

        worldShaderProgram.bind();
        textureAtlas.bind(0);
        worldShaderProgram.setInt("uAtlas", 0);
        worldShaderProgram.setMatrix4("uProjection", projectionMatrix);
        worldShaderProgram.setMatrix4("uView", viewMatrix);
        worldShaderProgram.setFloat("uTime", (float) (worldTime.getDayProgress() * 1200.0));
        worldShaderProgram.setVector2("uWaterTile", 7.0f, 0.0f);
        worldShaderProgram.setInt("uUseTiledAtlas", 1);
        worldShaderProgram.setVector2("uWaterUvMin", waterUv[0], waterUv[1]);
        worldShaderProgram.setVector2("uWaterUvMax", waterUv[2], waterUv[3]);
        worldShaderProgram.setVector3("uFogColor", palette.fogR(), palette.fogG(), palette.fogB());
        var eye = camera.getEyePosition();
        worldShaderProgram.setVector3("uCameraPosition", eye.x, eye.y, eye.z);
        int eyeBlockX = (int) Math.floor(eye.x);
        int eyeBlockY = (int) Math.floor(eye.y);
        int eyeBlockZ = (int) Math.floor(eye.z);
        worldShaderProgram.setInt("uUnderwater", world.getBlockAtWorld(eyeBlockX, eyeBlockY, eyeBlockZ) == BlockRegistry.WATER ? 1 : 0);
        worldShaderProgram.setVector3(
                "uTint",
                lerp(0.72f, 0.98f, daylight) + twilight * 0.03f,
                lerp(0.76f, 0.98f, daylight) + twilight * 0.01f,
                lerp(0.84f, 1.00f, daylight)
        );
        worldShaderProgram.setFloat("uAmbientLight", palette.ambient());
        worldShaderProgram.setFloat("uSunLight", palette.sunlight());
        for (ChunkMeshSet mesh : chunkMeshes.values()) {
            mesh.renderOpaque();
        }
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_CULL_FACE);
        glDepthMask(false);
        List<Map.Entry<Long, ChunkMeshSet>> transparentMeshes = new ArrayList<>(chunkMeshes.entrySet());
        transparentMeshes.sort((left, right) -> Double.compare(
                distanceSquaredToChunkCenter(right.getKey(), eye.x, eye.z),
                distanceSquaredToChunkCenter(left.getKey(), eye.x, eye.z)
        ));
        for (Map.Entry<Long, ChunkMeshSet> entry : transparentMeshes) {
            entry.getValue().renderTransparent();
        }
        glDepthMask(true);
        glEnable(GL_CULL_FACE);
        glDisable(GL_BLEND);
        worldShaderProgram.unbind();
    }

    private void renderTargetHighlights(Matrix4f projectionMatrix, Matrix4f viewMatrix, HudState hudState) {
        if (hudState.targetBlock().isEmpty()) {
            return;
        }

        renderBreakingCrackOverlay(projectionMatrix, viewMatrix, hudState);

        lineShaderProgram.bind();
        lineShaderProgram.setMatrix4("uProjection", projectionMatrix);
        lineShaderProgram.setMatrix4("uView", viewMatrix);

        var hit = hudState.targetBlock().get();
        lineShaderProgram.setVector4("uColor", 1.0f, 1.0f, 0.2f, 1.0f);
        lineMesh3D.render(buildBlockOutlineVertices(hit.blockX(), hit.blockY(), hit.blockZ(), 1.002f));

        lineShaderProgram.setVector4("uColor", 0.25f, 0.95f, 0.45f, 1.0f);
        lineMesh3D.render(buildBlockOutlineVertices(hit.adjacentX(), hit.adjacentY(), hit.adjacentZ(), 1.001f));
        lineShaderProgram.unbind();
    }

    private void renderBreakingCrackOverlay(Matrix4f projectionMatrix, Matrix4f viewMatrix, HudState hudState) {
        if (hudState.breakingBlock().isEmpty() || hudState.breakingProgress() <= 0.0f) {
            return;
        }

        TextureAtlas.Tile crackTile = crackTileForProgress(hudState.breakingProgress());
        var hit = hudState.breakingBlock().get();

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_CULL_FACE);
        glDepthMask(false);

        worldShaderProgram.bind();
        textureAtlas.bind(0);
        worldShaderProgram.setInt("uAtlas", 0);
        worldShaderProgram.setMatrix4("uProjection", projectionMatrix);
        worldShaderProgram.setMatrix4("uView", viewMatrix);
        worldShaderProgram.setFloat("uTime", 0.0f);
        worldShaderProgram.setVector2("uWaterTile", 7.0f, 0.0f);
        worldShaderProgram.setInt("uUseTiledAtlas", 0);
        worldShaderProgram.setVector3("uTint", 0.05f, 0.05f, 0.05f);
        worldShaderProgram.setFloat("uAmbientLight", 1.0f);
        worldShaderProgram.setFloat("uSunLight", 0.0f);
        crackMesh3D.render(buildCrackOverlayVertices(hit.blockX(), hit.blockY(), hit.blockZ(), textureAtlas.getUvBounds(crackTile)));
        worldShaderProgram.unbind();

        glDepthMask(true);
        glEnable(GL_CULL_FACE);
        glDisable(GL_BLEND);
    }

    private void syncChunkMeshes(FirstPersonCamera camera) {
        if (meshExecutor == null || meshExecutor.isShutdown()) {
            return;
        }

        List<Chunk> chunks = new ArrayList<>(world.getChunks());
        Set<Long> loadedKeys = new HashSet<>(chunks.size());
        for (Chunk chunk : chunks) {
            loadedKeys.add(chunkKey(chunk.getChunkX(), chunk.getChunkZ()));
        }

        Iterator<Map.Entry<Long, ChunkMeshSet>> iterator = chunkMeshes.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, ChunkMeshSet> entry = iterator.next();
            if (!loadedKeys.contains(entry.getKey())) {
                entry.getValue().close();
                iterator.remove();
            }
        }

        Iterator<Map.Entry<Long, MeshBuildJob>> pendingIterator = pendingMeshBuilds.entrySet().iterator();
        while (pendingIterator.hasNext()) {
            Map.Entry<Long, MeshBuildJob> entry = pendingIterator.next();
            if (!loadedKeys.contains(entry.getKey())) {
                entry.getValue().future().cancel(false);
                pendingIterator.remove();
            }
        }

        chunks.sort(Comparator.comparingInt(chunk -> chunkPriority(chunk, camera)));
        MeshWorldSnapshot snapshot = null;

        for (Chunk chunk : chunks) {
            long key = chunkKey(chunk.getChunkX(), chunk.getChunkZ());
            long version = chunk.getMeshVersion();
            boolean needsMesh = !chunkMeshes.containsKey(key) || chunk.isDirty();
            if (!needsMesh) {
                continue;
            }

            MeshBuildJob existingJob = pendingMeshBuilds.get(key);
            if (existingJob != null && existingJob.version() == version) {
                continue;
            }
            if (existingJob != null) {
                existingJob.future().cancel(false);
            }

            if (snapshot == null) {
                snapshot = createMeshSnapshot(chunks);
            }
            scheduleChunkMeshBuild(snapshot, chunk.getChunkX(), chunk.getChunkZ(), key, version);
        }
    }

    private MeshWorldSnapshot createMeshSnapshot(List<Chunk> chunks) {
        Map<Long, ChunkSnapshot> snapshots = new HashMap<>(chunks.size() * 2);
        for (Chunk chunk : chunks) {
            snapshots.put(
                    chunkKey(chunk.getChunkX(), chunk.getChunkZ()),
                    new ChunkSnapshot(chunk.getChunkX(), chunk.getChunkZ(), chunk.copyBlockIds(), chunk.copyWaterLevels(), chunk.copyLightLevels())
            );
        }
        return new MeshWorldSnapshot(snapshots);
    }

    private void scheduleChunkMeshBuild(MeshWorldSnapshot snapshot, int chunkX, int chunkZ, long key, long version) {
        Future<?> future = meshExecutor.submit(() -> {
            try {
                ChunkMeshData meshData = worldMeshBuilder.buildChunk(snapshot, chunkX, chunkZ);
                completedMeshBuilds.add(new MeshBuildResult(key, chunkX, chunkZ, version, meshData, null));
            } catch (RuntimeException exception) {
                completedMeshBuilds.add(new MeshBuildResult(key, chunkX, chunkZ, version, null, exception));
            }
        });
        pendingMeshBuilds.put(key, new MeshBuildJob(version, future));
    }

    private void uploadCompletedChunkMeshes() {
        int uploaded = 0;
        while (uploaded < MAX_MESH_UPLOADS_PER_FRAME) {
            MeshBuildResult result = completedMeshBuilds.poll();
            if (result == null) {
                return;
            }

            MeshBuildJob pendingJob = pendingMeshBuilds.get(result.key());
            if (pendingJob != null && pendingJob.version() == result.version()) {
                pendingMeshBuilds.remove(result.key());
            }
            if (result.failure() != null) {
                result.failure().printStackTrace();
                continue;
            }

            Chunk chunk = world.getChunk(result.chunkX(), result.chunkZ());
            if (chunk == null || chunk.getMeshVersion() != result.version()) {
                continue;
            }

            ChunkMeshSet previousMesh = chunkMeshes.put(result.key(), new ChunkMeshSet(result.meshData()));
            if (previousMesh != null) {
                previousMesh.close();
            }
            chunk.clearDirty();
            uploaded++;
        }
    }

    private int chunkPriority(Chunk chunk, FirstPersonCamera camera) {
        if (camera == null) {
            return 0;
        }
        var eye = camera.getEyePosition();
        int playerChunkX = Math.floorDiv((int) Math.floor(eye.x), Chunk.WIDTH);
        int playerChunkZ = Math.floorDiv((int) Math.floor(eye.z), Chunk.DEPTH);
        int dx = chunk.getChunkX() - playerChunkX;
        int dz = chunk.getChunkZ() - playerChunkZ;
        return dx * dx + dz * dz;
    }

    private int meshThreadCount() {
        return Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
    }

    private long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) ^ (chunkZ & 0xFFFFFFFFL);
    }

    private double distanceSquaredToChunkCenter(long chunkKey, float eyeX, float eyeZ) {
        int chunkX = (int) (chunkKey >> 32);
        int chunkZ = (int) chunkKey;
        double centerX = chunkX * Chunk.WIDTH + Chunk.WIDTH * 0.5;
        double centerZ = chunkZ * Chunk.DEPTH + Chunk.DEPTH * 0.5;
        double dx = centerX - eyeX;
        double dz = centerZ - eyeZ;
        return dx * dx + dz * dz;
    }

    private float[] buildBlockOutlineVertices(int x, int y, int z, float size) {
        float inset = (size - 1.0f) * 0.5f;
        float minX = x - inset;
        float minY = y - inset;
        float minZ = z - inset;
        float maxX = x + 1.0f + inset;
        float maxY = y + 1.0f + inset;
        float maxZ = z + 1.0f + inset;

        return new float[]{
                minX, minY, minZ, maxX, minY, minZ,
                maxX, minY, minZ, maxX, minY, maxZ,
                maxX, minY, maxZ, minX, minY, maxZ,
                minX, minY, maxZ, minX, minY, minZ,

                minX, maxY, minZ, maxX, maxY, minZ,
                maxX, maxY, minZ, maxX, maxY, maxZ,
                maxX, maxY, maxZ, minX, maxY, maxZ,
                minX, maxY, maxZ, minX, maxY, minZ,

                minX, minY, minZ, minX, maxY, minZ,
                maxX, minY, minZ, maxX, maxY, minZ,
                maxX, minY, maxZ, maxX, maxY, maxZ,
                minX, minY, maxZ, minX, maxY, maxZ
        };
    }

    private float[] buildCrackOverlayVertices(int x, int y, int z, float[] uvBounds) {
        float inset = 0.003f;
        float minX = x - inset;
        float minY = y - inset;
        float minZ = z - inset;
        float maxX = x + 1.0f + inset;
        float maxY = y + 1.0f + inset;
        float maxZ = z + 1.0f + inset;
        FloatBuilder builder = new FloatBuilder(6 * 6 * 6);

        appendTexturedFace(builder, uvBounds, minX, maxY, minZ, maxX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ);
        appendTexturedFace(builder, uvBounds, minX, minY, maxZ, maxX, minY, maxZ, minX, minY, minZ, maxX, minY, minZ);
        appendTexturedFace(builder, uvBounds, minX, maxY, maxZ, minX, maxY, minZ, minX, minY, maxZ, minX, minY, minZ);
        appendTexturedFace(builder, uvBounds, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, minZ, maxX, minY, maxZ);
        appendTexturedFace(builder, uvBounds, maxX, maxY, minZ, minX, maxY, minZ, maxX, minY, minZ, minX, minY, minZ);
        appendTexturedFace(builder, uvBounds, minX, maxY, maxZ, maxX, maxY, maxZ, minX, minY, maxZ, maxX, minY, maxZ);

        return builder.toArray();
    }

    private void appendTexturedFace(
            FloatBuilder builder,
            float[] uvBounds,
            float topLeftX,
            float topLeftY,
            float topLeftZ,
            float topRightX,
            float topRightY,
            float topRightZ,
            float bottomLeftX,
            float bottomLeftY,
            float bottomLeftZ,
            float bottomRightX,
            float bottomRightY,
            float bottomRightZ
    ) {
        float u0 = uvBounds[0];
        float v0 = uvBounds[1];
        float u1 = uvBounds[2];
        float v1 = uvBounds[3];
        appendTexturedVertex(builder, topLeftX, topLeftY, topLeftZ, u0, v0);
        appendTexturedVertex(builder, bottomLeftX, bottomLeftY, bottomLeftZ, u0, v1);
        appendTexturedVertex(builder, topRightX, topRightY, topRightZ, u1, v0);
        appendTexturedVertex(builder, bottomLeftX, bottomLeftY, bottomLeftZ, u0, v1);
        appendTexturedVertex(builder, bottomRightX, bottomRightY, bottomRightZ, u1, v1);
        appendTexturedVertex(builder, topRightX, topRightY, topRightZ, u1, v0);
    }

    private void appendTexturedVertex(FloatBuilder builder, float x, float y, float z, float textureU, float textureV) {
        builder.add(x);
        builder.add(y);
        builder.add(z);
        builder.add(textureU);
        builder.add(textureV);
        builder.add(1.0f);
    }

    private TextureAtlas.Tile crackTileForProgress(float progress) {
        float clampedProgress = saturate(progress);
        if (clampedProgress < 0.25f) {
            return TextureAtlas.Tile.CRACK_1;
        }
        if (clampedProgress < 0.50f) {
            return TextureAtlas.Tile.CRACK_2;
        }
        if (clampedProgress < 0.75f) {
            return TextureAtlas.Tile.CRACK_3;
        }
        return TextureAtlas.Tile.CRACK_4;
    }

    private float lerp(float start, float end, float alpha) {
        return start + (end - start) * alpha;
    }

    private float saturate(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    @Override
    public void close() {
        if (meshExecutor != null) {
            meshExecutor.shutdownNow();
            meshExecutor = null;
        }
        pendingMeshBuilds.clear();
        completedMeshBuilds.clear();
        for (ChunkMeshSet mesh : chunkMeshes.values()) {
            mesh.close();
        }
        chunkMeshes.clear();
        if (lineMesh3D != null) {
            lineMesh3D.close();
            lineMesh3D = null;
        }
        if (crackMesh3D != null) {
            crackMesh3D.close();
            crackMesh3D = null;
        }
        if (textureAtlas != null) {
            textureAtlas.close();
            textureAtlas = null;
        }
        if (worldShaderProgram != null) {
            worldShaderProgram.close();
            worldShaderProgram = null;
        }
        if (lineShaderProgram != null) {
            lineShaderProgram.close();
            lineShaderProgram = null;
        }
    }

    private record MeshBuildJob(long version, Future<?> future) {
    }

    private record MeshBuildResult(long key, int chunkX, int chunkZ, long version, ChunkMeshData meshData, RuntimeException failure) {
    }

    private static final class MeshThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "chunk-mesh-builder-" + threadNumber.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }

    private static final class FloatBuilder {
        private float[] values;
        private int size;

        private FloatBuilder(int initialCapacity) {
            values = new float[Math.max(1, initialCapacity)];
        }

        private void add(float value) {
            if (size == values.length) {
                float[] expanded = new float[values.length * 2];
                System.arraycopy(values, 0, expanded, 0, values.length);
                values = expanded;
            }
            values[size++] = value;
        }

        private float[] toArray() {
            float[] output = new float[size];
            System.arraycopy(values, 0, output, 0, size);
            return output;
        }
    }
}
