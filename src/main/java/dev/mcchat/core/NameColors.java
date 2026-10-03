package dev.mcchat.core;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public final class NameColors {
	/** Soft colours that read well on a dark translucent background. */
	static final int[] PALETTE = {0x7FB2FF, 0xFF8FB1, 0x8FE3A2, 0xFFC56F, 0xC9A0FF, 0x6FE0E0};

	private static final Pattern HEX = Pattern.compile("#?[0-9a-fA-F]{6}");

	private NameColors() {
	}

	/** RGB colour for a player's name: a config override if valid, otherwise a stable palette pick. */
	public static int colorFor(UUID id, String name, Map<String, String> overrides) {
		if (overrides != null && name != null) {
			for (Map.Entry<String, String> e : overrides.entrySet()) {
				if (e.getKey().equalsIgnoreCase(name) && e.getValue() != null && HEX.matcher(e.getValue()).matches()) {
					return Integer.parseInt(e.getValue().replace("#", ""), 16);
				}
			}
		}
		return PALETTE[Math.floorMod(id.hashCode(), PALETTE.length)];
	}
}
