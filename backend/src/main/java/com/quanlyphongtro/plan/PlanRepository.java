package com.quanlyphongtro.plan;

import com.quanlyphongtro.plan.PlanModels.AdminPlanResponse;
import com.quanlyphongtro.plan.PlanModels.TenantPlanResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class PlanRepository {

    private final JdbcTemplate jdbcTemplate;

    public PlanRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<TenantPlanResponse> findTenantPlan(long userId) {
        List<TenantPlanResponse> plans = jdbcTemplate.query("""
                SELECT COALESCE(active_plan.ma_goi, N'THUONG') AS ma_goi,
                       catalog.ten_goi,
                       active_plan.ngay_bat_dau,
                       active_plan.ngay_ket_thuc,
                       catalog.mo_ta
                FROM dbo.nguoi_dung u
                JOIN dbo.vai_tro role ON role.vai_tro_id = u.vai_tro_id
                JOIN dbo.nguoi_thue tenant ON tenant.nguoi_dung_id = u.nguoi_dung_id
                OUTER APPLY (
                    SELECT TOP (1) entitlement.ma_goi,
                           entitlement.ngay_bat_dau, entitlement.ngay_ket_thuc
                    FROM dbo.nguoi_thue_goi entitlement
                    WHERE entitlement.nguoi_thue_id = tenant.nguoi_thue_id
                      AND entitlement.trang_thai = N'DANG_HOAT_DONG'
                      AND entitlement.ngay_bat_dau <= CONVERT(DATE, SYSUTCDATETIME())
                      AND entitlement.ngay_ket_thuc >= CONVERT(DATE, SYSUTCDATETIME())
                    ORDER BY entitlement.ngay_bat_dau DESC, entitlement.nguoi_thue_goi_id DESC
                ) active_plan
                JOIN dbo.goi_dich_vu catalog
                  ON catalog.ma_goi = COALESCE(active_plan.ma_goi, N'THUONG')
                WHERE u.nguoi_dung_id = ?
                  AND u.trang_thai = N'HOAT_DONG'
                  AND role.ma_vai_tro = N'NGUOI_THUE'
                """,
                (rs, rowNum) -> {
                    Date startsOn = rs.getDate("ngay_bat_dau");
                    Date expiresOn = rs.getDate("ngay_ket_thuc");
                    String code = rs.getString("ma_goi");
                    return new TenantPlanResponse(
                            code,
                            rs.getString("ten_goi"),
                            startsOn == null ? null : startsOn.toLocalDate(),
                            expiresOn == null ? null : expiresOn.toLocalDate(),
                            "PREMIUM".equals(code),
                            rs.getString("mo_ta")
                    );
                },
                userId
        );
        return plans.stream().findFirst();
    }

    public Optional<Long> lockActiveTenantProfile(long userId) {
        List<Long> profiles = jdbcTemplate.query("""
                SELECT tenant.nguoi_thue_id
                FROM dbo.nguoi_thue tenant WITH (UPDLOCK, HOLDLOCK)
                JOIN dbo.nguoi_dung u ON u.nguoi_dung_id = tenant.nguoi_dung_id
                JOIN dbo.vai_tro role ON role.vai_tro_id = u.vai_tro_id
                WHERE u.nguoi_dung_id = ?
                  AND u.trang_thai = N'HOAT_DONG'
                  AND role.ma_vai_tro = N'NGUOI_THUE'
                """,
                (rs, rowNum) -> rs.getLong("nguoi_thue_id"),
                userId
        );
        return profiles.stream().findFirst();
    }

    public boolean hasOverlappingActiveEntitlement(long tenantId, LocalDate startsOn, LocalDate expiresOn) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM dbo.nguoi_thue_goi WITH (UPDLOCK, HOLDLOCK)
                WHERE nguoi_thue_id = ?
                  AND trang_thai = N'DANG_HOAT_DONG'
                  AND ngay_bat_dau <= ?
                  AND ngay_ket_thuc >= ?
                """,
                Integer.class,
                tenantId,
                Date.valueOf(expiresOn),
                Date.valueOf(startsOn)
        );
        return count != null && count > 0;
    }

    public void insertPremiumEntitlement(
            long tenantId, long adminUserId, LocalDate startsOn, LocalDate expiresOn
    ) {
        jdbcTemplate.update("""
                INSERT INTO dbo.nguoi_thue_goi
                    (nguoi_thue_id, ma_goi, ngay_bat_dau, ngay_ket_thuc, nguoi_cap_id)
                VALUES (?, N'PREMIUM', ?, ?, ?)
                """,
                tenantId,
                Date.valueOf(startsOn),
                Date.valueOf(expiresOn),
                adminUserId
        );
    }

    public int revokeActivePremium(long tenantId, long adminUserId) {
        return jdbcTemplate.update("""
                UPDATE dbo.nguoi_thue_goi
                SET trang_thai = N'DA_THU_HOI',
                    thu_hoi_at = SYSUTCDATETIME(),
                    nguoi_thu_hoi_id = ?
                WHERE nguoi_thue_id = ?
                  AND ma_goi = N'PREMIUM'
                  AND trang_thai = N'DANG_HOAT_DONG'
                  AND ngay_bat_dau <= CONVERT(DATE, SYSUTCDATETIME())
                  AND ngay_ket_thuc >= CONVERT(DATE, SYSUTCDATETIME())
                """,
                adminUserId,
                tenantId
        );
    }

    public Optional<AdminPlanResponse> findAdminPlan(long userId) {
        List<AdminPlanResponse> plans = jdbcTemplate.query("""
                SELECT u.nguoi_dung_id,
                       COALESCE(active_plan.ma_goi, N'THUONG') AS ma_goi,
                       active_plan.ngay_bat_dau,
                       active_plan.ngay_ket_thuc
                FROM dbo.nguoi_dung u
                JOIN dbo.vai_tro role ON role.vai_tro_id = u.vai_tro_id
                JOIN dbo.nguoi_thue tenant ON tenant.nguoi_dung_id = u.nguoi_dung_id
                OUTER APPLY (
                    SELECT TOP (1) entitlement.ma_goi,
                           entitlement.ngay_bat_dau, entitlement.ngay_ket_thuc
                    FROM dbo.nguoi_thue_goi entitlement
                    WHERE entitlement.nguoi_thue_id = tenant.nguoi_thue_id
                      AND entitlement.trang_thai = N'DANG_HOAT_DONG'
                      AND entitlement.ngay_bat_dau <= CONVERT(DATE, SYSUTCDATETIME())
                      AND entitlement.ngay_ket_thuc >= CONVERT(DATE, SYSUTCDATETIME())
                    ORDER BY entitlement.ngay_bat_dau DESC, entitlement.nguoi_thue_goi_id DESC
                ) active_plan
                WHERE u.nguoi_dung_id = ?
                  AND u.trang_thai = N'HOAT_DONG'
                  AND role.ma_vai_tro = N'NGUOI_THUE'
                """,
                (rs, rowNum) -> {
                    Date startsOn = rs.getDate("ngay_bat_dau");
                    Date expiresOn = rs.getDate("ngay_ket_thuc");
                    return new AdminPlanResponse(
                            rs.getLong("nguoi_dung_id"),
                            rs.getString("ma_goi"),
                            startsOn == null ? null : startsOn.toLocalDate(),
                            expiresOn == null ? null : expiresOn.toLocalDate()
                    );
                },
                userId
        );
        return plans.stream().findFirst();
    }
}
