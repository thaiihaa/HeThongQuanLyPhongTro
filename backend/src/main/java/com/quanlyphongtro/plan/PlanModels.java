package com.quanlyphongtro.plan;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public final class PlanModels {

    private PlanModels() {
    }

    public record TenantPlanResponse(
            String code,
            String name,
            LocalDate startsOn,
            LocalDate expiresOn,
            boolean premium,
            String benefits
    ) {
    }

    public record GrantPremiumRequest(@NotNull LocalDate expiresOn) {
    }

    public record AdminPlanResponse(
            long tenantUserId,
            String code,
            LocalDate startsOn,
            LocalDate expiresOn
    ) {
    }
}
