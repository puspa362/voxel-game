package com.example.voxelgame.render;

import com.example.voxelgame.world.WorldTime;

public record EnvironmentPalette(
        float upperR,
        float upperG,
        float upperB,
        float horizonR,
        float horizonG,
        float horizonB,
        float fogR,
        float fogG,
        float fogB,
        float cloudR,
        float cloudG,
        float cloudB,
        float ambient,
        float sunlight,
        float sunset
) {
    public static EnvironmentPalette from(WorldTime worldTime) {
        float progress = (float) worldTime.getDayProgress();
        float sunHeight = worldTime.getSunHeight();
        float day = smoothstep(-0.08f, 0.28f, sunHeight);
        float night = 1.0f - smoothstep(-0.28f, 0.04f, sunHeight);
        float horizon = 1.0f - Math.abs((progress * 2.0f) - 1.0f);
        float sunset = smoothstep(0.03f, 0.22f, horizon) * (1.0f - smoothstep(0.22f, 0.48f, Math.abs(sunHeight)));
        float twilight = smoothstep(-0.25f, 0.06f, sunHeight) * (1.0f - smoothstep(0.06f, 0.42f, sunHeight));

        float upperR = mix(0.025f, 0.42f, day);
        float upperG = mix(0.045f, 0.67f, day);
        float upperB = mix(0.120f, 0.94f, day);
        upperR = mix(upperR, 0.25f, twilight * 0.45f);
        upperG = mix(upperG, 0.20f, twilight * 0.35f);
        upperB = mix(upperB, 0.52f, twilight * 0.45f);

        float horizonR = mix(0.045f, 0.60f, day);
        float horizonG = mix(0.060f, 0.78f, day);
        float horizonB = mix(0.130f, 0.96f, day);
        horizonR = mix(horizonR, 1.00f, sunset);
        horizonG = mix(horizonG, 0.46f, sunset);
        horizonB = mix(horizonB, 0.26f, sunset);
        horizonR = mix(horizonR, 0.22f, twilight * (1.0f - sunset));
        horizonG = mix(horizonG, 0.16f, twilight * (1.0f - sunset));
        horizonB = mix(horizonB, 0.38f, twilight * (1.0f - sunset));

        float fogR = mix(horizonR, 0.08f, night * 0.45f);
        float fogG = mix(horizonG, 0.12f, night * 0.45f);
        float fogB = mix(horizonB, 0.20f, night * 0.35f);

        float cloudR = mix(0.22f, 0.95f, day);
        float cloudG = mix(0.25f, 0.97f, day);
        float cloudB = mix(0.34f, 1.00f, day);
        cloudR = mix(cloudR, 1.00f, sunset * 0.55f);
        cloudG = mix(cloudG, 0.62f, sunset * 0.45f);
        cloudB = mix(cloudB, 0.44f, sunset * 0.35f);

        float ambient = mix(0.13f, 0.52f, day) + twilight * 0.05f;
        float sunlight = mix(0.08f, 0.78f, day);
        return new EnvironmentPalette(upperR, upperG, upperB, horizonR, horizonG, horizonB, fogR, fogG, fogB, cloudR, cloudG, cloudB, ambient, sunlight, sunset);
    }

    private static float mix(float start, float end, float alpha) {
        return start + (end - start) * clamp01(alpha);
    }

    private static float smoothstep(float edge0, float edge1, float value) {
        float x = clamp01((value - edge0) / (edge1 - edge0));
        return x * x * (3.0f - 2.0f * x);
    }

    private static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
