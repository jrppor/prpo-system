package com.jirapat.prpo.ratelimit;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.jirapat.prpo.config.RateLimitProperties;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@EnableScheduling
@EnableConfigurationProperties(RateLimitProperties.class)
@RequiredArgsConstructor
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitProperties properties;

    private final ConcurrentHashMap<String, TokenBucket> generalBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, TokenBucket> authBuckets = new ConcurrentHashMap<>();

    private static final String AUTH_PATH_PREFIX = "/api/v1/auth/";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (!properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(request);
        String path = request.getRequestURI();

        TokenBucket bucket;
        if (path.startsWith(AUTH_PATH_PREFIX)) {
            bucket = authBuckets.computeIfAbsent(clientIp,
                    k -> createBucket(properties.getAuth()));
        } else {
            bucket = generalBuckets.computeIfAbsent(clientIp,
                    k -> createBucket(properties.getGeneral()));
        }

        if (!bucket.tryConsume()) {
            log.warn("Rate limit exceeded for IP: {} on path: {}", clientIp, path);
            writeRateLimitResponse(response, bucket);
            return;
        }

        response.setHeader("X-Rate-Limit-Remaining", String.valueOf(bucket.getAvailableTokens()));
        filterChain.doFilter(request, response);
    }

    private TokenBucket createBucket(RateLimitProperties.BucketConfig config) {
        return new TokenBucket(config.getCapacity(), config.getRefillTokens(), config.getRefillDurationSeconds());
    }

    String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeRateLimitResponse(HttpServletResponse response, TokenBucket bucket) throws IOException {
        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(bucket.getSecondsUntilRefill()));
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String json = """
                {"success":false,"message":"Too many requests. Please try again later.","timestamp":"%s"}"""
                .formatted(LocalDateTime.now());

        response.getWriter().write(json);
    }

    @Scheduled(fixedRate = 300_000) // every 5 minutes
    public void cleanupExpiredBuckets() {
        long maxIdleNanos = TimeUnit.MINUTES.toNanos(10);
        int generalRemoved = removeExpired(generalBuckets, maxIdleNanos);
        int authRemoved = removeExpired(authBuckets, maxIdleNanos);
        if (generalRemoved > 0 || authRemoved > 0) {
            log.debug("Rate limit cleanup: removed {} general, {} auth buckets", generalRemoved, authRemoved);
        }
    }

    private int removeExpired(ConcurrentHashMap<String, TokenBucket> buckets, long maxIdleNanos) {
        int sizeBefore = buckets.size();
        buckets.entrySet().removeIf(entry -> entry.getValue().isExpired(maxIdleNanos));
        return sizeBefore - buckets.size();
    }
}
