package com.quanlyphongtro.media;

import com.quanlyphongtro.auth.AuthService;
import com.quanlyphongtro.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RoomMediaController.class)
@Import(SecurityConfig.class)
class RoomMediaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RoomMediaService service;

    @MockBean
    private AuthService authService;

    @Test
    void roomMediaListCanBeRequestedWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/rooms/1/media"))
                .andExpect(status().isOk());
    }

    @Test
    void uploadingMediaRequiresAuthentication() throws Exception {
        mockMvc.perform(multipart("/api/v1/admin/phong-tro/1/media")
                        .file(new MockMultipartFile("files", "room.jpg", "image/jpeg", new byte[]{1})))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "NGUOI_THUE")
    void tenantCannotUploadRoomMedia() throws Exception {
        mockMvc.perform(multipart("/api/v1/admin/phong-tro/1/media")
                        .file(new MockMultipartFile("files", "room.jpg", "image/jpeg", new byte[]{1})))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "NGUOI_THUE")
    void tenantCannotUseAdminMediaList() throws Exception {
        mockMvc.perform(get("/api/v1/admin/phong-tro/1/media"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanAccessMediaUploadEndpoint() throws Exception {
        mockMvc.perform(multipart("/api/v1/admin/phong-tro/1/media")
                        .file(new MockMultipartFile("files", "room.jpg", "image/jpeg", new byte[]{1})))
                .andExpect(status().isCreated());
    }
}
