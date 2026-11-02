package com.quanlyphongtro.tenant;

import com.quanlyphongtro.tenant.AdminTenantModels.PageResponse;
import com.quanlyphongtro.tenant.AdminTenantModels.TenantSummary;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/admin/nguoi-thue")
public class AdminTenantController {

    private final AdminTenantService tenantService;

    public AdminTenantController(AdminTenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping
    public PageResponse<TenantSummary> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) @Size(max = 150) String search
    ) {
        return tenantService.list(page, size, search);
    }
}
