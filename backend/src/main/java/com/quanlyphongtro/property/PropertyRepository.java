package com.quanlyphongtro.property;

import com.quanlyphongtro.property.PropertyModels.KhuTroResponse;
import com.quanlyphongtro.property.PropertyModels.LoaiPhongResponse;
import com.quanlyphongtro.property.PropertyModels.PageResponse;
import com.quanlyphongtro.property.PropertyModels.PhongTroResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class PropertyRepository {

    private static final RowMapper<KhuTroResponse> KHU_TRO_MAPPER = (rs, row) -> new KhuTroResponse(
            rs.getLong("khu_tro_id"), rs.getString("ma_khu_tro"), rs.getString("ten_khu_tro"),
            rs.getString("dia_chi"), rs.getString("mo_ta"), rs.getString("trang_thai"),
            instant(rs.getTimestamp("created_at")), instant(rs.getTimestamp("updated_at"))
    );
    private static final RowMapper<LoaiPhongResponse> LOAI_PHONG_MAPPER = (rs, row) -> new LoaiPhongResponse(
            rs.getInt("loai_phong_id"), rs.getString("ma_loai_phong"), rs.getString("ten_loai_phong"),
            rs.getBigDecimal("dien_tich_m2"), rs.getBigDecimal("gia_thue_mac_dinh"), rs.getString("mo_ta"),
            rs.getString("trang_thai"), instant(rs.getTimestamp("created_at")), instant(rs.getTimestamp("updated_at"))
    );
    private static final RowMapper<PhongTroResponse> PHONG_TRO_MAPPER = (rs, row) -> new PhongTroResponse(
            rs.getLong("phong_tro_id"), rs.getLong("khu_tro_id"), rs.getString("ten_khu_tro"),
            rs.getInt("loai_phong_id"), rs.getString("ten_loai_phong"), rs.getString("ma_phong"),
            rs.getString("tang"), rs.getBigDecimal("dien_tich_m2"), rs.getBigDecimal("gia_thue"),
            rs.getString("trang_thai"), rs.getString("mo_ta"),
            instant(rs.getTimestamp("created_at")), instant(rs.getTimestamp("updated_at"))
    );

    private final JdbcTemplate jdbcTemplate;

    public PropertyRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public PageResponse<KhuTroResponse> listKhuTro(int page, int size, String search) {
        boolean filter = StringUtils.hasText(search);
        String where = filter ? " WHERE ten_khu_tro LIKE ? OR ma_khu_tro LIKE ?" : "";
        String pattern = filter ? "%" + search.trim() + "%" : null;
        Long total = filter
                ? jdbcTemplate.queryForObject("SELECT COUNT_BIG(*) FROM dbo.khu_tro" + where, Long.class, pattern, pattern)
                : jdbcTemplate.queryForObject("SELECT COUNT_BIG(*) FROM dbo.khu_tro", Long.class);
        List<KhuTroResponse> rows = filter
                ? jdbcTemplate.query("SELECT * FROM dbo.khu_tro" + where + " ORDER BY khu_tro_id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",
                        KHU_TRO_MAPPER, pattern, pattern, page * size, size)
                : jdbcTemplate.query("SELECT * FROM dbo.khu_tro ORDER BY khu_tro_id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",
                        KHU_TRO_MAPPER, page * size, size);
        return page(rows, page, size, total);
    }

    public Optional<KhuTroResponse> findKhuTro(long id) {
        return one("SELECT * FROM dbo.khu_tro WHERE khu_tro_id = ?", KHU_TRO_MAPPER, id);
    }

    public KhuTroResponse insertKhuTro(KhuTroRequestValue value) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO dbo.khu_tro (ma_khu_tro,ten_khu_tro,dia_chi,mo_ta,trang_thai)
                OUTPUT INSERTED.khu_tro_id,INSERTED.ma_khu_tro,INSERTED.ten_khu_tro,INSERTED.dia_chi,
                       INSERTED.mo_ta,INSERTED.trang_thai,INSERTED.created_at,INSERTED.updated_at
                VALUES (?,?,?,?,?)
                """, KHU_TRO_MAPPER,
                value.code(), value.name(), value.address(), value.description(), value.status());
    }

    public Optional<KhuTroResponse> updateKhuTro(long id, KhuTroRequestValue value) {
        int changed = jdbcTemplate.update("""
                UPDATE dbo.khu_tro SET ma_khu_tro=?,ten_khu_tro=?,dia_chi=?,mo_ta=?,trang_thai=?,updated_at=SYSUTCDATETIME()
                WHERE khu_tro_id=?
                """, value.code(), value.name(), value.address(), value.description(), value.status(), id);
        return changed == 0 ? Optional.empty() : findKhuTro(id);
    }

    public PageResponse<LoaiPhongResponse> listLoaiPhong(int page, int size, String search) {
        boolean filter = StringUtils.hasText(search);
        String where = filter ? " WHERE ten_loai_phong LIKE ? OR ma_loai_phong LIKE ?" : "";
        String pattern = filter ? "%" + search.trim() + "%" : null;
        Long total = filter
                ? jdbcTemplate.queryForObject("SELECT COUNT_BIG(*) FROM dbo.loai_phong" + where, Long.class, pattern, pattern)
                : jdbcTemplate.queryForObject("SELECT COUNT_BIG(*) FROM dbo.loai_phong", Long.class);
        List<LoaiPhongResponse> rows = filter
                ? jdbcTemplate.query("SELECT * FROM dbo.loai_phong" + where + " ORDER BY loai_phong_id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",
                        LOAI_PHONG_MAPPER, pattern, pattern, page * size, size)
                : jdbcTemplate.query("SELECT * FROM dbo.loai_phong ORDER BY loai_phong_id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",
                        LOAI_PHONG_MAPPER, page * size, size);
        return page(rows, page, size, total);
    }

    public Optional<LoaiPhongResponse> findLoaiPhong(int id) {
        return one("SELECT * FROM dbo.loai_phong WHERE loai_phong_id = ?", LOAI_PHONG_MAPPER, id);
    }

    public LoaiPhongResponse insertLoaiPhong(LoaiPhongRequestValue value) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO dbo.loai_phong (ma_loai_phong,ten_loai_phong,dien_tich_m2,gia_thue_mac_dinh,mo_ta,trang_thai)
                OUTPUT INSERTED.loai_phong_id,INSERTED.ma_loai_phong,INSERTED.ten_loai_phong,INSERTED.dien_tich_m2,
                       INSERTED.gia_thue_mac_dinh,INSERTED.mo_ta,INSERTED.trang_thai,INSERTED.created_at,INSERTED.updated_at
                VALUES (?,?,?,?,?,?)
                """, LOAI_PHONG_MAPPER, value.code(), value.name(), value.area(), value.price(), value.description(), value.status());
    }

    public Optional<LoaiPhongResponse> updateLoaiPhong(int id, LoaiPhongRequestValue value) {
        int changed = jdbcTemplate.update("""
                UPDATE dbo.loai_phong SET ma_loai_phong=?,ten_loai_phong=?,dien_tich_m2=?,gia_thue_mac_dinh=?,mo_ta=?,trang_thai=?,updated_at=SYSUTCDATETIME()
                WHERE loai_phong_id=?
                """, value.code(), value.name(), value.area(), value.price(), value.description(), value.status(), id);
        return changed == 0 ? Optional.empty() : findLoaiPhong(id);
    }

    public PageResponse<PhongTroResponse> listPhongTro(int page, int size, Long khuTroId, String status, String search) {
        List<String> filters = new ArrayList<>();
        List<Object> filterArgs = new ArrayList<>();
        if (khuTroId != null) { filters.add("p.khu_tro_id = ?"); filterArgs.add(khuTroId); }
        if (StringUtils.hasText(status)) { filters.add("p.trang_thai = ?"); filterArgs.add(status); }
        if (StringUtils.hasText(search)) {
            filters.add("(p.ma_phong LIKE ? OR k.ten_khu_tro LIKE ? OR lp.ten_loai_phong LIKE ?)");
            String pattern = "%" + search.trim() + "%";
            filterArgs.add(pattern); filterArgs.add(pattern); filterArgs.add(pattern);
        }
        String joins = " FROM dbo.phong_tro p JOIN dbo.khu_tro k ON k.khu_tro_id=p.khu_tro_id JOIN dbo.loai_phong lp ON lp.loai_phong_id=p.loai_phong_id";
        String where = filters.isEmpty() ? "" : " WHERE " + String.join(" AND ", filters);
                String select = "SELECT p.phong_tro_id,p.khu_tro_id,k.ten_khu_tro,p.loai_phong_id,lp.ten_loai_phong,p.ma_phong,p.tang,p.dien_tich_m2,p.gia_thue,p.trang_thai,p.mo_ta,p.created_at,p.updated_at";
                List<Object> countArgs = new ArrayList<>(filterArgs);
                Long total = jdbcTemplate.queryForObject("SELECT COUNT_BIG(*)" + joins + where, Long.class, countArgs.toArray());
                List<Object> listArgs = new ArrayList<>(filterArgs);
                listArgs.add(page * size);
                listArgs.add(size);
                List<PhongTroResponse> rows = jdbcTemplate.query(select + joins + where + " ORDER BY p.phong_tro_id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",
                                PHONG_TRO_MAPPER, listArgs.toArray());
                return page(rows, page, size, total);
        }

        public Optional<PhongTroResponse> findPhongTro(long id) {
                return one("""
                                SELECT p.phong_tro_id,p.khu_tro_id,k.ten_khu_tro,p.loai_phong_id,lp.ten_loai_phong,p.ma_phong,p.tang,
                                           p.dien_tich_m2,p.gia_thue,p.trang_thai,p.mo_ta,p.created_at,p.updated_at
                                FROM dbo.phong_tro p JOIN dbo.khu_tro k ON k.khu_tro_id=p.khu_tro_id
                                JOIN dbo.loai_phong lp ON lp.loai_phong_id=p.loai_phong_id
                                WHERE p.phong_tro_id=?
                                """, PHONG_TRO_MAPPER, id);
        }

        public PhongTroResponse insertPhongTro(PhongTroRequestValue value) {
                long id = jdbcTemplate.queryForObject("""
                                INSERT INTO dbo.phong_tro (khu_tro_id,loai_phong_id,ma_phong,tang,dien_tich_m2,gia_thue,trang_thai,mo_ta)
                                OUTPUT INSERTED.phong_tro_id
                                VALUES (?,?,?,?,?,?,?,?)
                                """, Long.class, value.khuTroId(), value.loaiPhongId(), value.code(), value.floor(),
                                value.area(), value.price(), value.status(), value.description());
                return findPhongTro(id).orElseThrow();
        }

        public Optional<PhongTroResponse> updatePhongTro(long id, PhongTroRequestValue value) {
                int changed = jdbcTemplate.update("""
                                UPDATE dbo.phong_tro SET khu_tro_id=?,loai_phong_id=?,ma_phong=?,tang=?,dien_tich_m2=?,gia_thue=?,
                                        trang_thai=?,mo_ta=?,updated_at=SYSUTCDATETIME()
                                WHERE phong_tro_id=?
                                """, value.khuTroId(), value.loaiPhongId(), value.code(), value.floor(), value.area(),
                                value.price(), value.status(), value.description(), id);
                return changed == 0 ? Optional.empty() : findPhongTro(id);
        }

        private <T> Optional<T> one(String sql, RowMapper<T> mapper, Object... args) {
                List<T> rows = jdbcTemplate.query(sql, mapper, args);
                return rows.stream().findFirst();
        }

        private static <T> PageResponse<T> page(List<T> rows, int page, int size, Long count) {
                long total = count == null ? 0 : count;
                int pages = total == 0 ? 0 : (int) Math.ceil((double) total / size);
                return new PageResponse<>(rows, page, size, total, pages);
        }

        private static Instant instant(Timestamp timestamp) {
                return timestamp == null ? null : timestamp.toInstant();
        }

        public record KhuTroRequestValue(String code, String name, String address, String description, String status) { }
        public record LoaiPhongRequestValue(String code, String name, java.math.BigDecimal area, java.math.BigDecimal price, String description, String status) { }
        public record PhongTroRequestValue(long khuTroId, int loaiPhongId, String code, String floor, java.math.BigDecimal area, java.math.BigDecimal price, String status, String description) { }
}