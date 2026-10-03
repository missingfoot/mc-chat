package dev.mcchat.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McChatConfigTest {
	@TempDir
	Path dir;

	@Test
	void missingFileGivesDefaultsAndCreatesFile() {
		Path file = dir.resolve("mcchat.json");
		McChatConfig cfg = McChatConfig.load(file);
		assertEquals(0.75, cfg.scale);
		assertEquals(200, cfg.width);
		assertEquals(5, cfg.visibleLines);
		assertEquals(15, cfg.expandedLines);
		assertEquals(30, cfg.brightSeconds);
		assertEquals(0.45, cfg.dimOpacity);
		assertTrue(cfg.soundEnabled);
		assertEquals(0.4, cfg.soundVolume);
		assertTrue(cfg.nameColors.isEmpty());
		assertTrue(Files.exists(file));
	}

	@Test
	void partialFileKeepsDefaultsForMissingKeys() throws IOException {
		Path file = dir.resolve("mcchat.json");
		Files.writeString(file, "{\"width\": 250, \"nameColors\": {\"Friend\": \"#FF00AA\"}}");
		McChatConfig cfg = McChatConfig.load(file);
		assertEquals(250, cfg.width);
		assertEquals(0.75, cfg.scale);
		assertEquals("#FF00AA", cfg.nameColors.get("Friend"));
	}

	@Test
	void outOfRangeValuesFallBackToDefaults() throws IOException {
		Path file = dir.resolve("mcchat.json");
		Files.writeString(file, "{\"scale\": 9, \"width\": 5, \"visibleLines\": -1, \"soundVolume\": 3, \"nameColors\": null}");
		McChatConfig cfg = McChatConfig.load(file);
		assertEquals(0.75, cfg.scale);
		assertEquals(200, cfg.width);
		assertEquals(5, cfg.visibleLines);
		assertEquals(0.4, cfg.soundVolume);
		assertTrue(cfg.nameColors.isEmpty());
	}

	@Test
	void corruptFileGivesDefaultsAndIsNotOverwritten() throws IOException {
		Path file = dir.resolve("mcchat.json");
		Files.writeString(file, "{ this is not json");
		McChatConfig cfg = McChatConfig.load(file);
		assertEquals(200, cfg.width);
		assertEquals("{ this is not json", Files.readString(file));
	}
}
