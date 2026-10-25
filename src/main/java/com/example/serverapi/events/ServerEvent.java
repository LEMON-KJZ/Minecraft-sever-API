package com.example.serverapi.events;

import java.util.Map;

public record ServerEvent(long id, String type, Map<String, ?> data) {}
