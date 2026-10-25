package com.example.serverapi.http;

import com.example.serverapi.config.ServerApiConfig;
import com.example.serverapi.events.EventBus;
import com.example.serverapi.logging.LogBuffer;
import com.example.serverapi.minecraft.CommandService;
import com.example.serverapi.minecraft.MinecraftExecutor;
import com.example.serverapi.minecraft.PlayerSnapshot;
import com.example.serverapi.minecraft.ServerSnapshot;
import com.example.serverapi.security.BearerAuthenticator;
import com.example.serverapi.security.RateLimiter;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class ApiRouter {
    private final ServerApiConfig config;
    private final MinecraftExecutor executor;
    private final Supplier<ServerSnapshot> status;
    private final Supplier<List<PlayerSnapshot>> players;
    private final CommandService commands;
    private final LogBuffer logs;
    private final EventBus events;
    private final Supplier<Map<String, String>> gameRules;
    private final RateLimiter rateLimiter;

    public ApiRouter(ServerApiConfig config, MinecraftExecutor executor, Supplier<ServerSnapshot> status,
                     Supplier<List<PlayerSnapshot>> players, CommandService commands) {
        this(config, executor, status, players, commands, null, null, Map::of);
    }

    public ApiRouter(ServerApiConfig config, MinecraftExecutor executor, Supplier<ServerSnapshot> status,
                     Supplier<List<PlayerSnapshot>> players, CommandService commands, LogBuffer logs, EventBus events) {
        this(config, executor, status, players, commands, logs, events, Map::of);
    }

    public ApiRouter(ServerApiConfig config, MinecraftExecutor executor, Supplier<ServerSnapshot> status,
                     Supplier<List<PlayerSnapshot>> players, CommandService commands, LogBuffer logs, EventBus events,
                     Supplier<Map<String, String>> gameRules) {
        this.config = config; this.executor = executor; this.status = status; this.players = players; this.commands = commands; this.logs = logs; this.events = events; this.gameRules = gameRules;
        this.rateLimiter = new RateLimiter(config.rateLimitPerMinute(), Duration.ofMinutes(1));
    }

    public HttpResponse route(HttpRequest request) {
        String path = request.path().split("\\?", 2)[0];
        if (request.method().equals("OPTIONS")) return HttpResponse.json(204, "");
        if (path.equals("/api/v1/health") && request.method().equals("GET")) return HttpResponse.json(200, "{\"status\":\"ok\"}");
        if (!rateLimiter.tryAcquire(request.remoteAddress())) return HttpResponse.error(429, "rate_limited", "Too many requests");
        if (!authenticated(request)) return HttpResponse.error(401, "unauthorized", "Bearer authentication is required");
        try {
            if (path.equals("/api/v1/status") && request.method().equals("GET")) return await(executor.submit(() -> HttpResponse.json(200, JsonCodec.object(JsonCodec.snapshot(status.get()))), configTimeout()), configTimeout());
            if (path.equals("/api/v1/players") && request.method().equals("GET")) return await(executor.submit(() -> HttpResponse.json(200, JsonCodec.array(players.get().stream().map(JsonCodec::snapshot).collect(Collectors.toList()))), configTimeout()), configTimeout());
            if (path.equals("/api/v1/command") && request.method().equals("POST")) return command(request.body());
            if (path.equals("/api/v1/announce") && request.method().equals("POST")) return announce(request.body());
            if (path.equals("/api/v1/logs") && request.method().equals("GET")) return HttpResponse.json(200, JsonCodec.array(logs == null ? List.of() : logs.recent(100)));
            if (path.equals("/api/v1/events") && request.method().equals("GET")) return HttpResponse.error(501, "not_implemented", "Event streaming is enabled during server lifecycle integration");
            if (path.equals("/api/v1/gamerules") && request.method().equals("GET")) return await(executor.submit(() -> HttpResponse.json(200, JsonCodec.object(gameRules.get())), configTimeout()), configTimeout());
            if (path.startsWith("/api/v1/gamerules/") && request.method().equals("PUT")) return gamerule(path.substring("/api/v1/gamerules/".length()), request.body());
            if (path.startsWith("/api/v1/players/")) return playerAction(request, path.substring("/api/v1/players/".length()));
            return HttpResponse.error(404, "not_found", "Unknown API path");
        } catch (IllegalArgumentException error) { return HttpResponse.error(400, "bad_request", error.getMessage()); }
        catch (Exception error) { return HttpResponse.error(500, "internal_error", "The request could not be completed"); }
    }

    private HttpResponse command(String body) throws Exception {
        String command = JsonCodec.requiredString(body, "command");
        return commandResult(commands.execute(command));
    }
    private HttpResponse announce(String body) throws Exception {
        String message = JsonCodec.requiredString(body, "message");
        return commandResult(commands.execute("say " + message));
    }
    private HttpResponse playerAction(HttpRequest request, String remainder) throws Exception {
        String[] parts = remainder.split("/", 2);
        if (parts.length != 2 || parts[0].isBlank() || !parts[0].matches("[A-Za-z0-9_]{1,16}")) return HttpResponse.error(400, "bad_request", "Invalid player name");
        String player = parts[0], action = parts[1];
        if (action.equals("message") && request.method().equals("POST")) return commandResult(commands.execute("tell " + player + " " + JsonCodec.requiredString(request.body(), "message")));
        if ((action.equals("kick") || action.equals("ban") || action.equals("pardon")) && !request.method().equals("POST")) return HttpResponse.error(405, "method_not_allowed", "This player action uses POST");
        if ((action.equals("whitelist") || action.equals("op")) && !(request.method().equals("POST") || request.method().equals("DELETE"))) return HttpResponse.error(405, "method_not_allowed", "This player action uses POST or DELETE");
        String command = switch (action) {
            case "kick" -> "kick " + player;
            case "ban" -> "ban " + player;
            case "pardon" -> "pardon " + player;
            case "whitelist" -> request.method().equals("POST") ? "whitelist add " + player : "whitelist remove " + player;
            case "op" -> request.method().equals("POST") ? "op " + player : "deop " + player;
            default -> null;
        };
        if (command == null || !(request.method().equals("POST") || request.method().equals("DELETE"))) return HttpResponse.error(404, "not_found", "Unknown player action");
        return commandResult(commands.execute(command));
    }
    private HttpResponse gamerule(String name, String body) throws Exception { return commandResult(commands.execute("gamerule " + name + " " + JsonCodec.requiredString(body, "value"))); }
    private HttpResponse commandResult(java.util.concurrent.CompletableFuture<CommandService.CommandResult> future) throws Exception {
        CommandService.CommandResult result = await(future, configTimeout());
        return HttpResponse.json(result.success() ? 200 : 422, JsonCodec.object(Map.of("success", result.success(), "resultCode", result.resultCode(), "feedback", result.feedback(), "durationMs", result.durationMs())));
    }
    public boolean authenticated(HttpRequest request) { return BearerAuthenticator.authenticate(request.header("Authorization"), config.apiKey()).authenticated(); }
    public EventBus events() { return events; }
    private Duration configTimeout() { return Duration.ofMillis(config.requestTimeoutMillis()); }
    private static <T> T await(java.util.concurrent.CompletableFuture<T> future, Duration timeout) throws Exception { return future.get(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS); }
}
