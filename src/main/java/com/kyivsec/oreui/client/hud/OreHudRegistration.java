package com.kyivsec.oreui.client.hud;

import com.kyivsec.oreui.OreUi;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@EventBusSubscriber(modid = OreUi.ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class OreHudRegistration {
    private OreHudRegistration() {}
    @SubscribeEvent public static void layers(RegisterGuiLayersEvent event) { OreHudManager.register(event, OreHudDemo.LAYER); }
    @SubscribeEvent public static void keys(RegisterKeyMappingsEvent event) { OreHudManager.registerInteractionKey(event); }
}
