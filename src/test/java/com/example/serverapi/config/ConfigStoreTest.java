package com.example.serverapi.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ConfigStoreTest {
    @TempDir Path temp;

    @Test
    void missingConfigUsesSafeDefaults() throws Exception {
        ServerApiConfig config = ServerApiConfig.load(temp.resolve("serverapi.json"));
        assertEquals("127.0.0.1", config.bindAddress());
        assertEquals(8080, config.port());
        assertTrue(config.apiKey().isEmpty());
        assertFalse(config.tlsEnabled());
    }

    @Test
    void configRoundTripsAllOperationalFields() throws Exception {
        Path file = temp.resolve("nested/serverapi.json");
        ServerApiConfig original = new ServerApiConfig("0.0.0.0", 18080, "secret", true,
                "cert/server.p12", "pass", 12, 77, 4321);
        original.save(file);
        ServerApiConfig loaded = ServerApiConfig.load(file);
        assertEquals(original, loaded);
    }

    @Test
    void apiPortCannotEqualMinecraftPort() {
        ServerApiConfig config = ServerApiConfig.defaults().withPort(25565);
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> config.validateAgainstMinecraftPort(25565));
        assertTrue(error.getMessage().contains("Minecraft"));
    }
}
