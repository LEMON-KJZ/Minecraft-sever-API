package com.example.serverapi.extensions;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class SchedulerService implements AutoCloseable {
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "serverapi-scheduler"); thread.setDaemon(true); return thread;
    });

    public void scheduleAtFixedRate(Runnable task, Duration initialDelay, Duration period) {
        if (period.isZero() || period.isNegative()) throw new IllegalArgumentException("Period must be positive");
        executor.scheduleAtFixedRate(task, initialDelay.toMillis(), period.toMillis(), TimeUnit.MILLISECONDS);
    }
    @Override public void close() { executor.shutdownNow(); }
}
