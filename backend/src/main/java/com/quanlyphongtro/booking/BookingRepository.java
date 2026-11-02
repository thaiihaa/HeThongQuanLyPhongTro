package com.quanlyphongtro.booking;

import com.quanlyphongtro.booking.BookingModels.AdminBookingResponse;
import com.quanlyphongtro.booking.BookingModels.RoomResponse;
import com.quanlyphongtro.booking.BookingModels.TenantBookingResponse;
import com.quanlyphongtro.property.PropertyModels.PageResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class BookingRepository {

    private final JdbcTemplate jdbcTemplate;

    public BookingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public PageResponse<RoomResponse> listAvailableRooms(
            int page, int size, String search, BigDecimal minPrice, BigDecimal maxPrice,
            BigDecimal minArea, BigDecimal maxArea
    ) {
        String pattern = search == null ? null : "%" + escapeLike(search) + "%";
        Long total = jdbcTemplate.queryForObject("""
                SELECT COUNT_BIG(*)
                FROM dbo.phong_tro p
                JOIN dbo.khu_tro k ON k.khu_tro_id = p.khu_tro_id
                JOIN dbo.loai_phong l ON l.loai_phong_id = p.loai_phong_id
                WHERE p.trang_thai = N'TRONG'
                  AND k.trang_thai = N'HOAT_DONG'
                  AND l.trang_thai = N'HOAT_DONG'
                  AND (? IS NULL OR p.ma_phong LIKE ? ESCAPE N'\\'
                       OR k.ten_khu_tro LIKE ? ESCAPE N'\\'
                       OR k.dia_chi LIKE ? ESCAPE N'\\'
                       OR l.ten_loai_phong LIKE ? ESCAPE N'\\')
                  AND (? IS NULL OR p.gia_thue >= ?)
                  AND (? IS NULL OR p.gia_thue <= ?)
                  AND (? IS NULL OR p.dien_tich_m2 >= ?)
                  AND (? IS NULL OR p.dien_tich_m2 <= ?)
                """, Long.class,
                pattern, pattern, pattern, pattern, pattern,
                minPrice, minPrice, maxPrice, maxPrice,
                minArea, minArea, maxArea, maxArea);

        List<RoomResponse> rooms = jdbcTemplate.query("""
                SELECT p.phong_tro_id, p.ma_phong, k.ten_khu_tro, k.dia_chi,
                       l.ten_loai_phong, p.dien_tich_m2, p.gia_thue, p.tang, p.mo_ta,
                       (SELECT TOP (1) m.duong_dan
                        FROM dbo.hinh_anh_video_phong m
                        WHERE m.phong_tro_id = p.phong_tro_id
                          AND m.loai_file = N'IMAGE'
                        ORDER BY m.la_anh_dai_dien DESC, m.thu_tu_hien_thi, m.media_id) AS primary_image_url
                FROM dbo.phong_tro p
                JOIN dbo.khu_tro k ON k.khu_tro_id = p.khu_tro_id
                JOIN dbo.loai_phong l ON l.loai_phong_id = p.loai_phong_id
                WHERE p.trang_thai = N'TRONG'
                  AND k.trang_thai = N'HOAT_DONG'
                  AND l.trang_thai = N'HOAT_DONG'
                  AND (? IS NULL OR p.ma_phong LIKE ? ESCAPE N'\\'
                       OR k.ten_khu_tro LIKE ? ESCAPE N'\\'
                       OR k.dia_chi LIKE ? ESCAPE N'\\'
                       OR l.ten_loai_phong LIKE ? ESCAPE N'\\')
                  AND (? IS NULL OR p.gia_thue >= ?)
                  AND (? IS NULL OR p.gia_thue <= ?)
                  AND (? IS NULL OR p.dien_tich_m2 >= ?)
                  AND (? IS NULL OR p.dien_tich_m2 <= ?)
                ORDER BY k.ten_khu_tro, p.ma_phong
                OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """,
                (rs, rowNum) -> new RoomResponse(
                        rs.getLong("phong_tro_id"),
                        rs.getString("ma_phong"),
                        rs.getString("ten_khu_tro"),
                        rs.getString("dia_chi"),
                        rs.getString("ten_loai_phong"),
                        rs.getBigDecimal("dien_tich_m2"),
                        rs.getBigDecimal("gia_thue"),
                        rs.getString("tang"),
                        rs.getString("mo_ta"),
                        rs.getString("primary_image_url")
                ),
                pattern, pattern, pattern, pattern, pattern,
                minPrice, minPrice, maxPrice, maxPrice,
                minArea, minArea, maxArea, maxArea,
                (long) page * size, size
        );
        long elements = total == null ? 0 : total;
        return new PageResponse<>(rooms, page, size, elements,
                elements == 0 ? 0 : (int) ((elements + size - 1) / size));
    }

    public boolean lockAvailableRoom(long roomId) {
        List<Long> matches = jdbcTemplate.query("""
                SELECT phong_tro_id
                FROM dbo.phong_tro WITH (UPDLOCK, HOLDLOCK)
                WHERE phong_tro_id = ? AND trang_thai = N'TRONG'
                """,
                (rs, rowNum) -> rs.getLong(1),
                roomId
        );
        return !matches.isEmpty();
    }

    public boolean lockRoom(long roomId) {
        List<Long> matches = jdbcTemplate.query("""
                SELECT phong_tro_id
                FROM dbo.phong_tro WITH (UPDLOCK, HOLDLOCK)
                WHERE phong_tro_id = ?
                """,
                (rs, rowNum) -> rs.getLong(1),
                roomId
        );
        return !matches.isEmpty();
    }

    public boolean hasActiveLeaseConflict(long roomId, LocalDate checkIn, LocalDate checkOut) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM dbo.hop_dong_thue WITH (UPDLOCK, HOLDLOCK)
                WHERE phong_tro_id = ?
                  AND trang_thai = N'HIEU_LUC'
                  AND (? IS NULL OR ngay_bat_dau < ?)
                  AND (ngay_ket_thuc IS NULL OR ngay_ket_thuc >= ?)
                """,
                Integer.class,
                roomId,
                checkOut == null ? null : Date.valueOf(checkOut),
                checkOut == null ? null : Date.valueOf(checkOut),
                Date.valueOf(checkIn)
        );
        return count != null && count > 0;
    }

    public boolean hasBookingConflict(
            long roomId, LocalDate checkIn, LocalDate checkOut, Long excludedBookingId
    ) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM dbo.dat_phong WITH (UPDLOCK, HOLDLOCK)
                WHERE phong_tro_id = ?
                  AND trang_thai IN (N'CHO_XAC_NHAN', N'DA_XAC_NHAN')
                  AND (? IS NULL OR dat_phong_id <> ?)
                  AND (? IS NULL OR ngay_nhan_phong_du_kien < ?)
                  AND (ngay_tra_phong_du_kien IS NULL OR ngay_tra_phong_du_kien > ?)
                """,
                Integer.class,
                roomId,
                excludedBookingId, excludedBookingId,
                checkOut == null ? null : Date.valueOf(checkOut),
                checkOut == null ? null : Date.valueOf(checkOut),
                Date.valueOf(checkIn)
        );
        return count != null && count > 0;
    }

    public long insertBooking(long userId, BookingRequestValue value) {
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO dbo.dat_phong
                    (phong_tro_id, nguoi_dung_id, ho_ten_nguoi_dat, so_dien_thoai, email,
                     ngay_nhan_phong_du_kien, ngay_tra_phong_du_kien, trang_thai, ghi_chu)
                OUTPUT INSERTED.dat_phong_id
                SELECT ?, u.nguoi_dung_id, u.ho_ten, ?, ?, ?, ?, N'CHO_XAC_NHAN', ?
                FROM dbo.nguoi_dung u
                JOIN dbo.vai_tro r ON r.vai_tro_id = u.vai_tro_id
                WHERE u.nguoi_dung_id = ?
                  AND u.trang_thai = N'HOAT_DONG'
                  AND r.ma_vai_tro = N'NGUOI_THUE'
                """,
                Long.class,
                value.roomId(),
                value.phone(),
                value.email(),
                Date.valueOf(value.checkIn()),
                value.checkOut() == null ? null : Date.valueOf(value.checkOut()),
                value.note(),
                userId
        );
        if (id == null) {
            throw new IllegalStateException("SQL Server did not return the new booking ID.");
        }
        return id;
    }

    public void insertAudit(long userId, String action, long bookingId, String oldStatus, String newStatus) {
        jdbcTemplate.update("""
                INSERT INTO dbo.nhat_ky_he_thong
                    (nguoi_dung_id, hanh_dong, doi_tuong, doi_tuong_id, du_lieu_cu, du_lieu_moi)
                VALUES (?, ?, N'dat_phong', ?, ?, ?)
                """,
                userId,
                action,
                Long.toString(bookingId),
                oldStatus == null ? null : "{\"trangThai\":\"" + oldStatus + "\"}",
                "{\"trangThai\":\"" + newStatus + "\"}"
        );
    }

    public PageResponse<TenantBookingResponse> listTenantBookings(long userId, int page, int size) {
        Long total = jdbcTemplate.queryForObject("""
                SELECT COUNT_BIG(*) FROM dbo.dat_phong WHERE nguoi_dung_id = ?
                """, Long.class, userId);
        List<TenantBookingResponse> rows = jdbcTemplate.query("""
                SELECT b.dat_phong_id, b.phong_tro_id, p.ma_phong, k.ten_khu_tro,
                       b.ngay_nhan_phong_du_kien, b.ngay_tra_phong_du_kien, b.trang_thai,
                       b.ghi_chu, b.created_at, b.updated_at
                FROM dbo.dat_phong b
                JOIN dbo.phong_tro p ON p.phong_tro_id = b.phong_tro_id
                JOIN dbo.khu_tro k ON k.khu_tro_id = p.khu_tro_id
                WHERE b.nguoi_dung_id = ?
                ORDER BY b.created_at DESC, b.dat_phong_id DESC
                OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """,
                (rs, rowNum) -> new TenantBookingResponse(
                        rs.getLong("dat_phong_id"),
                        rs.getLong("phong_tro_id"),
                        rs.getString("ma_phong"),
                        rs.getString("ten_khu_tro"),
                        localDate(rs.getDate("ngay_nhan_phong_du_kien")),
                        localDate(rs.getDate("ngay_tra_phong_du_kien")),
                        rs.getString("trang_thai"),
                        rs.getString("ghi_chu"),
                        instant(rs.getTimestamp("created_at")),
                        instant(rs.getTimestamp("updated_at"))
                ),
                userId, (long) page * size, size
        );
        return pageResponse(rows, total, page, size);
    }

    public PageResponse<AdminBookingResponse> listAdminBookings(
            String status, int page, int size
    ) {
        Long total = jdbcTemplate.queryForObject("""
                SELECT COUNT_BIG(*) FROM dbo.dat_phong WHERE (? IS NULL OR trang_thai = ?)
                """, Long.class, status, status);
        List<AdminBookingResponse> rows = jdbcTemplate.query("""
                SELECT b.dat_phong_id, b.phong_tro_id, p.ma_phong, k.ten_khu_tro,
                       b.ho_ten_nguoi_dat, b.so_dien_thoai, b.email,
                       CASE WHEN premium_plan.ma_goi IS NULL THEN N'THUONG' ELSE N'PREMIUM' END AS ma_goi,
                       b.ngay_nhan_phong_du_kien, b.ngay_tra_phong_du_kien, b.trang_thai,
                       b.ghi_chu, b.created_at, b.updated_at
                FROM dbo.dat_phong b
                JOIN dbo.phong_tro p ON p.phong_tro_id = b.phong_tro_id
                JOIN dbo.khu_tro k ON k.khu_tro_id = p.khu_tro_id
                OUTER APPLY (
                    SELECT MAX(entitlement.ma_goi) AS ma_goi
                    FROM dbo.nguoi_thue tenant
                    JOIN dbo.nguoi_thue_goi entitlement
                      ON entitlement.nguoi_thue_id = tenant.nguoi_thue_id
                    WHERE tenant.nguoi_dung_id = b.nguoi_dung_id
                      AND entitlement.ma_goi = N'PREMIUM'
                      AND entitlement.trang_thai = N'DANG_HOAT_DONG'
                      AND entitlement.ngay_bat_dau <= CONVERT(DATE, SYSUTCDATETIME())
                      AND entitlement.ngay_ket_thuc >= CONVERT(DATE, SYSUTCDATETIME())
                ) premium_plan
                WHERE (? IS NULL OR b.trang_thai = ?)
                ORDER BY CASE WHEN premium_plan.ma_goi IS NULL THEN 1 ELSE 0 END,
                         b.created_at ASC, b.dat_phong_id ASC
                OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """,
                (rs, rowNum) -> new AdminBookingResponse(
                        rs.getLong("dat_phong_id"),
                        rs.getLong("phong_tro_id"),
                        rs.getString("ma_phong"),
                        rs.getString("ten_khu_tro"),
                        rs.getString("ho_ten_nguoi_dat"),
                        rs.getString("so_dien_thoai"),
                        rs.getString("email"),
                        rs.getString("ma_goi"),
                        localDate(rs.getDate("ngay_nhan_phong_du_kien")),
                        localDate(rs.getDate("ngay_tra_phong_du_kien")),
                        rs.getString("trang_thai"),
                        rs.getString("ghi_chu"),
                        instant(rs.getTimestamp("created_at")),
                        instant(rs.getTimestamp("updated_at"))
                ),
                status, status, (long) page * size, size
        );
        return pageResponse(rows, total, page, size);
    }

    public Optional<BookingRecord> findBooking(long bookingId) {
        List<BookingRecord> rows = jdbcTemplate.query("""
                SELECT dat_phong_id, phong_tro_id, ngay_nhan_phong_du_kien,
                       ngay_tra_phong_du_kien, trang_thai
                FROM dbo.dat_phong
                WHERE dat_phong_id = ?
                """,
                (rs, rowNum) -> new BookingRecord(
                        rs.getLong("dat_phong_id"),
                        rs.getLong("phong_tro_id"),
                        localDate(rs.getDate("ngay_nhan_phong_du_kien")),
                        localDate(rs.getDate("ngay_tra_phong_du_kien")),
                        rs.getString("trang_thai")
                ),
                bookingId
        );
        return rows.stream().findFirst();
    }

    public Optional<BookingRecord> lockBooking(long bookingId) {
        List<BookingRecord> rows = jdbcTemplate.query("""
                SELECT dat_phong_id, phong_tro_id, ngay_nhan_phong_du_kien,
                       ngay_tra_phong_du_kien, trang_thai
                FROM dbo.dat_phong WITH (UPDLOCK, HOLDLOCK)
                WHERE dat_phong_id = ?
                """,
                (rs, rowNum) -> new BookingRecord(
                        rs.getLong("dat_phong_id"),
                        rs.getLong("phong_tro_id"),
                        localDate(rs.getDate("ngay_nhan_phong_du_kien")),
                        localDate(rs.getDate("ngay_tra_phong_du_kien")),
                        rs.getString("trang_thai")
                ),
                bookingId
        );
        return rows.stream().findFirst();
    }

    public int updateBookingStatus(long bookingId, String previousStatus, String newStatus) {
        return jdbcTemplate.update("""
                UPDATE dbo.dat_phong
                SET trang_thai = ?, updated_at = SYSUTCDATETIME()
                WHERE dat_phong_id = ? AND trang_thai = ?
                """,
                newStatus, bookingId, previousStatus
        );
    }

    private <T> PageResponse<T> pageResponse(List<T> rows, Long total, int page, int size) {
        long elements = total == null ? 0 : total;
        return new PageResponse<>(rows, page, size, elements,
                elements == 0 ? 0 : (int) ((elements + size - 1) / size));
    }

    private String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%")
                .replace("_", "\\_").replace("[", "\\[");
    }

    private LocalDate localDate(Date value) {
        return value == null ? null : value.toLocalDate();
    }

    private Instant instant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }

    public record BookingRequestValue(
            long roomId,
            String phone,
            String email,
            LocalDate checkIn,
            LocalDate checkOut,
            String note
    ) {
    }

    public record BookingRecord(
            long id,
            long roomId,
            LocalDate checkIn,
            LocalDate checkOut,
            String status
    ) {
    }
}
