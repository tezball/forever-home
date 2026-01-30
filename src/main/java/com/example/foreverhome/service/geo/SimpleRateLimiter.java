package com.example.foreverhome.service.geo;

import java.util.concurrent.TimeUnit;

/**
 * Simple rate limiter that allows a fixed number of permits per second.
 * Thread-safe implementation using synchronized blocks.
 */
public class SimpleRateLimiter {

    private final double permitsPerSecond;
    private final long minIntervalNanos;
    private long lastAcquireTimeNanos;

    /**
     * Creates a rate limiter with the given permits per second.
     *
     * @param permitsPerSecond the rate (e.g., 1.0 for one request per second)
     */
    public SimpleRateLimiter(double permitsPerSecond) {
        if (permitsPerSecond <= 0) {
            throw new IllegalArgumentException("permitsPerSecond must be positive");
        }
        this.permitsPerSecond = permitsPerSecond;
        this.minIntervalNanos = (long) (TimeUnit.SECONDS.toNanos(1) / permitsPerSecond);
        this.lastAcquireTimeNanos = System.nanoTime() - minIntervalNanos;
    }

    /**
     * Acquires a permit, blocking if necessary until one is available.
     */
    public synchronized void acquire() {
        long now = System.nanoTime();
        long waitTimeNanos = lastAcquireTimeNanos + minIntervalNanos - now;

        if (waitTimeNanos > 0) {
            try {
                TimeUnit.NANOSECONDS.sleep(waitTimeNanos);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        lastAcquireTimeNanos = System.nanoTime();
    }

    /**
     * Tries to acquire a permit without blocking.
     *
     * @return true if the permit was acquired, false otherwise
     */
    public synchronized boolean tryAcquire() {
        long now = System.nanoTime();
        if (now - lastAcquireTimeNanos >= minIntervalNanos) {
            lastAcquireTimeNanos = now;
            return true;
        }
        return false;
    }

    /**
     * Creates a rate limiter with the given permits per second.
     */
    public static SimpleRateLimiter create(double permitsPerSecond) {
        return new SimpleRateLimiter(permitsPerSecond);
    }
}
