package com.kyivsec.oreui.client.hud;

import com.kyivsec.oreui.OreUi;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.bus.api.EventPriority;

@EventBusSubscriber(modid = OreUi.ID, value = Dist.CLIENT)
public final class OreHudEvents {
    private OreHudEvents() {}
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) { OreHudManager.tick(); }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void screenOpening(ScreenEvent.Opening event) {
        if (event.getNewScreen() != null) OreHudManager.screenOpening();
    }

    // NeoForge's pushGuiLayer initializes the screen directly, without firing Opening.
    @SubscribeEvent
    public static void screenInitializing(ScreenEvent.Init.Pre event) {
        OreHudManager.screenOpening();
    }
}
