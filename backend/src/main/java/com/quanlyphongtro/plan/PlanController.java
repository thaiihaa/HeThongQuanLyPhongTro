package com.quanlyphongtro.plan;

import com.quanlyphongtro.auth.AuthModels.AuthenticatedUser;
import com.quanlyphongtro.plan.PlanModels.AdminPlanResponse;
import com.quanlyphongtro.plan.PlanModels.GrantPremiumRequest;
import com.quanlyphongtro.plan.PlanModels.TenantPlanResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @GetMapping("/api/v1/tenant/plan")
    public TenantPlanResponse getTenantPlan(@AuthenticationPrincipal AuthenticatedUser user) {
        return planService.getTenantPlan(user.id());
    }

    @PostMapping("/api/v1/admin/nguoi-thue/{userId}/plan")
    public AdminPlanResponse grantPremium(
            @AuthenticationPrincipal AuthenticatedUser admin,
            @PathVariable @Positive long userId,
            @Valid @RequestBody GrantPremiumRequest request
    ) {
        return planService.grantPremium(userId, admin.id(), request.expiresOn());
    }

    @DeleteMapping("/api/v1/admin/nguoi-thue/{userId}/plan")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokePremium(
            @AuthenticationPrincipal AuthenticatedUser admin,
            @PathVariable @Positive long userId
    ) {
        planService.revokePremium(userId, admin.id());
    }
}
