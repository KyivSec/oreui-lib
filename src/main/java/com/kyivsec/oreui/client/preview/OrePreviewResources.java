package com.kyivsec.oreui.client.preview;

import com.kyivsec.oreui.OreUi;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

/** Internal resource generation; models are rebaked lazily after the reload apply stage completes. */
@EventBusSubscriber(modid = OreUi.ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class OrePreviewResources {
    private static final AtomicLong GENERATION = new AtomicLong();
    private OrePreviewResources() {}
    public static long generation() { return GENERATION.get(); }
    @SubscribeEvent public static void register(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) resources -> GENERATION.incrementAndGet());
    }
}
