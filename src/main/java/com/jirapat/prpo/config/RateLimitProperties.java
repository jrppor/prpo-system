package com.jirapat.prpo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;

    private BucketConfig general = new BucketConfig();
    private BucketConfig auth = new BucketConfig();

    @Getter
    @Setter
    public static class BucketConfig {
        private int capacity = 100;
        private int refillTokens = 100;
        private long refillDurationSeconds = 60;
    }
}
