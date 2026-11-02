package com.quanlyphongtro.media;

import com.quanlyphongtro.media.RoomMediaModels.RoomMediaResponse;
import com.quanlyphongtro.media.RoomMediaStorage.StoredFile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class RoomMediaRepository {

    private static final String TABLE = "dbo.hinh_anh_video_phong";
    private static final RowMapper<RoomMediaResponse> MAPPER = (rs, row) -> new RoomMediaResponse(
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
    );

    private final JdbcTemplate jdbcTemplate;

    public RoomMediaRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean roomExists(long roomId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dbo.phong_tro WHERE phong_tro_id = ?", Integer.class, roomId);
        return count != null && count > 0;
    }

    public boolean isRoomPubliclyAvailable(long roomId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM dbo.phong_tro p
                JOIN dbo.khu_tro k ON k.khu_tro_id = p.khu_tro_id
                JOIN dbo.loai_phong l ON l.loai_phong_id = p.loai_phong_id
                WHERE p.phong_tro_id = ?
                  AND p.trang_thai = N'TRONG'
                  AND k.trang_thai = N'HOAT_DONG'
                  AND l.trang_thai = N'HOAT_DONG'
                """, Integer.class, roomId);
        return count != null && count > 0;
    }

    public boolean tenantCanViewRoom(long roomId, long userId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM dbo.dat_phong
                WHERE phong_tro_id = ?
                  AND nguoi_dung_id = ?
                  AND trang_thai IN (N'CHO_XAC_NHAN', N'DA_XAC_NHAN', N'HOAN_TAT')
                """, Integer.class, roomId, userId);
        return count != null && count > 0;
    }

    public List<RoomMediaResponse> list(long roomId) {
        return jdbcTemplate.query("""
                SELECT media_id, phong_tro_id, loai_file, ten_file_goc, ten_file_luu, duong_dan,
                       mime_type, kich_thuoc_bytes, thu_tu_hien_thi, la_anh_dai_dien, mo_ta,
                       ngay_tai_len, nguoi_tai_len_id
                FROM dbo.hinh_anh_video_phong
                WHERE phong_tro_id = ?
                ORDER BY thu_tu_hien_thi, media_id
                """, MAPPER, roomId);
    }

    public RoomMediaResponse insert(
            long roomId, String mediaType, String originalName, StoredFile storedFile, String mimeType,
            long sizeBytes, Long uploaderId
    ) {
        Integer order = jdbcTemplate.queryForObject("""
                SELECT COALESCE(MAX(thu_tu_hien_thi) + 1, 0)
                FROM dbo.hinh_anh_video_phong WITH (UPDLOCK, HOLDLOCK)
                WHERE phong_tro_id = ?
                """, Integer.class, roomId);
        return jdbcTemplate.queryForObject("""
                INSERT INTO dbo.hinh_anh_video_phong
                    (phong_tro_id, loai_file, ten_file_goc, ten_file_luu, duong_dan, mime_type,
                     kich_thuoc_bytes, thu_tu_hien_thi, nguoi_tai_len_id)
                OUTPUT INSERTED.media_id, INSERTED.phong_tro_id, INSERTED.loai_file,
                       INSERTED.ten_file_goc, INSERTED.ten_file_luu, INSERTED.duong_dan,
                       INSERTED.mime_type, INSERTED.kich_thuoc_bytes, INSERTED.thu_tu_hien_thi,
                       INSERTED.la_anh_dai_dien, INSERTED.mo_ta, INSERTED.ngay_tai_len,
                       INSERTED.nguoi_tai_len_id
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, MAPPER, roomId, mediaType, originalName, storedFile.fileName(), storedFile.url(),
                mimeType, sizeBytes, order == null ? 0 : order, uploaderId);
    }

    public Optional<RoomMediaResponse> find(long roomId, long mediaId) {
        List<RoomMediaResponse> results = jdbcTemplate.query("""
                SELECT media_id, phong_tro_id, loai_file, ten_file_goc, ten_file_luu, duong_dan,
                       mime_type, kich_thuoc_bytes, thu_tu_hien_thi, la_anh_dai_dien, mo_ta,
                       ngay_tai_len, nguoi_tai_len_id
                FROM dbo.hinh_anh_video_phong
                WHERE phong_tro_id = ? AND media_id = ?
                """, MAPPER, roomId, mediaId);
        return results.stream().findFirst();
    }

    public boolean setPrimaryImage(long roomId, long mediaId) {
        Optional<RoomMediaResponse> item = find(roomId, mediaId);
        if (item.isEmpty() || !"IMAGE".equals(item.get().mediaType())) {
            return false;
        }
        jdbcTemplate.update("""
                UPDATE dbo.hinh_anh_video_phong
                SET la_anh_dai_dien = 0
                WHERE phong_tro_id = ? AND la_anh_dai_dien = 1
                """, roomId);
        jdbcTemplate.update("""
                UPDATE dbo.hinh_anh_video_phong
                SET la_anh_dai_dien = 1
                WHERE phong_tro_id = ? AND media_id = ?
                """, roomId, mediaId);
        return true;
    }

    public boolean updateOrder(long roomId, long mediaId, int displayOrder) {
        List<Integer> currentOrders = jdbcTemplate.query("""
                SELECT thu_tu_hien_thi
                FROM dbo.hinh_anh_video_phong
                WHERE phong_tro_id = ? AND media_id = ?
                """, (rs, row) -> rs.getInt(1), roomId, mediaId);
        if (currentOrders.isEmpty()) {
            return false;
        }
        int currentOrder = currentOrders.get(0);
        if (currentOrder == displayOrder) {
            return true;
        }
        List<Long> adjacentMedia = jdbcTemplate.query("""
                SELECT TOP (1) media_id
                FROM dbo.hinh_anh_video_phong
                WHERE phong_tro_id = ? AND thu_tu_hien_thi = ? AND media_id <> ?
                ORDER BY media_id
                """, (rs, row) -> rs.getLong(1), roomId, displayOrder, mediaId);
        if (!adjacentMedia.isEmpty()) {
            jdbcTemplate.update("""
                    UPDATE dbo.hinh_anh_video_phong
                    SET thu_tu_hien_thi = ?
                    WHERE phong_tro_id = ? AND media_id = ?
                    """, currentOrder, roomId, adjacentMedia.get(0));
        }
        return jdbcTemplate.update("""
                UPDATE dbo.hinh_anh_video_phong
                SET thu_tu_hien_thi = ?
                WHERE phong_tro_id = ? AND media_id = ?
                """, displayOrder, roomId, mediaId) == 1;
    }

    public boolean updateDescription(long roomId, long mediaId, String description) {
        return jdbcTemplate.update("""
                UPDATE dbo.hinh_anh_video_phong
                SET mo_ta = ?
                WHERE phong_tro_id = ? AND media_id = ?
                """, description, roomId, mediaId) == 1;
    }

    public boolean delete(long roomId, long mediaId) {
        return jdbcTemplate.update("""
                DELETE FROM dbo.hinh_anh_video_phong
                WHERE phong_tro_id = ? AND media_id = ?
                """, roomId, mediaId) == 1;
    }

    private static Instant instant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private static Long nullableLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }
}
