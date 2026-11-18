package com.kyivsec.oreui.client.widget;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OreLabelAlignmentTest {
    @Test void plainLabelsRemainLeftAlignedUnlessExplicitlyCentered() {
        OreLabel label = new OreLabel(null, Component.literal("1 / 2"), 80);
        label.setX(30);
        assertEquals(30, label.plainTextX(22));
        label.setTextCentered(true);
        assertEquals(59, label.plainTextX(22));
        label.setTextCentered(false);
        assertEquals(30, label.plainTextX(22));
    }

    @Test void pageTextStaysCenteredAcrossWidthsAndCounterLengths() {
        OreLabel label = new OreLabel(null, Component.literal("1 / 2"), 80);
        label.setX(17);
        label.setTextCentered(true);
        for (int width : new int[] {60, 80, 120, 241}) {
            label.setWidth(width);
            for (int textWidth : new int[] {20, 25, 35}) {
                assertEquals(label.getX() + width / 2.0, label.plainTextX(textWidth) + textWidth / 2.0, 0.5);
            }
        }
    }
}
