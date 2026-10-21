package com.dailydiet.config;

import com.dailydiet.auth.JwtAuthFilter;
import com.dailydiet.common.ResponseResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/auth/**",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/actuator/**"
                ).permitAll()
                // 公共内容：食物库浏览无需登录
                .requestMatchers(HttpMethod.GET, "/api/foods", "/api/foods/**").permitAll()
                .anyRequest().authenticated()
            )
            // 未登录返回 401（而非默认 403），便于前端区分登录态
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, resp, ex) ->
                    writeJson(resp, HttpServletResponse.SC_UNAUTHORIZED, "未登录或登录已过期"))
                .accessDeniedHandler((req, resp, ex) ->
                    writeJson(resp, HttpServletResponse.SC_FORBIDDEN, "无权访问"))
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private void writeJson(HttpServletResponse resp, int status, String msg) throws IOException {
        resp.setStatus(status);
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setContentType(MediaType.APPLICATION_JSON_VALUE);
        resp.getWriter().write(objectMapper.writeValueAsString(ResponseResult.error(status, msg)));
    }
}
