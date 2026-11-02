package com.quanlyphongtro.auth;

import com.quanlyphongtro.auth.AuthModels.AuthenticatedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class BearerTokenFilter extends OncePerRequestFilter {

    private final AuthService authService;

    public BearerTokenFilter(AuthService authService) {
        this.authService = authService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String token = authorization.substring(7).trim();
            authService.authenticate(token).ifPresent(this::setAuthentication);
        }
        filterChain.doFilter(request, response);
    }

    private void setAuthentication(AuthenticatedUser user) {
        var authentication = new UsernamePasswordAuthenticationToken(
                user,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.role()))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
