package com.kyivsec.oreui.client.widget;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import static org.junit.jupiter.api.Assertions.*;

class OreSteppedSliderTest {
    private static final class HeadlessSlider extends OreSteppedSlider {
        private boolean focused;

        private HeadlessSlider() {
            super(Component.literal("Steps"), 10, 0.3, false, value -> {});
        }

        // Native setFocused reads Minecraft's input type; this fixture tests keyboard logic without a client.
        @Override
        public boolean isFocused() {
            return focused;
        }
    }

    @Test
    void valuesSnapAndOnlyChangedStepsNotify() {
        List<Double> changes = new ArrayList<>();
        OreSteppedSlider slider = new OreSteppedSlider(Component.literal("Steps"), 10, 0.31, true, changes::add);
        assertEquals(3, slider.getStep());
        assertTrue(changes.isEmpty());
        slider.setProgress(0.32);
        slider.setProgress(0.39);
        slider.setProgress(2);
        assertEquals(List.of(0.4, 1.0), changes);
        assertThrows(IllegalArgumentException.class, () -> slider.setProgress(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new OreSteppedSlider(Component.empty(), 0, 0, false, value -> {}));
    }

    @Test
    void keyboardMovesExactlyOneIntervalAndUnavailableControlsRejectInput() {
        HeadlessSlider slider = new HeadlessSlider();
        assertFalse(slider.keyPressed(GLFW.GLFW_KEY_RIGHT, 0, 0));
        assertEquals(3, slider.getStep());
        slider.focused = true;
        assertTrue(slider.keyPressed(GLFW.GLFW_KEY_RIGHT, 0, 0));
        assertEquals(4, slider.getStep());
        slider.keyPressed(GLFW.GLFW_KEY_HOME, 0, 0);
        assertEquals(0, slider.getStep());
        slider.keyPressed(GLFW.GLFW_KEY_END, 0, 0);
        assertEquals(10, slider.getStep());
        slider.active = false;
        assertFalse(slider.keyPressed(GLFW.GLFW_KEY_LEFT, 0, 0));
        assertEquals(10, slider.getStep());
        slider.active = true;
        slider.visible = false;
        assertFalse(slider.keyPressed(GLFW.GLFW_KEY_LEFT, 0, 0));
        assertEquals(10, slider.getStep());
    }
}
