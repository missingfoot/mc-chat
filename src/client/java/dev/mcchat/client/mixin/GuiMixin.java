package dev.mcchat.client.mixin;

import dev.mcchat.client.CompactChatScreen;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {
	// The "Open Chat" key (T) opens our screen. COMMAND ("/") still opens vanilla chat.
	@Inject(method = "openChatScreen", at = @At("HEAD"), cancellable = true)
	private void mcchat$openCompactChat(ChatComponent.ChatMethod method, CallbackInfo ci) {
		if (method == ChatComponent.ChatMethod.MESSAGE) {
			((Gui) (Object) this).setScreen(new CompactChatScreen());
			ci.cancel();
		}
	}
}
