package dev.mcchat.core;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/** Chat history kept in memory and appended to a JSON-lines file, one message per line. */
public final class ChatHistoryStore {
	public static final int DEFAULT_CAPACITY = 1000;

	private static final Logger LOG = LoggerFactory.getLogger("mcchat");
	private static final Gson GSON = new Gson();

	private final Path file;
	private final int capacity;
	private final ArrayDeque<ChatMessage> messages = new ArrayDeque<>();
	private int linesOnDisk;

	public ChatHistoryStore(Path file, int capacity) {
		this.file = file;
		this.capacity = capacity;
	}

	public synchronized void load() {
		messages.clear();
		linesOnDisk = 0;
		if (!Files.exists(file)) {
			return;
		}
		List<String> lines;
		try {
			lines = Files.readAllLines(file, StandardCharsets.UTF_8);
		} catch (IOException e) {
			LOG.warn("Couldn't read chat history {}", file, e);
			return;
		}
		for (String line : lines) {
			if (line.isBlank()) {
				continue;
			}
			linesOnDisk++;
			ChatMessage message = parse(line);
			if (message == null) {
				LOG.warn("Skipping corrupt chat history line: {}", line);
				continue;
			}
			push(message);
		}
	}

	public synchronized void append(ChatMessage message) {
		push(message);
		try {
			Files.createDirectories(file.getParent());
			Files.writeString(file, GSON.toJson(message) + "\n", StandardCharsets.UTF_8,
					StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			linesOnDisk++;
		} catch (IOException e) {
			LOG.warn("Couldn't append to chat history {}", file, e);
		}
	}

	/** The newest {@code n} messages, oldest first. */
	public synchronized List<ChatMessage> recent(int n) {
		List<ChatMessage> all = new ArrayList<>(messages);
		return List.copyOf(all.subList(Math.max(0, all.size() - n), all.size()));
	}

	public synchronized int size() {
		return messages.size();
	}

	/** Rewrites the file with only the in-memory messages if it has grown past capacity. */
	public synchronized void compact() {
		if (linesOnDisk <= capacity) {
			return;
		}
		Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
		try {
			List<String> lines = new ArrayList<>(messages.size());
			for (ChatMessage message : messages) {
				lines.add(GSON.toJson(message));
			}
			Files.write(tmp, lines, StandardCharsets.UTF_8);
			Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			linesOnDisk = messages.size();
		} catch (IOException e) {
			LOG.warn("Couldn't compact chat history {}", file, e);
		}
	}

	private void push(ChatMessage message) {
		messages.addLast(message);
		while (messages.size() > capacity) {
			messages.removeFirst();
		}
	}

	private static ChatMessage parse(String line) {
		try {
			ChatMessage m = GSON.fromJson(line, ChatMessage.class);
			if (m == null || m.senderId() == null || m.senderName() == null || m.text() == null) {
				return null;
			}
			return m;
		} catch (JsonParseException | IllegalStateException | IllegalArgumentException e) {
			return null;
		}
	}
}
