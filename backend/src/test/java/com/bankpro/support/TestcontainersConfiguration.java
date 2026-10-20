package com.bankpro.support;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Starts a throw-away MySQL 8.4 container for integration tests.
 * {@code @ServiceConnection} points the datasource at this container automatically,
 * so tests never touch your development database. Requires Docker Desktop to be running.
 * <p>
 * Without Docker, run against an existing, EMPTY test schema instead:
 * <pre>
 * mvn test -Dbankpro.test.local-db=true
 *          -Dspring.datasource.url=jdbc:mysql://localhost:3306/bankpro_test
 *          -Dspring.datasource.username=... -Dspring.datasource.password=...
 * </pre>
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    @ConditionalOnProperty(name = "bankpro.test.local-db", havingValue = "false", matchIfMissing = true)
    MySQLContainer<?> mysqlContainer() {
        return new MySQLContainer<>(DockerImageName.parse("mysql:8.4"));
    }
}
