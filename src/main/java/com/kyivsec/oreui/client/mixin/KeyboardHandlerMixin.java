package com.kyivsec.oreui.client.mixin;

import com.kyivsec.oreui.client.hud.OreHudManager;
import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void oreui$key(long window, int key, int scan, int action, int modifiers, CallbackInfo callback) {
        if (OreHudManager.key(window, key, scan, action, modifiers)) callback.cancel();
    }
    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void oreui$character(long window, int codepoint, int modifiers, CallbackInfo callback) {
        if (OreHudManager.character(window, codepoint, modifiers)) callback.cancel();
    }
}
