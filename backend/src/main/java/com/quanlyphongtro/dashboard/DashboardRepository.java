package com.quanlyphongtro.dashboard;

import com.quanlyphongtro.auth.AuthModels.AuthenticatedUser;
import com.quanlyphongtro.dashboard.DashboardModels.AdminDashboardResponse;
import com.quanlyphongtro.dashboard.DashboardModels.TenantDashboardResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

@Repository
public class DashboardRepository {

    private final JdbcTemplate jdbcTemplate;

    public DashboardRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public AdminDashboardResponse loadAdminDashboard() {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT_BIG(*) AS total_accounts,
                       COALESCE(SUM(CASE WHEN u.trang_thai = N'HOAT_DONG' THEN CONVERT(BIGINT, 1) ELSE CONVERT(BIGINT, 0) END), 0) AS active_accounts,
                       COALESCE(SUM(CASE WHEN u.trang_thai = N'KHOA' THEN CONVERT(BIGINT, 1) ELSE CONVERT(BIGINT, 0) END), 0) AS locked_accounts,
                       COALESCE(SUM(CASE WHEN r.ma_vai_tro = N'ADMIN' THEN CONVERT(BIGINT, 1) ELSE CONVERT(BIGINT, 0) END), 0) AS admin_accounts,
                       COALESCE(SUM(CASE WHEN r.ma_vai_tro = N'NGUOI_THUE' THEN CONVERT(BIGINT, 1) ELSE CONVERT(BIGINT, 0) END), 0) AS tenant_accounts,
                       (SELECT COUNT_BIG(*) FROM dbo.khu_tro) AS total_sites,
                       (SELECT COUNT_BIG(*) FROM dbo.phong_tro) AS total_rooms,
                       (SELECT COUNT_BIG(*) FROM dbo.phong_tro WHERE trang_thai = N'TRONG') AS vacant_rooms,
                       (SELECT COUNT_BIG(*) FROM dbo.phong_tro WHERE trang_thai = N'DANG_THUE') AS occupied_rooms
                FROM dbo.nguoi_dung u
                JOIN dbo.vai_tro r ON r.vai_tro_id = u.vai_tro_id
                """,
                (resultSet, rowNum) -> new AdminDashboardResponse(
                        resultSet.getLong("total_accounts"),
                        resultSet.getLong("active_accounts"),
                        resultSet.getLong("locked_accounts"),
                        resultSet.getLong("admin_accounts"),
                        resultSet.getLong("tenant_accounts"),
                        resultSet.getLong("total_sites"),
                        resultSet.getLong("total_rooms"),
                        resultSet.getLong("vacant_rooms"),
                        resultSet.getLong("occupied_rooms")
                )
        );
    }

    public Optional<TenantDashboardResponse> loadTenantDashboard(AuthenticatedUser user) {
        List<TenantDashboardResponse> responses = jdbcTemplate.query("""
                SELECT u.nguoi_dung_id, u.ten_dang_nhap, u.ho_ten, u.email, u.so_dien_thoai,
                       r.ma_vai_tro, t.cccd, t.ngay_sinh, t.dia_chi
                FROM dbo.nguoi_dung u
                JOIN dbo.vai_tro r ON r.vai_tro_id = u.vai_tro_id
                JOIN dbo.nguoi_thue t ON t.nguoi_dung_id = u.nguoi_dung_id
                WHERE u.nguoi_dung_id = ? AND r.ma_vai_tro = N'NGUOI_THUE'
                """,
                (resultSet, rowNum) -> {
                    Date birthDate = resultSet.getDate("ngay_sinh");
                    return new TenantDashboardResponse(
                            resultSet.getLong("nguoi_dung_id"),
                            resultSet.getString("ten_dang_nhap"),
                            resultSet.getString("ho_ten"),
                            resultSet.getString("email"),
                            resultSet.getString("so_dien_thoai"),
                            resultSet.getString("ma_vai_tro"),
                            resultSet.getString("cccd"),
                            birthDate == null ? null : birthDate.toLocalDate(),
                            resultSet.getString("dia_chi")
                    );
                },
                user.id()
        );
        return responses.stream().findFirst();
    }
}
