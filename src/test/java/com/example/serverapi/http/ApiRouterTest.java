package com.example.serverapi.http;

import com.example.serverapi.config.ServerApiConfig;
import com.example.serverapi.minecraft.CommandService;
import com.example.serverapi.minecraft.MinecraftExecutor;
import com.example.serverapi.minecraft.PlayerSnapshot;
import com.example.serverapi.minecraft.ServerSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ApiRouterTest {
    @Test
    void healthIsPublicAndReportsRunning() {
        ApiRouter router = router();
        HttpResponse response = router.route(new HttpRequest("GET", "/api/v1/health", "", java.util.Map.of(), "127.0.0.1"));
        assertEquals(200, response.status());
        assertTrue(response.body().contains("\"status\":\"ok\""));
    }

    @Test
    void protectedRouteWithoutBearerReturnsUniformJson401() {
        HttpResponse response = router().route(new HttpRequest("GET", "/api/v1/status", "", java.util.Map.of(), "127.0.0.1"));
        assertEquals(401, response.status());
        assertTrue(response.body().contains("\"error\":\"unauthorized\""));
    }

    @Test
    void malformedCommandBodyReturns400() {
        HttpResponse response = router().route(new HttpRequest("POST", "/api/v1/command", "not-json",
                java.util.Map.of("Authorization", "Bearer secret"), "127.0.0.1"));
        assertEquals(400, response.status());
        assertTrue(response.body().contains("\"error\":\"bad_request\""));
    }

    @Test
    void authenticatedCommandReturnsCommandResult() {
        HttpResponse response = router().route(new HttpRequest("POST", "/api/v1/command", "{\"command\":\"say hi\"}",
                java.util.Map.of("Authorization", "Bearer secret"), "127.0.0.1"));
        assertEquals(200, response.status());
        assertTrue(response.body().contains("\"success\":true"));
    }

    @Test
    void unknownPathReturns404Json() {
        HttpResponse response = router().route(new HttpRequest("GET", "/api/v1/missing", "",
                java.util.Map.of("Authorization", "Bearer secret"), "127.0.0.1"));
        assertEquals(404, response.status());
        assertTrue(response.body().contains("\"error\":\"not_found\""));
    }

    @Test
    void authenticatedRequestsAreRateLimitedPerClient() {
        ServerApiConfig config = new ServerApiConfig("127.0.0.1", 8080, "secret", false, "", "", 1, 500, 5000);
        MinecraftExecutor executor = new MinecraftExecutor(Runnable::run);
        CommandService command = new CommandService(executor, ignored -> new CommandService.CommandResult(true, 1, "ok", 1));
        ServerSnapshot status = new ServerSnapshot("26.3", "1.0.0", 10, 2, 4, null, null, 0, 20, true);
        ApiRouter router = new ApiRouter(config, executor, () -> status, List::of, command);
        var request = new HttpRequest("GET", "/api/v1/status", "", java.util.Map.of("Authorization", "Bearer secret"), "127.0.0.1");
        assertEquals(200, router.route(request).status());
        assertEquals(429, router.route(request).status());
        executor.close();
    }

    private static ApiRouter router() {
        ServerApiConfig config = new ServerApiConfig("127.0.0.1", 8080, "secret", false, "", "", 120, 500, 5000);
        MinecraftExecutor executor = new MinecraftExecutor(Runnable::run);
        CommandService command = new CommandService(executor, ignored -> new CommandService.CommandResult(true, 1, "ok", 1));
        ServerSnapshot status = new ServerSnapshot("26.3", "1.0.0", 10, 2, 4, null, null, 1, 20, true);
        List<PlayerSnapshot> players = List.of(new PlayerSnapshot("Steve", UUID.randomUUID(), "minecraft:overworld", 1, 2, 3, 20, 20, "survival", 5, 3));
        return new ApiRouter(config, executor, () -> status, () -> players, command);
    }
}
