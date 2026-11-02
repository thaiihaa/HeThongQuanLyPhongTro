package com.quanlyphongtro.property;

import com.quanlyphongtro.auth.AuthException;
import com.quanlyphongtro.property.PropertyModels.KhuTroRequest;
import com.quanlyphongtro.property.PropertyModels.KhuTroResponse;
import com.quanlyphongtro.property.PropertyModels.PageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PropertyServiceTest {

    private final PropertyRepository repository = mock(PropertyRepository.class);
    private final PropertyService service = new PropertyService(repository);

    @Test
    void clampsPageSizeAndNormalizesSearch() {
        when(repository.listKhuTro(0, 100, "An Bình")).thenReturn(new PageResponse<>(List.of(), 0, 100, 0, 0));

        PageResponse<KhuTroResponse> response = service.listKhuTro(-4, 500, " An Bình ");

        assertEquals(100, response.size());
        verify(repository).listKhuTro(0, 100, "An Bình");
    }

    @Test
    void createsPropertyWithTrimmedValuesAndDefaultStatus() {
        when(repository.insertKhuTro(any())).thenAnswer(invocation -> {
            PropertyRepository.KhuTroRequestValue value = invocation.getArgument(0);
            assertEquals("KHU-01", value.code());
            assertEquals("Khu trọ A", value.name());
            assertEquals("HOAT_DONG", value.status());
            return new KhuTroResponse(1L, value.code(), value.name(), value.address(), value.description(),
                    value.status(), Instant.EPOCH, Instant.EPOCH);
        });

        KhuTroResponse response = service.createKhuTro(new KhuTroRequest(
                " KHU-01 ", " Khu trọ A ", " 12 Đường X ", " Gần trung tâm ", null
        ));

        assertEquals("Khu trọ A", response.tenKhuTro());
        assertEquals("12 Đường X", response.diaChi());
        verify(repository).insertKhuTro(any());
    }

    @Test
    void updateMissingPropertyReturnsNotFound() {
        when(repository.updateKhuTro(org.mockito.ArgumentMatchers.eq(99L), any())).thenReturn(Optional.empty());

        AuthException exception = assertThrows(AuthException.class, () ->
                service.updateKhuTro(99L, new KhuTroRequest("K-99", "Khu trọ", "Địa chỉ", null, null)));

        assertEquals(HttpStatus.NOT_FOUND, exception.status());
    }
}
