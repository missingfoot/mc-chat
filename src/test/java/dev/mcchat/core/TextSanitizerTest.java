package dev.mcchat.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextSanitizerTest {
	@Test
	void nullAndBlankBecomeEmpty() {
		assertEquals("", TextSanitizer.sanitize(null));
		assertEquals("", TextSanitizer.sanitize(""));
		assertEquals("", TextSanitizer.sanitize("   \t  "));
	}

	@Test
	void trimsSurroundingWhitespace() {
		assertEquals("hi there", TextSanitizer.sanitize("  hi there  "));
	}

	@Test
	void stripsSectionSignFormattingCodes() {
		assertEquals("4red", TextSanitizer.sanitize("§4red"));
		assertEquals("", TextSanitizer.sanitize("§§§"));
	}

	@Test
	void stripsControlCharacters() {
		assertEquals("ab", TextSanitizer.sanitize("a\nb"));
		assertEquals("ab", TextSanitizer.sanitize("a\u0000\u0007b"));
	}

	@Test
	void keepsEmojiIntact() {
		assertEquals("hi 💖", TextSanitizer.sanitize("hi 💖"));
	}

	@Test
	void truncatesToMaxLengthInCodePoints() {
		String longText = "a".repeat(300);
		assertEquals(256, TextSanitizer.sanitize(longText).length());
	}

	@Test
	void truncationNeverSplitsASurrogatePair() {
		String text = "a".repeat(255) + "💖💖";
		String out = TextSanitizer.sanitize(text);
		assertEquals(256, out.codePointCount(0, out.length()));
		assertEquals("a".repeat(255) + "💖", out);
	}
}
