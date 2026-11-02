package com.quanlyphongtro.tenant;

import com.quanlyphongtro.media.RoomMediaModels.RoomMediaResponse;
import com.quanlyphongtro.tenant.TenantRoomModels.RoomAmenityResponse;
import com.quanlyphongtro.tenant.TenantRoomModels.RoomAssetResponse;
import com.quanlyphongtro.tenant.TenantRoomModels.RoomServiceResponse;
import com.quanlyphongtro.tenant.TenantRoomModels.TenantRoomResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class TenantRoomRepository {

    private final JdbcTemplate jdbcTemplate;

    public TenantRoomRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<TenantRoomResponse> findMyRoom(long userId) {
        List<ActiveLeaseRoom> activeRooms = jdbcTemplate.query("""
                SELECT TOP (1) h.hop_dong_thue_id, p.phong_tro_id, p.ma_phong,
                       k.ten_khu_tro, k.dia_chi, p.dien_tich_m2, p.gia_thue, p.trang_thai, p.mo_ta,
                       h.ngay_bat_dau, h.ngay_ket_thuc
                FROM dbo.nguoi_dung u
                JOIN dbo.vai_tro r ON r.vai_tro_id = u.vai_tro_id
                JOIN dbo.nguoi_thue nt ON nt.nguoi_dung_id = u.nguoi_dung_id
                JOIN dbo.hop_dong_nguoi_thue hn ON hn.nguoi_thue_id = nt.nguoi_thue_id
                JOIN dbo.hop_dong_thue h ON h.hop_dong_thue_id = hn.hop_dong_thue_id
                JOIN dbo.phong_tro p ON p.phong_tro_id = h.phong_tro_id
                JOIN dbo.khu_tro k ON k.khu_tro_id = p.khu_tro_id
                WHERE u.nguoi_dung_id = ?
                  AND u.trang_thai = N'HOAT_DONG'
                  AND r.ma_vai_tro = N'NGUOI_THUE'
                  AND h.trang_thai = N'HIEU_LUC'
                  AND h.ngay_bat_dau <= CONVERT(DATE, GETDATE())
                  AND (h.ngay_ket_thuc IS NULL OR h.ngay_ket_thuc >= CONVERT(DATE, GETDATE()))
                  AND (hn.ngay_roi_di IS NULL OR hn.ngay_roi_di >= CONVERT(DATE, GETDATE()))
                ORDER BY h.ngay_bat_dau DESC, h.updated_at DESC, h.hop_dong_thue_id DESC
                """,
                (rs, rowNum) -> new ActiveLeaseRoom(
                        rs.getLong("hop_dong_thue_id"),
                        rs.getLong("phong_tro_id"),
                        rs.getString("ma_phong"),
                        rs.getString("ten_khu_tro"),
                        rs.getString("dia_chi"),
                        rs.getBigDecimal("dien_tich_m2"),
                        rs.getBigDecimal("gia_thue"),
                        rs.getString("trang_thai"),
                        rs.getString("mo_ta"),
                        localDate(rs.getDate("ngay_bat_dau")),
                        localDate(rs.getDate("ngay_ket_thuc"))
                ),
                userId
        );

        if (activeRooms.isEmpty()) {
            return Optional.empty();
        }

        ActiveLeaseRoom room = activeRooms.get(0);
        return Optional.of(new TenantRoomResponse(
                room.roomId(),
                room.roomCode(),
                room.siteName(),
                room.address(),
                room.areaSquareMeters(),
                room.monthlyRent(),
                room.status(),
                room.description(),
                room.leaseStartsOn(),
                room.leaseEndsOn(),
                listAmenities(room.roomId()),
                listAssets(room.roomId()),
                listServices(room.contractId()),
                listMedia(room.roomId())
        ));
    }

    private List<RoomAmenityResponse> listAmenities(long roomId) {
        return jdbcTemplate.query("""
                SELECT d.ten_tien_nghi, tp.so_luong, tp.tinh_trang
                FROM dbo.tien_nghi_phong tp
                JOIN dbo.danh_muc_tien_nghi d
                  ON d.danh_muc_tien_nghi_id = tp.danh_muc_tien_nghi_id
                WHERE tp.phong_tro_id = ?
                ORDER BY d.ten_tien_nghi
                """,
                (rs, rowNum) -> new RoomAmenityResponse(
                        rs.getString("ten_tien_nghi"),
                        rs.getInt("so_luong"),
                        rs.getString("tinh_trang")
                ),
                roomId
        );
    }

    private List<RoomAssetResponse> listAssets(long roomId) {
        return jdbcTemplate.query("""
                SELECT ten_tai_san, so_luong, tinh_trang
                FROM dbo.tai_san_phong
                WHERE phong_tro_id = ?
                ORDER BY ten_tai_san
                """,
                (rs, rowNum) -> new RoomAssetResponse(
                        rs.getString("ten_tai_san"),
                        rs.getInt("so_luong"),
                        rs.getString("tinh_trang")
                ),
                roomId
        );
    }

    private List<RoomServiceResponse> listServices(long contractId) {
        return jdbcTemplate.query("""
                SELECT d.ten_dich_vu, d.don_vi_tinh, dhd.don_gia_ap_dung, d.cach_tinh
                FROM dbo.dich_vu_hop_dong dhd
                JOIN dbo.dich_vu d ON d.dich_vu_id = dhd.dich_vu_id
                WHERE dhd.hop_dong_thue_id = ?
                  AND dhd.ngay_bat_dau <= CONVERT(DATE, GETDATE())
                  AND (dhd.ngay_ket_thuc IS NULL OR dhd.ngay_ket_thuc >= CONVERT(DATE, GETDATE()))
                ORDER BY d.ten_dich_vu
                """,
                (rs, rowNum) -> new RoomServiceResponse(
                        rs.getString("ten_dich_vu"),
                        rs.getString("don_vi_tinh"),
                        rs.getBigDecimal("don_gia_ap_dung"),
                        rs.getString("cach_tinh")
                ),
                contractId
        );
    }

    private List<RoomMediaResponse> listMedia(long roomId) {
        return jdbcTemplate.query("""
                SELECT media_id, phong_tro_id, loai_file, ten_file_goc, ten_file_luu, duong_dan,
                       mime_type, kich_thuoc_bytes, thu_tu_hien_thi, la_anh_dai_dien, mo_ta,
                       ngay_tai_len, nguoi_tai_len_id
                FROM dbo.hinh_anh_video_phong
                WHERE phong_tro_id = ?
                ORDER BY thu_tu_hien_thi, media_id
                """,
                (rs, rowNum) -> new RoomMediaResponse(
                        rs.getLong("media_id"),
                        rs.getLong("phong_tro_id"),
                        rs.getString("loai_file"),
                        rs.getString("ten_file_goc"),
                        rs.getString("ten_file_luu"),
                        rs.getString("duong_dan"),
                        rs.getString("mime_type"),
                        rs.getLong("kich_thuoc_bytes"),
                        rs.getInt("thu_tu_hien_thi"),
                        rs.getBoolean("la_anh_dai_dien"),
                        rs.getString("mo_ta"),
                        instant(rs.getTimestamp("ngay_tai_len")),
                        nullableLong(rs.getObject("nguoi_tai_len_id"))
                ),
                roomId
        );
    }

    private static LocalDate localDate(Date value) {
        return value == null ? null : value.toLocalDate();
    }

    private static Instant instant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }

    private static Long nullableLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private record ActiveLeaseRoom(
            long contractId,
            long roomId,
            String roomCode,
            String siteName,
            String address,
            java.math.BigDecimal areaSquareMeters,
            java.math.BigDecimal monthlyRent,
            String status,
            String description,
            LocalDate leaseStartsOn,
            LocalDate leaseEndsOn
    ) {
    }
}
