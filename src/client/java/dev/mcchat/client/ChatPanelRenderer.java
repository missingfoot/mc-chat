package dev.mcchat.client;

import dev.mcchat.core.ChatMessage;
import dev.mcchat.core.ChatRules;
import dev.mcchat.core.McChatConfig;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.ArrayList;
import java.util.List;

/** Draws the chat card: chat heads, small wrapped lines, and a background box that fades away with the text. */
public final class ChatPanelRenderer {
	public static final int ACCENT = 0x7FB2FF;
	public static final int MARGIN_X = 4;
	public static final int BOTTOM_OFFSET = 50;

	private static final int PAD = 3;
	private static final int HEAD_GAP = 3;

	/** One wrapped line; {@code head} is set only on the first line of a message that starts a group. */
	private record Line(FormattedCharSequence text, float alpha, float boxAlpha, PlayerSkin head) {
	}

	private ChatPanelRenderer() {
	}

	/**
	 * Draws up to {@code maxLines} wrapped lines ending {@code scroll} lines back from the newest,
	 * with the card's bottom edge at {@code bottomY}. Returns the total number of wrapped lines.
	 */
	public static int draw(GuiGraphicsExtractor g, Font font, McChatConfig cfg, List<ClientChatState.Entry> entries,
			int maxLines, int scroll, boolean forceBright, int bottomY) {
		float scale = (float) cfg.scale;
		int headSize = font.lineHeight - 1;
		int textX = headSize + HEAD_GAP;
		int innerWidth = Math.max(40, (int) ((cfg.width - PAD * 2 - 2) / scale) - textX);
		long now = System.currentTimeMillis();

		List<Line> lines = new ArrayList<>();
		ChatMessage previous = null;
		for (ClientChatState.Entry entry : entries) {
			ChatMessage m = entry.message();
			float fade = forceBright ? 0f : ChatRules.fadeProgress(entry.receivedAtMillis(), now, cfg.brightSeconds);
			float alpha = ChatRules.lineAlpha(fade, (float) cfg.dimOpacity);
			float boxAlpha = ChatRules.boxAlpha(fade);
			PlayerSkin head = ChatRules.showHeader(previous, m) ? SkinCache.skinFor(m.senderId()) : null;
			for (FormattedCharSequence seq : font.split(Component.literal(m.text()), innerWidth)) {
				lines.add(new Line(seq, alpha, boxAlpha, head));
				head = null;
			}
			previous = m;
		}
		if (lines.isEmpty() || maxLines <= 0) {
			return lines.size();
		}

		int[] range = ChatRules.visibleRange(lines.size(), maxLines, scroll);
		List<Line> shown = lines.subList(range[0], range[1]);
		float boxAlpha = 0f;
		for (Line line : shown) {
			boxAlpha = Math.max(boxAlpha, line.boxAlpha());
		}

		int lineHeight = font.lineHeight + 1;
		int x0 = MARGIN_X;
		int y1 = bottomY;
		int y0 = y1 - (int) Math.ceil(shown.size() * lineHeight * scale) - PAD * 2;
		if (boxAlpha > 0f) {
			g.fill(x0, y0, x0 + cfg.width, y1, argb(boxAlpha * 0.25f, 0x000000));
			g.fill(x0, y0, x0 + 1, y1, argb(boxAlpha, ACCENT));
		}

		g.pose().pushMatrix();
		g.pose().translate(x0 + PAD + 2, y0 + PAD);
		g.pose().scale(scale, scale);
		int y = 0;
		for (Line line : shown) {
			if (line.head() != null) {
				PlayerFaceExtractor.extractRenderState(g, line.head(), 0, y, headSize, argb(line.alpha(), 0xFFFFFF));
			}
			g.text(font, line.text(), textX, y, argb(line.alpha(), 0xFFFFFF), true);
			y += lineHeight;
		}
		g.pose().popMatrix();
		return lines.size();
	}

	static int argb(float alpha, int rgb) {
		int a = Math.round(Math.clamp(alpha, 0f, 1f) * 255f);
		return (a << 24) | (rgb & 0xFFFFFF);
	}
}
