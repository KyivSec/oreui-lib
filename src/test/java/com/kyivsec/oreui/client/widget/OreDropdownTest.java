package com.kyivsec.oreui.client.widget;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OreDropdownTest {
    @Test
    void customMenuHostOpensOnceUntilClosedWithoutCreatingAScreen() {
        AtomicInteger openings = new AtomicInteger();
        OreDropdown dropdown = new OreDropdown(Component.literal("Menu"), List.of(Component.literal("A")), 0, value -> {});
        dropdown.setMenuOpener(owner -> openings.incrementAndGet());
        dropdown.onPress(); dropdown.onPress();
        assertEquals(1, openings.get());
        dropdown.closeMenu(); dropdown.onPress();
        assertEquals(2, openings.get());
    }

    @Test
    void selectionIsBoundedAndDoesNotEmitDuplicateChanges() {
        List<Integer> changes = new ArrayList<>();
        OreDropdown dropdown = new OreDropdown(Component.literal("Test"),
                List.of(Component.literal("A"), Component.literal("B")), 0, changes::add);
        dropdown.select(1);
        dropdown.select(1);
        assertEquals(1, dropdown.getSelectedIndex());
        assertEquals(List.of(1), changes);
        assertThrows(IndexOutOfBoundsException.class, () -> dropdown.select(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> dropdown.select(2));
        assertEquals(1, dropdown.getSelectedIndex());
    }
}
