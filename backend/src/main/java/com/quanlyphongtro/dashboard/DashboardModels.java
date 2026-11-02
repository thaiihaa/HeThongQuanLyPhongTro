package com.quanlyphongtro.dashboard;

import java.time.LocalDate;

public final class DashboardModels {

    private DashboardModels() {
    }

    public record AdminDashboardResponse(
            long totalAccounts,
            long activeAccounts,
            long lockedAccounts,
            long adminAccounts,
            long tenantAccounts,
            long totalSites,
            long totalRooms,
            long vacantRooms,
            long occupiedRooms
    ) {
    }

    public record TenantDashboardResponse(
            long userId,
            String username,
            String fullName,
            String email,
            String phone,
            String role,
            String citizenId,
            LocalDate birthDate,
            String address
    ) {
    }
}
