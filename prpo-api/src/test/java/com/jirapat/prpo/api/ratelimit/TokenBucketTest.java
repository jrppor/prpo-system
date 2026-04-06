package com.jirapat.prpo.api.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class TokenBucketTest {

    @Test
    void tryConsume_shouldSucceedWhenTokensAvailable() {
        TokenBucket bucket = new TokenBucket(5, 5, 60);

        assertThat(bucket.tryConsume()).isTrue();
        assertThat(bucket.getAvailableTokens()).isEqualTo(4);
    }

    @Test
    void tryConsume_shouldDrainAllTokens() {
        TokenBucket bucket = new TokenBucket(3, 3, 60);

        assertThat(bucket.tryConsume()).isTrue();
        assertThat(bucket.tryConsume()).isTrue();
        assertThat(bucket.tryConsume()).isTrue();
        assertThat(bucket.tryConsume()).isFalse();
    }

    @Test
    void tryConsume_shouldRejectWhenEmpty() {
        TokenBucket bucket = new TokenBucket(1, 1, 60);

        assertThat(bucket.tryConsume()).isTrue();
        assertThat(bucket.tryConsume()).isFalse();
    }

    @Test
    void getAvailableTokens_shouldReturnCapacityInitially() {
        TokenBucket bucket = new TokenBucket(10, 10, 60);

        assertThat(bucket.getAvailableTokens()).isEqualTo(10);
    }

    @Test
    void getSecondsUntilRefill_shouldReturnRefillDuration() {
        TokenBucket bucket = new TokenBucket(5, 5, 30);

        assertThat(bucket.getSecondsUntilRefill()).isEqualTo(30);
    }

    @Test
    void isExpired_shouldReturnFalseWhenRecentlyAccessed() {
        TokenBucket bucket = new TokenBucket(5, 5, 60);
        bucket.tryConsume();

        assertThat(bucket.isExpired(Long.MAX_VALUE)).isFalse();
    }

    @Test
    void isExpired_shouldReturnTrueWhenIdleTooLong() {
        TokenBucket bucket = new TokenBucket(5, 5, 60);

        // Expired with 0 nanos threshold = immediately expired
        assertThat(bucket.isExpired(0)).isTrue();
    }

    @Test
    void tryConsume_shouldRefillOverTime() throws InterruptedException {
        // 10 tokens refilled per 1 second = fast refill for test
        TokenBucket bucket = new TokenBucket(2, 2, 1);

        assertThat(bucket.tryConsume()).isTrue();
        assertThat(bucket.tryConsume()).isTrue();
        assertThat(bucket.tryConsume()).isFalse();

        // Wait for refill (slightly over 1 second to allow full refill)
        Thread.sleep(1100);

        assertThat(bucket.tryConsume()).isTrue();
    }
}
