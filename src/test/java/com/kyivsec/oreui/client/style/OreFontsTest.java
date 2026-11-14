package com.kyivsec.oreui.client.style;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.Font;
import com.kyivsec.oreui.client.widget.OreTextField;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OreFontsTest {
    private static final ResourceLocation BODY = ResourceLocation.fromNamespaceAndPath("example", "ui_body");
    private static final ResourceLocation HEADING = ResourceLocation.fromNamespaceAndPath("example", "ui_heading");

    private static final class RecordingFont extends Font {
        private final List<Style> drawn = new ArrayList<>();
        private int regularWidth = 8;
        private boolean outline;
        private final StringSplitter metrics = new StringSplitter((codepoint, style) -> {
            int width = style.getFont().equals(BODY) ? regularWidth : style.getFont().equals(HEADING) ? 10 : 4;
            return width + (style.isBold() ? 1 : 0);
        });

        private RecordingFont() { super(resource -> null, false); }

        @Override
        public StringSplitter getSplitter() { return metrics; }

        @Override
        public int drawInBatch(FormattedCharSequence text, float x, float y, int color, boolean shadow,
                               Matrix4f matrix, MultiBufferSource buffers, DisplayMode mode, int background, int light) {
            drawn.clear();
            text.accept((index, style, codepoint) -> { drawn.add(style); return true; });
            return (int) x + (int) Math.ceil(metrics.stringWidth(text));
        }

        @Override
        public void drawInBatch8xOutline(FormattedCharSequence text, float x, float y, int color, int outlineColor,
                                         Matrix4f matrix, MultiBufferSource buffers, int light) {
            outline = true;
            drawInBatch(text, x, y, color, false, matrix, buffers, DisplayMode.NORMAL, 0, light);
        }
    }

    @Test
    void defaultFontIsUnchangedAndCustomViewsAreLocal() {
        RecordingFont base = new RecordingFont();
        assertSame(base, OreFonts.font(base, Style.DEFAULT_FONT));
        Font regular = OreFonts.font(base, BODY);
        Font heading = OreFonts.font(base, HEADING);
        assertNotSame(base, regular);
        assertEquals(24, regular.width("ABC"));
        assertEquals(30, heading.width("ABC"));
        assertEquals(24, regular.width("ABC"));
        assertThrows(NullPointerException.class, () -> OreFonts.font(null, BODY));
        assertThrows(NullPointerException.class, () -> OreFonts.font(base, null));
    }

    @Test
    void everyExplicitResourceReachesTheRendererWithItsOwnMetrics() {
        ResourceLocation[] faces = {BODY, HEADING};
        for (int i = 0; i < faces.length; i++) {
            RecordingFont base = new RecordingFont();
            Font font = OreFonts.font(base, faces[i]);
            Component sample = Component.literal("Aa");
            int expected = 2 * (8 + 2 * i);
            assertEquals(expected, font.width(sample));
            assertEquals(expected, font.drawInBatch(sample, 0, 0, -1, false, new Matrix4f(), null,
                    Font.DisplayMode.NORMAL, 0, 0));
            assertEquals(faces[i], base.drawn.getFirst().getFont());
            assertFalse(base.drawn.getFirst().isBold());
            assertFalse(base.drawn.getFirst().isItalic());
        }
    }

    @Test
    void fontChoicePreservesNativeBoldItalicAndOtherFormatting() {
        Style original = Style.EMPTY.withBold(true).withItalic(true).withColor(0x123456).withUnderlined(true);
        Style resolved = OreFonts.resolveStyle(original, BODY);
        assertEquals(BODY, resolved.getFont());
        assertTrue(resolved.isBold());
        assertTrue(resolved.isItalic());
        assertTrue(resolved.isUnderlined());
        assertEquals(original.getColor(), resolved.getColor());
        assertTrue(original.isBold());
        assertTrue(original.isItalic());
        assertEquals(BODY, OreFonts.resolveStyle(Style.EMPTY.withBold(true), BODY).getFont());
        assertTrue(OreFonts.resolveStyle(Style.EMPTY.withItalic(true), BODY).isItalic());
    }

    @Test
    void explicitUnrelatedFontsKeepTheirOwnFormatting() {
        ResourceLocation other = ResourceLocation.fromNamespaceAndPath("example", "body");
        Style original = Style.EMPTY.withFont(other).withBold(true).withItalic(true);
        assertEquals(original, OreFonts.resolveStyle(original, BODY));
        Font font = OreFonts.font(new RecordingFont(), BODY);
        assertEquals(10, font.width(Component.literal("AB").setStyle(original)));
    }

    @Test
    void drawingAndMeasuringMixedComponentsUseTheSameStylesWithoutMutatingComponents() {
        RecordingFont base = new RecordingFont();
        Font font = OreFonts.font(base, BODY);
        Component text = Component.literal("A")
                .append(Component.literal("B").withStyle(ChatFormatting.BOLD))
                .append(Component.literal("C").withStyle(ChatFormatting.ITALIC))
                .append(Component.literal("D").withStyle(ChatFormatting.BOLD, ChatFormatting.ITALIC));
        assertEquals(34, font.width(text));
        assertEquals(34, font.width(text.getVisualOrderText()));
        assertEquals(39, font.drawInBatch(text, 5, 0, -1, false, new Matrix4f(), null, Font.DisplayMode.NORMAL, 0, 0));
        assertEquals(List.of(BODY, BODY, BODY, BODY), base.drawn.stream().map(Style::getFont).toList());
        assertFalse(base.drawn.getFirst().isBold());
        assertTrue(base.drawn.get(1).isBold());
        assertTrue(base.drawn.get(2).isItalic());
        assertTrue(base.drawn.get(3).isBold());
        assertTrue(base.drawn.get(3).isItalic());
        assertEquals(Style.DEFAULT_FONT, text.getStyle().getFont());
        assertTrue(text.getSiblings().getFirst().getStyle().isBold());
    }

    @Test
    void stringFormattingAndOutlineDrawingAlsoDelegateWithCorrectFaces() {
        RecordingFont base = new RecordingFont();
        Font font = OreFonts.font(base, BODY);
        assertEquals(18, font.width("\u00A7lAB"));
        assertEquals(18, font.drawInBatch("\u00A7lAB", 0, 0, -1, false, new Matrix4f(), null, Font.DisplayMode.NORMAL, 0, 0, false));
        assertEquals(BODY, base.drawn.getFirst().getFont());
        assertTrue(base.drawn.getFirst().isBold());
        font.drawInBatch("AB", 0, 0, -1, false, new Matrix4f(), null, Font.DisplayMode.NORMAL, 0, 0);
        assertEquals(BODY, base.drawn.getFirst().getFont());
        font.drawInBatch8xOutline(Component.literal("A").withStyle(ChatFormatting.ITALIC).getVisualOrderText(),
                0, 0, -1, 0, new Matrix4f(), null, 0);
        assertTrue(base.outline);
        assertEquals(BODY, base.drawn.getFirst().getFont());
        assertTrue(base.drawn.getFirst().isItalic());
    }

    @Test
    void clippingWrappingAndSplitterMetricsAgreeWithDrawing() {
        Font font = OreFonts.font(new RecordingFont(), BODY);
        assertEquals("AB", font.plainSubstrByWidth("ABCD", 16));
        assertEquals("CD", font.plainSubstrByWidth("ABCD", 16, true));
        assertEquals("AB", font.substrByWidth(Component.literal("ABCD"), 16).getString());
        assertEquals(2, font.getSplitter().plainIndexAtWidth("ABCD", 16, Style.EMPTY));
        assertEquals(18, font.wordWrapHeight("AB\nCD", 128));
        assertEquals(18, font.wordWrapHeight(Component.literal("AB\nCD"), 128));
        var lines = font.split(Component.literal("AB\nCD"), 128);
        assertEquals(2, lines.size());
        assertEquals(16, font.width(lines.getFirst()));
        lines.getFirst().accept((index, style, codepoint) -> { assertEquals(BODY, style.getFont()); return true; });
        assertEquals("A", font.plainSubstrByWidth("A\uD83D\uDE00", 8));
    }

    @Test
    void resourceMetricChangesAreNotCachedInTheFontView() {
        RecordingFont base = new RecordingFont();
        Font font = OreFonts.font(base, BODY);
        assertEquals(16, font.width("AB"));
        base.regularWidth = 12;
        assertEquals(24, font.width("AB"));
        assertEquals("A", font.plainSubstrByWidth("AB", 16));
    }

    @Test
    void nativeTextFieldsUseCustomMetricsForCursorSelectionAndVisibleText() {
        Font font = OreFonts.font(new RecordingFont(), BODY);
        OreTextField field = new OreTextField(font, 10, 20, 100, Component.literal("Name"), Component.empty());
        field.setValue("abcdef");
        field.setCursorPosition(3);
        field.setHighlightPos(1);
        assertEquals(38, field.getScreenX(3)); // x + four-pixel input inset + three eight-pixel advances.
        assertEquals(22, field.getScreenX(1));
        assertEquals("bc", field.getHighlighted());
        assertEquals("ab", font.plainSubstrByWidth(field.getValue(), 16));
        assertEquals(3, field.getCursorPosition());
    }
}
