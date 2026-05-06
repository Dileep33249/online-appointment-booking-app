package com.app.auth.config;

import com.app.auth.entity.User;
import com.app.auth.repository.UserRepository;
import com.app.auth.security.JwtFilter;
import com.app.auth.security.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private static final String FRONTEND_URL = "http://localhost:3000";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtUtil jwtUtil,
            UserRepository userRepository) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/auth/signup",
                        "/auth/login",
                        "/users/internal/**",
                        "/oauth2/**",
                        "/login/**",
                        "/error",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html"
                ).permitAll()
                .requestMatchers("/users/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
                )
                .oauth2Login(oauth -> oauth.successHandler((req, res, authentication) -> {
            String email = authentication.getName();

            User user = userRepository.findByEmail(email).orElseGet(()
                    -> userRepository.save(User.builder()
                            .fullname("OAuth User")
                            .email(email)
                            .password("OAUTH_USER")
                            .role("PATIENT")
                            .provider("GOOGLE")
                            .mustChangePassword(false)
                            .build())
            );

            String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole());
            res.sendRedirect(FRONTEND_URL + "/oauth-success?token=" + token);
        }))
                .addFilterBefore(new JwtFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
