package com.quanlyphongtro.property;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class PropertyModels {

    private PropertyModels() {
    }

    public record KhuTroRequest(
            @NotBlank @Size(max = 30) String maKhuTro,
            @NotBlank @Size(max = 150) String tenKhuTro,
            @NotBlank @Size(max = 500) String diaChi,
            @Size(max = 1000) String moTa,
            @Pattern(regexp = "HOAT_DONG|NGUNG_HOAT_DONG") String trangThai
    ) {
    }

    public record KhuTroResponse(
            long id,
            String maKhuTro,
            String tenKhuTro,
            String diaChi,
            String moTa,
            String trangThai,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record LoaiPhongRequest(
            @NotBlank @Size(max = 30) String maLoaiPhong,
            @NotBlank @Size(max = 120) String tenLoaiPhong,
            @NotNull @Positive BigDecimal dienTichM2,
            @NotNull @DecimalMin("0.00") BigDecimal giaThueMacDinh,
            @Size(max = 1000) String moTa,
            @Pattern(regexp = "HOAT_DONG|NGUNG_HOAT_DONG") String trangThai
    ) {
    }

    public record LoaiPhongResponse(
            int id,
            String maLoaiPhong,
            String tenLoaiPhong,
            BigDecimal dienTichM2,
            BigDecimal giaThueMacDinh,
            String moTa,
            String trangThai,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record PhongTroRequest(
            @Positive long khuTroId,
            @Positive int loaiPhongId,
            @NotBlank @Size(max = 30) String maPhong,
            @Size(max = 20) String tang,
            @NotNull @Positive BigDecimal dienTichM2,
            @NotNull @DecimalMin("0.00") BigDecimal giaThue,
            @Pattern(regexp = "TRONG|DANG_THUE|DANG_SUA_CHUA|KHONG_KHA_DUNG") String trangThai,
            @Size(max = 1000) String moTa
    ) {
    }

    public record PhongTroResponse(
            long id,
            long khuTroId,
            String tenKhuTro,
            int loaiPhongId,
            String tenLoaiPhong,
            String maPhong,
            String tang,
            BigDecimal dienTichM2,
            BigDecimal giaThue,
            String trangThai,
            String moTa,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    }
}
