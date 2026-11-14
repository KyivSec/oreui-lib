package com.kyivsec.oreui.client.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class OreEditBoxTest {
    @Test
    void textPaddingAgreesWithInnerWidthAndReportedCursorCoordinates() {
        Font font = new Font(resource -> null, false) {
            @Override
            public int width(String value) {
                return value.length() * 6;
            }

            @Override
            public String plainSubstrByWidth(String value, int width) {
                return value.substring(0, Math.max(0, Math.min(value.length(), width / 6)));
            }
        };
        OreEditBox field = new OreEditBox(font, 10, 20, 100, 24, Component.literal("Text"));
        field.setMaxLength(64);
        field.setValue("abcdef");
        assertEquals(86, field.getInnerWidth());
        field.setCursorPosition(3);
        assertEquals(3, field.getCursorPosition());
        assertEquals(35, field.getScreenX(3));
        assertFalse(field.getTextShadow());

        field.search();
        assertEquals(66, field.getInnerWidth());
        assertEquals(55, field.getScreenX(3));
        assertEquals(3, field.getCursorPosition());
    }
}
