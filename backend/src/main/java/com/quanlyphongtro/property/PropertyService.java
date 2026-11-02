package com.quanlyphongtro.property;

import com.quanlyphongtro.auth.AuthException;
import com.quanlyphongtro.property.PropertyModels.KhuTroRequest;
import com.quanlyphongtro.property.PropertyModels.KhuTroResponse;
import com.quanlyphongtro.property.PropertyModels.LoaiPhongRequest;
import com.quanlyphongtro.property.PropertyModels.LoaiPhongResponse;
import com.quanlyphongtro.property.PropertyModels.PageResponse;
import com.quanlyphongtro.property.PropertyModels.PhongTroRequest;
import com.quanlyphongtro.property.PropertyModels.PhongTroResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Service
public class PropertyService {

    private final PropertyRepository repository;

    public PropertyService(PropertyRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public PageResponse<KhuTroResponse> listKhuTro(int page, int size, String search) {
        PageRequest request = page(page, size);
        return repository.listKhuTro(request.page(), request.size(), normalizeSearch(search));
    }

    @Transactional(readOnly = true)
    public KhuTroResponse getKhuTro(long id) {
        return repository.findKhuTro(id).orElseThrow(() -> notFound("Không tìm thấy khu trọ."));
    }

    @Transactional
    public KhuTroResponse createKhuTro(KhuTroRequest request) {
        return repository.insertKhuTro(new PropertyRepository.KhuTroRequestValue(
                request.maKhuTro().trim(), request.tenKhuTro().trim(), request.diaChi().trim(),
                normalizeOptional(request.moTa()), status(request.trangThai(), "HOAT_DONG")
        ));
    }

    @Transactional
    public KhuTroResponse updateKhuTro(long id, KhuTroRequest request) {
        return repository.updateKhuTro(id, new PropertyRepository.KhuTroRequestValue(
                request.maKhuTro().trim(), request.tenKhuTro().trim(), request.diaChi().trim(),
                normalizeOptional(request.moTa()), status(request.trangThai(), "HOAT_DONG")
        )).orElseThrow(() -> notFound("Không tìm thấy khu trọ."));
    }

    @Transactional(readOnly = true)
    public PageResponse<LoaiPhongResponse> listLoaiPhong(int page, int size, String search) {
        PageRequest request = page(page, size);
        return repository.listLoaiPhong(request.page(), request.size(), normalizeSearch(search));
    }

    @Transactional(readOnly = true)
    public LoaiPhongResponse getLoaiPhong(int id) {
        return repository.findLoaiPhong(id).orElseThrow(() -> notFound("Không tìm thấy loại phòng."));
    }

    @Transactional
    public LoaiPhongResponse createLoaiPhong(LoaiPhongRequest request) {
        return repository.insertLoaiPhong(toLoaiPhong(request));
    }

    @Transactional
    public LoaiPhongResponse updateLoaiPhong(int id, LoaiPhongRequest request) {
        return repository.updateLoaiPhong(id, toLoaiPhong(request))
                .orElseThrow(() -> notFound("Không tìm thấy loại phòng."));
    }

    @Transactional(readOnly = true)
    public PageResponse<PhongTroResponse> listPhongTro(int page, int size, Long khuTroId, String roomStatus, String search) {
        PageRequest request = page(page, size);
        return repository.listPhongTro(request.page(), request.size(), khuTroId,
                StringUtils.hasText(roomStatus) ? roomStatus : null, normalizeSearch(search));
    }

    @Transactional(readOnly = true)
    public PhongTroResponse getPhongTro(long id) {
        return repository.findPhongTro(id).orElseThrow(() -> notFound("Không tìm thấy phòng trọ."));
    }

    @Transactional
    public PhongTroResponse createPhongTro(PhongTroRequest request) {
        return repository.insertPhongTro(toPhongTro(request));
    }

    @Transactional
    public PhongTroResponse updatePhongTro(long id, PhongTroRequest request) {
        return repository.updatePhongTro(id, toPhongTro(request))
                .orElseThrow(() -> notFound("Không tìm thấy phòng trọ."));
    }

    private PropertyRepository.LoaiPhongRequestValue toLoaiPhong(LoaiPhongRequest request) {
        return new PropertyRepository.LoaiPhongRequestValue(
                request.maLoaiPhong().trim(), request.tenLoaiPhong().trim(), request.dienTichM2(),
                request.giaThueMacDinh(), normalizeOptional(request.moTa()), status(request.trangThai(), "HOAT_DONG")
        );
    }

    private PropertyRepository.PhongTroRequestValue toPhongTro(PhongTroRequest request) {
        return new PropertyRepository.PhongTroRequestValue(
                request.khuTroId(), request.loaiPhongId(), request.maPhong().trim(), normalizeOptional(request.tang()),
                request.dienTichM2(), request.giaThue(), status(request.trangThai(), "TRONG"), normalizeOptional(request.moTa())
        );
    }

    private PageRequest page(int page, int size) {
        int safeSize = Math.max(1, Math.min(size, 100));
        int safePage = Math.min(Math.max(page, 0), Integer.MAX_VALUE / safeSize);
        return new PageRequest(safePage, safeSize);
    }

    private String normalizeSearch(String search) {
        return StringUtils.hasText(search) ? search.trim() : null;
    }

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String status(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : defaultValue;
    }

    private AuthException notFound(String message) {
        return new AuthException(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }

    private record PageRequest(int page, int size) { }
}
