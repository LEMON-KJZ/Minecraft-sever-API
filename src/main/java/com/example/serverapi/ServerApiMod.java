package com.example.serverapi;

import com.example.serverapi.commands.ServerApiCommand;
import com.example.serverapi.config.ServerApiConfig;
import com.example.serverapi.events.EventBus;
import com.example.serverapi.events.ServerEvent;
import com.example.serverapi.extensions.SchedulerService;
import com.example.serverapi.http.ApiRouter;
import com.example.serverapi.http.ApiServer;
import com.example.serverapi.logging.LogBuffer;
import com.example.serverapi.logging.ConsoleLogBridge;
import com.example.serverapi.minecraft.CommandService;
import com.example.serverapi.minecraft.FabricEventBridge;
import com.example.serverapi.minecraft.GameStateService;
import com.example.serverapi.minecraft.GameRuleService;
import com.example.serverapi.minecraft.MinecraftExecutor;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class ServerApiMod implements ModInitializer {
    public static final String MOD_ID = "serverapi";
    public static final String MOD_VERSION = "1.0.0";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static ServerApiConfig config;
    private static Path configPath;
    private static ApiServer apiServer;
    private static MinecraftExecutor minecraftExecutor;
    private static SchedulerService scheduler;
    private static ConsoleLogBridge logBridge;
    private static final EventBus EVENTS = new EventBus(256);
    private static final LogBuffer LOGS = new LogBuffer(500);

    @Override
    public void onInitialize() {
        ServerApiCommand.register(() -> config, () -> configPath);
        FabricEventBridge.register(EVENTS, LOGS);
        ServerLifecycleEvents.SERVER_STARTED.register(this::startServerApi);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> stopServerApi());
        LOGGER.info("Server API mod {} initialized for server use", MOD_VERSION);
    }

    private void startServerApi(MinecraftServer server) {
        try {
            configPath = server.getServerDirectory().resolve("config").resolve("serverapi.json");
            config = ServerApiConfig.load(configPath);
            config.validateAgainstMinecraftPort(server.getPort());
            com.example.serverapi.security.ApiKeyManager.ensureKey(config, configPath,
                    key -> LOGGER.info("Server API key generated once during first startup: {}", key));
            logBridge = ConsoleLogBridge.install(LOGS, EVENTS, () -> config.apiKey());
            long startedAt = System.currentTimeMillis();
            minecraftExecutor = new MinecraftExecutor(server::execute, java.time.Duration.ofMillis(config.requestTimeoutMillis()));
            scheduler = new SchedulerService();
            String fabricVersion = FabricLoader.getInstance().getModContainer(MOD_ID)
                    .map(container -> container.getMetadata().getVersion().getFriendlyString()).orElse(MOD_VERSION);
            GameStateService state = new GameStateService(server, startedAt, fabricVersion);
            CommandService commands = new CommandService(minecraftExecutor, command -> executeCommand(server, command));
            ApiRouter router = new ApiRouter(config, minecraftExecutor, state::status, state::players, commands, LOGS, EVENTS, () -> GameRuleService.snapshot(server));
            apiServer = new ApiServer(config, server.getPort(), router);
            apiServer.start();
            LOGS.append("Server API listening on " + config.bindAddress() + ":" + config.port());
            LOGGER.info("Server API listening on {}:{}", config.bindAddress(), config.port());
        } catch (Exception error) {
            LOGGER.error("Server API failed to start; Minecraft server remains running", error);
            stopServerApi();
        }
    }

    private static CommandService.CommandResult executeCommand(MinecraftServer server, String command) {
        long started = System.nanoTime();
        AtomicInteger resultCode = new AtomicInteger(0);
        CommandSourceStack source = server.createCommandSourceStack()
                .withCallback((success, result) -> resultCode.set(result));
        server.getCommands().performPrefixedCommand(source, command);
        CommandService.CommandResult result = new CommandService.CommandResult(resultCode.get() > 0, resultCode.get(), "Command executed", (System.nanoTime() - started) / 1_000_000);
        EVENTS.publish(new ServerEvent(System.nanoTime(), "command", Map.of("command", command, "success", result.success(), "resultCode", result.resultCode())));
        return result;
    }

    private static void stopServerApi() {
        if (apiServer != null) apiServer.stop();
        if (minecraftExecutor != null) minecraftExecutor.close();
        if (scheduler != null) scheduler.close();
        if (logBridge != null) logBridge.close();
        EVENTS.closeAll();
        apiServer = null;
        minecraftExecutor = null;
        scheduler = null;
        logBridge = null;
    }
}
