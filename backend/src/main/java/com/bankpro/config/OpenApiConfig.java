package com.bankpro.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Title and description shown at /swagger-ui.html (enabled in the dev profile only). */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI bankProOpenApi() {
        return new OpenAPI().info(new Info()
                .title("BankPro API")
                .version("v1")
                .description("Customer & Sales CRM for a fictional bank. Portfolio demonstration — synthetic data only."));
    }
}
