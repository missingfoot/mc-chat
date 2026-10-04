package dev.mcchat.client;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import dev.mcchat.core.ChatMessage;
import dev.mcchat.core.ChatRules;
import dev.mcchat.core.McChatConfig;
import dev.mcchat.core.SentHistory;
import dev.mcchat.net.DeliverPayload;
import dev.mcchat.net.HistoryPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.nio.file.Path;
import java.util.UUID;

public class McChatClient implements ClientModInitializer {
	private static final int SENT_HISTORY_SIZE = 50;

	private static Path configFile;
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
		configFile = configDir.resolve("mcchat.json");
		config = McChatConfig.load(configFile);
		sentHistory = new SentHistory(configDir.resolve("mcchat-sent.txt"), SENT_HISTORY_SIZE).load();

		ClientPlayNetworking.registerGlobalReceiver(HistoryPayload.TYPE,
				(payload, context) -> ClientChatState.replaceAll(payload.messages()));
		ClientPlayNetworking.registerGlobalReceiver(DeliverPayload.TYPE,
				(payload, context) -> onDeliver(context.client(), payload.message()));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(ClientChatState::clear));

		// /size [n] — chat text size; 0 is vanilla size, saved to the config.
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> dispatcher.register(
				ClientCommands.literal("size")
						.executes(c -> reportSize(c.getSource()))
						.then(ClientCommands.argument("size", IntegerArgumentType.integer(McChatConfig.MIN_TEXT_SIZE, McChatConfig.MAX_TEXT_SIZE))
								.executes(c -> {
									config.textSize = IntegerArgumentType.getInteger(c, "size");
									config.save(configFile);
									return reportSize(c.getSource());
								}))));
	}

	private static int reportSize(FabricClientCommandSource source) {
		source.sendFeedback(Component.literal("MC Chat text size: " + config.textSize + " (0 = normal, negative = smaller)"));
		return 1;
	}

	private static void onDeliver(Minecraft client, ChatMessage message) {
		ClientChatState.add(message);
		UUID localId = client.player != null ? client.player.getUUID() : null;
		if (!ChatRules.isFromOther(message, localId)) {
			return;
		}
		if (!(client.gui.screen() instanceof CompactChatScreen)) {
			ClientChatState.markUnread();
		}
		if (config.soundEnabled) {
			client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BELL.value(), 1.8f, (float) config.soundVolume));
		}
	}
}
