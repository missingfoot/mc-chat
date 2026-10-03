package dev.mcchat.client;

import dev.mcchat.core.McChatConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** The always-on corner panel. Called at the end of every HUD frame by HudMixin. */
public final class ChatHudPanel {
	private ChatHudPanel() {
	}

	public static void extract(GuiGraphicsExtractor g) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return;
		}
		if (mc.gui.hud.isHidden()) {
			if (ClientChatState.hasUnread()) {
				int y = g.guiHeight() - 10;
				g.fill(ChatPanelRenderer.MARGIN_X, y, ChatPanelRenderer.MARGIN_X + 4, y + 4, 0xFF000000 | ChatPanelRenderer.ACCENT);
			}
			return;
		}
		if (mc.gui.screen() instanceof CompactChatScreen) {
			return;
		}
		McChatConfig cfg = McChatClient.config();
		ChatPanelRenderer.draw(g, mc.font, cfg, ClientChatState.entries(), cfg.visibleLines, 0, false,
				g.guiHeight() - ChatPanelRenderer.BOTTOM_OFFSET);
	}
}
