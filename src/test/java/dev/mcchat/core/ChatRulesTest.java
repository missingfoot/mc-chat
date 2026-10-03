package dev.mcchat.core;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatRulesTest {
	private static final UUID A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
	private static final UUID B = UUID.fromString("00000000-0000-0000-0000-00000000000b");

	private static ChatMessage at(UUID who, long t) {
		return new ChatMessage(who, who.equals(A) ? "A" : "B", "x", t);
	}

	@Test
	void headerShownForFirstMessage() {
		assertTrue(ChatRules.showHeader(null, at(A, 0)));
	}

	@Test
	void headerHiddenForSameSenderWithinWindow() {
		assertFalse(ChatRules.showHeader(at(A, 0), at(A, 119_000)));
	}

	@Test
	void headerShownAfterWindowOrSenderChange() {
		assertTrue(ChatRules.showHeader(at(A, 0), at(A, 121_000)));
		assertTrue(ChatRules.showHeader(at(A, 0), at(B, 1_000)));
	}

	@Test
	void headerShownIfClockGoesBackwards() {
		assertTrue(ChatRules.showHeader(at(A, 10_000), at(A, 5_000)));
	}

	@Test
	void lineAlphaBrightThenDim() {
		assertEquals(1f, ChatRules.lineAlpha(1_000, 1_000 + 29_000, 30, 0.45f));
		assertEquals(0.45f, ChatRules.lineAlpha(1_000, 1_000 + 31_000, 30, 0.45f));
		assertEquals(0.45f, ChatRules.lineAlpha(0, 1_759_500_000_000L, 30, 0.45f)); // history entries (received at 0) start dim
	}

	@Test
	void ownMessagesAreNotFromOther() {
		assertFalse(ChatRules.isFromOther(at(A, 0), A));
		assertTrue(ChatRules.isFromOther(at(B, 0), A));
		assertTrue(ChatRules.isFromOther(at(A, 0), null));
	}

	@Test
	void scrollClampsToAvailableLines() {
		assertEquals(0, ChatRules.clampScroll(-3, 20, 15));
		assertEquals(5, ChatRules.clampScroll(99, 20, 15));
		assertEquals(0, ChatRules.clampScroll(4, 3, 15)); // fewer lines than the panel
	}

	@Test
	void visibleRangeShowsNewestLinesAndScrollsBack() {
		assertArrayEquals(new int[] {5, 20}, ChatRules.visibleRange(20, 15, 0));
		assertArrayEquals(new int[] {0, 15}, ChatRules.visibleRange(20, 15, 5));
		assertArrayEquals(new int[] {0, 15}, ChatRules.visibleRange(20, 15, 50));
		assertArrayEquals(new int[] {0, 3}, ChatRules.visibleRange(3, 15, 0));
		assertArrayEquals(new int[] {0, 0}, ChatRules.visibleRange(0, 15, 0));
	}
}
