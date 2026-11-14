package com.kyivsec.oreui.client.widget;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OreStatBarTest {
    @Test
    void samplesLiveValuesAndMaximumHealthChangesWithoutRebuilding() {
        double[] health = {20}, maximum = {20};
        var bar = new OreStatBar(Component.literal("Health"), 0xC43C3C, () -> health[0], () -> maximum[0]);
        assertEquals(1, bar.values().fraction());
        health[0] = 7.5;
        assertEquals(0.375, bar.values().fraction());
        maximum[0] = 40;
        assertEquals(0.1875, bar.values().fraction());
        assertEquals(7.5, bar.values().current());
        assertEquals(40, bar.values().maximum());
        assertEquals(0xFFC43C3C, bar.color());
        assertFalse(bar.mouseClicked(1, 1, 0));
    }

    @Test
    void clampsEmptyOverfullAndInvalidStats() {
        assertEquals(0, new OreStatBar.Values(-1, 20).fraction());
        assertEquals(1, new OreStatBar.Values(30, 20).fraction());
        assertEquals(0, new OreStatBar.Values(10, 0).fraction());
        assertEquals(0, new OreStatBar.Values(Double.NaN, 20).fraction());
        assertEquals(0, new OreStatBar.Values(10, Double.POSITIVE_INFINITY).fraction());
        assertEquals(0, new OreStatBar.Values(10, -1).current());
    }
}
