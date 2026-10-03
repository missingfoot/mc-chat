package dev.mcchat.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Skins for chat heads. Remembers skins seen this session so heads survive the sender logging off. */
public final class SkinCache {
	private static final Map<UUID, PlayerSkin> SEEN = new HashMap<>();

	private SkinCache() {
	}

	public static PlayerSkin skinFor(UUID playerId) {
		ClientPacketListener connection = Minecraft.getInstance().getConnection();
		PlayerInfo info = connection != null ? connection.getPlayerInfo(playerId) : null;
		if (info != null) {
			PlayerSkin skin = info.getSkin();
			SEEN.put(playerId, skin);
			return skin;
		}
		return SEEN.getOrDefault(playerId, DefaultPlayerSkin.get(playerId));
	}
}
