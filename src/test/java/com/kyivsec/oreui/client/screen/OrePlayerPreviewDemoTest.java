package com.kyivsec.oreui.client.screen;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrePlayerPreviewDemoTest {
    @Test
    void demoDoesNotAutomaticallySubscribeOrInjectScreenButtons() {
        assertFalse(OrePlayerPreviewDemo.class.isAnnotationPresent(EventBusSubscriber.class));
        for (var method : OrePlayerPreviewDemo.class.getDeclaredMethods()) {
            assertFalse(method.isAnnotationPresent(SubscribeEvent.class));
        }
    }
}
