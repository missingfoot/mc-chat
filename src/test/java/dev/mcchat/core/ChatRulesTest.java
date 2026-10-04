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

	@Test
	void textScaleKeepsWholeScreenPixelsPerFontPixel() {
		assertEquals(1f, ChatRules.textScale(3, 0));
		assertEquals(2f / 3f, ChatRules.textScale(3, -1), 0.0001f);
		assertEquals(1f / 3f, ChatRules.textScale(3, -2), 0.0001f);
		assertEquals(5f / 3f, ChatRules.textScale(3, 2), 0.0001f);
	}

	@Test
	void textScaleNeverGoesBelowOneScreenPixel() {
		assertEquals(1f / 3f, ChatRules.textScale(3, -9), 0.0001f);
		assertEquals(0.5f, ChatRules.textScale(2, -5), 0.0001f);
		assertEquals(1f, ChatRules.textScale(1, -1));
	}


	// Panel timeline from the newest message: 5s bright, 5s dimming, 5s fading out.
	private static final int BRIGHT = 5;
	private static final int DIM = 5;
	private static final int FADE = 5;

	private static float text(long sinceNewestMillis) {
		return ChatRules.textAlpha(1_000, 1_000 + sinceNewestMillis, BRIGHT, DIM, FADE, 0.45f);
	}

	@Test
	void textIsFullWhileBright() {
		assertEquals(1f, text(0));
		assertEquals(1f, text(4_999));
	}

	@Test
	void textDimsGraduallyOverTheDimPhase() {
		assertEquals(0.725f, text(7_500), 0.001f);
		assertEquals(0.45f, text(10_000), 0.001f);
	}

	@Test
	void textFadesDownToTheShaderFloorThenDisappears() {
		// Minecraft's text shader discards alpha < 0.1, so the fade ends at 0.1 and then everything goes at once.
		assertEquals(0.275f, text(12_500), 0.001f);
		assertEquals(0.1f, text(14_999), 0.001f);
		assertEquals(0f, text(15_000));
		assertEquals(0f, text(60_000));
	}

	@Test
	void fadeSecondsZeroKeepsTextDimForever() {
		assertEquals(0.45f, ChatRules.textAlpha(0, 1_759_500_000_000L, BRIGHT, DIM, 0, 0.45f));
	}

	@Test
	void zeroLengthPhasesDoNotDivideByZero() {
		assertEquals(0.38f, ChatRules.textAlpha(0, 6_000, 5, 0, 5, 0.45f), 0.001f); // no dim phase: straight into fading
		assertEquals(0.45f, ChatRules.textAlpha(0, 5_000, 5, 0, 0, 0.45f), 0.001f); // no dim, no fade: snaps to dim
	}

	@Test
	void historyFromBeforeJoiningStartsHidden() {
		assertEquals(0f, ChatRules.textAlpha(0, 1_759_500_000_000L, BRIGHT, DIM, FADE, 0.45f));
	}

	@Test
	void boxStaysForBrightSecondsThenFadesQuickly() {
		assertEquals(1f, ChatRules.boxAlpha(1_000, 1_000 + 4_999, BRIGHT));
		assertEquals(0.5f, ChatRules.boxAlpha(1_000, 1_000 + 5_250, BRIGHT), 0.001f);
		assertEquals(0f, ChatRules.boxAlpha(1_000, 1_000 + 5_500, BRIGHT));
	}
}
