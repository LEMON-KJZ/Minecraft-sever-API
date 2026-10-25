package com.example.serverapi.minecraft;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** Bridges HTTP worker threads to the Minecraft server thread. */
public final class MinecraftExecutor implements AutoCloseable {
    private final Consumer<Runnable> serverDispatcher;
    private final Duration defaultTimeout;
    private final ScheduledExecutorService timeoutScheduler;

    public MinecraftExecutor(Consumer<Runnable> serverDispatcher) {
        this(serverDispatcher, Duration.ofSeconds(5));
    }

    public MinecraftExecutor(Consumer<Runnable> serverDispatcher, Duration defaultTimeout) {
        this.serverDispatcher = Objects.requireNonNull(serverDispatcher);
        this.defaultTimeout = requirePositive(defaultTimeout);
        ThreadFactory factory = task -> {
            Thread thread = new Thread(task, "serverapi-timeouts");
            thread.setDaemon(true);
            return thread;
        };
        this.timeoutScheduler = Executors.newSingleThreadScheduledExecutor(factory);
    }

    public <T> CompletableFuture<T> submit(Callable<T> callable, Duration timeout) {
        Objects.requireNonNull(callable);
        Duration effectiveTimeout = timeout == null ? defaultTimeout : requirePositive(timeout);
        CompletableFuture<T> result = new CompletableFuture<>();
        ScheduledFuture<?> timeoutTask = timeoutScheduler.schedule(
                () -> result.completeExceptionally(new java.util.concurrent.TimeoutException("Minecraft server task timed out")),
                effectiveTimeout.toMillis(), TimeUnit.MILLISECONDS);
        result.whenComplete((ignored, error) -> timeoutTask.cancel(false));
        try {
            serverDispatcher.accept(() -> {
                if (result.isDone()) return;
                try { result.complete(callable.call()); }
                catch (Throwable error) { result.completeExceptionally(error); }
            });
        } catch (Throwable error) {
            result.completeExceptionally(error);
        }
        return result;
    }

    private static Duration requirePositive(Duration duration) {
        if (duration.isNegative() || duration.isZero()) throw new IllegalArgumentException("Timeout must be positive");
        return duration;
    }

    @Override public void close() { timeoutScheduler.shutdownNow(); }
}
