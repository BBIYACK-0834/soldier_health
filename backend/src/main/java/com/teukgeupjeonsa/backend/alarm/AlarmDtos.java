package com.teukgeupjeonsa.backend.alarm;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

public class AlarmDtos {

    @Getter
    @Setter
    public static class AlarmSaveRequest {
        @NotNull
        private Boolean enabled;
        @NotNull @Min(0) @Max(23)
        private Integer hour;
        @NotNull @Min(0) @Max(59)
        private Integer minute;
        @NotNull
        private String repeatDaysJson;
        private String label;
    }

    @Getter
    @Builder
    public static class AlarmResponse {
        private Long id;
        private Boolean enabled;
        private Integer hour;
        private Integer minute;
        private String repeatDaysJson;
        private String label;
    }
}
