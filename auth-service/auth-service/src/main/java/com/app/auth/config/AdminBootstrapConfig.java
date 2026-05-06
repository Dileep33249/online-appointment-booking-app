package com.app.auth.config;

import com.app.auth.entity.User;
import com.app.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class AdminBootstrapConfig {

    @Bean
    CommandLineRunner seedAdminUser(UserRepository userRepository,
                                    BCryptPasswordEncoder passwordEncoder,
                                    @Value("${app.admin.name:System Admin}") String adminName,
                                    @Value("${app.admin.email:admin@medibook.com}") String adminEmail,
                                    @Value("${app.admin.password:Admin@123}") String adminPassword) {
        return args -> {
            if (userRepository.countByRoleIgnoreCase("ADMIN") > 0) {
                return;
            }

            String normalizedEmail = adminEmail.trim().toLowerCase();
            if (userRepository.findByEmail(normalizedEmail).isPresent()) {
                return;
            }

            userRepository.save(User.builder()
                    .fullname(adminName)
                    .email(normalizedEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .role("ADMIN")
                    .provider("LOCAL")
                    .mustChangePassword(false)
                    .build());
        };
    }
}
