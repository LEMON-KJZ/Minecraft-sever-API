package com.example.serverapi.http;

import com.example.serverapi.minecraft.PlayerSnapshot;
import com.example.serverapi.minecraft.ServerSnapshot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JsonCodec {
    private JsonCodec() {}

    public static String object(Map<String, ?> values) { return value(values); }
    public static String array(Collection<?> values) { return value(values); }

    public static String requiredString(String json, String name) {
        String value = optionalString(json, name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing JSON string field: " + name);
        return value;
    }

    public static String optionalString(String json, String name) {
        String marker = "\"" + name + "\"";
        int key = json.indexOf(marker);
        if (key < 0) return null;
        int colon = json.indexOf(':', key + marker.length());
        if (colon < 0) return null;
        int start = json.indexOf('"', colon + 1);
        if (start < 0) return null;
        StringBuilder result = new StringBuilder();
        boolean escaped = false;
        for (int i = start + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) { result.append(switch (c) { case 'n' -> '\n'; case 'r' -> '\r'; case 't' -> '\t'; default -> c; }); escaped = false; }
            else if (c == '\\') escaped = true;
            else if (c == '"') return result.toString();
            else result.append(c);
        }
        return null;
    }

    public static Map<String, Object> snapshot(ServerSnapshot snapshot) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("minecraftVersion", snapshot.minecraftVersion()); map.put("fabricVersion", snapshot.fabricVersion());
        map.put("uptimeMillis", snapshot.uptimeMillis()); map.put("memoryUsedBytes", snapshot.memoryUsedBytes());
        map.put("memoryMaxBytes", snapshot.memoryMaxBytes()); map.put("tps", snapshot.tps()); map.put("mspt", snapshot.mspt());
        map.put("onlinePlayers", snapshot.onlinePlayers()); map.put("maxPlayers", snapshot.maxPlayers()); map.put("running", snapshot.running());
        return map;
    }

    public static Map<String, Object> snapshot(PlayerSnapshot player) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", player.name()); map.put("uuid", player.uuid()); map.put("dimension", player.dimension());
        map.put("x", player.x()); map.put("y", player.y()); map.put("z", player.z()); map.put("health", player.health());
        map.put("hunger", player.hunger()); map.put("gameMode", player.gameMode()); map.put("latency", player.latency()); map.put("level", player.level());
        return map;
    }

    private static String value(Object value) {
        if (value == null) return "null";
        if (value instanceof String || value instanceof Character || value instanceof java.util.UUID) return "\"" + escape(String.valueOf(value)) + "\"";
        if (value instanceof Number || value instanceof Boolean) return String.valueOf(value);
        if (value instanceof Map<?, ?> map) {
            List<String> parts = new ArrayList<>();
            map.forEach((key, item) -> parts.add("\"" + escape(String.valueOf(key)) + "\":" + value(item)));
            return "{" + String.join(",", parts) + "}";
        }
        if (value instanceof Iterable<?> iterable) { List<String> parts = new ArrayList<>(); iterable.forEach(item -> parts.add(value(item))); return "[" + String.join(",", parts) + "]"; }
        return "\"" + escape(String.valueOf(value)) + "\"";
    }
    private static String escape(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t"); }
}
