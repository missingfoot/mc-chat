package dev.mcchat.core;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NameColorsTest {
	private static final UUID ID = UUID.fromString("12345678-1234-1234-1234-123456789abc");

	@Test
	void overrideWinsCaseInsensitively() {
		assertEquals(0xFF00AA, NameColors.colorFor(ID, "Friend", Map.of("friend", "#FF00AA")));
		assertEquals(0x00FF00, NameColors.colorFor(ID, "Friend", Map.of("Friend", "00ff00")));
	}

	@Test
	void invalidOverrideFallsBackToPalette() {
		int c = NameColors.colorFor(ID, "Friend", Map.of("Friend", "pink"));
		assertTrue(Arrays.stream(NameColors.PALETTE).anyMatch(p -> p == c));
	}

	@Test
	void sameUuidAlwaysGetsSameColour() {
		assertEquals(NameColors.colorFor(ID, "A", Map.of()), NameColors.colorFor(ID, "B", Map.of()));
	}

	@Test
	void nullOverridesAreAllowed() {
		int c = NameColors.colorFor(ID, "Friend", null);
		assertTrue(Arrays.stream(NameColors.PALETTE).anyMatch(p -> p == c));
	}
}
