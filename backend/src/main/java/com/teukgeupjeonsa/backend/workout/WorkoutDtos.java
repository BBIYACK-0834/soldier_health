package com.teukgeupjeonsa.backend.workout;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

public class WorkoutDtos {

    @Getter
    @Builder
    public static class WorkoutExercise {
        private String name;
        private int sets;
        private String reps;
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
