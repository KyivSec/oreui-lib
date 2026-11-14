package com.kyivsec.oreui.client.widget;

import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OreModelPreviewTest {
    private static final class Preview extends OreModelPreview {
        private int modelTicks;

        private Preview(AtomicLong time) {
            super(Component.literal("Preview"), 100, time::get);
            setWidth(2); // Exercise the render clock without a GL context or Minecraft client.
        }

        private void frame(float worldPartialTick) {
            renderWidget(null, 0, 0, worldPartialTick);
        }

        private float seconds() { return animationSeconds(); }

        @Override protected void tickModel() { modelTicks++; }
        @Override protected float pixelsPerUnit() { return 1; }
        @Override protected void renderModel(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}
    }

    @Test
    void renderingAdvancesBetweenUiTicksEvenWhenTheWorldPartialTickIsFrozen() {
        AtomicLong time = new AtomicLong();
        Preview preview = new Preview(time);
        preview.tick();
        time.set(16_000_000);
        preview.frame(0.42F);
        assertEquals(0.016F, preview.seconds(), 1e-6);
        time.set(32_000_000);
        preview.frame(0.42F);
        assertEquals(0.032F, preview.seconds(), 1e-6);
        assertEquals(0, preview.modelTicks);
        time.set(50_000_000);
        preview.tick();
        preview.frame(0.42F);
        assertEquals(1, preview.modelTicks);
        assertEquals(0.05F, preview.seconds(), 1e-6);
        time.set(66_000_000);
        preview.frame(0.42F);
        assertEquals(0.066F, preview.seconds(), 1e-6);
        assertEquals(1, preview.modelTicks);
    }

    @Test
    void pauseFreezesTheExactInterpolatedPoseAndResumeDoesNotJump() {
        AtomicLong time = new AtomicLong();
        Preview preview = new Preview(time);
        preview.tick();
        time.set(25_000_000);
        preview.frame(0);
        preview.setAnimationRunning(false);
        time.set(1_025_000_000L);
        preview.tick();
        preview.frame(1);
        assertEquals(0.025F, preview.seconds(), 1e-6);
        assertEquals(0, preview.modelTicks);
        preview.setAnimationRunning(true);
        time.set(1_050_000_000L);
        preview.frame(0);
        assertEquals(0.05F, preview.seconds(), 1e-6);
        assertEquals(1, preview.modelTicks);
    }

    @Test
    void hiddenPreviewsDoNotTickModelsOrFastForwardWhenShown() {
        AtomicLong time = new AtomicLong();
        Preview preview = new Preview(time);
        preview.tick();
        time.set(25_000_000);
        preview.frame(0);
        preview.visible = false;
        preview.tick();
        time.set(5_000_000_000L);
        preview.tick();
        preview.visible = true;
        preview.tick();
        assertEquals(0.025F, preview.seconds(), 1e-6);
        assertEquals(0, preview.modelTicks);
        time.set(5_025_000_000L);
        preview.frame(0);
        assertEquals(1, preview.modelTicks);
        assertEquals(0.05F, preview.seconds(), 1e-6);
    }

    @Test
    void aHiddenHudHostSuspendsAnimationWithoutOverwritingWidgetVisibility() {
        AtomicLong time = new AtomicLong();
        Preview preview = new Preview(time);
        preview.tick();
        time.set(25_000_000);
        preview.frame(0);
        preview.setAnimationHostVisible(false);
        time.set(5_000_000_000L);
        preview.tick();
        assertTrue(preview.visible);
        assertEquals(0, preview.modelTicks);
        assertEquals(0.025F, preview.seconds(), 1e-6);
        preview.setAnimationHostVisible(true);
        time.set(5_025_000_000L);
        preview.frame(0);
        assertEquals(1, preview.modelTicks);
    }
}
