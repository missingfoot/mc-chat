package dev.mcchat.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Messages this player has sent, for up/down-arrow recall. Persisted one per line. */
public final class SentHistory {
	private static final Logger LOG = LoggerFactory.getLogger("mcchat");

	private final Path file;
	private final int capacity;
	private final List<String> entries = new ArrayList<>();
	private int cursor;

	public SentHistory(Path file, int capacity) {
		this.file = file;
		this.capacity = capacity;
	}

	public SentHistory load() {
		entries.clear();
		if (Files.exists(file)) {
			try {
				for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
					if (!line.isBlank()) {
						entries.add(line);
					}
				}
			} catch (IOException e) {
				LOG.warn("Couldn't read sent history {}", file, e);
			}
		}
		trim();
		resetCursor();
		return this;
	}

	public void add(String text) {
		if (text == null || text.isBlank()) {
			return;
		}
		if (entries.isEmpty() || !entries.getLast().equals(text)) {
			entries.add(text);
			trim();
			save();
		}
		resetCursor();
	}

	public void resetCursor() {
		cursor = entries.size();
	}

	/** Older entry, or null if there is no history. Stops at the oldest. */
	public String previous() {
		if (entries.isEmpty()) {
			return null;
		}
		if (cursor > 0) {
			cursor--;
		}
		return entries.get(cursor);
	}

	/** Newer entry; "" when stepping past the newest; null if already there. */
	public String next() {
		if (cursor >= entries.size()) {
			return null;
		}
		cursor++;
		return cursor == entries.size() ? "" : entries.get(cursor);
	}

	public List<String> entries() {
		return List.copyOf(entries);
	}

	private void trim() {
		while (entries.size() > capacity) {
			entries.removeFirst();
		}
	}

	private void save() {
		try {
			Files.createDirectories(file.getParent());
			Files.write(file, entries, StandardCharsets.UTF_8);
		} catch (IOException e) {
			LOG.warn("Couldn't save sent history {}", file, e);
		}
	}
}
