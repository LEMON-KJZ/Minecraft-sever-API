package com.example.serverapi.http;

import java.util.Map;

public record HttpResponse(int status, Map<String, String> headers, String body) {
    public static HttpResponse json(int status, String body) {
        return new HttpResponse(status, Map.of("Content-Type", "application/json; charset=utf-8", "Cache-Control", "no-store"), body);
    }
    public static HttpResponse error(int status, String error, String message) {
        return json(status, JsonCodec.object(Map.of("error", error, "message", message)));
    }
}
