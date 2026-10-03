package dev.mcchat;

import dev.mcchat.net.DeliverPayload;
import dev.mcchat.net.HistoryPayload;
import dev.mcchat.net.SendPayload;
import dev.mcchat.server.McChatServer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class McChat implements ModInitializer {
	public static final String MOD_ID = "mcchat";
	public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.serverboundPlay().register(SendPayload.TYPE, SendPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(DeliverPayload.TYPE, DeliverPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(HistoryPayload.TYPE, HistoryPayload.CODEC);
		McChatServer.init();
		LOG.info("MC Chat loaded");
	}
}
