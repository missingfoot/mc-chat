package dev.mcchat.net;

import dev.mcchat.core.ChatMessage;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public final class ChatMessageCodec {
	public static final int MAX_NAME_LENGTH = 64;
	public static final int MAX_TEXT_LENGTH = 1024;

	public static final StreamCodec<ByteBuf, ChatMessage> CODEC = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC, ChatMessage::senderId,
			ByteBufCodecs.stringUtf8(MAX_NAME_LENGTH), ChatMessage::senderName,
			ByteBufCodecs.stringUtf8(MAX_TEXT_LENGTH), ChatMessage::text,
			ByteBufCodecs.VAR_LONG, ChatMessage::timestampMillis,
			ChatMessage::new);

	private ChatMessageCodec() {
	}
}
