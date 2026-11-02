package com.quanlyphongtro.media;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoomMediaAccessInterceptorTest {

    private final RoomMediaRepository repository = mock(RoomMediaRepository.class);
    private final RoomMediaAccessInterceptor interceptor = new RoomMediaAccessInterceptor(repository);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void publicRoomFileCanBeReadWithoutAuthentication() throws Exception {
        when(repository.roomExists(7)).thenReturn(true);
        when(repository.isRoomPubliclyAvailable(7)).thenReturn(true);

        assertTrue(interceptor.preHandle(request(), new MockHttpServletResponse(), new Object()));
    }

    @Test
    void privateRoomFileCannotBeReadWithoutAuthentication() throws Exception {
        when(repository.roomExists(7)).thenReturn(true);
        when(repository.isRoomPubliclyAvailable(7)).thenReturn(false);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request(), response, new Object()));
        org.junit.jupiter.api.Assertions.assertEquals(403, response.getStatus());
    }

    @Test
    void rejectsPathThatDoesNotMatchGeneratedMediaFileNames() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/media-files/rooms/7/images/../../secret.txt");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));
        org.junit.jupiter.api.Assertions.assertEquals(404, response.getStatus());
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/media-files/rooms/7/images/123e4567-e89b-12d3-a456-426614174000.jpg");
        return request;
    }
}
