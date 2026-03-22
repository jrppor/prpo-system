package com.jirapat.prpo.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    private List<String> allowedOrigins = List.of("http://localhost:4200");
    private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
    private List<String> allowedHeaders = List.of("Authorization", "Content-Type", "X-Requested-With");
    private boolean allowCredentials = true;
    private long maxAge = 3600L;
}
