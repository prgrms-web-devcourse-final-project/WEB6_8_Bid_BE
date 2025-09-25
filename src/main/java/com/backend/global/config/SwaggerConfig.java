package com.backend.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI openAPI() {
        SecurityScheme bearerScheme = new SecurityScheme()
                .name("Authorization")                 // 헤더 이름..
                .type(SecurityScheme.Type.HTTP)        // HTTP 인증..
                .scheme("bearer")                      // Bearer..
                .bearerFormat("JWT");

        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("BearerAuth", bearerScheme))
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
                .info(new Info().title("Bid API").version("v1"));
    }

}
