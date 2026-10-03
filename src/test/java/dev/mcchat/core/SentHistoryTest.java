package dev.mcchat.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SentHistoryTest {
	@TempDir
	Path dir;

	@Test
	void emptyHistoryNavigationReturnsNull() {
		SentHistory h = new SentHistory(dir.resolve("sent.txt"), 5).load();
		assertNull(h.previous());
		assertNull(h.next());
	}

	@Test
	void upAndDownWalkHistory() {
		SentHistory h = new SentHistory(dir.resolve("sent.txt"), 5).load();
		h.add("one");
		h.add("two");
		h.resetCursor();
		assertEquals("two", h.previous());
		assertEquals("one", h.previous());
		assertEquals("one", h.previous()); // stays at oldest
		assertEquals("two", h.next());
		assertEquals("", h.next()); // past newest: empty input
		assertNull(h.next()); // already at the end
	}

	@Test
	void ignoresBlankAndConsecutiveDuplicates() {
		SentHistory h = new SentHistory(dir.resolve("sent.txt"), 5).load();
		h.add("hi");
		h.add("hi");
		h.add("   ");
		assertEquals(List.of("hi"), h.entries());
	}

	@Test
	void keepsOnlyNewestUpToCapacityAndPersists() {
		Path file = dir.resolve("sent.txt");
		SentHistory h = new SentHistory(file, 3).load();
		for (String s : List.of("a", "b", "c", "d")) {
			h.add(s);
		}
		assertEquals(List.of("b", "c", "d"), h.entries());
		assertEquals(List.of("b", "c", "d"), new SentHistory(file, 3).load().entries());
	}
}
