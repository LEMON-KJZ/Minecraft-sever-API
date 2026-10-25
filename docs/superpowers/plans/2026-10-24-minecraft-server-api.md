# Minecraft Server API Fabric Mod Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build a server-only Fabric mod for Minecraft Java 26.3 that exposes a configurable, authenticated HTTP API for safe main-thread server control and status/event inspection.

**Architecture:** A Fabric server entrypoint owns lifecycle and registrations. A Java 25 JDK HTTP server provides routing, Bearer authentication, JSON responses, rate limiting, SSE, and optional TLS. A main-thread executor is the only bridge to `MinecraftServer`; services create immutable snapshots and command results for HTTP handlers.

**Tech Stack:** Java 25, Gradle, Fabric Loom, Fabric Loader 0.19.5+, Fabric API 0.160.5+, Minecraft 26.3, Jackson-free JDK JSON helpers (small internal JSON codec), JUnit 5 for pure Java tests.

**Spec:** `docs/superpowers/specs/2026-10-24-minecraft-server-api-design.md`

## Global Constraints

- Target Minecraft Java Edition 26.3.
- Require Fabric Loader 0.19.5+ and Fabric API 0.160.5+.
- Compile and run with Java 25.
- Server-only mod; no client installation, web panel, proxy, or external backend.
- Default API bind address is `127.0.0.1`; configurable public binding such as `0.0.0.0`.
- API port is separate from the Minecraft game port and must fail clearly on conflict.
- Authentication is `Authorization: Bearer API_KEY`; key is generated, persisted, shown once on first generation, and never hard-coded.
- Arbitrary console commands are allowed and must execute on the Minecraft server thread.

## Review Focus

- Port equal to the Minecraft server port: startup must refuse the API with a clear error and leave the game server running.
- Missing, malformed, or wrong Bearer header: return stable JSON 401 without revealing the key.
- Slow or failing main-thread task: return a bounded 504/503 response and never mutate Minecraft state off-thread.
- Client disconnect during SSE or server shutdown: remove subscriber and terminate the connection without leaking threads.
- Malformed JSON, unknown player/rule, and command failure: return consistent JSON errors with no stack trace leakage.

### Task 1: Gradle/Fabric project scaffold

**Files:**
- Create: `settings.gradle`, `build.gradle`, `gradle.properties`, `gradle/wrapper/gradle-wrapper.properties`
- Create: `src/main/resources/fabric.mod.json`, `src/main/resources/serverapi.mixins.json` (only if required), `LICENSE`
- Create: `src/main/java/com/example/serverapi/ServerApiMod.java`
- Test: `src/test/java/com/example/serverapi/ProjectMetadataTest.java`

**Interfaces:**
- Produces Fabric entrypoint `com.example.serverapi.ServerApiMod` and mod id `serverapi`.
- Exposes Gradle tasks `test`, `build`, and `remapJar` with Java 25 toolchain.

- [ ] **Step 1: Add Fabric project metadata** with Minecraft 26.3, loader 0.19.5, Fabric API 0.160.5+, Java 25, and a server entrypoint.
- [ ] **Step 2: Add minimal entrypoint** that logs lifecycle start/stop without opening the API yet.
- [ ] **Step 3: Add metadata test** asserting mod id, version, and server-only environment are present in `fabric.mod.json`.
- [ ] **Step 4: Run `./gradlew test`** and confirm the scaffold compiles.

### Task 2: Configuration, API key, auth, and rate limit

**Files:**
- Create: `src/main/java/com/example/serverapi/config/ServerApiConfig.java`
- Create: `src/main/java/com/example/serverapi/config/ConfigStore.java`
- Create: `src/main/java/com/example/serverapi/security/ApiKeyManager.java`
- Create: `src/main/java/com/example/serverapi/security/BearerAuthenticator.java`
- Create: `src/main/java/com/example/serverapi/security/RateLimiter.java`
- Test: `src/test/java/com/example/serverapi/config/ConfigStoreTest.java`
- Test: `src/test/java/com/example/serverapi/security/SecurityTest.java`

