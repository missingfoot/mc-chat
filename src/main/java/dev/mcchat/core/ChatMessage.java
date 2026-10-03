package dev.mcchat.core;

import java.util.UUID;

public record ChatMessage(UUID senderId, String senderName, String text, long timestampMillis) {
}
