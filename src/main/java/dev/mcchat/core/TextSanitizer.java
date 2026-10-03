package dev.mcchat.core;

public final class TextSanitizer {
	public static final int MAX_LENGTH = 256;

	private TextSanitizer() {
	}

	/** Strips control characters and § formatting codes, trims, and truncates to MAX_LENGTH code points. */
	public static String sanitize(String raw) {
		if (raw == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder(raw.length());
		raw.codePoints()
				.filter(cp -> cp != '§' && !Character.isISOControl(cp))
				.forEach(sb::appendCodePoint);
		String text = sb.toString().strip();
		if (text.codePointCount(0, text.length()) > MAX_LENGTH) {
			text = text.substring(0, text.offsetByCodePoints(0, MAX_LENGTH)).strip();
		}
		return text;
	}
}
