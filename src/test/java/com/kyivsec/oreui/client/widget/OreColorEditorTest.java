package com.kyivsec.oreui.client.widget;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OreColorEditorTest {
    private static Font font() {
        return new Font(resource -> null, false) {
            @Override
            public int width(String value) { return value.length() * 6; }

            @Override
            public String plainSubstrByWidth(String value, int width) {
                return value.substring(0, Math.clamp(width / 6, 0, value.length()));
            }

            @Override
            public List<FormattedCharSequence> split(FormattedText value, int width) {
                List<FormattedCharSequence> lines = new ArrayList<>();
                String text = value.getString();
                int length = Math.max(1, width / 6);
                for (int i = 0; i < text.length(); i += length) {
                    lines.add(FormattedCharSequence.forward(text.substring(i, Math.min(text.length(), i + length)), Style.EMPTY));
                }
                return lines;
            }

            @Override
            public int width(FormattedCharSequence value) {
                int[] width = {0};
                value.accept((index, style, codepoint) -> { width[0] += 6; return true; });
                return width[0];
            }
        };
    }

    private static OreColorEditor editor(int color, List<Integer> changes) {
        return new OreColorEditor(font(), Component.literal("Color"), color, changes::add);
    }

    private static <T> T child(OreColorEditor editor, Class<T> type) {
        List<T> matches = new ArrayList<>();
        editor.visitWidgets(widget -> { if (type.isInstance(widget)) matches.add(type.cast(widget)); });
        assertEquals(1, matches.size());
        return matches.getFirst();
    }

    @Test
    void exposesInlineControlsWithoutPopupButtonsOrConstructionFeedback() {
        List<Integer> changes = new ArrayList<>();
        OreColorEditor editor = editor(0xFF387E24, changes);
        List<AbstractWidget> widgets = new ArrayList<>();
        editor.visitWidgets(widgets::add);
        assertEquals(4, widgets.size());
        assertTrue(widgets.stream().noneMatch(OreButton.class::isInstance));
        assertTrue(changes.isEmpty());
        assertEquals(0xFF387E24, editor.getColor());
        assertEquals("#FF387E24", child(editor, OreTextField.class).getValue());
    }

    @Test
    void squareAndHueApplyImmediatelyAndSynchronizePreviewAndHexOncePerChange() {
        List<Integer> changes = new ArrayList<>();
        OreColorEditor editor = editor(0x80FF0000, changes);
        OreColorSquare square = child(editor, OreColorSquare.class);
        OreHueSlider hue = child(editor, OreHueSlider.class);
        square.setHue(1.0 / 3);
        assertEquals(0x8000FF00, editor.getColor());
        assertEquals(1.0 / 3, hue.getHue(), 1e-12);
        hue.setHue(2.0 / 3);
        square.setSaturationBrightness(1, 0.5);
        square.setSaturationBrightness(1, 0.5);
        assertEquals(List.of(0x8000FF00, 0x800000FF, 0x80000080), changes);
        assertEquals("#80000080", child(editor, OreTextField.class).getValue());
        assertEquals("#80000080", child(editor, OreLabel.class).getMessage().getString());
    }

    @Test
    void validHexAppliesImmediatelyWithoutReplacingTypedTextAndIncompleteHexKeepsLastColor() {
        List<Integer> changes = new ArrayList<>();
        OreColorEditor editor = editor(0xFF387E24, changes);
        OreTextField hex = child(editor, OreTextField.class);
        hex.setValue("7345e5");
        assertEquals(0xFF7345E5, editor.getColor());
        assertEquals("7345e5", hex.getValue());
        assertEquals("#FF7345E5", child(editor, OreLabel.class).getMessage().getString());
        assertEquals(child(editor, OreColorSquare.class).getHue(), child(editor, OreHueSlider.class).getHue());
        hex.setValue("#12345");
        assertEquals(0xFF7345E5, editor.getColor());
        assertEquals(1, changes.size());
        hex.setValue("#GGGGGG");
        assertEquals("#12345", hex.getValue());
        hex.setValue("#807345E5");
        assertEquals(List.of(0xFF7345E5, 0x807345E5), changes);
        assertEquals(0x807345E5, editor.getColor());
    }

    @Test
    void programmaticColorsRefreshAllControlsWithoutRecursiveNotifications() {
        List<Integer> changes = new ArrayList<>();
        OreColorEditor editor = editor(0xFF387E24, changes);
        editor.setColor(0xFF387E24);
        editor.setColor(0x807345E5);
        editor.setColor(0x807345E5);
        assertEquals(List.of(0x807345E5), changes);
        assertEquals("#807345E5", child(editor, OreTextField.class).getValue());
        assertEquals("#807345E5", child(editor, OreLabel.class).getMessage().getString());
        child(editor, OreTextField.class).setValue("#");
        editor.setColor(editor.getColor());
        assertEquals("#807345E5", child(editor, OreTextField.class).getValue());
        assertEquals(1, changes.size());
    }

    @Test
    void achromaticHueChangesRemainAvailableForSubsequentSquareEdits() {
        List<Integer> changes = new ArrayList<>();
        OreColorEditor editor = editor(0x80000000, changes);
        child(editor, OreHueSlider.class).setHue(1.0 / 3);
        assertTrue(changes.isEmpty());
        assertEquals(0x80000000, editor.getColor());
        child(editor, OreColorSquare.class).setSaturationBrightness(1, 1);
        assertEquals(List.of(0x8000FF00), changes);
        assertEquals("#8000FF00", child(editor, OreTextField.class).getValue());
    }

    @Test
    void columnResizingAndPageVisibilityRetainEditsWithoutNotifications() {
        List<Integer> changes = new ArrayList<>();
        OreColorEditor editor = editor(0x807345E5, changes);
        OreColumn column = new OreColumn();
        column.addChild(editor);
        child(editor, OreTextField.class).setValue("#123");
        for (int width : new int[] {1024, 64, 140, 16}) {
            column.setWidth(width);
            column.setPosition(20, 40);
            int size = Math.clamp(width, 32, 128);
            assertEquals(size, child(editor, OreColorSquare.class).getWidth());
            assertEquals(size, child(editor, OreColorSquare.class).getHeight());
            assertEquals(size, child(editor, OreHueSlider.class).getWidth());
            assertEquals(20, child(editor, OreColorSquare.class).getX());
            assertTrue(child(editor, OreHueSlider.class).getY() > child(editor, OreColorSquare.class).getBottom());
            editor.visitWidgets(widget -> widget.visible = false);
            editor.visitWidgets(widget -> widget.visible = true);
        }
        assertEquals("#123", child(editor, OreTextField.class).getValue());
        assertEquals(0x807345E5, editor.getColor());
        assertTrue(changes.isEmpty());
    }

    @Test
    void editorsKeepIndependentColorAndHexState() {
        List<Integer> primaryChanges = new ArrayList<>(), secondaryChanges = new ArrayList<>();
        OreColorEditor primary = editor(0xFFFF0000, primaryChanges);
        OreColorEditor secondary = editor(0x807345E5, secondaryChanges);
        child(primary, OreHueSlider.class).setHue(1.0 / 3);
        assertEquals(0xFF00FF00, primary.getColor());
        assertEquals(0x807345E5, secondary.getColor());
        assertEquals("#807345E5", child(secondary, OreTextField.class).getValue());
        assertEquals(List.of(0xFF00FF00), primaryChanges);
        assertTrue(secondaryChanges.isEmpty());
    }
}
