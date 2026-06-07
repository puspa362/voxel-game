package com.example.voxelgame.render;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import com.example.voxelgame.world.Block;
import com.example.voxelgame.world.BlockRegistry;
import com.example.voxelgame.world.Chunk;
import com.example.voxelgame.world.VoxelWorld;

public final class WorldMeshBuilder {
    private static final int EMPTY_FACE = -1;
    private static final int STRIDE_FLOATS = 8;
    private static final int[] DIMENSIONS = {Chunk.WIDTH, Chunk.HEIGHT, Chunk.DEPTH};

    private final TextureAtlas atlas;

    public WorldMeshBuilder(TextureAtlas atlas) {
        this.atlas = Objects.requireNonNull(atlas, "Texture atlas cannot be null.");
    }

    public ChunkMeshData buildChunk(VoxelWorld world, Chunk chunk) {
        Objects.requireNonNull(world, "Voxel world cannot be null.");
        Objects.requireNonNull(chunk, "Chunk cannot be null.");
        return buildChunk(new LiveBlockReader(world), chunk.getChunkX(), chunk.getChunkZ());
    }

    public ChunkMeshData buildChunk(MeshWorldSnapshot snapshot, int chunkX, int chunkZ) {
        Objects.requireNonNull(snapshot, "Mesh world snapshot cannot be null.");
        return buildChunk(new SnapshotBlockReader(snapshot), chunkX, chunkZ);
    }

    private ChunkMeshData buildChunk(BlockReader blockReader, int chunkX, int chunkZ) {
        int chunkWorldX = chunkX * Chunk.WIDTH;
        int chunkWorldZ = chunkZ * Chunk.DEPTH;
        FloatList opaqueVertices = new FloatList(Chunk.WIDTH * Chunk.DEPTH * STRIDE_FLOATS);
        FloatList transparentVertices = new FloatList(Chunk.WIDTH * Chunk.DEPTH * STRIDE_FLOATS);

        int maxMaskSize = Math.max(Chunk.WIDTH * Chunk.HEIGHT, Math.max(Chunk.WIDTH * Chunk.DEPTH, Chunk.HEIGHT * Chunk.DEPTH));
        short[] maskBlockIds = new short[maxMaskSize];
        Block[] maskBlocks = new Block[maxMaskSize];
        int[] maskFaceIndexes = new int[maxMaskSize];
        boolean[] maskTransparent = new boolean[maxMaskSize];

        // Greedy meshing scans the chunk three times, once for each face normal
        // axis. For each scan we build a 2D visibility mask for the slice between
        // two neighboring voxel layers, then merge equal mask cells into maximal
        // rectangles before emitting one quad per rectangle.
        for (int axis = 0; axis < 3; axis++) {
            int uAxis = (axis + 1) % 3;
            int vAxis = (axis + 2) % 3;
            int axisSize = DIMENSIONS[axis];
            int uSize = DIMENSIONS[uAxis];
            int vSize = DIMENSIONS[vAxis];

            for (int slice = -1; slice < axisSize; slice++) {
                buildVisibilityMask(
                        blockReader,
                        chunkWorldX,
                        chunkWorldZ,
                        axis,
                        uAxis,
                        vAxis,
                        slice,
                        uSize,
                        vSize,
                        maskBlockIds,
                        maskBlocks,
                        maskFaceIndexes,
                        maskTransparent
                );

                // Walk the mask row by row. When a visible cell is found, expand
                // first along U to find the rectangle width, then along V while
                // every cell in the next row has the same merge key.
                int maskIndex = 0;
                int plane = slice + 1;
                for (int v = 0; v < vSize; v++) {
                    for (int u = 0; u < uSize; ) {
                        int faceIndex = maskFaceIndexes[maskIndex];
                        if (faceIndex == EMPTY_FACE) {
                            u++;
                            maskIndex++;
                            continue;
                        }

                        short blockId = maskBlockIds[maskIndex];
                        Block block = maskBlocks[maskIndex];
                        boolean transparent = maskTransparent[maskIndex];

                        int width = 1;
                        while (u + width < uSize && maskMatches(maskIndex + width, blockId, faceIndex, transparent, maskBlockIds, maskFaceIndexes, maskTransparent)) {
                            width++;
                        }

                        int height = 1;
                        boolean canGrow = true;
                        while (v + height < vSize && canGrow) {
                            int rowStart = maskIndex + height * uSize;
                            for (int x = 0; x < width; x++) {
                                if (!maskMatches(rowStart + x, blockId, faceIndex, transparent, maskBlockIds, maskFaceIndexes, maskTransparent)) {
                                    canGrow = false;
                                    break;
                                }
                            }
                            if (canGrow) {
                                height++;
                            }
                        }

                        // Emit one quad for the merged rectangle. Opaque and
                        // transparent geometry remain in separate primitive
                        // buffers so the renderer can draw them in the same order
                        // as before.
                        FloatList target = transparent ? transparentVertices : opaqueVertices;
                        appendGreedyQuad(target, blockReader, chunkWorldX, chunkWorldZ, axis, uAxis, vAxis, plane, u, v, width, height, block, faceIndex);

                        // Clear the consumed mask cells. The scan can then skip
                        // over this rectangle instead of emitting per-block faces.
                        for (int row = 0; row < height; row++) {
                            Arrays.fill(maskFaceIndexes, maskIndex + row * uSize, maskIndex + row * uSize + width, EMPTY_FACE);
                        }

                        u += width;
                        maskIndex += width;
                    }
                }
            }
        }

        appendWaterGeometry(blockReader, chunkWorldX, chunkWorldZ, transparentVertices);
        appendDecorativeGeometry(blockReader, chunkWorldX, chunkWorldZ, transparentVertices);

        return new ChunkMeshData(opaqueVertices.toArray(), transparentVertices.toArray());
    }

