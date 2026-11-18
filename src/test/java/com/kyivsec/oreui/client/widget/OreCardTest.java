package com.kyivsec.oreui.client.widget;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OreCardTest {
    private Font font() {
        return new Font(resource -> null, false) {
            @Override public int width(FormattedCharSequence text) {
                int[] width = {0};
                text.accept((index, style, codePoint) -> { width[0] += 6; return true; });
                return width[0];
            }
            @Override public List<FormattedCharSequence> split(FormattedText text, int width) {
                return List.of(FormattedCharSequence.forward(text.getString(), Style.EMPTY));
            }
        };
    }

    @Test void previewAndCaptionRemainSeparateInsideResizedAndMovedCards() {
        var card = new OreCard(font(), Component.literal("Hair 1"), 64, (g, b, x, y, p) -> {}, ignored -> {});
        card.setPosition(20, 30);
        for (int width : new int[] {69, 100, 145}) {
            card.setWidth(width);
            assertEquals(88, card.getHeight());
            var preview = card.previewBounds();
            var caption = card.captionBounds();
            assertEquals(21, preview.left());
            assertEquals(31, preview.top());
            assertEquals(width - 2, preview.width());
            assertEquals(64, preview.height());
            assertEquals(preview.bottom(), caption.top());
            assertEquals(OreCard.PREVIEW_CAPTION_HEIGHT, caption.height());
            assertEquals(card.getBottom() - 1, caption.bottom());
        }
        card.setHeight(72);
        assertEquals(48, card.previewBounds().height());
        card.setPosition(40, 50);
        assertEquals(41, card.previewBounds().left());
        assertEquals(51, card.previewBounds().top());
    }

    @Test void aCardIsOneSilentTooltipFreeActionAcrossPreviewAndCaption() {
        AtomicInteger renders = new AtomicInteger(), selections = new AtomicInteger();
        var title = Component.literal("Cotton hoodie");
        var card = new OreCard(font(), title, 64, (g, b, x, y, p) -> renders.incrementAndGet(), ignored -> selections.incrementAndGet());
        assertEquals(0, renders.get()); assertEquals(0, selections.get());
        assertEquals(title, card.getMessage()); assertNull(card.getTooltip());
        card.setWidth(100); card.setSelected(true);
        assertTrue(card.isSelected()); assertEquals(0, selections.get());
        card.setFocused(true);
        assertTrue(card.isMouseOver(50, 20));
        card.onClick(50, 20);
        card.onRelease(50, 20);
        assertTrue(card.isMouseOver(50, card.captionBounds().top() + 5));
        card.onClick(50, card.captionBounds().top() + 5);
        card.onRelease(50, card.captionBounds().top() + 5);
        assertEquals(2, selections.get()); assertEquals(0, renders.get());
        card.active = false;
        assertFalse(card.mouseClicked(50, 20, 0));
        assertEquals(2, selections.get());
    }

    @Test void invalidPreviewInputsAreRejectedAndTextOnlyCardsKeepTheirOldSizing() {
        var title = Component.literal("Title");
        assertThrows(IllegalArgumentException.class, () -> new OreCard(font(), title, 0, (g, b, x, y, p) -> {}, ignored -> {}));
        assertThrows(NullPointerException.class, () -> new OreCard(font(), title, 40, null, ignored -> {}));
        var old = new OreCard(font(), title, Component.literal("Description"), 0, ignored -> {});
        assertEquals(32, old.getHeight());
        old.setWidth(120);
        assertEquals(32, old.getHeight());
    }

    @Test void permanentCardFramePaintsEveryEdgeIncludingRightAndBottomInEveryState() {
        for (boolean selected : new boolean[] {false, true}) {
            for (boolean focused : new boolean[] {false, true}) {
                for (boolean enabled : new boolean[] {false, true}) {
                    for (int width : new int[] {66, 69, 94, 100, 145}) {
                        var card = new OreCard(font(), Component.literal("Option"), 64, (g, b, x, y, p) -> {}, ignored -> {});
                        card.setWidth(width);
                        card.setPosition(10, 12);
                        card.setSelected(selected);
                        card.setFocused(focused);
                        card.active = enabled;
                        var pixels = new BufferedImage(180, 120, BufferedImage.TYPE_INT_ARGB);
                        int contentColor = 0xFF242425;
                        for (int y = card.getY(); y < card.getBottom(); y++) {
                            for (int x = card.getX(); x < card.getRight(); x++) pixels.setRGB(x, y, contentColor);
                        }
                        card.paintPreviewBorder((left, top, right, bottom, color) -> {
                            for (int y = top; y < bottom; y++) {
                                for (int x = left; x < right; x++) pixels.setRGB(x, y, color);
                            }
                        });
                        int expected = selected ? 0xFF50913D : enabled && focused ? 0xFF8C8D90 : 0xFF5A5B5C;
                        for (int y = card.getY(); y < card.getBottom(); y++) {
                            assertEquals(expected, pixels.getRGB(card.getX(), y), "left edge");
                            assertEquals(expected, pixels.getRGB(card.getRight() - 1, y), "right edge");
                        }
                        for (int x = card.getX(); x < card.getRight(); x++) {
                            assertEquals(expected, pixels.getRGB(x, card.getY()), "top edge");
                            assertEquals(expected, pixels.getRGB(x, card.getBottom() - 1), "bottom edge");
                        }
                        assertEquals(contentColor, pixels.getRGB(card.getX() + 1, card.getY() + 1));
                        assertEquals(0, pixels.getRGB(card.getRight(), card.getY()));
                        assertEquals(0, pixels.getRGB(card.getX(), card.getBottom()));
                    }
                }
            }
        }
    }

    @Test void emptyCardBoundsDoNotPaintOutsideTheirArea() {
        var card = new OreCard(font(), Component.literal("Option"), 64, (g, b, x, y, p) -> {}, ignored -> {});
        AtomicInteger paints = new AtomicInteger();
        card.setWidth(0);
        card.paintPreviewBorder((l, t, r, b, c) -> paints.incrementAndGet());
        card.setWidth(100);
        card.setHeight(0);
        card.paintPreviewBorder((l, t, r, b, c) -> paints.incrementAndGet());
        assertEquals(0, paints.get());
    }
}
