package com.kyivsec.oreui.client.widget;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OreNumberFieldTest {
    @Test
    void acceptsIntermediateAndScientificNumericEntry() {
        for (String value : List.of("", "-", "+", ".", "-.", "12.", "-.5", "1e", "1e-", "1.5E+12", Double.toString(Double.MAX_VALUE))) {
            assertTrue(OreNumberField.isIntermediateValue(value), value);
        }
    }

    @Test
    void rejectsUnrelatedCharactersAndMalformedNumbers() {
        for (String value : List.of("text", "1..2", "--1", "12 3", "NaN", "Infinity", "0x20", "1e2e3")) {
            assertFalse(OreNumberField.isIntermediateValue(value), value);
        }
    }
}
