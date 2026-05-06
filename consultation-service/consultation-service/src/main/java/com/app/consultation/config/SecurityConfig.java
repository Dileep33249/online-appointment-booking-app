package com.app.consultation.config;

import com.app.consultation.security.JwtFilter;
import com.app.consultation.security.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtUtil jwtUtil) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.POST, "/consultations").hasRole("DOCTOR")
                        .requestMatchers(HttpMethod.PUT, "/consultations/*/start").hasRole("DOCTOR")
                        .requestMatchers(HttpMethod.PUT, "/consultations/*/complete").hasRole("DOCTOR")
                        .requestMatchers(HttpMethod.POST, "/consultations/*/prescription").hasRole("DOCTOR")
                        .requestMatchers(HttpMethod.GET, "/consultations/*").hasAnyRole("DOCTOR", "PATIENT", "ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new JwtFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
