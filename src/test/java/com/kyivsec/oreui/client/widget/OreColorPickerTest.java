package com.kyivsec.oreui.client.widget;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OreColorPickerTest {
    @Test
    void hexColorsRoundTripWithAlpha() {
        assertEquals(0xFF387E24, OreColorPicker.parse("#387e24"));
        assertEquals(0x807345E5, OreColorPicker.parse("807345E5"));
        assertEquals("#807345E5", OreColorPicker.format(0x807345E5));
        assertEquals(0, OreColorPicker.parse("00000000"));
    }

    @Test
    void incompleteAndMalformedColorsAreRejected() {
        for (String value : List.of("", "#", "12345", "1234567", "123456789", "GGGGGG", "-12345")) {
            assertThrows(IllegalArgumentException.class, () -> OreColorPicker.parse(value));
        }
    }

    @Test
    void colorChangesNotifyOnceWithoutConstructorFeedback() {
        List<Integer> changes = new ArrayList<>();
        OreColorPicker picker = new OreColorPicker(Component.literal("Color"), 0xFF387E24, changes::add);
        picker.setColor(0x807345E5);
        picker.setColor(0x807345E5);
        assertEquals(List.of(0x807345E5), changes);
        assertEquals(0x807345E5, picker.getColor());
    }
}