    private void buildVisibilityMask(
            BlockReader blockReader,
            int chunkWorldX,
            int chunkWorldZ,
            int axis,
            int uAxis,
            int vAxis,
            int slice,
            int uSize,
            int vSize,
            short[] maskBlockIds,
            Block[] maskBlocks,
            int[] maskFaceIndexes,
            boolean[] maskTransparent
    ) {
        // Each mask cell represents one potential face between the block at
        // "slice" and the neighboring block at "slice + 1". Empty cells mean no
        // face is visible. Non-empty cells carry the exact merge key: block type,
        // face/texture, and transparency.
        int maskIndex = 0;
        for (int v = 0; v < vSize; v++) {
            for (int u = 0; u < uSize; u++) {
                Block current = blockAtSlice(blockReader, chunkWorldX, chunkWorldZ, axis, uAxis, vAxis, slice, u, v);
                Block next = blockAtSlice(blockReader, chunkWorldX, chunkWorldZ, axis, uAxis, vAxis, slice + 1, u, v);
                Block visibleBlock = null;
                int faceIndex = EMPTY_FACE;

                if (isFaceVisible(current, next)) {
                    visibleBlock = current;
                    faceIndex = positiveFaceIndex(axis);
                } else if (isFaceVisible(next, current)) {
                    visibleBlock = next;
                    faceIndex = negativeFaceIndex(axis);
                }

                if (visibleBlock == null) {
                    maskFaceIndexes[maskIndex] = EMPTY_FACE;
                    maskBlocks[maskIndex] = null;
                    maskTransparent[maskIndex] = false;
                } else {
                    maskBlockIds[maskIndex] = visibleBlock.getId();
                    maskBlocks[maskIndex] = visibleBlock;
                    maskFaceIndexes[maskIndex] = faceIndex;
                    maskTransparent[maskIndex] = visibleBlock.isTransparent();
                }
                maskIndex++;
            }
        }
    }

    private Block blockAtSlice(BlockReader blockReader, int chunkWorldX, int chunkWorldZ, int axis, int uAxis, int vAxis, int axisCoordinate, int u, int v) {
        if (axisCoordinate < 0 || axisCoordinate >= DIMENSIONS[axis]) {
            return BlockRegistry.AIR;
        }

        int x = coordinateFor(0, axis, axisCoordinate, uAxis, u, vAxis, v);
        int y = coordinateFor(1, axis, axisCoordinate, uAxis, u, vAxis, v);
        int z = coordinateFor(2, axis, axisCoordinate, uAxis, u, vAxis, v);
        return blockReader.getBlockAtWorld(chunkWorldX + x, y, chunkWorldZ + z);
    }

    private boolean maskMatches(
            int index,
            short blockId,
            int faceIndex,
            boolean transparent,
            short[] maskBlockIds,
            int[] maskFaceIndexes,
            boolean[] maskTransparent
    ) {
        return maskFaceIndexes[index] == faceIndex
                && maskBlockIds[index] == blockId
                && maskTransparent[index] == transparent;
    }

    private boolean isFaceVisible(Block current, Block next) {
        if (!current.isRenderable()) {
            return false;
        }
        if (current.isFluid()) {
            return false;
        }
        if (current.getId() == next.getId()) {
            return false;
        }
        return current.isSolid() && (!next.isSolid() || next.isTransparent());
    }

