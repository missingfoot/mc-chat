package dev.mcchat.client;

import dev.mcchat.core.ChatMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Messages this client has received this session. Touched only on the client main thread. */
public final class ClientChatState {
	public record Entry(ChatMessage message, long receivedAtMillis) {
	}

	private static final int MAX_ENTRIES = 1000;
	private static final List<Entry> ENTRIES = new ArrayList<>();
	private static boolean unread;

	private ClientChatState() {
	}

	/** Replaces everything with join history. receivedAt 0 makes history show dimmed straight away. */
	public static void replaceAll(List<ChatMessage> history) {
		ENTRIES.clear();
		for (ChatMessage m : history) {
			ENTRIES.add(new Entry(m, 0L));
		}
		trim();
	}

	public static void add(ChatMessage message) {
		ENTRIES.add(new Entry(message, System.currentTimeMillis()));
		trim();
	}

	public static List<Entry> entries() {
		return Collections.unmodifiableList(ENTRIES);
	}

	public static boolean hasUnread() {
		return unread;
	}

	public static void markUnread() {
		unread = true;
	}

	public static void markRead() {
		unread = false;
	}

	public static void clear() {
		ENTRIES.clear();
		unread = false;
	}

	private static void trim() {
		while (ENTRIES.size() > MAX_ENTRIES) {
			ENTRIES.removeFirst();
		}
	}
}
