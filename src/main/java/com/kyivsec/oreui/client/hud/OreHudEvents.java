package com.kyivsec.oreui.client.hud;

import com.kyivsec.oreui.OreUi;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = OreUi.ID, value = Dist.CLIENT)
public final class OreHudEvents {
    private OreHudEvents() {}
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) { OreHudManager.tick(); }
}
