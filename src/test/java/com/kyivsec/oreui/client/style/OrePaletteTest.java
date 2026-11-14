package com.kyivsec.oreui.client.style;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class OrePaletteTest {
    @Test
    void usesSourceColors() {
        assertEquals(0xFF387E24, OrePalette.COLORED.background(true, false, false));
        assertEquals(0xFF275B1A, OrePalette.COLORED.background(true, true, false));
        assertEquals(0xFF1C4A12, OrePalette.COLORED.background(true, false, true));
        assertEquals(0xFF7345E5, OrePalette.REALMS.background(true, false, false));
        assertEquals(0xFFB1B2B6, OrePalette.GENERAL.background(true, false, true));
        assertEquals(0xFFC09A0D, OrePalette.WARNING.background(true, false, true));
    }

    @Test
    void enabledLabelsUseOriginalPaletteColors() {
        assertEquals(0xFF1E1E20, OrePalette.GENERAL.text(true));
        assertEquals(0xFF1E1E20, OrePalette.WARNING.text(true));
        assertEquals(0xFFFFFFFF, OrePalette.COLORED.text(true));
        assertEquals(0xFFFFFFFF, OrePalette.DESTRUCTIVE.text(true));
        assertEquals(0xFFFFFFFF, OrePalette.REALMS.text(true));
        for (OrePalette palette : OrePalette.values()) {
            assertEquals((palette.text(true) & 0x00FFFFFF) | 0x80000000, palette.text(false));
        }
    }

    @Test
    void disabledAndPressedStatesHavePredictablePrecedence() {
        for (OrePalette palette : OrePalette.values()) {
            assertEquals(palette.background(true, true, false), palette.background(false, true, true));
            assertEquals(palette.background(false, false, false), palette.background(false, true, true));
            assertEquals(palette.background(true, false, true), palette.background(true, true, true));
            assertNotEquals(palette.text(true), palette.text(false));
        }
    }

    @Test
    void buttonBevelUsesSourceDirectionalStateTokens() {
        assertEquals(0xFF50913D, OrePalette.COLORED.stroke(false, false));
        assertEquals(0xFF6B935F, OrePalette.COLORED.stroke(false, true));
        assertEquals(0xFF6B935F, OrePalette.COLORED.stroke(true, true));
        assertEquals(0x66FFFFFF, OrePalette.COLORED.buttonHighlight(false, true));
        assertEquals(0x33FFFFFF, OrePalette.GENERAL.buttonHighlight(true, false));
        assertEquals(0x66FAB0FF, OrePalette.REALMS.buttonHighlight(true, false));
    }
}
