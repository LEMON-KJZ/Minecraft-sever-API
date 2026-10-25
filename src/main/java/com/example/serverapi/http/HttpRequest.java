package com.example.serverapi.http;

import java.util.Map;

public record HttpRequest(String method, String path, String body, Map<String, String> headers, String remoteAddress) {
    public String header(String name) {
        return headers.entrySet().stream().filter(entry -> entry.getKey().equalsIgnoreCase(name)).map(Map.Entry::getValue).findFirst().orElse(null);
    }
}
