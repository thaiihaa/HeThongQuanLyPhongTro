package com.quanlyphongtro.plan;

import com.quanlyphongtro.auth.AuthException;
import com.quanlyphongtro.plan.PlanModels.AdminPlanResponse;
import com.quanlyphongtro.plan.PlanModels.TenantPlanResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlanServiceTest {

    private final PlanRepository repository = mock(PlanRepository.class);
    private final PlanService service = new PlanService(repository);

    @Test
    void returnsEffectiveStandardPlanWhenRepositoryFindsNoPremiumEntitlement() {
        TenantPlanResponse standard = new TenantPlanResponse(
                "THUONG", "Thường", null, null, false, "Standard tenant capabilities"
        );
        when(repository.findTenantPlan(12L)).thenReturn(Optional.of(standard));

        assertEquals(standard, service.getTenantPlan(12L));
    }

    @Test
    void grantsPremiumForTenantAndKeepsTheAdminUserIdAsGrantor() {
        LocalDate startsOn = LocalDate.now(ZoneOffset.UTC);
        LocalDate expiresOn = startsOn.plusDays(30);
        when(repository.lockActiveTenantProfile(12L)).thenReturn(Optional.of(5L));
        when(repository.hasOverlappingActiveEntitlement(5L, startsOn, expiresOn)).thenReturn(false);
        when(repository.findAdminPlan(12L)).thenReturn(Optional.of(
                new AdminPlanResponse(12L, "PREMIUM", startsOn, expiresOn)
        ));

        AdminPlanResponse response = service.grantPremium(12L, 1L, expiresOn);

        assertEquals("PREMIUM", response.code());
        verify(repository).insertPremiumEntitlement(5L, 1L, startsOn, expiresOn);
    }

    @Test
    void rejectsExpiredPremiumGrantBeforeDatabaseWrites() {
        AuthException exception = assertThrows(AuthException.class,
                () -> service.grantPremium(12L, 1L, LocalDate.now(ZoneOffset.UTC).minusDays(1)));

        assertEquals(HttpStatus.BAD_REQUEST, exception.status());
        assertEquals("INVALID_PLAN_PERIOD", exception.code());
        verify(repository, never()).lockActiveTenantProfile(12L);
    }

    @Test
    void rejectsOverlappingPremiumGrant() {
        LocalDate startsOn = LocalDate.now(ZoneOffset.UTC);
        LocalDate expiresOn = startsOn.plusDays(30);
        when(repository.lockActiveTenantProfile(12L)).thenReturn(Optional.of(5L));
        when(repository.hasOverlappingActiveEntitlement(5L, startsOn, expiresOn)).thenReturn(true);

        AuthException exception = assertThrows(AuthException.class,
                () -> service.grantPremium(12L, 1L, expiresOn));

        assertEquals(HttpStatus.CONFLICT, exception.status());
        assertEquals("PREMIUM_ALREADY_ACTIVE", exception.code());
        verify(repository, never()).insertPremiumEntitlement(5L, 1L, startsOn, expiresOn);
    }

    @Test
    void doesNotAllowAnAdminAccountToReceiveATenantPlan() {
        when(repository.lockActiveTenantProfile(1L)).thenReturn(Optional.empty());

        AuthException exception = assertThrows(AuthException.class,
                () -> service.grantPremium(1L, 1L, LocalDate.now(ZoneOffset.UTC).plusDays(30)));

        assertEquals(HttpStatus.NOT_FOUND, exception.status());
        assertEquals("TENANT_NOT_FOUND", exception.code());
        verify(repository, never()).insertPremiumEntitlement(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void requiresAnActivePremiumEntitlementBeforeRevocation() {
        when(repository.lockActiveTenantProfile(12L)).thenReturn(Optional.of(5L));
        when(repository.revokeActivePremium(5L, 1L)).thenReturn(0);

        AuthException exception = assertThrows(AuthException.class, () -> service.revokePremium(12L, 1L));

        assertEquals(HttpStatus.CONFLICT, exception.status());
        assertEquals("PREMIUM_NOT_ACTIVE", exception.code());
    }
}