**Interfaces:**
- `ServerApiConfig.load(Path)` / `save(Path)` reads and writes JSON fields `bindAddress`, `port`, `apiKey`, `tlsEnabled`, `keystorePath`, `keystorePassword`, `rateLimitPerMinute`, `logBufferSize`, `requestTimeoutMillis`.
- `ApiKeyManager.ensureKey(ServerApiConfig, Path, Consumer<String>)` returns a key and invokes the callback only for a newly generated key; `generateNewKey(...)` rotates and persists it.
- `BearerAuthenticator.authenticate(String authorization, String expectedKey) -> AuthResult` uses constant-time comparison.
- `RateLimiter.tryAcquire(String clientKey) -> boolean` enforces a bounded per-client window.

- [ ] **Step 1: Write failing tests** for first-start generation, persistence, explicit rotation, Bearer parsing, wrong/missing key, port defaults, and rate limiting.
- [ ] **Step 2: Implement JSON config store** with safe defaults (`127.0.0.1`, `8080`, key absent until generated) and atomic replacement writes.
- [ ] **Step 3: Implement key generation** with `SecureRandom` and URL-safe Base64; never log the key except the one-time callback.
- [ ] **Step 4: Implement constant-time auth and rate limiter** with bounded in-memory state.
- [ ] **Step 5: Run the focused tests** and confirm PASS.

### Task 3: Main-thread execution and state services

**Files:**
- Create: `src/main/java/com/example/serverapi/minecraft/MinecraftExecutor.java`
- Create: `src/main/java/com/example/serverapi/minecraft/GameStateService.java`
- Create: `src/main/java/com/example/serverapi/minecraft/CommandService.java`
- Create: `src/main/java/com/example/serverapi/minecraft/PlayerSnapshot.java`
- Create: `src/main/java/com/example/serverapi/minecraft/ServerSnapshot.java`
- Test: `src/test/java/com/example/serverapi/minecraft/MinecraftExecutorTest.java`

**Interfaces:**
- `MinecraftExecutor.submit(Callable<T>, Duration) -> CompletableFuture<T>` schedules work on `MinecraftServer.execute` and completes exceptionally on timeout.
- `GameStateService.status()` and `.players()` return immutable snapshots created on the server thread.
- `CommandService.execute(String command)` returns `{success, resultCode, feedback, durationMs}` and uses the server command dispatcher with a server source.

- [ ] **Step 1: Write failing executor tests** for server-thread dispatch, completion, timeout, and propagated exception.
- [ ] **Step 2: Implement executor** with one bounded timeout scheduler and no direct off-thread Minecraft access.
- [ ] **Step 3: Implement status/player snapshots** including versions, uptime, memory, online/max players, dimension, coordinates, health, hunger, gamemode, ping, level, and optional TPS/MSPT.
- [ ] **Step 4: Implement arbitrary command execution** and structured feedback capture.
- [ ] **Step 5: Run focused tests and compile against mapped Minecraft classes.**

### Task 4: HTTP server, routing, JSON, and TLS

**Files:**
- Create: `src/main/java/com/example/serverapi/http/ApiServer.java`
- Create: `src/main/java/com/example/serverapi/http/ApiRouter.java`
- Create: `src/main/java/com/example/serverapi/http/HttpRequest.java`
- Create: `src/main/java/com/example/serverapi/http/HttpResponse.java`
- Create: `src/main/java/com/example/serverapi/http/JsonCodec.java`
- Create: `src/main/java/com/example/serverapi/http/TlsContextFactory.java`
- Test: `src/test/java/com/example/serverapi/http/ApiRouterTest.java`

**Interfaces:**
- `ApiServer.start()` / `stop()` own the executor, listener, TLS context, and shutdown cleanup.
- `ApiRouter.route(HttpRequest) -> HttpResponse` handles `/api/v1/*`, OPTIONS, JSON errors, auth, and request size limits.
- `JsonCodec` serializes only internal DTOs and safely escapes user strings.

