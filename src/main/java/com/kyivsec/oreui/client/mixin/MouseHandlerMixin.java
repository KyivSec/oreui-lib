package com.kyivsec.oreui.client.mixin;

import com.kyivsec.oreui.client.hud.OreHudManager;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Inject(method = "onPress", at = @At("HEAD"), cancellable = true)
    private void oreui$button(long window, int button, int action, int modifiers, CallbackInfo callback) {
        if (OreHudManager.mouseButton(window, button, action, modifiers)) callback.cancel();
    }
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void oreui$scroll(long window, double horizontal, double vertical, CallbackInfo callback) {
        if (OreHudManager.scroll(window, horizontal, vertical)) callback.cancel();
    }
    @Inject(method = "onMove", at = @At("TAIL"))
    private void oreui$move(long window, double x, double y, CallbackInfo callback) {
        OreHudManager.move(window, x, y);
    }
}