    private void appendWaterGeometry(BlockReader blockReader, int chunkWorldX, int chunkWorldZ, FloatList vertices) {
        TextureAtlas.Tile waterTile = TextureAtlas.Tile.WATER;
        for (int y = 0; y < Chunk.HEIGHT; y++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                for (int x = 0; x < Chunk.WIDTH; x++) {
                    int worldX = chunkWorldX + x;
                    int worldZ = chunkWorldZ + z;
                    if (blockReader.getBlockAtWorld(worldX, y, worldZ) != BlockRegistry.WATER) {
                        continue;
                    }

                    float topY = waterSurfaceY(blockReader, worldX, y, worldZ);
                    float minX = worldX;
                    float maxX = worldX + 1.0f;
                    float minZ = worldZ;
                    float maxZ = worldZ + 1.0f;
                    float light = lightFor(0) * Math.max(0.28f, blockReader.getLightLevelAtWorld(worldX, y, worldZ) / 15.0f);

                    // Top surfaces are emitted only where water meets air or a
                    // non-water transparent block, which prevents hidden internal
                    // overdraw while preserving a connected surface across chunks.
                    Block above = blockReader.getBlockAtWorld(worldX, y + 1, worldZ);
                    if (above != BlockRegistry.WATER && (!above.isRenderable() || above.isTransparent())) {
                        appendWaterFace(vertices, waterTile, light, 1.0f, 1.0f,
                                minX, topY, minZ,
                                maxX, topY, minZ,
                                minX, topY, maxZ,
                                maxX, topY, maxZ);
                    }

                    appendWaterSide(blockReader, vertices, waterTile, lightFor(3), worldX, y, worldZ, 1, 0,
                            maxX, topY, minZ, maxX, topY, maxZ);
                    appendWaterSide(blockReader, vertices, waterTile, lightFor(2), worldX, y, worldZ, -1, 0,
                            minX, topY, maxZ, minX, topY, minZ);
                    appendWaterSide(blockReader, vertices, waterTile, lightFor(5), worldX, y, worldZ, 0, 1,
                            minX, topY, maxZ, maxX, topY, maxZ);
                    appendWaterSide(blockReader, vertices, waterTile, lightFor(4), worldX, y, worldZ, 0, -1,
                            maxX, topY, minZ, minX, topY, minZ);
                }
            }
        }
    }

    private void appendWaterSide(
            BlockReader blockReader,
            FloatList vertices,
            TextureAtlas.Tile tile,
            float faceShade,
            int worldX,
            int y,
            int worldZ,
            int offsetX,
            int offsetZ,
            float topLeftX,
            float topLeftY,
            float topLeftZ,
            float topRightX,
            float topRightY,
            float topRightZ
    ) {
        float bottomY = visibleWaterSideBottom(blockReader, worldX, y, worldZ, offsetX, offsetZ, topLeftY);
        if (Float.isNaN(bottomY) || bottomY >= topLeftY - 0.001f) {
            return;
        }

        float light = faceShade * Math.max(0.28f, blockReader.getLightLevelAtWorld(worldX, y, worldZ) / 15.0f);
        appendWaterFace(vertices, tile, light, 1.0f, topLeftY - bottomY,
                topLeftX, topLeftY, topLeftZ,
                topRightX, topRightY, topRightZ,
                topLeftX, bottomY, topLeftZ,
                topRightX, bottomY, topRightZ);
    }

    private float visibleWaterSideBottom(BlockReader blockReader, int worldX, int y, int worldZ, int offsetX, int offsetZ, float topY) {
        int neighborX = worldX + offsetX;
        int neighborZ = worldZ + offsetZ;
        Block neighbor = blockReader.getBlockAtWorld(neighborX, y, neighborZ);
        if (neighbor == BlockRegistry.WATER) {
            float neighborTopY = waterSurfaceY(blockReader, neighborX, y, neighborZ);
            return neighborTopY < topY - 0.001f ? neighborTopY : Float.NaN;
        }
        if (!neighbor.isRenderable() || neighbor.isTransparent()) {
            return y;
        }
        return Float.NaN;
    }

    private float waterSurfaceY(BlockReader blockReader, int worldX, int y, int worldZ) {
        if (blockReader.getBlockAtWorld(worldX, y + 1, worldZ) == BlockRegistry.WATER) {
            return y + 1.0f;
        }
        int level = blockReader.getWaterLevelAtWorld(worldX, y, worldZ);
        return y + VoxelWorld.waterHeightForLevel(level < 0 ? 0 : level);
    }

    private void appendWaterFace(
            FloatList vertices,
            TextureAtlas.Tile tile,
            float light,
            float repeatU,
            float repeatV,
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
        appendWaterVertex(vertices, topLeftX, topLeftY, topLeftZ, 0.0f, 0.0f, light, tile);
        appendWaterVertex(vertices, bottomLeftX, bottomLeftY, bottomLeftZ, 0.0f, repeatV, light, tile);
        appendWaterVertex(vertices, topRightX, topRightY, topRightZ, repeatU, 0.0f, light, tile);
        appendWaterVertex(vertices, bottomLeftX, bottomLeftY, bottomLeftZ, 0.0f, repeatV, light, tile);
        appendWaterVertex(vertices, bottomRightX, bottomRightY, bottomRightZ, repeatU, repeatV, light, tile);
        appendWaterVertex(vertices, topRightX, topRightY, topRightZ, repeatU, 0.0f, light, tile);
    }

    private void appendWaterVertex(FloatList vertices, float x, float y, float z, float localU, float localV, float light, TextureAtlas.Tile tile) {
        vertices.add(x);
        vertices.add(y);
        vertices.add(z);
        vertices.add(localU);
        vertices.add(localV);
        vertices.add(light);
        vertices.add(tile.tileX());
        vertices.add(tile.tileY());
    }

    private void appendDecorativeGeometry(BlockReader blockReader, int chunkWorldX, int chunkWorldZ, FloatList vertices) {
        for (int y = 0; y < Chunk.HEIGHT; y++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                for (int x = 0; x < Chunk.WIDTH; x++) {
                    int worldX = chunkWorldX + x;
                    int worldZ = chunkWorldZ + z;
                    Block block = blockReader.getBlockAtWorld(worldX, y, worldZ);
                    if (!BlockRegistry.usesCustomMesh(block)) {
                        continue;
                    }

                    float light = Math.max(0.28f, blockReader.getLightLevelAtWorld(worldX, y, worldZ) / 15.0f);
                    if (BlockRegistry.isDecorativePlant(block)) {
                        appendPlant(vertices, block, worldX, y, worldZ, light);
                    } else if (BlockRegistry.isStairs(block)) {
                        appendStairs(vertices, block, worldX, y, worldZ, light);
                    } else if (BlockRegistry.isFence(block)) {
                        appendFence(vertices, blockReader, worldX, y, worldZ, light);
                    } else if (BlockRegistry.isFenceGate(block)) {
                        appendFenceGate(vertices, block, worldX, y, worldZ, light);
                    } else if (BlockRegistry.isDoor(block)) {
                        appendDoor(vertices, block, worldX, y, worldZ, light);
                    } else if (block == BlockRegistry.TORCH) {
                        appendBox(vertices, TextureAtlas.Tile.WOOD_LOG, worldX + 0.42f, y, worldZ + 0.42f, worldX + 0.58f, y + 0.72f, worldZ + 0.58f, light);
                    } else if (block == BlockRegistry.LAMP) {
                        appendBox(vertices, TextureAtlas.Tile.LAMP, worldX + 0.16f, y + 0.05f, worldZ + 0.16f, worldX + 0.84f, y + 0.88f, worldZ + 0.84f, light);
                    } else if (block == BlockRegistry.LANTERN) {
                        appendBox(vertices, TextureAtlas.Tile.LANTERN, worldX + 0.28f, y + 0.10f, worldZ + 0.28f, worldX + 0.72f, y + 0.78f, worldZ + 0.72f, light);
                    } else if (block == BlockRegistry.BED) {
                        appendBox(vertices, TextureAtlas.Tile.BED, worldX, y, worldZ, worldX + 1.0f, y + 0.36f, worldZ + 1.0f, light);
                    } else if (block == BlockRegistry.BELL) {
                        appendBox(vertices, TextureAtlas.Tile.STONE, worldX + 0.20f, y + 0.78f, worldZ + 0.20f, worldX + 0.80f, y + 0.94f, worldZ + 0.80f, light);
                        appendBox(vertices, TextureAtlas.Tile.LANTERN, worldX + 0.30f, y + 0.22f, worldZ + 0.30f, worldX + 0.70f, y + 0.78f, worldZ + 0.70f, light);
                    }
                }
            }
        }
    }

    private void appendPlant(FloatList vertices, Block block, int x, int y, int z, float light) {
        TextureAtlas.Tile tile = atlas.tileFor(block, 0);
        float height = block == BlockRegistry.SHORT_GRASS ? 0.42f : (block == BlockRegistry.TALL_GRASS ? 0.82f : 0.92f);
        float minY = y;
        float maxY = y + height;
        appendDoubleSidedQuad(vertices, tile, light,
                x + 0.08f, maxY, z + 0.08f,
                x + 0.92f, maxY, z + 0.92f,
                x + 0.08f, minY, z + 0.08f,
                x + 0.92f, minY, z + 0.92f);
        appendDoubleSidedQuad(vertices, tile, light,
                x + 0.92f, maxY, z + 0.08f,
                x + 0.08f, maxY, z + 0.92f,
                x + 0.92f, minY, z + 0.08f,
                x + 0.08f, minY, z + 0.92f);
    }

    private void appendStairs(FloatList vertices, Block block, int x, int y, int z, float light) {
        TextureAtlas.Tile tile = atlas.tileFor(block, 0);
        appendBox(vertices, tile, x, y, z, x + 1.0f, y + 0.5f, z + 1.0f, light);
        Direction direction = directionFor(block);
        switch (direction) {
            case NORTH -> appendBox(vertices, tile, x, y + 0.5f, z, x + 1.0f, y + 1.0f, z + 0.5f, light);
            case SOUTH -> appendBox(vertices, tile, x, y + 0.5f, z + 0.5f, x + 1.0f, y + 1.0f, z + 1.0f, light);
            case EAST -> appendBox(vertices, tile, x + 0.5f, y + 0.5f, z, x + 1.0f, y + 1.0f, z + 1.0f, light);
            case WEST -> appendBox(vertices, tile, x, y + 0.5f, z, x + 0.5f, y + 1.0f, z + 1.0f, light);
        }
    }

    private void appendFence(FloatList vertices, BlockReader blockReader, int x, int y, int z, float light) {
        TextureAtlas.Tile tile = TextureAtlas.Tile.WOOD_PLANK;
        appendBox(vertices, tile, x + 0.38f, y, z + 0.38f, x + 0.62f, y + 1.18f, z + 0.62f, light);
        if (connectsFence(blockReader, x, y, z - 1)) {
            appendBox(vertices, tile, x + 0.43f, y + 0.36f, z, x + 0.57f, y + 0.52f, z + 0.50f, light);
            appendBox(vertices, tile, x + 0.43f, y + 0.78f, z, x + 0.57f, y + 0.94f, z + 0.50f, light);
        }
        if (connectsFence(blockReader, x, y, z + 1)) {
            appendBox(vertices, tile, x + 0.43f, y + 0.36f, z + 0.50f, x + 0.57f, y + 0.52f, z + 1.0f, light);
            appendBox(vertices, tile, x + 0.43f, y + 0.78f, z + 0.50f, x + 0.57f, y + 0.94f, z + 1.0f, light);
        }
        if (connectsFence(blockReader, x - 1, y, z)) {
            appendBox(vertices, tile, x, y + 0.36f, z + 0.43f, x + 0.50f, y + 0.52f, z + 0.57f, light);
            appendBox(vertices, tile, x, y + 0.78f, z + 0.43f, x + 0.50f, y + 0.94f, z + 0.57f, light);
        }
        if (connectsFence(blockReader, x + 1, y, z)) {
            appendBox(vertices, tile, x + 0.50f, y + 0.36f, z + 0.43f, x + 1.0f, y + 0.52f, z + 0.57f, light);
            appendBox(vertices, tile, x + 0.50f, y + 0.78f, z + 0.43f, x + 1.0f, y + 0.94f, z + 0.57f, light);
        }
    }

    private boolean connectsFence(BlockReader blockReader, int x, int y, int z) {
        Block block = blockReader.getBlockAtWorld(x, y, z);
        return BlockRegistry.isFence(block) || BlockRegistry.isFenceGate(block) || (block.isSolid() && block.isCollidable());
    }

    private void appendFenceGate(FloatList vertices, Block block, int x, int y, int z, float light) {
        TextureAtlas.Tile tile = TextureAtlas.Tile.WOOD_PLANK;
        boolean eastWest = block == BlockRegistry.OAK_FENCE_GATE_CLOSED_EAST || block == BlockRegistry.OAK_FENCE_GATE_OPEN_EAST;
        boolean open = block == BlockRegistry.OAK_FENCE_GATE_OPEN_NORTH || block == BlockRegistry.OAK_FENCE_GATE_OPEN_EAST;
        if (eastWest ^ open) {
            appendBox(vertices, tile, x, y + 0.30f, z + 0.42f, x + 1.0f, y + 0.92f, z + 0.58f, light);
        } else {
            appendBox(vertices, tile, x + 0.42f, y + 0.30f, z, x + 0.58f, y + 0.92f, z + 1.0f, light);
        }
    }

    private void appendDoor(FloatList vertices, Block block, int x, int y, int z, float light) {
        TextureAtlas.Tile tile = TextureAtlas.Tile.WOOD_PLANK;
        boolean eastWest = block == BlockRegistry.OAK_DOOR_LOWER_CLOSED_EAST || block == BlockRegistry.OAK_DOOR_UPPER_CLOSED_EAST
                || block == BlockRegistry.OAK_DOOR_LOWER_OPEN_EAST || block == BlockRegistry.OAK_DOOR_UPPER_OPEN_EAST;
        boolean open = block == BlockRegistry.OAK_DOOR_LOWER_OPEN_NORTH || block == BlockRegistry.OAK_DOOR_UPPER_OPEN_NORTH
                || block == BlockRegistry.OAK_DOOR_LOWER_OPEN_EAST || block == BlockRegistry.OAK_DOOR_UPPER_OPEN_EAST;
        if (eastWest ^ open) {
            appendBox(vertices, tile, x, y, z + 0.44f, x + 1.0f, y + 1.0f, z + 0.56f, light);
        } else {
            appendBox(vertices, tile, x + 0.44f, y, z, x + 0.56f, y + 1.0f, z + 1.0f, light);
        }
    }

    private Direction directionFor(Block block) {
        if (block == BlockRegistry.OAK_STAIRS_EAST || block == BlockRegistry.STONE_STAIRS_EAST) {
            return Direction.EAST;
        }
        if (block == BlockRegistry.OAK_STAIRS_SOUTH || block == BlockRegistry.STONE_STAIRS_SOUTH) {
            return Direction.SOUTH;
        }
        if (block == BlockRegistry.OAK_STAIRS_WEST || block == BlockRegistry.STONE_STAIRS_WEST) {
            return Direction.WEST;
        }
        return Direction.NORTH;
    }

    private void appendBox(FloatList vertices, TextureAtlas.Tile tile, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, float light) {
        appendQuad(vertices, tile, lightFor(0) * light, minX, maxY, minZ, maxX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ);
        appendQuad(vertices, tile, lightFor(1) * light, minX, minY, maxZ, maxX, minY, maxZ, minX, minY, minZ, maxX, minY, minZ);
        appendQuad(vertices, tile, lightFor(2) * light, minX, maxY, maxZ, minX, maxY, minZ, minX, minY, maxZ, minX, minY, minZ);
        appendQuad(vertices, tile, lightFor(3) * light, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, minZ, maxX, minY, maxZ);
        appendQuad(vertices, tile, lightFor(4) * light, maxX, maxY, minZ, minX, maxY, minZ, maxX, minY, minZ, minX, minY, minZ);
        appendQuad(vertices, tile, lightFor(5) * light, minX, maxY, maxZ, maxX, maxY, maxZ, minX, minY, maxZ, maxX, minY, maxZ);
    }

    private void appendDoubleSidedQuad(FloatList vertices, TextureAtlas.Tile tile, float light,
                                       float topLeftX, float topLeftY, float topLeftZ,
                                       float topRightX, float topRightY, float topRightZ,
                                       float bottomLeftX, float bottomLeftY, float bottomLeftZ,
                                       float bottomRightX, float bottomRightY, float bottomRightZ) {
        appendQuad(vertices, tile, light, topLeftX, topLeftY, topLeftZ, topRightX, topRightY, topRightZ,
                bottomLeftX, bottomLeftY, bottomLeftZ, bottomRightX, bottomRightY, bottomRightZ);
        appendQuad(vertices, tile, light, topRightX, topRightY, topRightZ, topLeftX, topLeftY, topLeftZ,
                bottomRightX, bottomRightY, bottomRightZ, bottomLeftX, bottomLeftY, bottomLeftZ);
    }

    private void appendQuad(FloatList vertices, TextureAtlas.Tile tile, float light,
                            float topLeftX, float topLeftY, float topLeftZ,
                            float topRightX, float topRightY, float topRightZ,
                            float bottomLeftX, float bottomLeftY, float bottomLeftZ,
                            float bottomRightX, float bottomRightY, float bottomRightZ) {
        appendRawVertex(vertices, topLeftX, topLeftY, topLeftZ, 0.0f, 0.0f, light, tile);
        appendRawVertex(vertices, bottomLeftX, bottomLeftY, bottomLeftZ, 0.0f, 1.0f, light, tile);
        appendRawVertex(vertices, topRightX, topRightY, topRightZ, 1.0f, 0.0f, light, tile);
        appendRawVertex(vertices, bottomLeftX, bottomLeftY, bottomLeftZ, 0.0f, 1.0f, light, tile);
        appendRawVertex(vertices, bottomRightX, bottomRightY, bottomRightZ, 1.0f, 1.0f, light, tile);
        appendRawVertex(vertices, topRightX, topRightY, topRightZ, 1.0f, 0.0f, light, tile);
    }

    private void appendRawVertex(FloatList vertices, float x, float y, float z, float localU, float localV, float light, TextureAtlas.Tile tile) {
        vertices.add(x);
        vertices.add(y);
        vertices.add(z);
        vertices.add(localU);
        vertices.add(localV);
        vertices.add(light);
        vertices.add(tile.tileX());
        vertices.add(tile.tileY());
    }

    private void appendGreedyQuad(
            FloatList vertices,
            BlockReader blockReader,
            int chunkWorldX,
            int chunkWorldZ,
            int axis,
            int uAxis,
            int vAxis,
            int plane,
            int u,
            int v,
            int width,
            int height,
            Block block,
            int faceIndex
    ) {
        TextureAtlas.Tile tile = atlas.tileFor(block, faceIndex);
        float light = lightFor(faceIndex) * calculateQuadLight(blockReader, chunkWorldX, chunkWorldZ, axis, uAxis, vAxis, plane, u, v, width, height);

        int p0u = u;
        int p0v = v;
        int p1u = u + width;
        int p1v = v;
        int p2u = u + width;
        int p2v = v + height;
        int p3u = u;
        int p3v = v + height;

        // The four logical quad corners always use the same atlas UV layout:
        // top-left, top-right, bottom-left, bottom-right. Positive and negative
        // faces only choose different geometric corners so winding remains valid
        // with GL_CULL_FACE enabled.
        if (isNegativeFace(faceIndex)) {
            appendQuadTriangles(
                    vertices,
                    chunkWorldX,
                    chunkWorldZ,
                    axis,
                    uAxis,
                    vAxis,
                    plane,
                    p0u,
                    p0v,
                    p1u,
                    p1v,
                    p3u,
                    p3v,
                    p2u,
                    p2v,
                    tile,
                    u,
                    v,
                    light
            );
        } else {
            appendQuadTriangles(
                    vertices,
                    chunkWorldX,
                    chunkWorldZ,
                    axis,
                    uAxis,
                    vAxis,
                    plane,
                    p0u,
                    p0v,
                    p3u,
                    p3v,
                    p1u,
                    p1v,
                    p2u,
                    p2v,
                    tile,
                    u,
                    v,
                    light
            );
        }
    }

    private float calculateQuadLight(
            BlockReader blockReader,
            int chunkWorldX,
            int chunkWorldZ,
            int axis,
            int uAxis,
            int vAxis,
            int plane,
            int u,
            int v,
            int width,
            int height
    ) {
        int centerU = u + width / 2;
        int centerV = v + height / 2;
        int x = coordinateFor(0, axis, plane, uAxis, centerU, vAxis, centerV);
        int y = coordinateFor(1, axis, plane, uAxis, centerU, vAxis, centerV);
        int z = coordinateFor(2, axis, plane, uAxis, centerU, vAxis, centerV);
        int light = blockReader.getLightLevelAtWorld(chunkWorldX + x, y, chunkWorldZ + z);
        return light / 15.0f; // Normalize to 0-1
    }

    private void appendQuadTriangles(
            FloatList vertices,
            int chunkWorldX,
            int chunkWorldZ,
            int axis,
            int uAxis,
            int vAxis,
            int plane,
            int topLeftU,
            int topLeftV,
            int topRightU,
            int topRightV,
            int bottomLeftU,
            int bottomLeftV,
            int bottomRightU,
            int bottomRightV,
            TextureAtlas.Tile tile,
            int originU,
            int originV,
            float light
    ) {
        // Triangle order is fixed for every merged quad:
        // 1. top-left, bottom-left, top-right
        // 2. bottom-left, bottom-right, top-right
        // Both triangles share the same four UV corners; no per-triangle UV
        // reordering is performed.
        int maxU = Math.max(Math.max(topLeftU, topRightU), Math.max(bottomLeftU, bottomRightU));
        int maxV = Math.max(Math.max(topLeftV, topRightV), Math.max(bottomLeftV, bottomRightV));

        boolean flipSideV = axis != 1 && tile == TextureAtlas.Tile.GRASS_SIDE;
        float[] topLeftUv = localUvForFace(axis, uAxis, vAxis, topLeftU, topLeftV, originU, originV, maxU, maxV, flipSideV);
        float[] bottomLeftUv = localUvForFace(axis, uAxis, vAxis, bottomLeftU, bottomLeftV, originU, originV, maxU, maxV, flipSideV);
        float[] topRightUv = localUvForFace(axis, uAxis, vAxis, topRightU, topRightV, originU, originV, maxU, maxV, flipSideV);
        float[] bottomRightUv = localUvForFace(axis, uAxis, vAxis, bottomRightU, bottomRightV, originU, originV, maxU, maxV, flipSideV);

        appendVertex(vertices, chunkWorldX, chunkWorldZ, axis, uAxis, vAxis, plane, topLeftU, topLeftV, topLeftUv[0], topLeftUv[1], light, tile);
        appendVertex(vertices, chunkWorldX, chunkWorldZ, axis, uAxis, vAxis, plane, bottomLeftU, bottomLeftV, bottomLeftUv[0], bottomLeftUv[1], light, tile);
        appendVertex(vertices, chunkWorldX, chunkWorldZ, axis, uAxis, vAxis, plane, topRightU, topRightV, topRightUv[0], topRightUv[1], light, tile);

        appendVertex(vertices, chunkWorldX, chunkWorldZ, axis, uAxis, vAxis, plane, bottomLeftU, bottomLeftV, bottomLeftUv[0], bottomLeftUv[1], light, tile);
        appendVertex(vertices, chunkWorldX, chunkWorldZ, axis, uAxis, vAxis, plane, bottomRightU, bottomRightV, bottomRightUv[0], bottomRightUv[1], light, tile);
        appendVertex(vertices, chunkWorldX, chunkWorldZ, axis, uAxis, vAxis, plane, topRightU, topRightV, topRightUv[0], topRightUv[1], light, tile);
    }

    private float[] localUvForFace(int axis, int uAxis, int vAxis, int u, int v, int originU, int originV, int maxU, int maxV, boolean flipSideV) {
        if (axis == 1) {
            return new float[]{u - originU, v - originV};
        }
        if (uAxis == 1) {
            float localV = flipSideV ? u - originU : maxU - u;
            return new float[]{v - originV, localV};
        }
        if (vAxis == 1) {
            float localV = flipSideV ? v - originV : maxV - v;
            return new float[]{u - originU, localV};
        }
        return new float[]{u - originU, v - originV};
    }

    private void appendVertex(
            FloatList vertices,
            int chunkWorldX,
            int chunkWorldZ,
            int axis,
            int uAxis,
            int vAxis,
            int plane,
            int u,
            int v,
            float localU,
            float localV,
            float light,
            TextureAtlas.Tile tile
    ) {
        int x = coordinateFor(0, axis, plane, uAxis, u, vAxis, v);
        int y = coordinateFor(1, axis, plane, uAxis, u, vAxis, v);
        int z = coordinateFor(2, axis, plane, uAxis, u, vAxis, v);

        vertices.add(chunkWorldX + x);
        vertices.add(y);
        vertices.add(chunkWorldZ + z);
        vertices.add(localU);
        vertices.add(localV);
        vertices.add(light);
        vertices.add(tile.tileX());
        vertices.add(tile.tileY());
    }

    private int coordinateFor(int component, int axis, int axisCoordinate, int uAxis, int u, int vAxis, int v) {
        if (component == axis) {
            return axisCoordinate;
        }
        if (component == uAxis) {
            return u;
        }
        if (component == vAxis) {
            return v;
        }
        throw new IllegalArgumentException("Invalid coordinate component: " + component);
    }

    private int positiveFaceIndex(int axis) {
        return switch (axis) {
            case 0 -> 3;
            case 1 -> 0;
            case 2 -> 5;
            default -> throw new IllegalArgumentException("Unsupported axis: " + axis);
        };
    }

    private int negativeFaceIndex(int axis) {
        return switch (axis) {
            case 0 -> 2;
            case 1 -> 1;
            case 2 -> 4;
            default -> throw new IllegalArgumentException("Unsupported axis: " + axis);
        };
    }

    private boolean isNegativeFace(int faceIndex) {
        return faceIndex == 1 || faceIndex == 2 || faceIndex == 4;
    }

    private float lightFor(int faceIndex) {
        return switch (faceIndex) {
            case 0 -> 1.00f;
            case 1 -> 0.55f;
            case 2, 3 -> 0.78f;
            default -> 0.88f;
        };
    }

    private static final class FloatList {
        private float[] values;
        private int size;

        private FloatList(int initialCapacity) {
            values = new float[Math.max(1, initialCapacity)];
        }

        private void add(float value) {
            if (size == values.length) {
                values = Arrays.copyOf(values, values.length * 2);
            }
            values[size++] = value;
        }

        private float[] toArray() {
            return Arrays.copyOf(values, size);
        }
    }

    public record MeshWorldSnapshot(Map<Long, ChunkSnapshot> chunks) {
        public MeshWorldSnapshot {
            chunks = Map.copyOf(Objects.requireNonNull(chunks, "Snapshot chunks cannot be null."));
        }
    }

    public record ChunkSnapshot(int chunkX, int chunkZ, short[] blockIds, byte[] waterLevels, byte[] lightLevels) {
        public ChunkSnapshot {
            blockIds = Objects.requireNonNull(blockIds, "Snapshot block ids cannot be null.").clone();
            waterLevels = Objects.requireNonNull(waterLevels, "Snapshot water levels cannot be null.").clone();
            lightLevels = Objects.requireNonNull(lightLevels, "Snapshot light levels cannot be null.").clone();
            if (blockIds.length != Chunk.VOLUME || waterLevels.length != Chunk.VOLUME || lightLevels.length != Chunk.VOLUME) {
                throw new IllegalArgumentException("Chunk snapshot arrays must match chunk volume.");
            }
        }

        private Block getBlock(int x, int y, int z) {
            return BlockRegistry.get(blockIds[x + (z * Chunk.WIDTH) + (y * Chunk.WIDTH * Chunk.DEPTH)]);
        }

        private int getLightLevel(int x, int y, int z) {
            return Byte.toUnsignedInt(lightLevels[x + (z * Chunk.WIDTH) + (y * Chunk.WIDTH * Chunk.DEPTH)]);
        }

        private int getWaterLevel(int x, int y, int z) {
            int index = x + (z * Chunk.WIDTH) + (y * Chunk.WIDTH * Chunk.DEPTH);
            if (blockIds[index] != BlockRegistry.WATER.getId()) {
                return -1;
            }
            return Byte.toUnsignedInt(waterLevels[index]);
        }
    }

    private interface BlockReader {
        Block getBlockAtWorld(int worldX, int worldY, int worldZ);
        int getWaterLevelAtWorld(int worldX, int worldY, int worldZ);
        int getLightLevelAtWorld(int worldX, int worldY, int worldZ);
    }

    private record LiveBlockReader(VoxelWorld world) implements BlockReader {
        private LiveBlockReader {
            Objects.requireNonNull(world, "Voxel world cannot be null.");
        }

        @Override
        public Block getBlockAtWorld(int worldX, int worldY, int worldZ) {
            return world.getBlockAtWorld(worldX, worldY, worldZ);
        }

        @Override
        public int getLightLevelAtWorld(int worldX, int worldY, int worldZ) {
            return world.getLightLevelAtWorld(worldX, worldY, worldZ);
        }

        @Override
        public int getWaterLevelAtWorld(int worldX, int worldY, int worldZ) {
            return world.getWaterLevelAtWorld(worldX, worldY, worldZ);
        }
    }

    private record SnapshotBlockReader(MeshWorldSnapshot snapshot) implements BlockReader {
        private SnapshotBlockReader {
            Objects.requireNonNull(snapshot, "Mesh world snapshot cannot be null.");
        }

        @Override
        public Block getBlockAtWorld(int worldX, int worldY, int worldZ) {
            if (worldY < 0 || worldY >= Chunk.HEIGHT) {
                return BlockRegistry.AIR;
            }

            int chunkX = Math.floorDiv(worldX, Chunk.WIDTH);
            int chunkZ = Math.floorDiv(worldZ, Chunk.DEPTH);
            ChunkSnapshot chunk = snapshot.chunks().get(chunkKey(chunkX, chunkZ));
            if (chunk == null) {
                return BlockRegistry.AIR;
            }

            int localX = Math.floorMod(worldX, Chunk.WIDTH);
            int localZ = Math.floorMod(worldZ, Chunk.DEPTH);
            return chunk.getBlock(localX, worldY, localZ);
        }

        @Override
        public int getLightLevelAtWorld(int worldX, int worldY, int worldZ) {
            if (worldY < 0 || worldY >= Chunk.HEIGHT) {
                return 0;
            }

            int chunkX = Math.floorDiv(worldX, Chunk.WIDTH);
            int chunkZ = Math.floorDiv(worldZ, Chunk.DEPTH);
            ChunkSnapshot chunk = snapshot.chunks().get(chunkKey(chunkX, chunkZ));
            if (chunk == null) {
                return 0;
            }

            int localX = Math.floorMod(worldX, Chunk.WIDTH);
            int localZ = Math.floorMod(worldZ, Chunk.DEPTH);
            return chunk.getLightLevel(localX, worldY, localZ);
        }

        @Override
        public int getWaterLevelAtWorld(int worldX, int worldY, int worldZ) {
            if (worldY < 0 || worldY >= Chunk.HEIGHT) {
                return -1;
            }

            int chunkX = Math.floorDiv(worldX, Chunk.WIDTH);
            int chunkZ = Math.floorDiv(worldZ, Chunk.DEPTH);
            ChunkSnapshot chunk = snapshot.chunks().get(chunkKey(chunkX, chunkZ));
            if (chunk == null) {
                return -1;
            }

            int localX = Math.floorMod(worldX, Chunk.WIDTH);
            int localZ = Math.floorMod(worldZ, Chunk.DEPTH);
            return chunk.getWaterLevel(localX, worldY, localZ);
        }
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) ^ (chunkZ & 0xFFFFFFFFL);
    }

    private enum Direction {
        NORTH,
        EAST,
        SOUTH,
        WEST
    }
}
