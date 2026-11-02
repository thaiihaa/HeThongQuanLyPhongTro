package com.quanlyphongtro.tenant;

import com.quanlyphongtro.tenant.AdminTenantModels.PageResponse;
import com.quanlyphongtro.tenant.AdminTenantModels.TenantSummary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.sql.Date;
import java.util.List;
import java.util.Objects;

@Repository
public class AdminTenantRepository {

    private static final String SELECT_COLUMNS = """
            SELECT t.nguoi_thue_id, u.nguoi_dung_id, u.ten_dang_nhap, u.ho_ten,
                   u.email, u.so_dien_thoai, u.trang_thai, t.ngay_sinh, t.dia_chi,
                   COALESCE(active_plan.ma_goi, N'THUONG') AS ma_goi,
                   active_plan.ngay_ket_thuc
            FROM dbo.nguoi_thue t
            JOIN dbo.nguoi_dung u ON u.nguoi_dung_id = t.nguoi_dung_id
            JOIN dbo.vai_tro r ON r.vai_tro_id = u.vai_tro_id
            OUTER APPLY (
                SELECT MAX(entitlement.ma_goi) AS ma_goi,
                       MAX(entitlement.ngay_ket_thuc) AS ngay_ket_thuc
                FROM dbo.nguoi_thue_goi entitlement
                WHERE entitlement.nguoi_thue_id = t.nguoi_thue_id
                  AND entitlement.trang_thai = N'DANG_HOAT_DONG'
                  AND entitlement.ngay_bat_dau <= CONVERT(DATE, SYSUTCDATETIME())
                  AND entitlement.ngay_ket_thuc >= CONVERT(DATE, SYSUTCDATETIME())
            ) active_plan
            WHERE r.ma_vai_tro = N'NGUOI_THUE'
            """;

    private final JdbcTemplate jdbcTemplate;

    public AdminTenantRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public PageResponse<TenantSummary> list(int page, int size, String search) {
        boolean filter = StringUtils.hasText(search);
        String searchClause = filter
                ? " AND (u.ten_dang_nhap LIKE ? OR u.ho_ten LIKE ? OR u.email LIKE ? OR u.so_dien_thoai LIKE ?)"
                : "";
        String pattern = filter ? "%" + search.trim() + "%" : null;
        String countSql = """
                SELECT COUNT_BIG(*)
                FROM dbo.nguoi_thue t
                JOIN dbo.nguoi_dung u ON u.nguoi_dung_id = t.nguoi_dung_id
                JOIN dbo.vai_tro r ON r.vai_tro_id = u.vai_tro_id
                WHERE r.ma_vai_tro = N'NGUOI_THUE'
                """ + searchClause;
        Long total = filter
                ? jdbcTemplate.queryForObject(countSql, Long.class, pattern, pattern, pattern, pattern)
                : jdbcTemplate.queryForObject(countSql, Long.class);

        String rowsSql = SELECT_COLUMNS + searchClause
                + " ORDER BY t.nguoi_thue_id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        List<TenantSummary> rows = filter
                ? jdbcTemplate.query(rowsSql, AdminTenantRepository::mapTenant,
                        pattern, pattern, pattern, pattern, page * size, size)
                : jdbcTemplate.query(rowsSql, AdminTenantRepository::mapTenant, page * size, size);
        long totalElements = Objects.requireNonNull(total, "Tenant count query returned no result");
        int totalPages = totalElements == 0 ? 0 : (int) Math.min(Integer.MAX_VALUE, (totalElements - 1) / size + 1);
        return new PageResponse<>(rows, page, size, totalElements, totalPages);
    }

    private static TenantSummary mapTenant(java.sql.ResultSet resultSet, int rowNum) throws java.sql.SQLException {
        Date birthDate = resultSet.getDate("ngay_sinh");
        return new TenantSummary(
                resultSet.getLong("nguoi_thue_id"),
                resultSet.getLong("nguoi_dung_id"),
                resultSet.getString("ten_dang_nhap"),
                resultSet.getString("ho_ten"),
                resultSet.getString("email"),
                resultSet.getString("so_dien_thoai"),
                resultSet.getString("trang_thai"),
                birthDate == null ? null : birthDate.toLocalDate(),
                resultSet.getString("dia_chi"),
                resultSet.getString("ma_goi"),
                resultSet.getDate("ngay_ket_thuc") == null
                        ? null
                        : resultSet.getDate("ngay_ket_thuc").toLocalDate()
        );
    }
}
