package com.example.voxelgame.game.world;

import java.util.Optional;

import com.example.voxelgame.world.Block;
import com.example.voxelgame.world.BlockRegistry;

public final class SaplingGrowthRules {
    private SaplingGrowthRules() {
    }

    public static Optional<TreeKind> treeKindFor(Block saplingBlock) {
        if (saplingBlock == BlockRegistry.OAK_SAPLING) {
            return Optional.of(TreeKind.OAK);
        }
        if (saplingBlock == BlockRegistry.DARK_OAK_SAPLING) {
            return Optional.of(TreeKind.DARK_OAK);
        }
        if (saplingBlock == BlockRegistry.BIRCH_SAPLING) {
            return Optional.of(TreeKind.BIRCH);
        }
        if (saplingBlock == BlockRegistry.SPRUCE_SAPLING) {
            return Optional.of(TreeKind.SPRUCE);
        }
        return Optional.empty();
    }

    public static boolean canGrowOn(Block groundBlock) {
        return groundBlock == BlockRegistry.GRASS || groundBlock == BlockRegistry.DIRT;
    }

    public enum TreeKind {
        OAK,
        DARK_OAK,
        BIRCH,
        SPRUCE
    }
}
