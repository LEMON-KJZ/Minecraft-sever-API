package com.example.serverapi.security;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class RateLimiter {
    private final int limit;
    private final long windowMillis;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final AtomicLong calls = new AtomicLong();

    public RateLimiter(int limit, Duration window) {
        if (limit < 1 || window.isNegative() || window.isZero()) throw new IllegalArgumentException("Invalid rate limit");
        this.limit = limit;
        this.windowMillis = window.toMillis();
    }

    public boolean tryAcquire(String clientKey) {
        long now = System.currentTimeMillis();
        Window window = windows.computeIfAbsent(clientKey == null ? "unknown" : clientKey, ignored -> new Window(now));
        synchronized (window) {
            if (now - window.startedAt >= windowMillis) {
                window.startedAt = now;
                window.count = 0;
            }
            boolean allowed = ++window.count <= limit;
            if (calls.incrementAndGet() % 1024 == 0) prune(now);
            return allowed;
        }
    }

    private void prune(long now) { windows.entrySet().removeIf(entry -> now - entry.getValue().startedAt >= windowMillis * 2); }

    private static final class Window {
        private long startedAt;
        private int count;
        private Window(long startedAt) { this.startedAt = startedAt; }
    }
}
