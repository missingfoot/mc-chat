package dev.mcchat.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.mcchat.core.ChatRules;
import dev.mcchat.core.McChatConfig;
import dev.mcchat.core.SentHistory;
import dev.mcchat.core.TextSanitizer;
import dev.mcchat.net.SendPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * T screen: the panel expanded with scrollback, and a slim input box below it. Does not pause or dim the game.
 * The input box, typed text and command suggestions are drawn at the /size text scale, so their layout is
 * kept in scaled units and mouse positions are converted to match.
 */
public class CompactChatScreen extends Screen {
	private static final int SUGGESTION_FILL = 0xD0000000;
	private static final int ERROR_COLOR = 0xFFFF6B6B;
	private static final String NOT_INSTALLED = "MC Chat isn't installed on the server";

	private EditBox input;
	private CommandSuggestions suggestions;
	private float scale;
	private int boxX;
	private int boxTop;
	private int boxWidth;
	private int boxHeight;
	private int scroll;
	private int totalLines;
	private String error;

	public CompactChatScreen() {
		super(Component.literal("MC Chat"));
	}

	@Override
	protected void init() {
		McChatConfig cfg = McChatClient.config();
		scale = ChatRules.textScale(minecraft.getWindow().getGuiScale(), cfg.textSize);
		boxHeight = font.lineHeight + ChatPanelRenderer.INPUT_PAD;
		boxX = Math.round(ChatPanelRenderer.MARGIN_X / scale);
		boxWidth = Math.round(cfg.width / scale);
		boxTop = ChatPanelRenderer.inputTop(height, font, scale);

		input = new EditBox(font, boxX + 4, boxTop + 3, boxWidth - 8, boxHeight - 4, Component.literal("Message"));
		input.setMaxLength(TextSanitizer.MAX_LENGTH);
		input.setBordered(false);
		input.setHint(Component.literal("Say something…").withColor(0x808080));
		input.setResponder(text -> {
			suggestions.setAllowSuggestions(!text.isEmpty());
			suggestions.updateCommandInfo();
		});
		addRenderableWidget(input);
		setInitialFocus(input);

		// CommandSuggestions anchors its popup to its screen's bottom edge (vanilla's input sits there).
		// Give it a stand-in screen whose bottom edge is just below our input box, in scaled units.
		Screen anchor = new Screen(Component.empty()) {
		};
		anchor.width = (int) (width / scale);
		anchor.height = boxTop + 14;
		suggestions = new CommandSuggestions(minecraft, anchor, input, font, false, false, 1, 10, true, SUGGESTION_FILL);
		suggestions.updateCommandInfo();

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
		int panelBottom = ChatPanelRenderer.panelBottom(height, font, scale);

		if (error != null) {
			graphics.text(font, error, ChatPanelRenderer.MARGIN_X + 4, panelBottom - font.lineHeight - 1, ERROR_COLOR, true);
			panelBottom -= font.lineHeight + 3;
		}
		totalLines = ChatPanelRenderer.draw(graphics, font, cfg, ClientChatState.entries(), cfg.expandedLines, scroll, true, panelBottom);
		scroll = ChatRules.clampScroll(scroll, totalLines, cfg.expandedLines);

		int scaledMouseX = (int) (mouseX / scale);
		int scaledMouseY = (int) (mouseY / scale);
		graphics.pose().pushMatrix();
		graphics.pose().scale(scale, scale);
		graphics.fill(boxX, boxTop, boxX + boxWidth, boxTop + boxHeight, ChatPanelRenderer.argb(ChatPanelRenderer.BOX_OPACITY, 0x000000));
		super.extractRenderState(graphics, scaledMouseX, scaledMouseY, partialTick);
		suggestions.extractRenderState(graphics, scaledMouseX, scaledMouseY);
		graphics.pose().popMatrix();
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		// Right arrow at the end of the text accepts the highlighted suggestion, same as Tab.
		if (event.isRight() && suggestions.isVisible() && input.getCursorPosition() == input.getValue().length()) {
			event = new KeyEvent(InputConstants.KEY_TAB, 0, 0);
		}
		// Suggestions get first go: Tab, arrows while the popup is open, Esc to close the popup.
		if (suggestions.keyPressed(event)) {
			return true;
		}
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
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		MouseButtonEvent scaled = new MouseButtonEvent(event.x() / scale, event.y() / scale, event.buttonInfo());
		if (suggestions.mouseClicked(scaled)) {
			return true;
		}
		return super.mouseClicked(scaled, doubleClick);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (suggestions.mouseScrolled(Mth.clamp(scrollY, -1.0, 1.0))) {
			return true;
		}
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
		String raw = input.getValue().strip();
		if (raw.startsWith("/")) {
			// Same path vanilla chat uses, so server commands and Fabric client commands (like /size) both work.
			if (raw.length() > 1 && minecraft.player != null) {
				minecraft.player.connection.sendCommand(raw.substring(1));
				McChatClient.sentHistory().add(raw);
			}
			onClose();
			return;
		}
		String text = TextSanitizer.sanitize(raw);
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
