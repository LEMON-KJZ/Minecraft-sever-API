package com.example.serverapi.minecraft;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;

import java.util.LinkedHashMap;
import java.util.Map;

public final class GameRuleService {
    private GameRuleService() {}

    public static Map<String, String> snapshot(MinecraftServer server) {
        GameRules rules = server.getGameRules();
        Map<String, String> result = new LinkedHashMap<>();
        rules.availableRules().forEach(rule -> addRule(result, rules, rule));
        return Map.copyOf(result);
    }

    private static <T> void addRule(Map<String, String> result, GameRules rules, GameRule<T> rule) {
        result.put(rule.id(), rules.getAsString(rule));
    }
}
