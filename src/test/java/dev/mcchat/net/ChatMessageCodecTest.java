package dev.mcchat.net;

import dev.mcchat.core.ChatMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatMessageCodecTest {
	@Test
	void roundTripsAllFieldsIncludingEmoji() {
		ChatMessage original = new ChatMessage(UUID.randomUUID(), "Friend", "hello 💖 §-free", 1_759_500_000_000L);
		ByteBuf buf = Unpooled.buffer();
		ChatMessageCodec.CODEC.encode(buf, original);
		assertEquals(original, ChatMessageCodec.CODEC.decode(buf));
		assertEquals(0, buf.readableBytes());
	}
}
