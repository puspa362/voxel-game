package com.example.voxelgame.world.gen.village;

import java.util.List;

import org.joml.Vector3i;

public record VillageFeature(
        long id,
        int centerX,
        int centerY,
        int centerZ,
        int radius,
        List<Vector3i> beds,
        List<Vector3i> workstations
) {
}
