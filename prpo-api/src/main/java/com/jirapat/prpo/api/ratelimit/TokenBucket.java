package com.jirapat.prpo.api.ratelimit;

import java.util.concurrent.TimeUnit;

public class TokenBucket {

    private final int capacity;
    private final int refillTokens;
    private final long refillDurationNanos;
    private double availableTokens;
    private long lastRefillTimestamp;
    private long lastAccessTimestamp;

    public TokenBucket(int capacity, int refillTokens, long refillDurationSeconds) {
        this.capacity = capacity;
        this.refillTokens = refillTokens;
        this.refillDurationNanos = TimeUnit.SECONDS.toNanos(refillDurationSeconds);
        this.availableTokens = capacity;
        this.lastRefillTimestamp = System.nanoTime();
        this.lastAccessTimestamp = System.nanoTime();
    }

    public synchronized boolean tryConsume() {
        refill();
        lastAccessTimestamp = System.nanoTime();
        if (availableTokens >= 1.0) {
            availableTokens -= 1.0;
            return true;
        }
        return false;
    }

    public synchronized int getAvailableTokens() {
        refill();
        return (int) availableTokens;
    }

    public long getSecondsUntilRefill() {
        return TimeUnit.NANOSECONDS.toSeconds(refillDurationNanos);
    }

    public boolean isExpired(long maxIdleNanos) {
        return (System.nanoTime() - lastAccessTimestamp) > maxIdleNanos;
    }

    private void refill() {
        long now = System.nanoTime();
        long elapsed = now - lastRefillTimestamp;
        if (elapsed > 0) {
            double tokensToAdd = ((double) elapsed / refillDurationNanos) * refillTokens;
            availableTokens = Math.min(capacity, availableTokens + tokensToAdd);
            lastRefillTimestamp = now;
        }
    }
}
