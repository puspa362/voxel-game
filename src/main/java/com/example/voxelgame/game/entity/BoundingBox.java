package com.example.voxelgame.game.entity;

import org.joml.Vector3f;
import java.util.Optional;

public record BoundingBox(Vector3f min, Vector3f max) {
    public BoundingBox {
        min = new Vector3f(min);
        max = new Vector3f(max);
    }

    public static BoundingBox fromCenter(Vector3f center, Vector3f halfExtents) {
        return new BoundingBox(
                new Vector3f(center.x - halfExtents.x, center.y - halfExtents.y, center.z - halfExtents.z),
                new Vector3f(center.x + halfExtents.x, center.y + halfExtents.y, center.z + halfExtents.z)
        );
    }

    @Override
    public Vector3f min() {
        return new Vector3f(min);
    }

    @Override
    public Vector3f max() {
        return new Vector3f(max);
    }

    public BoundingBox translated(Vector3f offset) {
        return new BoundingBox(new Vector3f(min).add(offset), new Vector3f(max).add(offset));
    }

    public boolean intersects(BoundingBox other) {
        Vector3f otherMin = other.min();
        Vector3f otherMax = other.max();
        return min.x < otherMax.x && max.x > otherMin.x
                && min.y < otherMax.y && max.y > otherMin.y
                && min.z < otherMax.z && max.z > otherMin.z;
    }

    public Optional<Float> rayIntersection(Vector3f origin, Vector3f direction, float maxDistance) {
        Vector3f ray = new Vector3f(direction);
        if (ray.lengthSquared() <= 0.000001f || maxDistance < 0.0f) {
            return Optional.empty();
        }
        ray.normalize();

        float tMin = 0.0f;
        float tMax = maxDistance;
        float[] originValues = {origin.x, origin.y, origin.z};
        float[] directionValues = {ray.x, ray.y, ray.z};
        float[] minValues = {min.x, min.y, min.z};
        float[] maxValues = {max.x, max.y, max.z};

        for (int axis = 0; axis < 3; axis++) {
            float axisOrigin = originValues[axis];
            float axisDirection = directionValues[axis];
            if (Math.abs(axisDirection) < 0.000001f) {
                if (axisOrigin < minValues[axis] || axisOrigin > maxValues[axis]) {
                    return Optional.empty();
                }
                continue;
            }

            float inverse = 1.0f / axisDirection;
            float near = (minValues[axis] - axisOrigin) * inverse;
            float far = (maxValues[axis] - axisOrigin) * inverse;
            if (near > far) {
                float swap = near;
                near = far;
                far = swap;
            }

            tMin = Math.max(tMin, near);
            tMax = Math.min(tMax, far);
            if (tMin > tMax) {
                return Optional.empty();
            }
        }

        return Optional.of(tMin);
    }
}
