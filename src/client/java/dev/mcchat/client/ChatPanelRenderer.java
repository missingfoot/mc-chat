package dev.mcchat.client;

import dev.mcchat.core.ChatMessage;
import dev.mcchat.core.ChatRules;
import dev.mcchat.core.McChatConfig;
import net.minecraft.client.Minecraft;
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

	/** Background box opacity while messages are bright (and while typing). */
	public static final float BOX_OPACITY = 0.55f;

	private static final float MIN_VISIBLE_ALPHA = 0.02f;
	private static final int PAD = 3;
	private static final int HEAD_GAP = 3;

	/** One wrapped line; {@code head} is set only on the first line of a message that starts a group. */
	private record Line(FormattedCharSequence text, PlayerSkin head) {
	}

	/** Input box height in scaled units is font.lineHeight + INPUT_PAD. */
	public static final int INPUT_PAD = 5;

	private ChatPanelRenderer() {
	}

	/** Top of the chat input box, in scaled units (the box itself is drawn at the text scale). */
	public static int inputTop(int guiHeight, Font font, float scale) {
		return (int) Math.floor((guiHeight - BOTTOM_OFFSET) / scale) - (font.lineHeight + INPUT_PAD);
	}

	/**
	 * Bottom edge of the message panel in GUI pixels: just above where the input box goes, whether or not chat
	 * is open, so the panel doesn't jump when the input box appears or disappears.
	 */
	public static int panelBottom(int guiHeight, Font font, float scale) {
		return (int) Math.floor(inputTop(guiHeight, font, scale) * scale) - 2;
	}

	/**
	 * Draws up to {@code maxLines} wrapped lines ending {@code scroll} lines back from the newest,
	 * with the card's bottom edge at {@code bottomY}. Returns the total number of wrapped lines.
	 * The whole panel fades as one, timed from the newest message, unless {@code forceBright}.
	 */
	public static int draw(GuiGraphicsExtractor g, Font font, McChatConfig cfg, List<ClientChatState.Entry> entries,
			int maxLines, int scroll, boolean forceBright, int bottomY) {
		float scale = ChatRules.textScale(Minecraft.getInstance().getWindow().getGuiScale(), cfg.textSize);
		int headSize = font.lineHeight - 1;
		int textX = headSize + HEAD_GAP;
		int innerWidth = Math.max(40, (int) ((cfg.width - PAD * 2 - 2) / scale) - textX);

		List<Line> lines = new ArrayList<>();
		ChatMessage previous = null;
		for (ClientChatState.Entry entry : entries) {
			ChatMessage m = entry.message();
			PlayerSkin head = ChatRules.showHeader(previous, m) ? SkinCache.skinFor(m.senderId()) : null;
			for (FormattedCharSequence seq : font.split(Component.literal(m.text()), innerWidth)) {
				lines.add(new Line(seq, head));
				head = null;
			}
			previous = m;
		}
		if (lines.isEmpty() || maxLines <= 0) {
			return lines.size();
		}

		float textAlpha = 1f;
		float boxAlpha = 1f;
		if (!forceBright) {
			long newest = entries.getLast().receivedAtMillis();
			long now = System.currentTimeMillis();
			textAlpha = ChatRules.textAlpha(newest, now, cfg.brightSeconds, cfg.dimSeconds, cfg.fadeSeconds, (float) cfg.dimOpacity);
			boxAlpha = ChatRules.boxAlpha(newest, now, cfg.brightSeconds);
			if (textAlpha < MIN_VISIBLE_ALPHA) {
				return lines.size();
			}
		}

		int[] range = ChatRules.visibleRange(lines.size(), maxLines, scroll);
		List<Line> shown = lines.subList(range[0], range[1]);
		int lineHeight = font.lineHeight + 1;
		int x0 = MARGIN_X;
		int y1 = bottomY;
		int y0 = y1 - (int) Math.ceil(shown.size() * lineHeight * scale) - PAD * 2;
		if (boxAlpha > 0f) {
			g.fill(x0, y0, x0 + cfg.width, y1, argb(boxAlpha * BOX_OPACITY, 0x000000));
		}

		int color = argb(textAlpha, 0xFFFFFF);
		// Lay lines out from the bottom edge up, so the newest line never moves when lines are added
		// (a line's height in GUI pixels is often fractional, so top-down layout would jitter by a pixel).
		g.pose().pushMatrix();
		g.pose().translate(x0 + PAD + 2, y1 - PAD);
		g.pose().scale(scale, scale);
		int y = -shown.size() * lineHeight;
		for (Line line : shown) {
			if (line.head() != null) {
				PlayerFaceExtractor.extractRenderState(g, line.head(), 0, y, headSize, color);
			}
			g.text(font, line.text(), textX, y, color, true);
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
