package com.jirapat.prpo.api.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT Authorization header using the Bearer scheme. Enter your token in the text input below."
)
public class OpenApiConfig {

    @Value("${app.openapi.server-url}")
    private String serverUrl;

    @Value("${app.openapi.server-description}")
    private String serverDescription;

    @Value("${app.company.name}")
    private String companyName;

    @Value("${app.company.email}")
    private String companyEmail;

    @Value("${app.company.url}")
    private String companyUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(companyName + " API")
                        .version("1.0.0")
                        .description("API สำหรับ " + companyName)
                        .contact(new Contact()
                                .name(companyName)
                                .email(companyEmail)
                                .url(companyUrl))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url(serverUrl).description(serverDescription)
                ));
    }
}
