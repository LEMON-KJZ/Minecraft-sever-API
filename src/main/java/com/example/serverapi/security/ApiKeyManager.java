package com.example.serverapi.security;

import com.example.serverapi.config.ServerApiConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.function.Consumer;

public final class ApiKeyManager {
    private static final SecureRandom RANDOM = new SecureRandom();

    private ApiKeyManager() {}

    public static String ensureKey(ServerApiConfig config, Path path, Consumer<String> firstGenerationNotice) throws IOException {
        if (!config.apiKey().isBlank()) return config.apiKey();
        String key = generateNewKeyValue();
        config.setApiKey(key);
        config.save(path);
        firstGenerationNotice.accept(key);
        return key;
    }

    public static String generateNewKey(ServerApiConfig config, Path path) throws IOException {
        String key = generateNewKeyValue();
        config.setApiKey(key);
        config.save(path);
        return key;
    }

    private static String generateNewKeyValue() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
