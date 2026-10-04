package dev.mcchat.core;

import java.util.UUID;

/** Small display rules shared by the HUD panel and the chat screen. */
public final class ChatRules {
	public static final long GROUP_WINDOW_MILLIS = 120_000;
	public static final long BOX_FADE_MILLIS = 500;

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

	/**
	 * Panel text opacity, timed from the newest message: full for brightSeconds, then dims linearly to dimOpacity
	 * over dimSeconds, then fades linearly to 0 over fadeSeconds. fadeSeconds 0 keeps it dim forever.
	 */
	public static float textAlpha(long newestReceivedMillis, long nowMillis, int brightSeconds, int dimSeconds,
			int fadeSeconds, float dimOpacity) {
		long t = nowMillis - newestReceivedMillis;
		long brightEnd = brightSeconds * 1000L;
		long dimEnd = brightEnd + dimSeconds * 1000L;
		if (t < brightEnd) {
			return 1f;
		}
		if (t < dimEnd) {
			return lerp(1f, dimOpacity, (t - brightEnd) / (float) (dimEnd - brightEnd));
		}
		if (fadeSeconds <= 0) {
			return dimOpacity;
		}
		long fadeEnd = dimEnd + fadeSeconds * 1000L;
		return t < fadeEnd ? lerp(dimOpacity, 0f, (t - dimEnd) / (float) (fadeEnd - dimEnd)) : 0f;
	}

	/** Background box opacity: full for brightSeconds after the newest message, then gone over BOX_FADE_MILLIS. */
	public static float boxAlpha(long newestReceivedMillis, long nowMillis, int brightSeconds) {
		long sinceFadeStart = nowMillis - newestReceivedMillis - brightSeconds * 1000L;
		return 1f - Math.clamp(sinceFadeStart / (float) BOX_FADE_MILLIS, 0f, 1f);
	}

	private static float lerp(float from, float to, float progress) {
		return from + (to - from) * progress;
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
