package dev.mcchat.core;

import java.util.UUID;

/** Small display rules shared by the HUD panel and the chat screen. */
public final class ChatRules {
	public static final long GROUP_WINDOW_MILLIS = 120_000;
	public static final long FADE_MILLIS = 1_000;

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

	/** 0 while a message is new, ramping to 1 over FADE_MILLIS once it has been visible for brightSeconds. */
	public static float fadeProgress(long receivedAtMillis, long nowMillis, int brightSeconds) {
		long sinceFadeStart = nowMillis - receivedAtMillis - brightSeconds * 1000L;
		return Math.clamp(sinceFadeStart / (float) FADE_MILLIS, 0f, 1f);
	}

	/** 0 until hideSeconds after arrival, then ramps to 1 (gone) over FADE_MILLIS. hideSeconds 0 never hides. */
	public static float hideProgress(long receivedAtMillis, long nowMillis, int hideSeconds) {
		if (hideSeconds <= 0) {
			return 0f;
		}
		return fadeProgress(receivedAtMillis, nowMillis, hideSeconds);
	}

	/** Text and head opacity while visible: full when new, settling to the dim opacity. */
	public static float lineAlpha(float fadeProgress, float dimOpacity) {
		return 1f + (dimOpacity - 1f) * fadeProgress;
	}

	/** Background box opacity: full when new, gone once faded. */
	public static float boxAlpha(float fadeProgress) {
		return 1f - fadeProgress;
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

	/**
	 * Pose scale for chat text: each font pixel covers {@code guiScale + sizeStep} whole screen pixels
	 * (at least one), so the pixel font stays crisp. Step 0 is vanilla size.
	 */
	public static float textScale(int guiScale, int sizeStep) {
		return Math.max(1, guiScale + sizeStep) / (float) guiScale;
	}
}
