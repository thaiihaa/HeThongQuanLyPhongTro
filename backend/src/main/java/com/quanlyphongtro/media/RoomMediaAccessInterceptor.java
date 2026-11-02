package com.quanlyphongtro.media;

import com.quanlyphongtro.auth.AuthModels.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RoomMediaAccessInterceptor implements HandlerInterceptor {

    private static final Pattern MEDIA_PATH = Pattern.compile(
            "^/api/v1/media-files/rooms/([1-9][0-9]*)/(images|videos)/"
                    + "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
                    + "\\.(jpg|jpeg|png|webp|mp4|webm)$"
    );

    private final RoomMediaRepository repository;

    public RoomMediaAccessInterceptor(RoomMediaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler
    ) throws IOException {
        String contextPath = request.getContextPath();
        String requestPath = request.getRequestURI().substring(contextPath.length());
        Matcher matcher = MEDIA_PATH.matcher(requestPath);
        if (!matcher.matches()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return false;
        }

        long roomId;
        try {
            roomId = Long.parseLong(matcher.group(1));
        } catch (NumberFormatException exception) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return false;
        }
        if (!repository.roomExists(roomId)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return false;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication == null ? null : authentication.getPrincipal();
        if (principal instanceof AuthenticatedUser user) {
            if ("ADMIN".equals(user.role())
                    || ("NGUOI_THUE".equals(user.role())
                    && (repository.isRoomPubliclyAvailable(roomId)
                    || repository.tenantCanViewRoom(roomId, user.id())))) {
                return true;
            }
        } else if (repository.isRoomPubliclyAvailable(roomId)) {
            return true;
        }

        response.sendError(HttpServletResponse.SC_FORBIDDEN);
        return false;
    }
}
