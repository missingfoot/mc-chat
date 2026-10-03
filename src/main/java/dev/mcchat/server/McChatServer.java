package dev.mcchat.server;

import dev.mcchat.core.ChatHistoryStore;
import dev.mcchat.core.ChatMessage;
import dev.mcchat.core.TextSanitizer;
import dev.mcchat.net.DeliverPayload;
import dev.mcchat.net.HistoryPayload;
import dev.mcchat.net.SendPayload;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;

/** Relays chat between players and keeps the per-world history file. Runs on integrated and dedicated servers. */
public final class McChatServer {
	public static final int JOIN_HISTORY_SIZE = 100;

	private static ChatHistoryStore store;

	private McChatServer() {
	}

	public static void init() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			Path file = server.getWorldPath(LevelResource.ROOT).resolve("mcchat").resolve("history.jsonl");
			store = new ChatHistoryStore(file, ChatHistoryStore.DEFAULT_CAPACITY);
			store.load();
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			if (store != null) {
				store.compact();
			}
			store = null;
		});
		ServerPlayNetworking.registerGlobalReceiver(SendPayload.TYPE,
				(payload, context) -> onSend(context.server(), context.player(), payload.text()));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			if (store != null && ServerPlayNetworking.canSend(handler, HistoryPayload.TYPE)) {
				sender.sendPacket(new HistoryPayload(store.recent(JOIN_HISTORY_SIZE)));
			}
		});
	}

	private static void onSend(MinecraftServer server, ServerPlayer player, String raw) {
		String text = TextSanitizer.sanitize(raw);
		if (text.isEmpty() || store == null) {
			return;
		}
		ChatMessage message = new ChatMessage(player.getUUID(), player.getName().getString(), text, System.currentTimeMillis());
		store.append(message);
		DeliverPayload delivery = new DeliverPayload(message);
		for (ServerPlayer target : server.getPlayerList().getPlayers()) {
			if (ServerPlayNetworking.canSend(target, DeliverPayload.TYPE)) {
				ServerPlayNetworking.send(target, delivery);
			}
		}
	}
}
