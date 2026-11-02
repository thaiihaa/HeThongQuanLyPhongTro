package com.quanlyphongtro.dashboard;

import com.quanlyphongtro.dashboard.DashboardModels.AdminDashboardResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
public class AdminDashboardController {

    private final DashboardRepository dashboardRepository;

    public AdminDashboardController(DashboardRepository dashboardRepository) {
        this.dashboardRepository = dashboardRepository;
    }

    @GetMapping
    public AdminDashboardResponse dashboard() {
        return dashboardRepository.loadAdminDashboard();
    }
}
