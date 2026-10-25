package com.example.serverapi;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectMetadataTest {
    @Test
    void fabricMetadataDeclaresServerOnlyEntrypoint() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/fabric.mod.json")) {
            assertTrue(stream != null, "fabric.mod.json must be packaged");
            String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"id\": \"serverapi\""));
            assertTrue(json.contains("\"environment\": \"server\""));
            assertTrue(json.contains("com.example.serverapi.ServerApiMod"));
            assertTrue(json.contains("\"version\": \"1.0.0\""));
        }
    }
}
