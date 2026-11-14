package com.kyivsec.oreui.client.widget;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrePaginationTest {
    private static List<OreButton> buttons(OrePagination pagination) {
        List<OreButton> buttons = new ArrayList<>();
        pagination.visitWidgets(widget -> buttons.add((OreButton) widget));
        return buttons;
    }

    @Test
    void numberedButtonsReachEveryPageIncludingThoseAfterThree() {
        List<Integer> changes = new ArrayList<>();
        OrePagination pagination = new OrePagination(8, 1, changes::add);
        List<OreButton> buttons = buttons(pagination);
        List<OreButton> numbers = buttons.subList(1, buttons.size() - 1);
        assertEquals(List.of("1", "2", "3", "4", "5", "...", "8"),
                numbers.stream().map(button -> button.getMessage().getString()).toList());
        assertFalse(buttons.getFirst().active);
        assertFalse(numbers.get(5).active);
        numbers.get(3).onPress();
        assertEquals(4, pagination.getPage());
        assertTrue(numbers.get(3).isSelected());
        buttons.getLast().onPress();
        assertEquals(5, pagination.getPage());
        numbers.stream().filter(button -> button.getMessage().getString().equals("6")).findFirst().orElseThrow().onPress();
        assertEquals(6, pagination.getPage());
        assertEquals(List.of(4, 5, 6), changes);
        for (int page = 1; page <= 8; page++) {
            pagination.select(page);
            assertEquals(1L, numbers.stream().filter(OreButton::isSelected).count());
            assertEquals(Integer.toString(page), numbers.stream().filter(OreButton::isSelected)
                    .findFirst().orElseThrow().getMessage().getString());
        }
        assertFalse(buttons.getLast().active);
        pagination.select(99);
        assertEquals(8, pagination.getPage());
        pagination.select(-1);
        assertEquals(1, pagination.getPage());
        assertEquals(8, pagination.getPageCount());
        assertThrows(IllegalArgumentException.class, () -> new OrePagination(0, 1, ignored -> {}));
    }

    @Test
    void openEndedPaginationKeepsItsFirstWindowStableUntilPageSix() {
        OrePagination pagination = new OrePagination(1, ignored -> {});
        List<OreButton> buttons = buttons(pagination);
        pagination.select(4);
        assertEquals("1", buttons.get(1).getMessage().getString());
        assertEquals("5", buttons.get(5).getMessage().getString());
        pagination.select(6);
        assertEquals("6", buttons.get(1).getMessage().getString());
        assertTrue(buttons.get(1).isSelected());
    }

    @Test
    void finitePaginationFitsTheNarrowGalleryAndRestoresSourceTileWidths() {
        OrePagination pagination = new OrePagination(8, 1, ignored -> {});
        pagination.setWidth(140);
        assertTrue(pagination.getWidth() <= 140);
        pagination.setWidth(200);
        assertEquals(17, buttons(pagination).get(1).getWidth());
    }
}
