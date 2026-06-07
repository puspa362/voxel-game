package com.example.voxelgame.game.entity.animal;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joml.Vector3f;

import com.example.voxelgame.world.VoxelWorld;

public final class GroundPathfinder {
    private static final int MAX_SEARCH_RADIUS = 10;
    private static final int MAX_EXPANSIONS = 120;
    private static final int[][] CARDINALS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    public List<Vector3f> findPath(VoxelWorld world, Vector3f start, Vector3f target) {
        int startX = (int) Math.floor(start.x);
        int startZ = (int) Math.floor(start.z);
        int targetX = (int) Math.floor(target.x);
        int targetZ = (int) Math.floor(target.z);
        int baseY = Math.max(1, (int) Math.floor(start.y));

        PathNode startNode = new PathNode(startX, startZ);
        PathNode targetNode = new PathNode(targetX, targetZ);
        ArrayDeque<PathNode> queue = new ArrayDeque<>();
        Map<PathNode, PathNode> parent = new HashMap<>();
        queue.add(startNode);
        parent.put(startNode, startNode);
        PathNode bestNode = startNode;
        int bestDistance = distanceSquared(startNode, targetNode);

        int expansions = 0;
        while (!queue.isEmpty() && expansions++ < MAX_EXPANSIONS) {
            PathNode current = queue.removeFirst();
            if (current.equals(targetNode)) {
                return reconstruct(parent, startNode, targetNode, baseY);
            }
            int currentDistance = distanceSquared(current, targetNode);
            if (currentDistance < bestDistance) {
                bestDistance = currentDistance;
                bestNode = current;
            }

            for (int[] offset : CARDINALS) {
                PathNode next = new PathNode(current.x() + offset[0], current.z() + offset[1]);
                if (parent.containsKey(next) || distanceSquared(startNode, next) > MAX_SEARCH_RADIUS * MAX_SEARCH_RADIUS) {
                    continue;
                }
                if (!isWalkable(world, next.x(), baseY, next.z())) {
                    continue;
                }
                parent.put(next, current);
                queue.addLast(next);
            }
        }
        if (bestNode.equals(startNode)) {
            return List.of();
        }
        return reconstruct(parent, startNode, bestNode, baseY);
    }

    private List<Vector3f> reconstruct(Map<PathNode, PathNode> parent, PathNode start, PathNode target, int y) {
        ArrayDeque<PathNode> reversed = new ArrayDeque<>();
        PathNode current = target;
        while (!current.equals(start)) {
            reversed.addFirst(current);
            current = parent.get(current);
            if (current == null) {
                return List.of();
            }
        }

        List<Vector3f> path = new ArrayList<>(reversed.size());
        for (PathNode node : reversed) {
            path.add(new Vector3f(node.x() + 0.5f, y, node.z() + 0.5f));
        }
        return path;
    }

    private boolean isWalkable(VoxelWorld world, int x, int y, int z) {
        return world.isBlockLoadedAtWorld(x, y, z)
                && world.isBlockLoadedAtWorld(x, y + 1, z)
                && world.isBlockLoadedAtWorld(x, y - 1, z)
                && !world.isSolidBlockAtWorld(x, y, z)
                && !world.isSolidBlockAtWorld(x, y + 1, z)
                && world.isSolidBlockAtWorld(x, y - 1, z);
    }

    private int distanceSquared(PathNode left, PathNode right) {
        int dx = left.x() - right.x();
        int dz = left.z() - right.z();
        return dx * dx + dz * dz;
    }
}
