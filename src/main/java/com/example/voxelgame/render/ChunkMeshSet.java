package com.example.voxelgame.render;

public final class ChunkMeshSet implements AutoCloseable {
    private final WorldMesh opaqueMesh;
    private final WorldMesh transparentMesh;

    public ChunkMeshSet(ChunkMeshData meshData) {
        this.opaqueMesh = new WorldMesh(meshData.opaqueVertices());
        this.transparentMesh = new WorldMesh(meshData.transparentVertices());
    }

    public void renderOpaque() {
        opaqueMesh.render();
    }

    public void renderTransparent() {
        transparentMesh.render();
    }

    @Override
    public void close() {
        opaqueMesh.close();
        transparentMesh.close();
    }
}
