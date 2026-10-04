package dev.mcchat.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.mcchat.core.ChatRules;
import dev.mcchat.core.McChatConfig;
import dev.mcchat.core.SentHistory;
import dev.mcchat.core.TextSanitizer;
import dev.mcchat.net.SendPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

/** T screen: the panel expanded with scrollback, and a slim input box inside it. Does not pause or dim the game. */
public class CompactChatScreen extends Screen {
	private static final int INPUT_HEIGHT = 14;
	private static final int ERROR_COLOR = 0xFFFF6B6B;
	private static final String NOT_INSTALLED = "MC Chat isn't installed on the server";

	private EditBox input;
	private int scroll;
	private int totalLines;
	private String error;

	public CompactChatScreen() {
		super(Component.literal("MC Chat"));
	}

	@Override
	protected void init() {
		McChatConfig cfg = McChatClient.config();
		int inputTop = height - ChatPanelRenderer.BOTTOM_OFFSET - INPUT_HEIGHT;
		input = new EditBox(font, ChatPanelRenderer.MARGIN_X + 4, inputTop + 3, cfg.width - 8, INPUT_HEIGHT - 4, Component.literal("Message"));
		input.setMaxLength(TextSanitizer.MAX_LENGTH);
		input.setBordered(false);
		input.setHint(Component.literal("Say something…").withColor(0x808080));
		addRenderableWidget(input);
		setInitialFocus(input);
		McChatClient.sentHistory().resetCursor();
		ClientChatState.markRead();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		// Intentionally empty: keep the world fully visible (no blur, no dim).
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		McChatConfig cfg = McChatClient.config();
		int inputTop = height - ChatPanelRenderer.BOTTOM_OFFSET - INPUT_HEIGHT;
		int panelBottom = inputTop - 2;

		if (error != null) {
			graphics.text(font, error, ChatPanelRenderer.MARGIN_X + 4, panelBottom - font.lineHeight - 1, ERROR_COLOR, true);
			panelBottom -= font.lineHeight + 3;
		}
		totalLines = ChatPanelRenderer.draw(graphics, font, cfg, ClientChatState.entries(), cfg.expandedLines, scroll, true, panelBottom);
		scroll = ChatRules.clampScroll(scroll, totalLines, cfg.expandedLines);

		int x0 = ChatPanelRenderer.MARGIN_X;
		graphics.fill(x0, inputTop, x0 + cfg.width, inputTop + INPUT_HEIGHT, ChatPanelRenderer.argb(ChatPanelRenderer.BOX_OPACITY, 0x000000));
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		SentHistory history = McChatClient.sentHistory();
		int pageLines = McChatClient.config().expandedLines - 1;
		if (event.isConfirmation()) {
			send();
		} else if (event.isUp()) {
			setInput(history.previous());
		} else if (event.isDown()) {
			setInput(history.next());
		} else if (event.shortcutKey() == InputConstants.KEY_PAGEUP) {
			scrollBy(pageLines);
		} else if (event.shortcutKey() == InputConstants.KEY_PAGEDOWN) {
			scrollBy(-pageLines);
		} else {
			return super.keyPressed(event);
		}
		return true;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		scrollBy((int) Math.signum(scrollY) * 3);
		return true;
	}

	private void scrollBy(int lines) {
		scroll = ChatRules.clampScroll(scroll + lines, totalLines, McChatClient.config().expandedLines);
	}

	private void setInput(String value) {
		if (value != null) {
			input.setValue(value);
			input.moveCursorToEnd(false);
		}
	}

	private void send() {
		String text = TextSanitizer.sanitize(input.getValue());
		if (text.isEmpty()) {
			onClose();
			return;
		}
		if (!ClientPlayNetworking.canSend(SendPayload.TYPE)) {
			error = NOT_INSTALLED;
			return;
		}
		ClientPlayNetworking.send(new SendPayload(text));
		McChatClient.sentHistory().add(text);
		onClose();
	}
}
