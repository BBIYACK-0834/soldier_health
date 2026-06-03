package com.teukgeupjeonsa.backend.workout;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

public class WorkoutDtos {

    @Getter
    @Builder
    public static class WorkoutExercise {
        private String name;
        private String category;
        private int sets;
        private String reps;
        private int durationSeconds;
        private int restSeconds;
        private String intensity;
        private String requiredEquipment;
        private String recommendationReason;
        private String alternative;
    }

    @Getter
    @Builder
    public static class WorkoutRecommendationResponse {
        private String routineType;
        private String todayFocus;
        private List<WorkoutExercise> exercises;
        private String note;
    }
}
