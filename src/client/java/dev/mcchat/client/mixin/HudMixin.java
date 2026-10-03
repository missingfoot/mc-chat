package dev.mcchat.client.mixin;

import dev.mcchat.client.ChatHudPanel;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public class HudMixin {
	// RETURN (not TAIL) so we still run when the HUD is hidden with F1 and returns early.
	@Inject(method = "extractRenderState", at = @At("RETURN"))
	private void mcchat$drawPanel(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		ChatHudPanel.extract(graphics);
	}
}
