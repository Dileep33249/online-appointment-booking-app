package com.app.appointment.config;

import com.app.appointment.security.JwtFilter;
import com.app.appointment.security.JwtUtil;
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
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/slots/public").permitAll()
                        .requestMatchers(HttpMethod.GET, "/slots/provider/*").permitAll()
                        .requestMatchers("/appointments/*/internal/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/slots").hasRole("DOCTOR")
                        .requestMatchers(HttpMethod.GET, "/slots/my").hasRole("DOCTOR")
                        .requestMatchers(HttpMethod.POST, "/appointments/book/*").hasRole("PATIENT")
                        .requestMatchers(HttpMethod.PUT, "/appointments/*/cancel").hasRole("PATIENT")
                        .requestMatchers(HttpMethod.PUT, "/appointments/*/reschedule").hasRole("PATIENT")
                        .requestMatchers(HttpMethod.GET, "/appointments/my").hasRole("PATIENT")
                        .requestMatchers(HttpMethod.GET, "/appointments/provider").hasRole("DOCTOR")
                        .requestMatchers(HttpMethod.PUT, "/appointments/*/complete").hasRole("DOCTOR")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new JwtFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
