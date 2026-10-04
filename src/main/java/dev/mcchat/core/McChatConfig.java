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

	public double scale = 0.75;
	public int width = 200;
	public int visibleLines = 5;
	public int expandedLines = 15;
	public int brightSeconds = 10;
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
			try {
				Files.createDirectories(file.getParent());
				Files.writeString(file, GSON.toJson(cfg), StandardCharsets.UTF_8);
			} catch (IOException e) {
				LOG.warn("Couldn't write default config {}", file, e);
			}
		}
		return cfg;
	}

	private void validate() {
		McChatConfig d = new McChatConfig();
		if (!(scale >= 0.25 && scale <= 2.0)) {
			warn("scale", scale);
			scale = d.scale;
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
