package com.example.serverapi.http;

import com.example.serverapi.config.ServerApiConfig;
import com.example.serverapi.events.EventBus;
import com.example.serverapi.events.ServerEvent;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ApiServer implements AutoCloseable {
    private static final int MAX_BODY_BYTES = 1_048_576;
    private final ServerApiConfig config;
    private final int minecraftPort;
    private final ApiRouter router;
    private HttpServer server;
    private ExecutorService executor;

    public ApiServer(ServerApiConfig config, int minecraftPort, ApiRouter router) {
        this.config = config;
        this.minecraftPort = minecraftPort;
        this.router = router;
    }

    public synchronized void start() throws Exception {
        if (server != null) return;
        config.validateAgainstMinecraftPort(minecraftPort);
        InetSocketAddress address = new InetSocketAddress(config.bindAddress(), config.port());
        if (config.tlsEnabled()) {
            HttpsServer https = HttpsServer.create(address, 0);
            SSLContext context = TlsContextFactory.create(config);
            https.setHttpsConfigurator(new HttpsConfigurator(context));
            server = https;
        } else {
            server = HttpServer.create(address, 0);
        }
        server.createContext("/", this::handle);
        executor = Executors.newFixedThreadPool(8, task -> {
            Thread thread = new Thread(task, "serverapi-http");
            thread.setDaemon(true);
            return thread;
        });
        server.setExecutor(executor);
        server.start();
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            byte[] bytes = exchange.getRequestBody().readNBytes(MAX_BODY_BYTES + 1);
            if (bytes.length > MAX_BODY_BYTES) {
                write(exchange, HttpResponse.error(413, "payload_too_large", "Request body is too large"));
                return;
            }
            HashMap<String, String> headers = new HashMap<>();
            exchange.getRequestHeaders().forEach((key, values) -> { if (!values.isEmpty()) headers.put(key, values.get(0)); });
            HttpRequest request = new HttpRequest(exchange.getRequestMethod(), exchange.getRequestURI().toString(),
                    new String(bytes, StandardCharsets.UTF_8), headers, exchange.getRemoteAddress().getAddress().getHostAddress());
            if (request.path().split("\\?", 2)[0].equals("/api/v1/events") && request.method().equals("GET") && router.events() != null) {
                if (!router.authenticated(request)) { write(exchange, HttpResponse.error(401, "unauthorized", "Bearer authentication is required")); return; }
                streamEvents(exchange, router.events());
                return;
            }
            write(exchange, router.route(request));
        } catch (Exception error) {
            write(exchange, HttpResponse.error(500, "internal_error", "The request could not be completed"));
        } finally {
            exchange.close();
        }
    }

    private static void streamEvents(HttpExchange exchange, EventBus bus) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/event-stream; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        exchange.getResponseHeaders().set("Connection", "keep-alive");
        exchange.sendResponseHeaders(200, 0);
        EventBus.Subscription subscription = bus.subscribe();
        try {
            var output = exchange.getResponseBody();
            output.write(": connected\n\n".getBytes(StandardCharsets.UTF_8));
            output.flush();
            while (!subscription.isClosed()) {
                ServerEvent event;
                try { event = subscription.poll(java.time.Duration.ofSeconds(15)); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); break; }
                String payload = event == null
                        ? ": heartbeat\n\n"
                        : "id: " + event.id() + "\nevent: " + event.type() + "\ndata: " + JsonCodec.object(event.data()) + "\n\n";
                output.write(payload.getBytes(StandardCharsets.UTF_8));
                output.flush();
            }
        } catch (IOException disconnected) {
            // The client closed the stream; cleanup happens in finally.
        } finally { subscription.close(); }
    }

    private static void write(HttpExchange exchange, HttpResponse response) throws IOException {
        response.headers().forEach((key, value) -> exchange.getResponseHeaders().set(key, value));
        byte[] body = response.body().getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(response.status(), body.length);
        exchange.getResponseBody().write(body);
    }

    public synchronized void stop() {
        if (server != null) server.stop(0);
        if (executor != null) executor.shutdownNow();
        server = null;
        executor = null;
    }

    @Override public void close() { stop(); }
}
