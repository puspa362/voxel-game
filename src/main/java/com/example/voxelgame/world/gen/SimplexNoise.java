package com.example.voxelgame.world.gen;

import java.util.Random;

public final class SimplexNoise implements NoiseSampler {
    private static final int[][] GRADIENTS = {
            {1, 1}, {-1, 1}, {1, -1}, {-1, -1},
            {1, 0}, {-1, 0}, {1, 0}, {-1, 0},
            {0, 1}, {0, -1}, {0, 1}, {0, -1}
    };
    private static final double SKEW_FACTOR = 0.5 * (Math.sqrt(3.0) - 1.0);
    private static final double UNSKEW_FACTOR = (3.0 - Math.sqrt(3.0)) / 6.0;

    private final short[] permutation = new short[512];

    public SimplexNoise(long seed) {
        short[] source = new short[256];
        for (short i = 0; i < source.length; i++) {
            source[i] = i;
        }

        Random random = new Random(seed);
        for (int i = source.length - 1; i >= 0; i--) {
            int swapIndex = random.nextInt(i + 1);
            short value = source[swapIndex];
            source[swapIndex] = source[i];
            source[i] = value;
        }

        for (int i = 0; i < permutation.length; i++) {
            permutation[i] = source[i & 255];
        }
    }

    @Override
    public double sample(double x, double z) {
        double skew = (x + z) * SKEW_FACTOR;
        int cellX = fastFloor(x + skew);
        int cellZ = fastFloor(z + skew);

        double unskew = (cellX + cellZ) * UNSKEW_FACTOR;
        double x0 = x - (cellX - unskew);
        double z0 = z - (cellZ - unskew);

        int offsetX;
        int offsetZ;
        if (x0 > z0) {
            offsetX = 1;
            offsetZ = 0;
        } else {
            offsetX = 0;
            offsetZ = 1;
        }

        double x1 = x0 - offsetX + UNSKEW_FACTOR;
        double z1 = z0 - offsetZ + UNSKEW_FACTOR;
        double x2 = x0 - 1.0 + 2.0 * UNSKEW_FACTOR;
        double z2 = z0 - 1.0 + 2.0 * UNSKEW_FACTOR;

        int ii = cellX & 255;
        int jj = cellZ & 255;
        int gradient0 = permutation[ii + permutation[jj]] % GRADIENTS.length;
        int gradient1 = permutation[ii + offsetX + permutation[jj + offsetZ]] % GRADIENTS.length;
        int gradient2 = permutation[ii + 1 + permutation[jj + 1]] % GRADIENTS.length;

        double contribution0 = contribution(gradient0, x0, z0);
        double contribution1 = contribution(gradient1, x1, z1);
        double contribution2 = contribution(gradient2, x2, z2);

        return 70.0 * (contribution0 + contribution1 + contribution2);
    }

    private static double contribution(int gradientIndex, double x, double z) {
        double attenuation = 0.5 - x * x - z * z;
        if (attenuation <= 0.0) {
            return 0.0;
        }

        attenuation *= attenuation;
        return attenuation * attenuation * dot(GRADIENTS[gradientIndex], x, z);
    }

    private static double dot(int[] gradient, double x, double z) {
        return gradient[0] * x + gradient[1] * z;
    }

    private static int fastFloor(double value) {
        int integer = (int) value;
        return value < integer ? integer - 1 : integer;
    }
}
