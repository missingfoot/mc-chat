package dev.mcchat.core;

import java.util.UUID;

/** Small display rules shared by the HUD panel and the chat screen. */
public final class ChatRules {
	public static final long GROUP_WINDOW_MILLIS = 120_000;

	private ChatRules() {
	}

	/** Whether to print the time + name before this message, or group it under the previous one. */
	public static boolean showHeader(ChatMessage previous, ChatMessage current) {
		if (previous == null || !previous.senderId().equals(current.senderId())) {
			return true;
		}
		long gap = current.timestampMillis() - previous.timestampMillis();
		return gap < 0 || gap > GROUP_WINDOW_MILLIS;
	}

	/** Full opacity while new, then settle to the dim opacity. Never fades out completely. */
	public static float lineAlpha(long receivedAtMillis, long nowMillis, int brightSeconds, float dimOpacity) {
		return nowMillis - receivedAtMillis < brightSeconds * 1000L ? 1f : dimOpacity;
	}

	/** True unless the message was sent by the local player. Unknown local id counts as "other". */
	public static boolean isFromOther(ChatMessage message, UUID localPlayerId) {
		return localPlayerId == null || !localPlayerId.equals(message.senderId());
	}

	/** Scroll offset in lines from the newest, clamped to what is available. */
	public static int clampScroll(int scroll, int total, int maxLines) {
		return Math.clamp(scroll, 0, Math.max(0, total - maxLines));
	}

	/** {begin, end} (end exclusive) of the lines to show, given a scroll offset back from the newest. */
	public static int[] visibleRange(int total, int maxLines, int scroll) {
		int end = total - clampScroll(scroll, total, maxLines);
		int begin = Math.max(0, end - maxLines);
		return new int[] {begin, end};
	}
}
