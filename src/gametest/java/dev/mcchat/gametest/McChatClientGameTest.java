package dev.mcchat.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import dev.mcchat.client.ClientChatState;
import dev.mcchat.client.CompactChatScreen;
import dev.mcchat.client.McChatClient;
import dev.mcchat.core.ChatMessage;
import dev.mcchat.net.DeliverPayload;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.gui.screens.ChatScreen;

import java.util.List;
import java.util.UUID;

/** Drives a real client through the chat flow. Screenshots land in the run directory's screenshots folder. */
public class McChatClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		TestWorldSave save;
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			save = world.getWorldSave();
			world.getConnection().waitForChunksRender();

			// T opens our screen, typing + Enter sends through the server and back.
			send(context, world, "hello 💖 §cred");
			check(lastText(context).equals("hello 💖 cred"), "sanitized message received, got: " + lastText(context));
			send(context, world, "second message");
			send(context, world, "third message");
			check(entryCount(context) == 3, "three messages received");
			context.takeScreenshot("mcchat-01-panel");

			// Own messages never flag unread.
			check(!context.computeOnClient(mc -> ClientChatState.hasUnread()), "own messages don't set unread");

			// Up-arrow recall, shown in the open screen.
			context.getInput().pressKey(options -> options.keyChat);
			context.waitForScreen(CompactChatScreen.class);
			context.getInput().pressKey(InputConstants.KEY_UP);
			context.waitTick();
			context.takeScreenshot("mcchat-02-screen-recall");
			context.getInput().pressKey(InputConstants.KEY_ESCAPE);
			context.waitForScreen(null);

			// "/" still opens vanilla command chat.
			context.getInput().pressKey(options -> options.keyCommand);
			context.waitForScreen(ChatScreen.class);
			context.getInput().pressKey(InputConstants.KEY_ESCAPE);
			context.waitForScreen(null);

			// A message from someone else sets unread; with F1 the dot shows.
			UUID other = UUID.randomUUID();
			world.getServer().runOnServer(server -> {
				ChatMessage m = new ChatMessage(other, "Friend", "hi from friend", System.currentTimeMillis());
				ServerPlayNetworking.send(server.getPlayerList().getPlayers().getFirst(), new DeliverPayload(m));
			});
			world.getConnection().waitForClientboundPackets();
			context.waitTick();
			check(context.computeOnClient(mc -> ClientChatState.hasUnread()), "other player's message sets unread");
			context.takeScreenshot("mcchat-03-friend-message");
			context.runOnClient(mc -> mc.gui.hud.toggle());
			context.waitTick();
			context.takeScreenshot("mcchat-04-f1-unread-dot");
			context.runOnClient(mc -> mc.gui.hud.toggle());

			// /size is a client command. At this window's GUI scale (2), -1 draws each font pixel as 1 screen pixel.
			// (No window resize here: Vulkan in the sandbox loses the device when the swapchain is recreated.)
			context.getInput().pressKey(options -> options.keyCommand);
			context.waitForScreen(ChatScreen.class);
			context.getInput().typeChars("size -1");
			context.getInput().pressKey(InputConstants.KEY_RETURN);
			context.waitForScreen(null);
			check(context.computeOnClient(mc -> McChatClient.config().textSize) == -1, "/size -1 sets text size");
			send(context, world, "crisp text at size -1");
			context.takeScreenshot("mcchat-05a-size-minus-1");

			// Typing a command into the T chat runs it instead of sending it as a message.
			int entriesBefore = entryCount(context);
			context.getInput().pressKey(options -> options.keyChat);
			context.waitForScreen(CompactChatScreen.class);
			context.getInput().typeChars("/size 0");
			context.getInput().pressKey(InputConstants.KEY_RETURN);
			context.waitForScreen(null);
			world.getConnection().waitForServerboundPackets();
			world.getConnection().waitForClientboundPackets();
			check(context.computeOnClient(mc -> McChatClient.config().textSize) == 0, "/size 0 typed in T chat runs as a command");
			check(entryCount(context) == entriesBefore, "a command typed in T chat is not sent as a chat message");
		}

		// History survives leaving and reopening the world.
		try (TestSingleplayerContext world = save.open()) {
			world.getConnection().waitForChunksRender();
			world.getConnection().waitForClientboundPackets();
			context.waitTick();
			List<String> texts = context.computeOnClient(mc -> ClientChatState.entries().stream().map(e -> e.message().text()).toList());
			check(texts.equals(List.of("hello 💖 cred", "second message", "third message", "crisp text at size -1")), "history restored, got: " + texts);
			context.takeScreenshot("mcchat-05-history-after-rejoin");
		}

		// Over a real network connection to a dedicated server (exercises payload encoding end to end).
		try (TestDedicatedServerContext server = context.worldBuilder().createServer();
				TestDedicatedServerConnection connection = server.connect()) {
			connection.waitForChunksRender();
			send(context, connection, "over the network 💖");
			check(lastText(context).equals("over the network 💖"), "dedicated server round trip, got: " + lastText(context));
			context.takeScreenshot("mcchat-06-dedicated-server");
		}
	}

	private static void send(ClientGameTestContext context, TestSingleplayerContext world, String text) {
		send(context, world.getConnection(), text);
	}

	private static void send(ClientGameTestContext context, TestServerConnection connection, String text) {
		int before = entryCount(context);
		context.getInput().pressKey(options -> options.keyChat);
		context.waitForScreen(CompactChatScreen.class);
		context.getInput().typeChars(text);
		context.getInput().pressKey(InputConstants.KEY_RETURN);
		context.waitForScreen(null);
		connection.waitForServerboundPackets();
		connection.waitForClientboundPackets();
		context.waitFor(mc -> ClientChatState.entries().size() > before);
	}

	private static int entryCount(ClientGameTestContext context) {
		return context.computeOnClient(mc -> ClientChatState.entries().size());
	}

	private static String lastText(ClientGameTestContext context) {
		return context.computeOnClient(mc -> ClientChatState.entries().getLast().message().text());
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Check failed: " + what);
		}
	}
}
