package com.teukgeupjeonsa.backend.unit;

import com.teukgeupjeonsa.backend.user.BranchType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UnitResponse {
    private Long id;
    private String unitCode;
    private String unitName;
    private BranchType branchType;
    private String regionName;
}
