package com.example.serverapi.config;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

public final class ServerApiConfig {
    public static final String DEFAULT_BIND_ADDRESS = "127.0.0.1";
    public static final int DEFAULT_PORT = 8080;
    public static final int DEFAULT_RATE_LIMIT = 120;
    public static final int DEFAULT_LOG_BUFFER_SIZE = 500;
    public static final long DEFAULT_REQUEST_TIMEOUT_MILLIS = 5000;

    private String bindAddress;
    private int port;
    private String apiKey;
    private boolean tlsEnabled;
    private String keystorePath;
    private String keystorePassword;
    private int rateLimitPerMinute;
    private int logBufferSize;
    private long requestTimeoutMillis;

    public ServerApiConfig(String bindAddress, int port, String apiKey, boolean tlsEnabled,
                           String keystorePath, String keystorePassword, int rateLimitPerMinute,
                           int logBufferSize, long requestTimeoutMillis) {
        this.bindAddress = Objects.requireNonNull(bindAddress);
        this.port = port;
        this.apiKey = apiKey == null ? "" : apiKey;
        this.tlsEnabled = tlsEnabled;
        this.keystorePath = keystorePath == null ? "" : keystorePath;
        this.keystorePassword = keystorePassword == null ? "" : keystorePassword;
        this.rateLimitPerMinute = rateLimitPerMinute;
        this.logBufferSize = logBufferSize;
        this.requestTimeoutMillis = requestTimeoutMillis;
    }

    public static ServerApiConfig defaults() {
        return new ServerApiConfig(DEFAULT_BIND_ADDRESS, DEFAULT_PORT, "", false, "", "",
                DEFAULT_RATE_LIMIT, DEFAULT_LOG_BUFFER_SIZE, DEFAULT_REQUEST_TIMEOUT_MILLIS);
    }

    public static ServerApiConfig load(Path path) throws IOException { return ConfigStore.read(path); }
    public void save(Path path) throws IOException { ConfigStore.write(path, this); }

    public void validateAgainstMinecraftPort(int minecraftPort) {
        if (port < 1 || port > 65535) throw new IllegalArgumentException("API port must be between 1 and 65535");
        if (port == minecraftPort) throw new IllegalArgumentException("API port cannot equal the Minecraft game port");
        if (bindAddress.isBlank()) throw new IllegalArgumentException("API bind address cannot be blank");
        if (rateLimitPerMinute < 1) throw new IllegalArgumentException("rateLimitPerMinute must be positive");
        if (logBufferSize < 1) throw new IllegalArgumentException("logBufferSize must be positive");
        if (requestTimeoutMillis < 1) throw new IllegalArgumentException("requestTimeoutMillis must be positive");
    }

    public ServerApiConfig withPort(int value) { return new ServerApiConfig(bindAddress, value, apiKey, tlsEnabled, keystorePath, keystorePassword, rateLimitPerMinute, logBufferSize, requestTimeoutMillis); }
    public void setApiKey(String value) { apiKey = value == null ? "" : value; }
    public String bindAddress() { return bindAddress; }
    public int port() { return port; }
    public String apiKey() { return apiKey; }
    public boolean tlsEnabled() { return tlsEnabled; }
    public String keystorePath() { return keystorePath; }
    public String keystorePassword() { return keystorePassword; }
    public int rateLimitPerMinute() { return rateLimitPerMinute; }
    public int logBufferSize() { return logBufferSize; }
    public long requestTimeoutMillis() { return requestTimeoutMillis; }

    @Override public boolean equals(Object other) {
        if (!(other instanceof ServerApiConfig that)) return false;
        return port == that.port && tlsEnabled == that.tlsEnabled && rateLimitPerMinute == that.rateLimitPerMinute
                && logBufferSize == that.logBufferSize && requestTimeoutMillis == that.requestTimeoutMillis
                && bindAddress.equals(that.bindAddress) && apiKey.equals(that.apiKey)
                && keystorePath.equals(that.keystorePath) && keystorePassword.equals(that.keystorePassword);
    }
    @Override public int hashCode() { return Objects.hash(bindAddress, port, apiKey, tlsEnabled, keystorePath, keystorePassword, rateLimitPerMinute, logBufferSize, requestTimeoutMillis); }
}