- [ ] **Step 1: Write failing route tests** for health, auth gating, malformed JSON, unknown path, and port conflict validation.
- [ ] **Step 2: Implement JDK HTTP/HTTPS listener** with configurable bind/port, a bounded request executor, optional PKCS12/JKS loading, and a clear port-conflict error.
- [ ] **Step 3: Implement JSON DTO serialization and uniform error envelopes** `{error, message}`.
- [ ] **Step 4: Implement core routes** `health`, `status`, `players`, `command`, `announce`, and player action routes, using `MinecraftExecutor` for all stateful work.
- [ ] **Step 5: Implement gamerule GET/PUT** with safe parsing of boolean/integer/enum values through the server command/game-rule APIs.
- [ ] **Step 6: Run route tests and a local loopback smoke test.**

### Task 5: Logs, events, SSE, and lifecycle integration

**Files:**
- Create: `src/main/java/com/example/serverapi/events/EventBus.java`
- Create: `src/main/java/com/example/serverapi/events/ServerEvent.java`
- Create: `src/main/java/com/example/serverapi/logging/LogBuffer.java`
- Create: `src/main/java/com/example/serverapi/minecraft/FabricEventBridge.java`
- Modify: `src/main/java/com/example/serverapi/ServerApiMod.java`
- Test: `src/test/java/com/example/serverapi/events/EventBusTest.java`

**Interfaces:**
- `EventBus.subscribe()` returns an `AutoCloseable` SSE subscription; `publish(ServerEvent)` is non-blocking.
- `LogBuffer.append(String)` and `.recent(int)` provide a bounded ring buffer.
- `FabricEventBridge.register(MinecraftServer, EventBus, LogBuffer)` maps join, quit, chat, death, command, and console log events.

- [ ] **Step 1: Write failing tests** for bounded logs, event fan-out, subscriber cleanup, and slow subscriber isolation.
- [ ] **Step 2: Implement log buffer and event bus** with bounded queues and JSON/SSE event formatting.
- [ ] **Step 3: Register Fabric callbacks** for player join/quit/chat/death and command/log publication.
- [ ] **Step 4: Implement `/logs` and `/events`** with `Last-Event-ID` support where available, heartbeat comments, disconnect cleanup, and shutdown completion.
- [ ] **Step 5: Run focused tests and a server lifecycle smoke test.**

### Task 6: Commands, extensions, documentation, packaging

**Files:**
- Modify: `src/main/java/com/example/serverapi/ServerApiMod.java`
- Create: `src/main/java/com/example/serverapi/commands/ServerApiCommand.java`
- Create: `src/main/java/com/example/serverapi/extensions/BackupService.java`
- Create: `src/main/java/com/example/serverapi/extensions/SchedulerService.java`
- Create: `config/serverapi.json.example`
- Create: `README.md`, `docs/API.md`, `docs/INSTALL.md`, `docs/CURL.md`
- Create: `outputs/serverapi-fabric-26.3-1.0.0.jar` (build output)

**Interfaces:**
- `/serverapi token generate` rotates the key, persists it, and sends the new key only to the command source once.
- `BackupService.createBackup()` saves the world and creates a timestamped zip/directory under a configured backup directory with path traversal checks.
- `SchedulerService` supports configured periodic announcements and is stopped with the server.

- [ ] **Step 1: Add server command registration** and tests for permission/source behavior.
- [ ] **Step 2: Add optional save/backup, loaded-mod list, config snapshot, world/weather/difficulty, and periodic broadcast endpoints only where the mapped API is stable; keep core endpoints independent.**
- [ ] **Step 3: Write installation/API/config/Key rotation/port/TLS/curl docs with explicit public-bind and firewall notes.**
- [ ] **Step 4: Run `./gradlew clean test build` with Java 25, inspect the remapped JAR, and copy the verified artifact to `outputs/`.**
- [ ] **Step 5: Run final smoke checks** for manifest, no client entrypoint, key generation, route auth, port separation, and clean shutdown.

## Self-review

The plan covers all required core endpoints, authentication, main-thread execution, configuration, events, logs, gamerules, command management, port separation, optional TLS, and delivery artifacts. Optional extensions are isolated behind services so failure there cannot disable health/status/command APIs. The tests cover the five highest-risk inputs listed in Review Focus and keep Minecraft-dependent tests thin enough for a Gradle build without a live server.
