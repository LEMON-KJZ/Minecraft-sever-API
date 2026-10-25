package com.example.serverapi.security;

import com.example.serverapi.config.ServerApiConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class SecurityTest {
    @TempDir Path temp;

    @Test
    void firstStartGeneratesAndPersistsKeyOnce() throws Exception {
        Path file = temp.resolve("serverapi.json");
        ServerApiConfig config = ServerApiConfig.defaults();
        AtomicInteger printed = new AtomicInteger();
        String key = ApiKeyManager.ensureKey(config, file, ignored -> printed.incrementAndGet());
        assertFalse(key.isBlank());
        assertEquals(1, printed.get());
        ServerApiConfig reloaded = ServerApiConfig.load(file);
        assertEquals(key, reloaded.apiKey());
        ApiKeyManager.ensureKey(reloaded, file, ignored -> printed.incrementAndGet());
        assertEquals(1, printed.get());
    }

    @Test
    void tokenGenerateRotatesKey() throws Exception {
        Path file = temp.resolve("serverapi.json");
        ServerApiConfig config = ServerApiConfig.defaults();
        String first = ApiKeyManager.ensureKey(config, file, ignored -> {});
        String second = ApiKeyManager.generateNewKey(config, file);
        assertNotEquals(first, second);
        assertEquals(second, ServerApiConfig.load(file).apiKey());
    }

    @Test
    void bearerAuthenticationRejectsMissingMalformedAndWrongKeys() {
        assertFalse(BearerAuthenticator.authenticate(null, "secret").authenticated());
        assertFalse(BearerAuthenticator.authenticate("Basic secret", "secret").authenticated());
        assertFalse(BearerAuthenticator.authenticate("Bearer wrong", "secret").authenticated());
        assertTrue(BearerAuthenticator.authenticate("Bearer secret", "secret").authenticated());
        assertTrue(BearerAuthenticator.authenticate("bearer secret", "secret").authenticated());
    }

    @Test
    void rateLimiterStopsRequestsAfterWindowQuota() {
        RateLimiter limiter = new RateLimiter(2, Duration.ofMinutes(1));
        assertTrue(limiter.tryAcquire("127.0.0.1"));
        assertTrue(limiter.tryAcquire("127.0.0.1"));
        assertFalse(limiter.tryAcquire("127.0.0.1"));
        assertTrue(limiter.tryAcquire("other"));
    }
}
