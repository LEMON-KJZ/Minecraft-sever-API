package com.example.serverapi.logging;

import com.example.serverapi.events.EventBus;
import com.example.serverapi.events.ServerEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;

import java.util.Map;
import java.util.function.Supplier;

public final class ConsoleLogBridge extends AbstractAppender implements AutoCloseable {
    private final Logger root;
    private final LogBuffer buffer;
    private final EventBus events;
    private final Supplier<String> apiKey;

    private ConsoleLogBridge(LogBuffer buffer, EventBus events, Supplier<String> apiKey) {
        super("serverapi-log-buffer", null, null, true);
        this.root = (Logger) LogManager.getRootLogger();
        this.buffer = buffer;
        this.events = events;
        this.apiKey = apiKey;
    }

    public static ConsoleLogBridge install(LogBuffer buffer, EventBus events, Supplier<String> apiKey) {
        ConsoleLogBridge bridge = new ConsoleLogBridge(buffer, events, apiKey);
        bridge.start();
        bridge.root.addAppender(bridge);
        return bridge;
    }

    @Override public void append(LogEvent event) {
        String message = redact(event.getMessage().getFormattedMessage(), apiKey.get());
        buffer.append(message);
        events.publish(new ServerEvent(System.nanoTime(), "log", Map.of("level", event.getLevel().name(), "message", message)));
    }

    public static String redact(String message, String key) {
        if (message == null || key == null || key.isEmpty()) return message;
        return message.replace(key, "[REDACTED]");
    }

    @Override public void close() { root.removeAppender(this); stop(); }
}
