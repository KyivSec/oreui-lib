package com.kyivsec.oreui.client.widget;

import java.util.ArrayList;
import java.util.List;
import com.kyivsec.oreui.client.host.OreWidgetSupport;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OreSliderTest {
    @Test
    void trackOnlyModeFitsCompactHeightAndRetainsNarrationAndInput() {
        OreSlider slider = new OreSlider(10, 20, 144, 34, Component.literal("Rotation"), 0.5, value -> {});
        Component narration = slider.getMessage();
        assertTrue(slider.isLabelVisible());
        assertEquals(42, slider.trackY());
        slider.setLabelVisible(false);
        slider.setHeight(18);
        assertFalse(slider.isLabelVisible());
        assertTrue(slider.trackY() - 4 >= slider.getY());
        assertTrue(slider.trackY() + 11 <= slider.getBottom());
        assertEquals(narration, slider.getMessage());
        slider.onClick(10 + 144 - 7.5, 25);
        assertEquals(1, slider.getProgress());
        slider.setLabelVisible(true);
        slider.setHeight(34);
        assertEquals(42, slider.trackY());
    }

    @Test
    void steppedSlidersAlsoHonorTrackOnlyMode() {
        OreSteppedSlider slider = new OreSteppedSlider(Component.literal("Angle"), 12, 0.5, true, value -> {});
        slider.setLabelVisible(false);
        slider.setHeight(18);
        assertFalse(slider.isLabelVisible());
        assertTrue(slider.trackY() - 4 >= slider.getY());
        assertTrue(slider.trackY() + 11 <= slider.getBottom());
        slider.setProgress(0.8);
        assertEquals(10, slider.getStep());
    }
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
