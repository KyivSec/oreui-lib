package com.kyivsec.oreui.client.widget;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OreScrollbarTest {
    @Test
    void metricsClampSilentlyAndValueChangesNotifyOnce() {
        List<Integer> changes = new ArrayList<>();
        OreScrollbar bar = new OreScrollbar(Component.empty(), OreScrollbar.Orientation.HORIZONTAL, changes::add);
        bar.setWidth(100);
        bar.setMetrics(500, 100, 0);
        assertEquals(400, bar.getMaximum());
        assertEquals(20, bar.thumbLength());
        bar.setValue(700);
        bar.setValue(400);
        assertEquals(List.of(400), changes);
        bar.setMetrics(50, 100, 400);
        assertEquals(0, bar.getValue());
        assertEquals(100, bar.thumbLength());
        assertEquals(List.of(400), changes);
        assertThrows(IllegalArgumentException.class, () -> bar.setMetrics(-1, 10, 0));
    }

    @Test
    void clickingTrackAndDraggingUsesTheThumbTravel() {
        OreScrollbar bar = new OreScrollbar(Component.empty(), OreScrollbar.Orientation.VERTICAL, value -> {});
        bar.setPosition(10, 20);
        bar.setHeight(100);
        bar.setMetrics(500, 100, 0);
        bar.onClick(12, 70);
        assertEquals(200, bar.getValue());
        bar.onDrag(12, 110, 0, 40);
        assertEquals(400, bar.getValue());
        bar.onRelease(12, 110);
        bar.onDrag(12, 20, 0, -90);
        assertEquals(400, bar.getValue());
    }
}
