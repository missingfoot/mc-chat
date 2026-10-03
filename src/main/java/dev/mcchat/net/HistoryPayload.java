package dev.mcchat.net;

import dev.mcchat.McChat;
import dev.mcchat.core.ChatMessage;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/** Server → client on join: recent history, oldest first. */
public record HistoryPayload(List<ChatMessage> messages) implements CustomPacketPayload {
	public static final int MAX_MESSAGES = 200;
	public static final Type<HistoryPayload> TYPE = new Type<>(McChat.id("history"));
	public static final StreamCodec<RegistryFriendlyByteBuf, HistoryPayload> CODEC = StreamCodec.composite(
			ChatMessageCodec.CODEC.apply(ByteBufCodecs.<ByteBuf, ChatMessage>list(MAX_MESSAGES)), HistoryPayload::messages,
			HistoryPayload::new);

	@Override
	public Type<HistoryPayload> type() {
		return TYPE;
	}
}
