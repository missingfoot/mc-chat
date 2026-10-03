package dev.mcchat.client;

import dev.mcchat.core.ChatMessage;
import dev.mcchat.core.ChatRules;
import dev.mcchat.core.McChatConfig;
import dev.mcchat.core.NameColors;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Draws the chat card: translucent background, accent strip, small wrapped lines. */
public final class ChatPanelRenderer {
	public static final int ACCENT = 0x7FB2FF;
	public static final int MARGIN_X = 4;
	public static final int BOTTOM_OFFSET = 46;

	private static final int PAD = 3;
	private static final int TIME_COLOR = 0x8A8A8A;
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

	private record Line(FormattedCharSequence text, float alpha) {
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
		int innerWidth = Math.max(40, (int) ((cfg.width - PAD * 2 - 2) / scale));
		long now = System.currentTimeMillis();

		List<Line> lines = new ArrayList<>();
		ChatMessage previous = null;
		for (ClientChatState.Entry entry : entries) {
			ChatMessage m = entry.message();
			float alpha = forceBright ? 1f : ChatRules.lineAlpha(entry.receivedAtMillis(), now, cfg.brightSeconds, (float) cfg.dimOpacity);
			for (FormattedCharSequence seq : font.split(format(m, ChatRules.showHeader(previous, m), cfg), innerWidth)) {
				lines.add(new Line(seq, alpha));
			}
			previous = m;
		}
		if (lines.isEmpty() || maxLines <= 0) {
			return lines.size();
		}

		int[] range = ChatRules.visibleRange(lines.size(), maxLines, scroll);
		List<Line> shown = lines.subList(range[0], range[1]);
		float cardAlpha = 0f;
		for (Line line : shown) {
			cardAlpha = Math.max(cardAlpha, line.alpha());
		}

		int lineHeight = font.lineHeight + 1;
		int x0 = MARGIN_X;
		int x1 = MARGIN_X + cfg.width;
		int y1 = bottomY;
		int y0 = y1 - (int) Math.ceil(shown.size() * lineHeight * scale) - PAD * 2;
		g.fill(x0, y0, x1, y1, argb(cardAlpha * 0.25f, 0x000000));
		g.fill(x0, y0, x0 + 1, y1, argb(cardAlpha, ACCENT));

		g.pose().pushMatrix();
		g.pose().translate(x0 + PAD + 2, y0 + PAD);
		g.pose().scale(scale, scale);
		int y = 0;
		for (Line line : shown) {
			g.text(font, line.text(), 0, y, argb(line.alpha(), 0xFFFFFF), true);
			y += lineHeight;
		}
		g.pose().popMatrix();
		return lines.size();
	}

	private static Component format(ChatMessage m, boolean header, McChatConfig cfg) {
		if (!header) {
			return Component.literal(m.text());
		}
		int nameColor = NameColors.colorFor(m.senderId(), m.senderName(), cfg.nameColors);
		return Component.empty()
				.append(Component.literal(TIME.format(Instant.ofEpochMilli(m.timestampMillis())) + " ").withColor(TIME_COLOR))
				.append(Component.literal(m.senderName()).withColor(nameColor))
				.append(Component.literal(" " + m.text()));
	}

	static int argb(float alpha, int rgb) {
		int a = Math.round(Math.clamp(alpha, 0f, 1f) * 255f);
		return (a << 24) | (rgb & 0xFFFFFF);
	}
}
