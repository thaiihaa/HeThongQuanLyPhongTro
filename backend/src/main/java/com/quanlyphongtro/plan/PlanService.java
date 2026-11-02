package com.quanlyphongtro.plan;

import com.quanlyphongtro.auth.AuthException;
import com.quanlyphongtro.plan.PlanModels.AdminPlanResponse;
import com.quanlyphongtro.plan.PlanModels.TenantPlanResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
public class PlanService {

    private final PlanRepository repository;

    public PlanService(PlanRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public TenantPlanResponse getTenantPlan(long userId) {
        return repository.findTenantPlan(userId)
                .orElseThrow(() -> new AuthException(
                        HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND", "Không tìm thấy hồ sơ người thuê đang hoạt động."
                ));
    }

    @Transactional
    public AdminPlanResponse grantPremium(long tenantUserId, long adminUserId, LocalDate expiresOn) {
        LocalDate startsOn = LocalDate.now(ZoneOffset.UTC);
        if (expiresOn == null || expiresOn.isBefore(startsOn)) {
            throw new AuthException(
                    HttpStatus.BAD_REQUEST, "INVALID_PLAN_PERIOD", "Ngày hết hạn phải từ hôm nay trở đi."
            );
        }

        long tenantId = repository.lockActiveTenantProfile(tenantUserId)
                .orElseThrow(() -> new AuthException(
                        HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND", "Không tìm thấy hồ sơ người thuê đang hoạt động."
                ));
        if (repository.hasOverlappingActiveEntitlement(tenantId, startsOn, expiresOn)) {
            throw new AuthException(
                    HttpStatus.CONFLICT, "PREMIUM_ALREADY_ACTIVE", "Người thuê đã có gói Premium còn hiệu lực."
            );
        }

        repository.insertPremiumEntitlement(tenantId, adminUserId, startsOn, expiresOn);
        return repository.findAdminPlan(tenantUserId)
                .orElseThrow(() -> new IllegalStateException("Granted tenant plan could not be reloaded."));
    }

    @Transactional
    public void revokePremium(long tenantUserId, long adminUserId) {
        long tenantId = repository.lockActiveTenantProfile(tenantUserId)
                .orElseThrow(() -> new AuthException(
                        HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND", "Không tìm thấy hồ sơ người thuê đang hoạt động."
                ));
        if (repository.revokeActivePremium(tenantId, adminUserId) != 1) {
            throw new AuthException(
                    HttpStatus.CONFLICT, "PREMIUM_NOT_ACTIVE", "Người thuê không có gói Premium đang hiệu lực."
            );
        }
    }
}
