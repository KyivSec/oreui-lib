package com.kyivsec.oreui.client.widget;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OreNavigationBarTest {
    @Test
    void navigationUpdatesSelectionAndOnlyNotifiesRealUserChanges() {
        List<Integer> changes = new ArrayList<>();
        OreNavigationBar bar = new OreNavigationBar(List.of(Component.literal("One"), Component.literal("Two")),
                LinearLayout.Orientation.VERTICAL, changes::add);
        List<OreButton> buttons = new ArrayList<>();
        bar.visitWidgets(widget -> buttons.add((OreButton) widget));
        buttons.get(1).onPress();
        buttons.get(1).onPress();
        assertEquals(1, bar.getSelectedIndex());
        assertEquals(List.of(1), changes);
        assertFalse(buttons.get(0).isSelected());
        assertTrue(buttons.get(1).isSelected());
        bar.select(0);
        assertEquals(List.of(1), changes);
        assertThrows(IndexOutOfBoundsException.class, () -> bar.select(2));
    }

    @Test
    void allEightEntriesFitTheMinimumGalleryHeight() {
        OreNavigationBar bar = new OreNavigationBar(java.util.stream.IntStream.range(0, 8)
                .mapToObj(i -> (Component) Component.literal("Page " + i)).toList(), LinearLayout.Orientation.VERTICAL, index -> {});
        bar.setWidth(88);
        assertEquals(200, bar.getHeight());
        bar.fitHeight(156);
        assertEquals(152, bar.getHeight());
        assertEquals(88, bar.getWidth());
        bar.setPosition(12, 54);
        assertTrue(bar.getY() + bar.getHeight() <= 210);

        bar.fitHeight(240);
        assertEquals(200, bar.getHeight());
        assertEquals(88, bar.getWidth());
    }
}
