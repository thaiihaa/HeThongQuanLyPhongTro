package com.quanlyphongtro.tenant;

import com.quanlyphongtro.tenant.AdminTenantModels.PageResponse;
import com.quanlyphongtro.tenant.AdminTenantModels.TenantSummary;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminTenantService {

    private final AdminTenantRepository repository;

    public AdminTenantService(AdminTenantRepository repository) {
        this.repository = repository;
    }

    public PageResponse<TenantSummary> list(int page, int size, String search) {
        int safeSize = Math.max(1, Math.min(size, 100));
        int safePage = Math.min(Math.max(page, 0), Integer.MAX_VALUE / safeSize);
        String normalizedSearch = StringUtils.hasText(search) ? search.trim() : null;
        return repository.list(safePage, safeSize, normalizedSearch);
    }
}
