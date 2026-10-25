package com.example.serverapi.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ConfigStore {
    private ConfigStore() {}

    static ServerApiConfig read(Path path) throws IOException {
        if (!Files.exists(path)) return ServerApiConfig.defaults();
        String json = Files.readString(path, StandardCharsets.UTF_8);
        ServerApiConfig defaults = ServerApiConfig.defaults();
        return new ServerApiConfig(
                stringValue(json, "bindAddress", defaults.bindAddress()),
                intValue(json, "port", defaults.port()),
                stringValue(json, "apiKey", defaults.apiKey()),
                boolValue(json, "tlsEnabled", defaults.tlsEnabled()),
                stringValue(json, "keystorePath", defaults.keystorePath()),
                stringValue(json, "keystorePassword", defaults.keystorePassword()),
                intValue(json, "rateLimitPerMinute", defaults.rateLimitPerMinute()),
                intValue(json, "logBufferSize", defaults.logBufferSize()),
                longValue(json, "requestTimeoutMillis", defaults.requestTimeoutMillis()));
    }

    static void write(Path path, ServerApiConfig config) throws IOException {
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        Path temp = path.resolveSibling(path.getFileName() + ".tmp");
        Files.writeString(temp, toJson(config), StandardCharsets.UTF_8);
        try { Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch (AtomicMoveNotSupportedException ignored) { Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING); }
    }

    private static String toJson(ServerApiConfig c) {
        return "{\n" +
                "  \"bindAddress\": \"" + escape(c.bindAddress()) + "\",\n" +
                "  \"port\": " + c.port() + ",\n" +
                "  \"apiKey\": \"" + escape(c.apiKey()) + "\",\n" +
                "  \"tlsEnabled\": " + c.tlsEnabled() + ",\n" +
                "  \"keystorePath\": \"" + escape(c.keystorePath()) + "\",\n" +
                "  \"keystorePassword\": \"" + escape(c.keystorePassword()) + "\",\n" +
                "  \"rateLimitPerMinute\": " + c.rateLimitPerMinute() + ",\n" +
                "  \"logBufferSize\": " + c.logBufferSize() + ",\n" +
                "  \"requestTimeoutMillis\": " + c.requestTimeoutMillis() + "\n" +
                "}\n";
    }

    private static String stringValue(String json, String key, String fallback) {
        Matcher m = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"").matcher(json);
        return m.find() ? unescape(m.group(1)) : fallback;
    }
    private static int intValue(String json, String key, int fallback) { return (int) longValue(json, key, fallback); }
    private static long longValue(String json, String key, long fallback) {
        Matcher m = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*(-?\\d+)").matcher(json);
        if (!m.find()) return fallback;
        try { return Long.parseLong(m.group(1)); } catch (NumberFormatException e) { return fallback; }
    }
    private static boolean boolValue(String json, String key, boolean fallback) {
        Matcher m = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*(true|false)").matcher(json);
        return m.find() ? Boolean.parseBoolean(m.group(1)) : fallback;
    }
    private static String escape(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t"); }
    private static String unescape(String value) { return value.replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t").replace("\\\"", "\"").replace("\\\\", "\\"); }
}
