package com.kyivsec.oreui.client.widget;

import java.util.Comparator;
import java.util.List;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OreTableTest {
    private static OreTable.Row row(String name, String value) {
        return new OreTable.Row(Component.literal(name), List.of(Component.literal(value)));
    }

    @Test
    void sortsBothDirectionsWithCallerSuppliedNumericOrdering() {
        OreTable table = new OreTable(Component.literal("Table"), List.of(Component.literal("Count")),
                List.of(row("A", "12"), row("B", "2"), row("C", "100")));
        table.setComparator(0, Comparator.comparingInt(component -> Integer.parseInt(component.getString())));
        table.sortBy(0);
        assertTrue(table.isAscending());
        assertEquals("B", table.getRows().getFirst().heading().getString());
        table.sortBy(0);
        assertFalse(table.isAscending());
        assertEquals("C", table.getRows().getFirst().heading().getString());
        assertThrows(UnsupportedOperationException.class, () -> table.getRows().clear());
        assertThrows(IndexOutOfBoundsException.class, () -> table.sortBy(1));
    }

    @Test
    void rejectsMismatchedRowsWithoutDiscardingExistingData() {
        OreTable table = new OreTable(Component.empty(), List.of(Component.literal("Count")), List.of(row("A", "1")));
        assertThrows(IllegalArgumentException.class, () -> table.setRows(List.of(new OreTable.Row(Component.empty(), List.of()))));
        assertEquals(1, table.getRows().size());
        assertEquals(64, table.getHeight());
        assertThrows(IllegalArgumentException.class, () -> new OreTable(Component.empty(), List.of(), List.of()));
    }
}
