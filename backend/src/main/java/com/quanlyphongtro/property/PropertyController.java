package com.quanlyphongtro.property;

import com.quanlyphongtro.property.PropertyModels.KhuTroRequest;
import com.quanlyphongtro.property.PropertyModels.KhuTroResponse;
import com.quanlyphongtro.property.PropertyModels.LoaiPhongRequest;
import com.quanlyphongtro.property.PropertyModels.LoaiPhongResponse;
import com.quanlyphongtro.property.PropertyModels.PageResponse;
import com.quanlyphongtro.property.PropertyModels.PhongTroRequest;
import com.quanlyphongtro.property.PropertyModels.PhongTroResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin")
public class PropertyController {

    private final PropertyService propertyService;

    public PropertyController(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    @GetMapping("/khu-tro")
    public PageResponse<KhuTroResponse> listKhuTro(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search
    ) {
        return propertyService.listKhuTro(page, size, search);
    }

    @GetMapping("/khu-tro/{id}")
    public KhuTroResponse getKhuTro(@PathVariable long id) {
        return propertyService.getKhuTro(id);
    }

    @PostMapping("/khu-tro")
    @ResponseStatus(HttpStatus.CREATED)
    public KhuTroResponse createKhuTro(@Valid @RequestBody KhuTroRequest request) {
        return propertyService.createKhuTro(request);
    }

    @PutMapping("/khu-tro/{id}")
    public KhuTroResponse updateKhuTro(@PathVariable long id, @Valid @RequestBody KhuTroRequest request) {
        return propertyService.updateKhuTro(id, request);
    }

    @GetMapping("/loai-phong")
    public PageResponse<LoaiPhongResponse> listLoaiPhong(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search
    ) {
        return propertyService.listLoaiPhong(page, size, search);
    }

    @GetMapping("/loai-phong/{id}")
    public LoaiPhongResponse getLoaiPhong(@PathVariable int id) {
        return propertyService.getLoaiPhong(id);
    }

    @PostMapping("/loai-phong")
    @ResponseStatus(HttpStatus.CREATED)
    public LoaiPhongResponse createLoaiPhong(@Valid @RequestBody LoaiPhongRequest request) {
        return propertyService.createLoaiPhong(request);
    }

    @PutMapping("/loai-phong/{id}")
    public LoaiPhongResponse updateLoaiPhong(@PathVariable int id, @Valid @RequestBody LoaiPhongRequest request) {
        return propertyService.updateLoaiPhong(id, request);
    }

    @GetMapping("/phong-tro")
    public PageResponse<PhongTroResponse> listPhongTro(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long khuTroId,
            @RequestParam(required = false)
            @Pattern(regexp = "TRONG|DANG_THUE|DANG_SUA_CHUA|KHONG_KHA_DUNG") String status,
            @RequestParam(required = false) String search
    ) {
        return propertyService.listPhongTro(page, size, khuTroId, status, search);
    }

    @GetMapping("/phong-tro/{id}")
    public PhongTroResponse getPhongTro(@PathVariable long id) {
        return propertyService.getPhongTro(id);
    }

    @PostMapping("/phong-tro")
    @ResponseStatus(HttpStatus.CREATED)
    public PhongTroResponse createPhongTro(@Valid @RequestBody PhongTroRequest request) {
        return propertyService.createPhongTro(request);
    }

    @PutMapping("/phong-tro/{id}")
    public PhongTroResponse updatePhongTro(@PathVariable long id, @Valid @RequestBody PhongTroRequest request) {
        return propertyService.updatePhongTro(id, request);
    }
}
