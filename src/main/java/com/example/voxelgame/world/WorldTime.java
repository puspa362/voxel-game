package com.example.voxelgame.world;

public final class WorldTime {
    public static final double DEFAULT_DAY_LENGTH_SECONDS = 20.0 * 60.0;

    private final double dayLengthSeconds;
    private double elapsedSeconds;

    public WorldTime(double dayLengthSeconds, double startingProgress) {
        if (dayLengthSeconds <= 0.0) {
            throw new IllegalArgumentException("Day length must be greater than zero.");
        }
        if (startingProgress < 0.0 || startingProgress >= 1.0) {
            throw new IllegalArgumentException("Starting day progress must be in [0, 1).");
        }

        this.dayLengthSeconds = dayLengthSeconds;
        this.elapsedSeconds = startingProgress * dayLengthSeconds;
    }

    public void advance(double deltaSeconds) {
        elapsedSeconds = wrap(elapsedSeconds + Math.max(0.0, deltaSeconds), dayLengthSeconds);
    }

    public double getDayProgress() {
        return elapsedSeconds / dayLengthSeconds;
    }

    public float getSunHeight() {
        return (float) Math.sin(getDayProgress() * Math.PI * 2.0);
    }

    public float getDaylight() {
        float sunHeight = getSunHeight();
        return smoothstep(-0.10f, 0.32f, sunHeight);
    }

    public float getMoonlight() {
        return 1.0f - getDaylight();
    }

    public float getCelestialAngleRadians() {
        return (float) (getDayProgress() * Math.PI * 2.0);
    }

    public String formatTimeOfDay() {
        int totalMinutes = (int) Math.floor(getDayProgress() * 24.0 * 60.0);
        int hours = Math.floorDiv(totalMinutes, 60) % 24;
        int minutes = Math.floorMod(totalMinutes, 60);
        return "%02d:%02d".formatted(hours, minutes);
    }

    private static double wrap(double value, double modulus) {
        double result = value % modulus;
        return result < 0.0 ? result + modulus : result;
    }

    private static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static float smoothstep(float edge0, float edge1, float value) {
        float x = clamp01((value - edge0) / (edge1 - edge0));
        return x * x * (3.0f - 2.0f * x);
    }
}
