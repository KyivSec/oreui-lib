package com.kyivsec.oreui.client.widget;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import static org.junit.jupiter.api.Assertions.*;

class OreColorSquareTest {
    private static OreColorSquare square(int color) {
        return new OreColorSquare(Component.literal("Color"), color, value -> {});
    }

    @Test
    void hsvConversionHasExactPrimarySecondaryAndNeutralEndpoints() {
        int[] rainbow = {0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF, 0xFFFF0000};
        for (int i = 0; i < rainbow.length; i++) {
            assertEquals(rainbow[i], OreColorMath.rgb(i / 6.0, 1, 1));
            assertEquals(0xFFFFFFFF, OreColorMath.rgb(i / 6.0, 0, 1));
            assertEquals(0xFF000000, OreColorMath.rgb(i / 6.0, 1, 0));
        }
        assertEquals(0xFF808080, OreColorMath.rgb(0.5, 0, 0.5));
        assertEquals(0xFFFF8080, OreColorMath.rgb(0, 0.5, 1));
    }

    @Test
    void rgbColorsRoundTripWithoutChannelDrift() {
        Random random = new Random(917);
        for (int i = 0; i < 10000; i++) {
            int color = random.nextInt();
            var hsv = OreColorMath.hsv(color);
            assertEquals(color | 0xFF000000, OreColorMath.rgb(hsv.hue(), hsv.saturation(), hsv.brightness()));
        }
    }

    @Test
    void changesNotifyOnlyNewColorsAndPreserveAlpha() {
        List<Integer> changes = new ArrayList<>();
        OreColorSquare square = new OreColorSquare(Component.literal("Color"), 0x80FF0000, changes::add);
        assertTrue(changes.isEmpty());
        square.setColor(0x80FF0000);
        square.setHue(1.0 / 3);
        square.setHue(1.0 / 3);
        square.setSaturationBrightness(1, 0.5);
        square.setSaturationBrightness(1, 0.5);
        assertEquals(List.of(0x8000FF00, 0x80008000), changes);
        square.setColor(OreColorPicker.parse("#7345E5"));
        assertEquals(0xFF7345E5, square.getColor());
        assertEquals(3, changes.size());
    }

    @Test
    void achromaticColorsRetainHueAndBlackRetainsSaturationForLaterEdits() {
        OreColorSquare square = square(0xFFFF0000);
        square.setColor(0xFF000000);
        square.setHue(2.0 / 3);
        assertEquals(0xFF000000, square.getColor());
        square.setSaturationBrightness(square.getSaturation(), 1);
        assertEquals(0xFF0000FF, square.getColor());
        square.setColor(0xFF808080);
        assertEquals(2.0 / 3, square.getHue(), 1e-12);
        assertEquals(0, square.getSaturation());
        square.setSaturationBrightness(1, 1);
        assertEquals(0xFF0000FF, square.getColor());
    }

    @Test
    void pointerCoversAllCornersAndClampsOutsideDuringDrag() {
        OreColorSquare square = square(0xFFFF0000);
        square.setWidth(107);
        square.setPosition(20, 30);
        square.onClick(23, 33);
        assertEquals(0xFFFFFFFF, square.getColor());
        assertTrue(square.mouseDragged(123, 33, 0, 100, 0));
        assertEquals(0xFFFF0000, square.getColor());
        square.mouseDragged(123, 133, 0, 0, 100);
        assertEquals(0xFF000000, square.getColor());
        assertEquals(1, square.getSaturation());
        square.mouseDragged(-100, 500, 0, -223, 367);
        assertEquals(0, square.getSaturation());
        assertEquals(0, square.getBrightness());
        square.mouseDragged(500, -100, 0, 600, -600);
        assertEquals(0xFFFF0000, square.getColor());
        square.onRelease(500, -100);
        assertFalse(square.mouseDragged(23, 133, 0, -477, 233));
        assertEquals(0xFFFF0000, square.getColor());
    }

    @Test
    void keyboardRequiresFocusAndSupportsFineAndCoarseChanges() {
        OreColorSquare square = square(0xFFFF0000);
        assertFalse(square.keyPressed(GLFW.GLFW_KEY_LEFT, 0, 0));
        square.setFocused(true);
        assertTrue(square.keyPressed(GLFW.GLFW_KEY_LEFT, 0, 0));
        assertEquals(0.99, square.getSaturation(), 1e-12);
        square.keyPressed(GLFW.GLFW_KEY_DOWN, 0, GLFW.GLFW_MOD_SHIFT);
        assertEquals(0.9, square.getBrightness(), 1e-12);
        square.keyPressed(GLFW.GLFW_KEY_UP, 0, GLFW.GLFW_MOD_SHIFT);
        square.keyPressed(GLFW.GLFW_KEY_RIGHT, 0, GLFW.GLFW_MOD_SHIFT);
        assertEquals(0xFFFF0000, square.getColor());
        assertFalse(square.keyPressed(GLFW.GLFW_KEY_TAB, 0, 0));
    }

    @Test
    void disabledHiddenUnfocusedAndCancelledControlsDoNotContinueEditing() {
        OreColorSquare square = square(0xFFFF0000);
        square.setFocused(true);
        square.onClick(124, 3);
        square.cancelDrag();
        assertFalse(square.mouseDragged(3, 124, 0, -121, 121));
        square.onClick(124, 3);
        square.setFocused(false);
        assertFalse(square.mouseDragged(3, 124, 0, -121, 121));
        for (boolean hidden : new boolean[] {false, true}) {
            square.setFocused(true);
            square.active = hidden;
            square.visible = !hidden;
            square.onClick(3, 124);
            assertFalse(square.mouseDragged(3, 124, 0, 0, 0));
            assertFalse(square.keyPressed(GLFW.GLFW_KEY_LEFT, 0, 0));
            assertEquals(0xFFFF0000, square.getColor());
        }
    }

    @Test
    void layoutRemainsSquareAndCoordinatesRejectNonfiniteValuesAtomically() {
        OreColorSquare square = square(0xFFFF0000);
        square.setWidth(154);
        assertEquals(154, square.getHeight());
        square.setHeight(110);
        assertEquals(110, square.getWidth());
        square.setWidth(0);
        assertEquals(8, square.getWidth());
        assertEquals(8, square.getHeight());
        square.setSaturationBrightness(2, -1);
        assertEquals(1, square.getSaturation());
        assertEquals(0, square.getBrightness());
        for (double value : new double[] {Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> square.setHue(value));
            assertThrows(IllegalArgumentException.class, () -> square.setSaturationBrightness(value, 1));
            assertThrows(IllegalArgumentException.class, () -> square.setSaturationBrightness(0, value));
        }
        assertEquals(1, square.getSaturation());
        assertEquals(0, square.getBrightness());
    }
}
