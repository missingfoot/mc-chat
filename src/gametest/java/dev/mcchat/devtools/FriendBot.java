package dev.mcchat.devtools;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.mcchat.server.McChatServer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Dev-only fake chat partner for ./gradlew runDevServer. Inert unless -Dmcchat.friendbot=true.
 * Replies to your messages after a short delay; /friend say|auto|spam for manual control.
 */
public class FriendBot implements ModInitializer {
	private static final UUID ID = UUID.nameUUIDFromBytes("mcchat-friend-bot".getBytes(StandardCharsets.UTF_8));
	private static final String NAME = "Friend";
	private static final int REPLY_DELAY_TICKS = 40;
	private static final String[] REPLIES = {
			"haha yes", "omg same", "wait where are you?", "brb getting food", "love that 💖",
			"ok coming over", "did you see that creeper??", "this is so much nicer than normal chat",
			"can you grab me some iron", "lol"
	};

	private record Pending(int dueTick, String text) {
	}

	private static final List<Pending> PENDING = new ArrayList<>();
	private static boolean autoReply = true;
	private static int tick;

	@Override
	public void onInitialize() {
		if (!Boolean.getBoolean("mcchat.friendbot")) {
			return;
		}
		McChatServer.addListener(message -> {
			if (autoReply && !message.senderId().equals(ID)) {
				PENDING.add(new Pending(tick + REPLY_DELAY_TICKS, REPLIES[ThreadLocalRandom.current().nextInt(REPLIES.length)]));
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(FriendBot::onTick);
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> dispatcher.register(
				Commands.literal("friend")
						.then(Commands.literal("say").then(Commands.argument("message", StringArgumentType.greedyString())
								.executes(c -> {
									say(c.getSource().getServer(), StringArgumentType.getString(c, "message"));
									return 1;
								})))
						.then(Commands.literal("auto")
								.then(Commands.literal("on").executes(c -> setAuto(c.getSource(), true)))
								.then(Commands.literal("off").executes(c -> setAuto(c.getSource(), false))))
						.then(Commands.literal("spam").executes(c -> {
							for (int i = 1; i <= 25; i++) {
								say(c.getSource().getServer(), "scroll test message " + i);
							}
							return 1;
						}))));
	}

	private static void onTick(MinecraftServer server) {
		tick++;
		Iterator<Pending> it = PENDING.iterator();
		while (it.hasNext()) {
			Pending p = it.next();
			if (p.dueTick() <= tick) {
				it.remove();
				say(server, p.text());
			}
		}
	}

	private static void say(MinecraftServer server, String text) {
		McChatServer.broadcast(server, ID, NAME, text);
	}

	private static int setAuto(net.minecraft.commands.CommandSourceStack source, boolean on) {
		autoReply = on;
		source.sendSystemMessage(Component.literal("Friend auto-reply " + (on ? "on" : "off")));
		return 1;
	}
}
