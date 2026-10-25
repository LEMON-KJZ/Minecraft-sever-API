package com.example.serverapi.minecraft;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class CommandService {
    @FunctionalInterface
    public interface CommandRunner { CommandResult run(String command) throws Exception; }

    public record CommandResult(boolean success, int resultCode, String feedback, long durationMs) {}

    private final MinecraftExecutor executor;
    private final CommandRunner runner;

    public CommandService(MinecraftExecutor executor, CommandRunner runner) {
        this.executor = Objects.requireNonNull(executor);
        this.runner = Objects.requireNonNull(runner);
    }

    public CompletableFuture<CommandResult> execute(String command) {
        if (command == null || command.isBlank()) return CompletableFuture.failedFuture(new IllegalArgumentException("Command cannot be blank"));
        return executor.submit(() -> runner.run(command.trim()), Duration.ofSeconds(5));
    }
}
