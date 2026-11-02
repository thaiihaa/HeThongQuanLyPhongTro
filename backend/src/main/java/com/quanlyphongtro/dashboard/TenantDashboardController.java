package com.quanlyphongtro.dashboard;

import com.quanlyphongtro.auth.AuthModels.AuthenticatedUser;
import com.quanlyphongtro.dashboard.DashboardModels.TenantDashboardResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenant/dashboard")
public class TenantDashboardController {

    private final DashboardRepository dashboardRepository;

    public TenantDashboardController(DashboardRepository dashboardRepository) {
        this.dashboardRepository = dashboardRepository;
    }

    @GetMapping
    public ResponseEntity<TenantDashboardResponse> dashboard(@AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.of(dashboardRepository.loadTenantDashboard(user));
    }
}
