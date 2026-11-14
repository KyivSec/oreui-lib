package com.kyivsec.oreui.client.widget;

import java.util.ArrayList;
import java.util.List;
import com.kyivsec.oreui.client.host.OreWidgetSupport;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OreSliderTest {
    private static final class AudibleSlider extends OreSlider {
        int sounds;
        AudibleSlider() { super(0, 0, 108, 34, Component.literal("Progress"), 0.5, value -> {}); }
        @Override protected void playReleaseSound() { sounds++; }
    }

    @Test
    void cancellationAndUncapturedReleasesAreSilent() {
        AudibleSlider slider = new AudibleSlider();
        slider.onRelease(0, 0);
        slider.onClick(54, 10);
        for (int tick = 0; tick < 100; tick++) OreWidgetSupport.cancel(slider);
        slider.onRelease(0, 0);
        assertEquals(0, slider.sounds);
        assertEquals(0.5, slider.getProgress());
        slider.onClick(54, 10);
        slider.onRelease(54, 10);
        slider.onRelease(54, 10);
        assertEquals(1, slider.sounds);
    }
    @Test
    void clampsValuesAndOnlyReportsChanges() {
        List<Double> changes = new ArrayList<>();
        OreSlider slider = new OreSlider(10, 20, 108, 34, Component.literal("Progress"), 0.5, changes::add);
        assertEquals(List.of(), changes);
        slider.setProgress(0.5);
        slider.setProgress(2);
        slider.setProgress(1);
        slider.setProgress(-1);
        assertEquals(List.of(1.0, 0.0), changes);
        assertEquals(0, slider.getProgress());
    }

    @Test
    void nativePointerMathMatchesRenderedHandleBounds() {
        OreSlider slider = new OreSlider(10, 20, 108, 34, Component.literal("Progress"), 0, value -> {});
        slider.onClick(64, 30);
        assertEquals(0.5, slider.getProgress());
        slider.mouseDragged(500, 30, 0, 436, 0);
        assertEquals(1, slider.getProgress());
        slider.mouseDragged(-50, 30, 0, -550, 0);
        assertEquals(0, slider.getProgress());
    }

    @Test
    void rejectsNonFiniteValues() {
        OreSlider slider = new OreSlider(0, 0, 108, 34, Component.literal("Progress"), 0, value -> {});
        assertThrows(IllegalArgumentException.class, () -> slider.setProgress(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> slider.setProgress(Double.POSITIVE_INFINITY));
        assertEquals(0, slider.getProgress());
    }

    @Test
    void unavailableControlsIgnoreKeyboardAndCapturedDragEvents() {
        OreSlider slider = new OreSlider(10, 20, 108, 34, Component.literal("Progress"), 0.5, value -> {});
        slider.active = false;
        assertFalse(slider.keyPressed(262, 0, 0));
        assertFalse(slider.mouseDragged(500, 30, 0, 436, 0));
        slider.active = true;
        slider.visible = false;
        assertFalse(slider.keyPressed(262, 0, 0));
        assertFalse(slider.mouseDragged(500, 30, 0, 436, 0));
        assertEquals(0.5, slider.getProgress());
    }
}
