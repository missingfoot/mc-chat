package dev.mcchat.net;

import dev.mcchat.McChat;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client → server: "I typed this". */
public record SendPayload(String text) implements CustomPacketPayload {
	public static final Type<SendPayload> TYPE = new Type<>(McChat.id("send"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SendPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.stringUtf8(ChatMessageCodec.MAX_TEXT_LENGTH), SendPayload::text,
			SendPayload::new);

	@Override
	public Type<SendPayload> type() {
		return TYPE;
	}
}
