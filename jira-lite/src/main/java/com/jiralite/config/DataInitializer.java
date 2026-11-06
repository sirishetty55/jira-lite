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
import org.springframework.util.StringUtils;

/**
 * Seeds default workspace users across roles if the database is uninitialized.
 *
 * <p>All seed passwords are sourced exclusively from environment variables
 * ({@code SEED_ADMIN_PASSWORD}, {@code SEED_PM_PASSWORD}, {@code SEED_DEV_PASSWORD}).
 * No hardcoded fallback passwords exist. If seeding is enabled and a required
 * password variable is absent or blank, the application will log a clear error
 * and throw an {@link IllegalStateException} at startup.
 *
 * <p>Set {@code app.seed.enabled=false} to disable seeding entirely (recommended
 * for production environments where accounts are managed externally).
 */
@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Value("${app.seed.enabled:true}")
    private boolean seedEnabled;

    /**
     * Admin seed password — must be supplied via SEED_ADMIN_PASSWORD env var.
     * No default is provided; a missing value causes startup failure.
     */
    @Value("${SEED_ADMIN_PASSWORD:}")
    private String adminPassword;

    /**
     * Project Manager seed password — must be supplied via SEED_PM_PASSWORD env var.
     */
    @Value("${SEED_PM_PASSWORD:}")
    private String pmPassword;

    /**
     * Developer seed password — must be supplied via SEED_DEV_PASSWORD env var.
     */
    @Value("${SEED_DEV_PASSWORD:}")
    private String devPassword;

    @Bean
    CommandLineRunner initData(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (!seedEnabled) {
                log.info("Database seeding disabled via app.seed.enabled=false");
                return;
            }

            if (userRepository.count() > 0) {
                log.debug("Database already contains users — skipping seed.");
                return;
            }

            // Validate that all seed passwords have been externally configured
            validateSeedPassword("SEED_ADMIN_PASSWORD", adminPassword);
            validateSeedPassword("SEED_PM_PASSWORD",    pmPassword);
            validateSeedPassword("SEED_DEV_PASSWORD",   devPassword);

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
            log.info("Seeded default users: ADMIN (admin@jiralite.com), " +
                     "PROJECT_MANAGER (pm@jiralite.com), DEVELOPER (dev@jiralite.com)");
        };
    }

    private void validateSeedPassword(String variableName, String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(
                "Seed account password not configured. " +
                "Set the '" + variableName + "' environment variable before starting the application. " +
                "To skip seeding, set app.seed.enabled=false."
            );
        }
    }
}
