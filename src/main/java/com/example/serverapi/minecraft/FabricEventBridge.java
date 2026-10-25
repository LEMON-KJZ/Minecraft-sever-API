package com.example.serverapi.minecraft;

import com.example.serverapi.events.EventBus;
import com.example.serverapi.events.ServerEvent;
import com.example.serverapi.logging.LogBuffer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public final class FabricEventBridge {
    private FabricEventBridge() {}

    public static void register(EventBus events, LogBuffer logs) {
        AtomicLong ids = new AtomicLong();
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            events.publish(new ServerEvent(ids.incrementAndGet(), "join", Map.of("name", player.getGameProfile().name(), "uuid", player.getUUID().toString())));
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.getPlayer();
            events.publish(new ServerEvent(ids.incrementAndGet(), "quit", Map.of("name", player.getGameProfile().name(), "uuid", player.getUUID().toString())));
        });
        ServerMessageEvents.CHAT_MESSAGE.register((message, player, bound) ->
                events.publish(new ServerEvent(ids.incrementAndGet(), "chat", Map.of("name", player.getGameProfile().name(), "message", message.signedContent()))));
        ServerLivingEntityEvents.AFTER_DEATH.register((LivingEntity entity, net.minecraft.world.damagesource.DamageSource source) -> {
            if (entity instanceof ServerPlayer player) {
                events.publish(new ServerEvent(ids.incrementAndGet(), "death", Map.of("name", player.getGameProfile().name(), "message", String.valueOf(source.getMsgId()))));
            }
        });
        logs.append("Server API event bridge registered");
        events.publish(new ServerEvent(ids.incrementAndGet(), "log", Map.of("message", "Server API event bridge registered")));
    }
}
