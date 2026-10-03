package dev.mcchat.client;

import dev.mcchat.core.ChatMessage;
import dev.mcchat.core.ChatRules;
import dev.mcchat.core.McChatConfig;
import dev.mcchat.core.SentHistory;
import dev.mcchat.net.DeliverPayload;
import dev.mcchat.net.HistoryPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

import java.nio.file.Path;
import java.util.UUID;

public class McChatClient implements ClientModInitializer {
	private static final int SENT_HISTORY_SIZE = 50;

	private static McChatConfig config;
	private static SentHistory sentHistory;

	public static McChatConfig config() {
		return config;
	}

	public static SentHistory sentHistory() {
		return sentHistory;
	}

	@Override
	public void onInitializeClient() {
		Path configDir = FabricLoader.getInstance().getConfigDir();
		config = McChatConfig.load(configDir.resolve("mcchat.json"));
		sentHistory = new SentHistory(configDir.resolve("mcchat-sent.txt"), SENT_HISTORY_SIZE).load();

		ClientPlayNetworking.registerGlobalReceiver(HistoryPayload.TYPE,
				(payload, context) -> ClientChatState.replaceAll(payload.messages()));
		ClientPlayNetworking.registerGlobalReceiver(DeliverPayload.TYPE,
				(payload, context) -> onDeliver(context.client(), payload.message()));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(ClientChatState::clear));
	}

	private static void onDeliver(Minecraft client, ChatMessage message) {
		ClientChatState.add(message);
		UUID localId = client.player != null ? client.player.getUUID() : null;
		if (!ChatRules.isFromOther(message, localId)) {
			return;
		}
		ClientChatState.markUnread();
		if (config.soundEnabled) {
			client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BELL.value(), 1.8f, (float) config.soundVolume));
		}
	}
}
