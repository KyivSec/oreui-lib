package com.kyivsec.oreui.client.widget;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import static org.junit.jupiter.api.Assertions.*;

class OreHueSliderTest {
    @Test
    void hueChangesClampNotifyOnceAndRejectInvalidValues() {
        List<Double> changes = new ArrayList<>();
        OreHueSlider hue = new OreHueSlider(Component.literal("Hue"), 0, changes::add);
        assertTrue(changes.isEmpty());
        hue.setHue(0.5);
        hue.setHue(0.5);
        hue.setHue(2);
        hue.setHue(-1);
        assertEquals(List.of(0.5, 1.0, 0.0), changes);
        for (double value : new double[] {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> hue.setHue(value));
            assertThrows(IllegalArgumentException.class, () -> new OreHueSlider(Component.empty(), value, v -> {}));
        }
    }

    @Test
    void hueDragUsesTrackEndpointsAndStopsOnReleaseFocusLossOrCancellation() {
        OreHueSlider hue = new OreHueSlider(Component.literal("Hue"), 0, value -> {});
        hue.setPosition(20, 30);
        hue.setWidth(107);
        hue.onClick(73, 50);
        assertEquals(0.5, hue.getHue());
        assertTrue(hue.mouseDragged(200, 50, 0, 127, 0));
        assertEquals(1, hue.getHue());
        hue.mouseDragged(0, 50, 0, -200, 0);
        assertEquals(0, hue.getHue());
        hue.onRelease(0, 50);
        assertFalse(hue.mouseDragged(123, 50, 0, 123, 0));
        hue.onClick(73, 50);
        hue.setFocused(false);
        assertFalse(hue.mouseDragged(123, 50, 0, 50, 0));
        hue.onClick(73, 50);
        hue.cancelDrag();
        assertFalse(hue.mouseDragged(123, 50, 0, 50, 0));
        assertEquals(0.5, hue.getHue());
    }

    @Test
    void keyboardSupportsDegreesShiftAndEndpointsWhileUnavailableControlsRejectInput() {
        OreHueSlider hue = new OreHueSlider(Component.literal("Hue"), 0, value -> {});
        assertFalse(hue.keyPressed(GLFW.GLFW_KEY_RIGHT, 0, 0));
        hue.setFocused(true);
        assertTrue(hue.keyPressed(GLFW.GLFW_KEY_RIGHT, 0, 0));
        assertEquals(1.0 / 360, hue.getHue(), 1e-12);
        hue.keyPressed(GLFW.GLFW_KEY_UP, 0, GLFW.GLFW_MOD_SHIFT);
        assertEquals(11.0 / 360, hue.getHue(), 1e-12);
        hue.keyPressed(GLFW.GLFW_KEY_END, 0, 0);
        assertEquals(1, hue.getHue());
        hue.keyPressed(GLFW.GLFW_KEY_HOME, 0, 0);
        assertEquals(0, hue.getHue());
        assertFalse(hue.keyPressed(GLFW.GLFW_KEY_TAB, 0, 0));
        for (boolean hidden : new boolean[] {false, true}) {
            hue.active = hidden;
            hue.visible = !hidden;
            hue.onClick(64, 20);
            assertFalse(hue.keyPressed(GLFW.GLFW_KEY_RIGHT, 0, 0));
            assertFalse(hue.mouseDragged(64, 20, 0, 0, 0));
            assertEquals(0, hue.getHue());
        }
    }

    @Test
    void hueAndSquareSynchronizeWithoutNotifyingTheOwningPickerUntilCommit() {
        List<Integer> committed = new ArrayList<>();
        OreColorPicker picker = new OreColorPicker(Component.literal("Color"), 0x80FF0000, committed::add);
        OreColorSquare square = new OreColorSquare(Component.literal("Color"), picker.getColor(), value -> {});
        OreHueSlider hue = new OreHueSlider(Component.literal("Hue"), square.getHue(), square::setHue);
        hue.setHue(1.0 / 3);
        square.setSaturationBrightness(1, 0.5);
        assertEquals(0x80008000, square.getColor());
        assertEquals(0x80FF0000, picker.getColor());
        assertTrue(committed.isEmpty());
        picker.setColor(square.getColor());
        picker.setColor(square.getColor());
        assertEquals(List.of(0x80008000), committed);
    }
}
