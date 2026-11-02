package com.quanlyphongtro.tenant;

import java.time.LocalDate;
import java.util.List;

public final class AdminTenantModels {

    private AdminTenantModels() {
    }

    public record TenantSummary(
            long id,
            long userId,
            String username,
            String fullName,
            String email,
            String phone,
            String accountStatus,
            LocalDate birthDate,
            String address,
            String planCode,
            LocalDate planExpiresOn
    ) {
    }

    public record PageResponse<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
    }
}
