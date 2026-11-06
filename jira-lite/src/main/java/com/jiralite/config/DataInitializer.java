package com.jiralite.config;

import com.jiralite.entity.User;
import com.jiralite.model.Role;
import com.jiralite.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Seeds default workspace users across roles if the database is uninitialized.
 * Passwords and seeding behavior can be configured or disabled via environment variables.
 */
@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Value("${app.seed.enabled:true}")
    private boolean seedEnabled;

    @Value("${SEED_ADMIN_PASSWORD:Admin@JiraLite2026!}")
    private String adminPassword;

    @Value("${SEED_PM_PASSWORD:Pm@JiraLite2026!}")
    private String pmPassword;

    @Value("${SEED_DEV_PASSWORD:Dev@JiraLite2026!}")
    private String devPassword;

    @Bean
    CommandLineRunner initData(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (!seedEnabled) {
                log.info("Database seeding disabled via app.seed.enabled=false");
                return;
            }

            if (userRepository.count() == 0) {
                User admin = User.builder()
                        .firstName("Admin")
                        .lastName("User")
                        .email("admin@jiralite.com")
                        .password(passwordEncoder.encode(adminPassword))
                        .role(Role.ADMIN)
                        .active(true)
                        .build();

                User pm = User.builder()
                        .firstName("Sarah")
                        .lastName("Connor")
                        .email("pm@jiralite.com")
                        .password(passwordEncoder.encode(pmPassword))
                        .role(Role.PROJECT_MANAGER)
                        .active(true)
                        .build();

                User dev = User.builder()
                        .firstName("Alex")
                        .lastName("Chen")
                        .email("dev@jiralite.com")
                        .password(passwordEncoder.encode(devPassword))
                        .role(Role.DEVELOPER)
                        .active(true)
                        .build();

                userRepository.save(admin);
                userRepository.save(pm);
                userRepository.save(dev);
                log.info("Initialized default users for ADMIN, PROJECT_MANAGER, and DEVELOPER roles.");
            }
        };
    }
}
