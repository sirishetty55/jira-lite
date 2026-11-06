package com.jiralite;

import com.jiralite.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end integration test executed against live PostgreSQL 16 and Redis 7
 * service containers in GitHub Actions CI environment.
 * Automatically skipped during local workstation unit test runs where CI is not set.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "CI", matches = "true")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/jiralite",
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.jpa.hibernate.ddl-auto=update",
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379",
        "spring.cache.type=redis",
        "app.jwt.secret=bXlfc3VwZXJfc2VjcmV0X2tleV9mb3Jfand0X3Rva2VuX2dlbmVyYXRpb25fMjAyNg=="
})
class InfrastructureIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Test
    @DisplayName("Verify live PostgreSQL database connectivity and query execution")
    void testPostgresConnectivity() {
        assertNotNull(userRepository, "UserRepository bean should be initialized");
        assertTrue(userRepository.count() >= 0, "PostgreSQL query should execute successfully");
    }

    @Test
    @DisplayName("Verify live Redis connectivity and ping response")
    void testRedisConnectivity() {
        assertNotNull(redisConnectionFactory, "RedisConnectionFactory should be initialized");
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            String ping = connection.ping();
            assertNotNull(ping, "Redis ping response should not be null");
        }
    }
}
