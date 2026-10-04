package dev.mcchat.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Client settings, read from config/mcchat.json. Field initializers are the defaults. */
public final class McChatConfig {
	private static final Logger LOG = LoggerFactory.getLogger("mcchat");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static final int MIN_TEXT_SIZE = -5;
	public static final int MAX_TEXT_SIZE = 10;

	/** Chat text size step: 0 is vanilla size, each step is one screen pixel per font pixel (see ChatRules.textScale). */
	public int textSize = 0;
	public int width = 200;
	public int visibleLines = 5;
	public int expandedLines = 15;
	public int brightSeconds = 10;
	/** Seconds after arrival when a message disappears from the corner panel (0 = never). Still shown when chat is open. */
	public int hideSeconds = 30;
	public double dimOpacity = 0.45;
	public boolean soundEnabled = true;
	public double soundVolume = 0.4;

	/** Loads the config, creating it with defaults if missing. A broken file is left alone so it can be fixed by hand. */
	public static McChatConfig load(Path file) {
		McChatConfig cfg = null;
		boolean exists = Files.exists(file);
		if (exists) {
			try {
				cfg = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), McChatConfig.class);
			} catch (IOException | JsonParseException e) {
				LOG.warn("Couldn't read {}, using defaults", file, e);
			}
		}
		if (cfg == null) {
			cfg = new McChatConfig();
		}
		cfg.validate();
		if (!exists) {
			cfg.save(file);
		}
		return cfg;
	}

	public void save(Path file) {
		try {
			Files.createDirectories(file.getParent());
			Files.writeString(file, GSON.toJson(this), StandardCharsets.UTF_8);
		} catch (IOException e) {
			LOG.warn("Couldn't write config {}", file, e);
		}
	}

	private void validate() {
		McChatConfig d = new McChatConfig();
		if (textSize < MIN_TEXT_SIZE || textSize > MAX_TEXT_SIZE) {
			warn("textSize", textSize);
			textSize = d.textSize;
		}
		if (width < 80 || width > 600) {
			warn("width", width);
			width = d.width;
		}
		if (visibleLines < 0 || visibleLines > 50) {
			warn("visibleLines", visibleLines);
			visibleLines = d.visibleLines;
		}
		if (expandedLines < 1 || expandedLines > 100) {
			warn("expandedLines", expandedLines);
			expandedLines = d.expandedLines;
		}
		if (brightSeconds < 0 || brightSeconds > 3600) {
			warn("brightSeconds", brightSeconds);
			brightSeconds = d.brightSeconds;
		}
		if (hideSeconds < 0 || hideSeconds > 86400) {
			warn("hideSeconds", hideSeconds);
			hideSeconds = d.hideSeconds;
		}
		if (!(dimOpacity >= 0.1 && dimOpacity <= 1.0)) {
			warn("dimOpacity", dimOpacity);
			dimOpacity = d.dimOpacity;
		}
		if (!(soundVolume >= 0.0 && soundVolume <= 1.0)) {
			warn("soundVolume", soundVolume);
			soundVolume = d.soundVolume;
		}
	}

	private static void warn(String key, Object value) {
		LOG.warn("Invalid mcchat.json value {}={}, using default", key, value);
	}
}
