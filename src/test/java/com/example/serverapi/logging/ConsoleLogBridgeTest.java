package com.example.serverapi.logging;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConsoleLogBridgeTest {
    @Test
    void apiKeyIsRedactedBeforeLogBufferStorage() {
        assertEquals("key=[REDACTED]", ConsoleLogBridge.redact("key=secret", "secret"));
    }
}
