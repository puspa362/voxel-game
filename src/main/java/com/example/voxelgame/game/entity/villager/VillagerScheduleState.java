package com.example.voxelgame.game.entity.villager;

import com.example.voxelgame.world.WorldTime;

public enum VillagerScheduleState {
    MORNING,
    DAY,
    EVENING,
    NIGHT;

    public static VillagerScheduleState from(WorldTime worldTime) {
        if (worldTime == null) {
            return DAY;
        }
        double day = worldTime.getDayProgress();
        if (day < 0.25) {
            return MORNING;
        }
        if (day < 0.70) {
            return DAY;
        }
        if (day < 0.82) {
            return EVENING;
        }
        return NIGHT;
    }
}
