package com.kyivsec.oreui.client.widget;

import com.kyivsec.oreui.client.style.OrePalette;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OreToggleTest {
    @Test
    void checkboxChangesOncePerActivationAndProgrammaticChange() {
        List<Boolean> changes = new ArrayList<>();
        OreToggle toggle = new OreToggle(Component.literal("Test"), OreToggle.Kind.CHECKBOX, false, changes::add);
        toggle.onPress();
        toggle.setChecked(true);
        toggle.onRelease(0, 0);
        toggle.onPress();
        assertEquals(List.of(true, false), changes);
        assertFalse(toggle.isChecked());
    }

    @Test
    void checkboxUsesTheCommunityBorderAndKeepsAdditionalPalettes() {
        List<Boolean> changes = new ArrayList<>();
        OreToggle checkbox = new OreToggle(Component.literal("Checkbox"), OreToggle.Kind.CHECKBOX, true, changes::add);
        assertEquals(0xFF3C8527, checkbox.checkboxFill());
        assertEquals(0xFF639D52, checkbox.checkboxStroke());
        assertEquals(checkbox.checkboxStroke(), checkbox.checkboxHighlight());
        checkbox.setCheckboxPalette(OrePalette.DESTRUCTIVE);
        assertEquals(0xFFAD1D1D, checkbox.checkboxFill());
        assertEquals(0xFFD98180, checkbox.checkboxStroke());
        checkbox.setCheckboxPalette(OrePalette.WARNING);
        assertEquals(0xFFC09A0D, checkbox.checkboxFill());
        checkbox.setCheckboxPalette(OrePalette.REALMS);
        assertEquals(0xFF4A1CAC, checkbox.checkboxFill());
        checkbox.setIndeterminate(true);
        assertEquals(0xFF4A1CAC, checkbox.checkboxFill());
        assertEquals(List.of(), changes);
        checkbox.setIndeterminate(false);
        checkbox.setChecked(false);
        assertEquals(0xFF8C8D90, checkbox.checkboxFill());
        assertEquals(0xFF97989B, checkbox.checkboxStroke());
        assertEquals(checkbox.checkboxStroke(), checkbox.checkboxHighlight());
        checkbox.active = false;
        assertEquals(0xFFD0D1D4, checkbox.checkboxFill());
        assertEquals(0xFFD0D1D4, checkbox.checkboxStroke());
    }

    @Test
    void radioDoesNotDeselectItselfOrNotifyRepeatedSelections() {
        List<Boolean> changes = new ArrayList<>();
        OreToggle radio = new OreToggle(Component.literal("Radio"), OreToggle.Kind.RADIO, false, changes::add);
        radio.onPress();
        radio.onPress();
        assertTrue(radio.isChecked());
        assertEquals(List.of(true), changes);
        radio.setChecked(false);
        assertEquals(List.of(true, false), changes);
    }

    @Test
    void switchHasSourceProportionsAndItsHandleTravelsElevenPixels() {
        List<Boolean> changes = new ArrayList<>();
        OreToggle toggle = new OreToggle(Component.literal("Switch"), OreToggle.Kind.SWITCH, false, changes::add);
        toggle.setPosition(10, 20);
        assertEquals(new ScreenRectangle(14, 27, 25, 14), toggle.switchBounds());
        assertEquals(new ScreenRectangle(14, 27, 14, 14), toggle.switchHandleBounds());
        toggle.onPress();
        assertEquals(new ScreenRectangle(25, 27, 14, 14), toggle.switchHandleBounds());
        toggle.onRelease(0, 0);
        toggle.onPress();
        assertEquals(List.of(true, false), changes);
    }

    @Test
    void switchStylingDoesNotChangeValueOrNotifyAndUnavailableClicksAreIgnored() {
        List<Boolean> changes = new ArrayList<>();
        OreToggle toggle = new OreToggle(Component.literal("Switch"), OreToggle.Kind.SWITCH, true, changes::add);
        assertEquals(OrePalette.COLORED, toggle.getSwitchPalette());
        assertFalse(toggle.hasSwitchSymbols());
        for (OrePalette palette : OrePalette.values()) {
            toggle.setSwitchPalette(palette);
            assertEquals(palette, toggle.getSwitchPalette());
        }
        toggle.setSwitchSymbols(true);
        assertTrue(toggle.hasSwitchSymbols());
        toggle.active = false;
        assertFalse(toggle.mouseClicked(5, 5, 0));
        toggle.active = true;
        toggle.visible = false;
        assertFalse(toggle.mouseClicked(5, 5, 0));
        assertTrue(toggle.isChecked());
        assertEquals(List.of(), changes);
        assertThrows(NullPointerException.class, () -> toggle.setSwitchPalette(null));
    }
}
