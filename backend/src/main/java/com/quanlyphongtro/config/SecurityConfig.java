package com.quanlyphongtro.config;

import com.quanlyphongtro.auth.BearerTokenFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, BearerTokenFilter bearerTokenFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/v1/health/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/rooms").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/rooms/*/media").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/media-files/rooms/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/api-docs/**"
                        ).permitAll()
                            .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                            .requestMatchers("/api/v1/tenant/**").hasRole("NGUOI_THUE")
                        .anyRequest().authenticated()
                        )
                        .exceptionHandling(exceptions -> exceptions
                            .authenticationEntryPoint((request, response, exception) -> {
                                response.setStatus(401);
                                response.setContentType("application/json;charset=UTF-8");
                                response.getWriter().write("{\"code\":\"UNAUTHENTICATED\",\"message\":\"Vui lòng đăng nhập.\"}");
                            })
                            .accessDeniedHandler((request, response, exception) -> {
                                response.setStatus(403);
                                response.setContentType("application/json;charset=UTF-8");
                                response.getWriter().write("{\"code\":\"FORBIDDEN\",\"message\":\"Bạn không có quyền thực hiện thao tác này.\"}");
                            })
                        )
                        .addFilterBefore(bearerTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("Password-based Spring Security authentication is not configured.");
        };
    }
}
