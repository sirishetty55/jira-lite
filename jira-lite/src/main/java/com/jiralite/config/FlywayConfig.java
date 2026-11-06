package com.jiralite.config;

import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Programmatic Flyway configuration for database migrations.
 * Ensures Flyway runs migrations (or baselines pre-existing schemas)
 * prior to JPA EntityManagerFactory initialization and Hibernate schema validation.
 */
@Configuration
@ConditionalOnProperty(name = "spring.flyway.enabled", havingValue = "true", matchIfMissing = true)
public class FlywayConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayConfig.class);

    @Value("${spring.flyway.baseline-on-migrate:true}")
    private boolean baselineOnMigrate;

    @Value("${spring.flyway.baseline-version:0}")
    private String baselineVersion;

    @Value("${spring.flyway.locations:classpath:db/migration}")
    private String locations;

    @Bean
    public Flyway flyway(DataSource dataSource) {
        log.info("Initializing Flyway migration (locations={}, baselineOnMigrate={}, baselineVersion={})",
                locations, baselineOnMigrate, baselineVersion);
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations(locations)
                .baselineOnMigrate(baselineOnMigrate)
                .baselineVersion(baselineVersion)
                .load();

        flyway.migrate();
        log.info("Flyway migration completed successfully.");
        return flyway;
    }

    /**
     * Enforces that JPA EntityManagerFactory depends on the flyway bean,
     * guaranteeing that schema migrations run before Hibernate validates the schema.
     */
    @Bean
    public static BeanFactoryPostProcessor flywayDependsOnPostProcessor() {
        return beanFactory -> {
            if (!beanFactory.containsBeanDefinition("flyway")) {
                return;
            }
            String[] emNames = beanFactory.getBeanNamesForType(EntityManagerFactory.class, true, false);
            for (String name : emNames) {
                BeanDefinition bd = beanFactory.getBeanDefinition(name);
                String[] dependsOn = bd.getDependsOn();
                if (dependsOn == null) {
                    bd.setDependsOn(new String[]{"flyway"});
                } else {
                    String[] newDependsOn = new String[dependsOn.length + 1];
                    System.arraycopy(dependsOn, 0, newDependsOn, 0, dependsOn.length);
                    newDependsOn[dependsOn.length] = "flyway";
                    bd.setDependsOn(newDependsOn);
                }
            }
        };
    }
}

