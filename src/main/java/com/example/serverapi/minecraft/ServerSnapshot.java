package com.example.serverapi.minecraft;

public record ServerSnapshot(String minecraftVersion, String fabricVersion, long uptimeMillis,
                             long memoryUsedBytes, long memoryMaxBytes, Double tps, Double mspt,
                             int onlinePlayers, int maxPlayers, boolean running) {}
