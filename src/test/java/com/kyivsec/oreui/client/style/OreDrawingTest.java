package com.kyivsec.oreui.client.style;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OreDrawingTest {
    @Test
    void pressedFaceMovesDownAndRemovesDepthWithoutChangingTheWidgetBounds() {
        ScreenRectangle raised = OreDrawing.buttonFaceBounds(10, 20, 162, 28, true, false);
        ScreenRectangle pressed = OreDrawing.buttonFaceBounds(10, 20, 162, 28, true, true);
        ScreenRectangle disabled = OreDrawing.buttonFaceBounds(10, 20, 162, 28, false, false);
        assertEquals(new ScreenRectangle(11, 21, 160, 24), raised);
        assertEquals(new ScreenRectangle(11, 24, 160, 23), pressed);
        assertEquals(new ScreenRectangle(11, 21, 160, 26), disabled);
        assertEquals(disabled, OreDrawing.buttonFaceBounds(10, 20, 162, 28, false, true));
        assertEquals(pressed.bottom(), disabled.bottom());
    }

    @Test
    void squareSwitchHandleUsesTheSameFaceGeometryAndSmallBoundsAreNonnegative() {
        assertEquals(new ScreenRectangle(1, 1, 12, 10), OreDrawing.buttonFaceBounds(0, 0, 14, 14, true, false));
        for (int size = 0; size < 6; size++) {
            ScreenRectangle face = OreDrawing.buttonFaceBounds(0, 0, size, size, true, true);
            assertTrue(face.width() >= 0);
            assertTrue(face.height() >= 0);
        }
    }
}
