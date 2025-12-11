package com.example.foreverhome.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for API documentation.
 * Access the Swagger UI at /swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Forever Home API")
                        .version("1.0.0")
                        .description("""
                                Forever Home is a pet adoption platform that connects pet owners
                                looking to rehome their pets with adopters through rescue organizations.

                                ## Key Features
                                - Pet registration and management by fosters
                                - Rescue organization verification and pet approval
                                - Veterinary health sign-off
                                - Adoption application processing

                                ## Authentication
                                This API uses JWT Bearer tokens for authentication.
                                Access tokens are valid for 15 minutes.
                                Use the `/api/auth/login` endpoint to obtain tokens.
                                """)
                        .contact(new Contact()
                                .name("Forever Home Support")
                                .email("support@foreverhome.local"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url(baseUrl)
                                .description("Forever Home API Server")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT access token obtained from /api/auth/login")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
