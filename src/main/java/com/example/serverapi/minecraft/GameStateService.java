package com.example.serverapi.minecraft;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Builds immutable snapshots while the caller is already on the server thread. */
public final class GameStateService {
    private final Object server;
    private final long startedAtMillis;
    private final String fabricVersion;

    public GameStateService(Object server, long startedAtMillis, String fabricVersion) {
        this.server = server;
        this.startedAtMillis = startedAtMillis;
        this.fabricVersion = fabricVersion;
    }

    public ServerSnapshot status() {
        Object playerList = invoke(server, "getPlayerList");
        int online = size(invoke(playerList, "getPlayers"));
        int max = intValue(invoke(playerList, "getMaxPlayers"), 0);
        long used = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        double msptValue = doubleValue(invoke(server, "getAverageTickTimeNanos"), 0) / 1_000_000.0;
        Double mspt = msptValue > 0 ? msptValue : null;
        Double tps = mspt == null ? null : Math.min(20.0, 1000.0 / mspt);
        return new ServerSnapshot(stringValue(invoke(server, "getServerVersion"), "26.3"), fabricVersion,
                Math.max(0, System.currentTimeMillis() - startedAtMillis), used,
                Runtime.getRuntime().maxMemory(), tps, mspt, online, max,
                boolValue(invoke(server, "isRunning"), true));
    }

    public List<PlayerSnapshot> players() {
        Object playerList = invoke(server, "getPlayerList");
        Object values = invoke(playerList, "getPlayers");
        if (!(values instanceof Iterable<?> iterable)) return List.of();
        List<PlayerSnapshot> result = new ArrayList<>();
        for (Object player : iterable) result.add(snapshot(player));
        return Collections.unmodifiableList(result);
    }

    private static PlayerSnapshot snapshot(Object player) {
        Object profile = invoke(player, "getGameProfile");
        UUID uuid = uuidValue(invoke(profile, "id", "getId"));
        String name = stringValue(invoke(profile, "name", "getName"), stringValue(invoke(player, "getName"), "unknown"));
        Object level = invoke(player, "level");
        Object dimensionKey = invoke(invoke(level, "dimension"), "location");
        String dimension = stringValue(dimensionKey, stringValue(invoke(level, "dimension"), "unknown"));
        Object food = invoke(player, "getFoodData");
        Object gameMode = invoke(invoke(player, "gameMode"), "getGameModeForPlayer");
        Object connection = invoke(player, "connection");
        return new PlayerSnapshot(name, uuid, dimension, doubleValue(invoke(player, "getX"), 0),
                doubleValue(invoke(player, "getY"), 0), doubleValue(invoke(player, "getZ"), 0),
                floatValue(invoke(player, "getHealth"), 0), intValue(invoke(food, "getFoodLevel"), 0),
                stringValue(invoke(gameMode, "getName"), "unknown"), intValue(invoke(player, "getLatency"), intField(connection, "latency", 0)),
                intValue(invoke(player, "getExperienceLevel"), intField(player, "experienceLevel", 0)));
    }

    private static Object invoke(Object target, String... names) {
        if (target == null) return null;
        for (String name : names) {
            try {
                Method method = target.getClass().getMethod(name);
                method.setAccessible(true);
                return method.invoke(target);
            } catch (ReflectiveOperationException ignored) { }
            try {
                Field field = target.getClass().getField(name);
                return field.get(target);
            } catch (ReflectiveOperationException ignored) { }
        }
        return null;
    }
    private static int intField(Object target, String name, int fallback) {
        if (target == null) return fallback;
        try { Field field = target.getClass().getField(name); return field.getInt(target); }
        catch (ReflectiveOperationException ignored) { return fallback; }
    }
    private static int size(Object value) { return value instanceof java.util.Collection<?> collection ? collection.size() : 0; }
    private static String stringValue(Object value, String fallback) { return value == null ? fallback : String.valueOf(value); }
    private static boolean boolValue(Object value, boolean fallback) { return value instanceof Boolean b ? b : fallback; }
    private static int intValue(Object value, int fallback) { return value instanceof Number n ? n.intValue() : fallback; }
    private static float floatValue(Object value, float fallback) { return value instanceof Number n ? n.floatValue() : fallback; }
    private static double doubleValue(Object value, double fallback) { return value instanceof Number n ? n.doubleValue() : fallback; }
    private static UUID uuidValue(Object value) { return value instanceof UUID uuid ? uuid : new UUID(0, 0); }
}
