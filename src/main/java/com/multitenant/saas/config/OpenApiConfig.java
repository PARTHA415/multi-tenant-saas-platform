package com.multitenant.saas.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Multi-Tenant SaaS Platform API")
                        .description("Production-grade Multi-Tenant SaaS Platform with JWT Authentication, RBAC, and Tenant Isolation")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("SaaS Platform Team")
                                .email("support@saas-platform.com")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter JWT token")));
    }

    @Bean
    public OperationCustomizer globalHeaderCustomizer() {
        return (operation, handlerMethod) -> {
            Parameter tenantHeader = new Parameter()
                    .in("header")
                    .name("X-Tenant-ID")
                    .description("Tenant identifier")
                    .required(false)
                    .schema(new io.swagger.v3.oas.models.media.StringSchema());
            operation.addParametersItem(tenantHeader);
            return operation;
        };
    }
}

