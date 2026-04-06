package com.jirapat.prpo.api.ratelimit;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jirapat.prpo.api.config.RateLimitProperties;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class RateLimitingFilterTest {

    @Mock
    private RateLimitProperties properties;

    @InjectMocks
    private RateLimitingFilter filter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private RateLimitProperties.BucketConfig generalConfig;
    private RateLimitProperties.BucketConfig authConfig;

    @BeforeEach
    void setUp() {
        generalConfig = new RateLimitProperties.BucketConfig();
        generalConfig.setCapacity(100);
        generalConfig.setRefillTokens(100);
        generalConfig.setRefillDurationSeconds(60);

        authConfig = new RateLimitProperties.BucketConfig();
        authConfig.setCapacity(5);
        authConfig.setRefillTokens(5);
        authConfig.setRefillDurationSeconds(60);
    }

    @Test
    void shouldPassThroughWhenDisabled() throws Exception {
        when(properties.isEnabled()).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldAllowRequestWhenUnderGeneralLimit() throws Exception {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getGeneral()).thenReturn(generalConfig);
        when(request.getRequestURI()).thenReturn("/api/v1/vendors");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response).setHeader("X-Rate-Limit-Remaining", "99");
    }

    @Test
    void shouldUseAuthBucketForAuthPaths() throws Exception {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getAuth()).thenReturn(authConfig);
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response).setHeader("X-Rate-Limit-Remaining", "4");
    }

    @Test
    void shouldReturn429WhenAuthLimitExceeded() throws Exception {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getAuth()).thenReturn(authConfig);
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getRemoteAddr()).thenReturn("10.0.0.2");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        // Exhaust auth bucket (capacity=5)
        for (int i = 0; i < 5; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }

        // 6th request should be blocked
        when(response.getWriter()).thenReturn(printWriter);
        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(429);
        verify(response).setHeader("Retry-After", "60");
        verify(response).setContentType("application/json");

        String body = stringWriter.toString();
        assertThat(body).contains("Too many requests");
        assertThat(body).contains("\"success\":false");
    }

    @Test
    void shouldReturn429WhenGeneralLimitExceeded() throws Exception {
        // Set a small general capacity for test
        generalConfig.setCapacity(2);
        generalConfig.setRefillTokens(2);

        when(properties.isEnabled()).thenReturn(true);
        when(properties.getGeneral()).thenReturn(generalConfig);
        when(request.getRequestURI()).thenReturn("/api/v1/vendors");
        when(request.getRemoteAddr()).thenReturn("10.0.0.3");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        // Exhaust general bucket (capacity=2)
        for (int i = 0; i < 2; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }

        // 3rd request should be blocked
        when(response.getWriter()).thenReturn(printWriter);
        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(429);
    }

    @Test
    void shouldUseXForwardedForHeader() throws Exception {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getGeneral()).thenReturn(generalConfig);
        when(request.getRequestURI()).thenReturn("/api/v1/vendors");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.50, 70.41.3.18");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        // Uses first IP from X-Forwarded-For
    }

    @Test
    void shouldUsRemoteAddrWhenNoXForwardedFor() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        String ip = filter.resolveClientIp(request);

        assertThat(ip).isEqualTo("127.0.0.1");
    }

    @Test
    void shouldUseFirstIpFromXForwardedFor() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.50, 70.41.3.18, 150.172.238.178");

        String ip = filter.resolveClientIp(request);

        assertThat(ip).isEqualTo("203.0.113.50");
    }

    @Test
    void shouldTrackDifferentIpsSeparately() throws Exception {
        authConfig.setCapacity(1);
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getAuth()).thenReturn(authConfig);
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);

        // First IP uses its token
        when(request.getRemoteAddr()).thenReturn("10.0.0.10");
        filter.doFilterInternal(request, response, filterChain);

        // Second IP should still have tokens
        when(request.getRemoteAddr()).thenReturn("10.0.0.11");
        filter.doFilterInternal(request, response, filterChain);

        // filterChain should have been called twice (once per IP)
        verify(filterChain, org.mockito.Mockito.times(2)).doFilter(request, response);
    }

    @Test
    void cleanupExpiredBuckets_shouldNotThrow() {
        // Simply verifies cleanup doesn't throw when maps are empty
        filter.cleanupExpiredBuckets();
    }
}
