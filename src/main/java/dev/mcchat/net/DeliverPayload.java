package dev.mcchat.net;

import dev.mcchat.McChat;
import dev.mcchat.core.ChatMessage;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server → client: one new chat message. */
public record DeliverPayload(ChatMessage message) implements CustomPacketPayload {
	public static final Type<DeliverPayload> TYPE = new Type<>(McChat.id("deliver"));
	public static final StreamCodec<RegistryFriendlyByteBuf, DeliverPayload> CODEC = StreamCodec.composite(
			ChatMessageCodec.CODEC, DeliverPayload::message,
			DeliverPayload::new);

	@Override
	public Type<DeliverPayload> type() {
		return TYPE;
	}
}
