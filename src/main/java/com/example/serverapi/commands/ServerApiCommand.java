package com.example.serverapi.commands;

import com.example.serverapi.config.ServerApiConfig;
import com.example.serverapi.security.ApiKeyManager;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionLevel;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Supplier;

public final class ServerApiCommand {
    private ServerApiCommand() {}

    public static void register(Supplier<ServerApiConfig> configSupplier, Supplier<Path> pathSupplier) {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, selection) -> register(dispatcher, configSupplier, pathSupplier));
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher, Supplier<ServerApiConfig> configSupplier, Supplier<Path> pathSupplier) {
        dispatcher.register(Commands.literal("serverapi")
                .requires(source -> source.permissions() instanceof LevelBasedPermissionSet level
                        && level.level().isEqualOrHigherThan(PermissionLevel.ADMINS))
                .then(Commands.literal("token")
                        .then(Commands.literal("generate").executes(context -> generate(context.getSource(), configSupplier, pathSupplier)))));
    }

    private static int generate(CommandSourceStack source, Supplier<ServerApiConfig> configSupplier, Supplier<Path> pathSupplier) {
        try {
            ServerApiConfig config = configSupplier.get();
            Path path = pathSupplier.get();
            if (config == null || path == null) throw new IllegalStateException("Server API has not started yet");
            String key = ApiKeyManager.generateNewKey(config, path);
            source.sendSystemMessage(Component.literal("Server API key regenerated: " + key));
            return 1;
        } catch (IOException | RuntimeException error) {
            source.sendSystemMessage(Component.literal("Could not regenerate Server API key: " + error.getMessage()));
            return 0;
        }
    }
}
