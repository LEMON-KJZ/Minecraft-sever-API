package com.example.serverapi.minecraft;

import java.util.UUID;

public record PlayerSnapshot(String name, UUID uuid, String dimension, double x, double y, double z,
                             float health, int hunger, String gameMode, int latency, int level) {}
