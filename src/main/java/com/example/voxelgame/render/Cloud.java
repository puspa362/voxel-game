package com.example.voxelgame.render;

public record Cloud(
        float x,
        float z,
        float altitude,
        float width,
        float depth,
        float height,
        float density,
        CloudPart[] parts
) {
}

record CloudPart(
        float offsetX,
        float offsetZ,
        float offsetY,
        float halfWidth,
        float halfDepth,
        float height
) {
}
