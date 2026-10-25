package com.example.serverapi.minecraft;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class MinecraftExecutorTest {
    @Test
    void taskRunsOnlyWhenServerDispatcherRunsIt() throws Exception {
        AtomicReference<Runnable> queued = new AtomicReference<>();
        MinecraftExecutor executor = new MinecraftExecutor(queued::set);
        var future = executor.submit(() -> "done", Duration.ofSeconds(1));
        assertFalse(future.isDone());
        queued.get().run();
        assertEquals("done", future.get());
        executor.close();
    }

    @Test
    void timeoutCompletesFutureWithoutBlockingServerThread() throws Exception {
        MinecraftExecutor executor = new MinecraftExecutor(ignored -> {}, Duration.ofMillis(40));
        var future = executor.submit(() -> "never", Duration.ofMillis(40));
        ExecutionException error = assertThrows(ExecutionException.class, () -> future.get());
        assertInstanceOf(TimeoutException.class, error.getCause());
        executor.close();
    }

    @Test
    void callableFailureIsPropagated() {
        MinecraftExecutor executor = new MinecraftExecutor(Runnable::run);
        var future = executor.submit(() -> { throw new IllegalStateException("boom"); }, Duration.ofSeconds(1));
        ExecutionException error = assertThrows(ExecutionException.class, future::get);
        assertInstanceOf(IllegalStateException.class, error.getCause());
        executor.close();
    }
}
