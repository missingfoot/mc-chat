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
	/** Corner panel timeline after the newest message: bright with box, then dimming, then fading out (0 = stay dim). */
	public int brightSeconds = 5;
	public int dimSeconds = 5;
	public int fadeSeconds = 5;
	public double dimOpacity = 0.45;
	public boolean soundEnabled = true;
	public double soundVolume = 0.4;

	/** False if the file failed to parse or had invalid values; save() then leaves it alone. Not serialized. */
	private transient boolean writable = true;

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
			cfg.writable = !exists;
		}
		if (!cfg.validate()) {
			cfg.writable = false;
		}
		if (!exists) {
			cfg.save(file);
		}
		return cfg;
	}

	/** Writes the config. Returns false (and writes nothing) if the file on disk has errors the user should fix. */
	public boolean save(Path file) {
		if (!writable) {
			LOG.warn("Not saving {}: it has errors, fix them by hand first", file);
			return false;
		}
		try {
			Files.createDirectories(file.getParent());
			Files.writeString(file, GSON.toJson(this), StandardCharsets.UTF_8);
			return true;
		} catch (IOException e) {
			LOG.warn("Couldn't write config {}", file, e);
			return false;
		}
	}

	/** Resets invalid values to defaults; returns true if everything was already valid. */
	private boolean validate() {
		McChatConfig d = new McChatConfig();
		boolean valid = true;
		if (textSize < MIN_TEXT_SIZE || textSize > MAX_TEXT_SIZE) {
			warn("textSize", textSize);
			valid = false;
			textSize = d.textSize;
		}
		if (width < 80 || width > 600) {
			warn("width", width);
			valid = false;
			width = d.width;
		}
		if (visibleLines < 0 || visibleLines > 50) {
			warn("visibleLines", visibleLines);
			valid = false;
			visibleLines = d.visibleLines;
		}
		if (expandedLines < 1 || expandedLines > 100) {
			warn("expandedLines", expandedLines);
			valid = false;
			expandedLines = d.expandedLines;
		}
		if (brightSeconds < 0 || brightSeconds > 3600) {
			warn("brightSeconds", brightSeconds);
			valid = false;
			brightSeconds = d.brightSeconds;
		}
		if (dimSeconds < 0 || dimSeconds > 3600) {
			warn("dimSeconds", dimSeconds);
			valid = false;
			dimSeconds = d.dimSeconds;
		}
		if (fadeSeconds < 0 || fadeSeconds > 3600) {
			warn("fadeSeconds", fadeSeconds);
			valid = false;
			fadeSeconds = d.fadeSeconds;
		}
		if (!(dimOpacity >= 0.1 && dimOpacity <= 1.0)) {
			warn("dimOpacity", dimOpacity);
			valid = false;
			dimOpacity = d.dimOpacity;
		}
		if (!(soundVolume >= 0.0 && soundVolume <= 1.0)) {
			warn("soundVolume", soundVolume);
			valid = false;
			soundVolume = d.soundVolume;
		}
		return valid;
	}

	private static void warn(String key, Object value) {
		LOG.warn("Invalid mcchat.json value {}={}, using default", key, value);
	}
}
