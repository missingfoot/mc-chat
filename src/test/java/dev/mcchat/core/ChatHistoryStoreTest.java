package dev.mcchat.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatHistoryStoreTest {
	private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-000000000001");

	@TempDir
	Path dir;

	private static ChatMessage msg(int i) {
		return new ChatMessage(ALICE, "Alice", "message " + i, 1_000L + i);
	}

	@Test
	void missingFileLoadsEmpty() {
		ChatHistoryStore store = new ChatHistoryStore(dir.resolve("mcchat/history.jsonl"), 10);
		store.load();
		assertEquals(0, store.size());
		assertEquals(List.of(), store.recent(5));
	}

	@Test
	void appendCreatesParentDirsAndSurvivesReload() {
		Path file = dir.resolve("mcchat/history.jsonl");
		ChatHistoryStore store = new ChatHistoryStore(file, 10);
		store.load();
		store.append(msg(1));
		store.append(msg(2));
		assertTrue(Files.exists(file));

		ChatHistoryStore reloaded = new ChatHistoryStore(file, 10);
		reloaded.load();
		assertEquals(List.of(msg(1), msg(2)), reloaded.recent(10));
	}

	@Test
	void keepsOnlyNewestUpToCapacity() {
		ChatHistoryStore store = new ChatHistoryStore(dir.resolve("h.jsonl"), 3);
		store.load();
		for (int i = 1; i <= 5; i++) {
			store.append(msg(i));
		}
		assertEquals(List.of(msg(3), msg(4), msg(5)), store.recent(10));
	}

	@Test
	void recentReturnsLastNOldestFirst() {
		ChatHistoryStore store = new ChatHistoryStore(dir.resolve("h.jsonl"), 10);
		store.load();
		for (int i = 1; i <= 5; i++) {
			store.append(msg(i));
		}
		assertEquals(List.of(msg(4), msg(5)), store.recent(2));
	}

	@Test
	void corruptLinesAreSkipped() throws IOException {
		Path file = dir.resolve("h.jsonl");
		ChatHistoryStore writer = new ChatHistoryStore(file, 10);
		writer.load();
		writer.append(msg(1));
		Files.writeString(file, Files.readString(file)
				+ "not json at all\n"
				+ "{\"senderId\":\"not-a-uuid\",\"senderName\":\"x\",\"text\":\"y\",\"timestampMillis\":1}\n"
				+ "{\"text\":\"missing fields\"}\n"
				+ "\n");
		writer.append(msg(2));

		ChatHistoryStore reloaded = new ChatHistoryStore(file, 10);
		reloaded.load();
		assertEquals(List.of(msg(1), msg(2)), reloaded.recent(10));
	}

	@Test
	void compactRewritesFileToCapacity() throws IOException {
		Path file = dir.resolve("h.jsonl");
		ChatHistoryStore store = new ChatHistoryStore(file, 3);
		store.load();
		for (int i = 1; i <= 5; i++) {
			store.append(msg(i));
		}
		assertEquals(5, Files.readAllLines(file).size());

		store.compact();
		assertEquals(3, Files.readAllLines(file).size());

		ChatHistoryStore reloaded = new ChatHistoryStore(file, 3);
		reloaded.load();
		assertEquals(List.of(msg(3), msg(4), msg(5)), reloaded.recent(10));
	}
}
