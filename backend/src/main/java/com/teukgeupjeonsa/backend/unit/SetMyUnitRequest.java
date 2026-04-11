package com.teukgeupjeonsa.backend.unit;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SetMyUnitRequest {
    @NotNull
    private Long unitId;
}
