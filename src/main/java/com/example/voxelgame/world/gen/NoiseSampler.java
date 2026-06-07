package com.example.voxelgame.world.gen;

@FunctionalInterface
public interface NoiseSampler {
    double sample(double x, double z);
}
