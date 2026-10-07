package ru.akkarin.is_lab1;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DemoUserConfig {
    @Bean
    ApplicationRunner seedDemoUser(UserRepository users, PasswordEncoder encoder,
                                   @Value("${app.demo.username}") String username,
                                   @Value("${app.demo.password}") String password) {
        return args -> {
            if (username.isBlank() || username.length() > 64 || password.length() < 12) {
                throw new IllegalArgumentException("Set DEMO_USERNAME (1-64 chars) and DEMO_PASSWORD (12+ chars)");
            }
            if (users.passwordHash(username).isEmpty()) {
                users.createIfAbsent(username, encoder.encode(password));
            }
        };
    }
}
