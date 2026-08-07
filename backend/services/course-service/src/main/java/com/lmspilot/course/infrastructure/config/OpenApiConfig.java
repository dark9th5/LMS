package com.lmspilot.course.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI courseOpenApi() {
        // Swagger UI uses this definition to show the Authorize button for JWT-protected endpoints.
        return new OpenAPI()
            .info(new Info()
                .title("LMSPilot Course Service API")
                .version("0.24.0")
                .description("Course, category, lesson, publication, and discussion APIs."))
            .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
            .schemaRequirement(BEARER_AUTH, new SecurityScheme()
                .name(BEARER_AUTH)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT"));
    }
}
