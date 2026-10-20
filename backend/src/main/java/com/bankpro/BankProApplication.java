package com.bankpro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point of the BankPro backend.
 * <p>
 * {@code @SpringBootApplication} turns on component scanning for the {@code com.bankpro} package
 * and Spring Boot's auto-configuration (web server, JPA, Flyway, ...).
 * {@code @ConfigurationPropertiesScan} picks up our typed settings classes such as {@link com.bankpro.config.AppProperties}.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class BankProApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankProApplication.class, args);
    }
}
