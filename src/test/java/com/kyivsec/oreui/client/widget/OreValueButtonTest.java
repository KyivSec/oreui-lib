package com.kyivsec.oreui.client.widget;

import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OreValueButtonTest {
    @Test void currentValueIsIncludedInTheAccessibleMessageWithoutConstructionCallbacks() {
        AtomicInteger presses = new AtomicInteger();
        OreValueButton button = new OreValueButton(Component.literal("Gender"), Component.literal("Male"), ignored -> presses.incrementAndGet());
        assertEquals("Male", button.getValue().getString());
        Component previous = button.getMessage();
        button.setValue(Component.literal("Female"));
        assertEquals("Female", button.getValue().getString());
        assertNotEquals(previous, button.getMessage());
        button.setSwatchColor(0xFF123456);
        assertEquals(Integer.valueOf(0xFF123456), button.getSwatchColor());
        assertEquals(0, presses.get());
        button.onPress();
        assertEquals(1, presses.get());
    }
}
