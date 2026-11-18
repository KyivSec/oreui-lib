package com.kyivsec.oreui.client.widget;

import com.kyivsec.oreui.client.style.OrePalette;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OreButtonTest {
    @Test void explicitIconSizesAreHonoredByEveryReferenceAppearanceWithoutChangingDefaults() {
        for (var appearance : OreButton.Appearance.values()) {
            if (appearance == OreButton.Appearance.RAISED) continue;
            OreButton button = new OreButton(0, 0, 24, 22, Component.literal("Camera"), OrePalette.GENERAL, ignored -> {});
            button.setAppearance(appearance);
            assertEquals(appearance == OreButton.Appearance.NAVIGATION ? 14 : 8, button.referenceIconSize());
            button.setIconSize(16);
            assertEquals(16, button.referenceIconSize());
            button.setIconSize(12);
            assertEquals(12, button.referenceIconSize());
            assertThrows(IllegalArgumentException.class, () -> button.setIconSize(0));
        }
    }

    @Test void externalSixteenPixelIconsAreCenteredInCompactIconOnlyButtons() {
        OreButton button = new OreButton(10, 20, 24, 22, Component.literal("Head"), OrePalette.GENERAL, ignored -> {});
        button.setAppearance(OreButton.Appearance.SEAMLESS);
        button.setIcon(ResourceLocation.fromNamespaceAndPath("another_mod", "textures/gui/head.png"), true);
        button.setIconSize(16);
        var content = button.referenceIconContent(16, 28, 60, button.referenceIconSize(), 4, false);
        assertEquals(14, content.iconX());
        assertEquals(22, content.iconX() + button.referenceIconSize() / 2);
        assertEquals(content.textLeft(), content.textRight());
        assertEquals(16, content.textLeft() - content.iconX());
    }

    @Test
    void iconAndLabelAreCenteredAsOneGroupAndLongLabelsStayPadded() {
        int width = 140;
        int label = 54;
        int left = OreButton.contentStart(width, label);
        assertEquals(width / 2, left + (16 + 6 + label) / 2);
        assertEquals(6, OreButton.contentStart(width, width));
        assertEquals(59, OreButton.contentStart(width, 0));
    }

    @Test
    void referenceContentKeepsIconAndLabelTogetherWithBadgeSpaceReserved() {
        OreButton.ReferenceContent content = OreButton.referenceContent(250, 16, 460, 80, 8, 4, false);
        assertEquals(204, content.iconX());
        assertEquals(216, content.textLeft());
        assertEquals(296, content.textRight());
        assertEquals(250, (content.iconX() + content.textRight()) / 2);
        OreButton.ReferenceContent clipped = OreButton.referenceContent(200, 10, 80, 100, 8, 4, false);
        assertEquals(10, clipped.iconX());
        assertEquals(22, clipped.textLeft());
        assertEquals(80, clipped.textRight());
    }

    @Test
    void selectionStaysDepressedAndSelectableIconsKeepTheirFaceHeight() {
        assertEquals(new ScreenRectangle(1, 1, 98, 24), OreButton.referenceFaceBounds(0, 0, 100, 28,
                OreButton.Appearance.SELECTION, false, false, true));
        assertEquals(new ScreenRectangle(1, 4, 98, 23), OreButton.referenceFaceBounds(0, 0, 100, 28,
                OreButton.Appearance.SELECTION, true, false, true));
        assertEquals(new ScreenRectangle(1, 1, 98, 22), OreButton.referenceFaceBounds(0, 0, 100, 26,
                OreButton.Appearance.SELECTABLE_ICON, false, false, true));
        assertEquals(new ScreenRectangle(1, 3, 98, 22), OreButton.referenceFaceBounds(0, 0, 100, 26,
                OreButton.Appearance.SELECTABLE_ICON, true, false, true));
        assertEquals(new ScreenRectangle(4, 16, 8, 1), OreButton.referenceMarkerBounds(0, 0, 17, 17,
                OreButton.Appearance.PAGINATION));
    }

    @Test
    void releaseAndCancellationDoNotActivateAgain() {
        AtomicInteger clicks = new AtomicInteger();
        OreButton button = new OreButton(10, 10, 100, 28, Component.literal("Test"),
                OrePalette.COLORED, ignored -> clicks.incrementAndGet());
        button.onClick(15, 15);
        button.onRelease(15, 15);
        button.onRelease(15, 15);
        button.cancelPress();
        assertEquals(1, clicks.get());
    }

    @Test
    void disabledHiddenAndOutOfBoundsClicksAreIgnored() {
        AtomicInteger clicks = new AtomicInteger();
        OreButton button = new OreButton(10, 10, 100, 28, Component.literal("Test"),
                OrePalette.COLORED, ignored -> clicks.incrementAndGet());
        button.active = false;
        assertFalse(button.mouseClicked(15, 15, 0));
        button.active = true;
        button.visible = false;
        assertFalse(button.mouseClicked(15, 15, 0));
        button.visible = true;
        assertFalse(button.mouseClicked(5, 5, 0));
        assertFalse(button.mouseClicked(15, 15, 1));
        assertEquals(0, clicks.get());
    }

    @Test
    void pointerPressEndsOnReleaseFocusLossAndCancellation() {
        OreButton button = new OreButton(0, 0, 100, 28, Component.literal("Test"), OrePalette.COLORED, ignored -> {});
        button.setFocused(true);
        button.onClick(5, 5);
        assertTrue(button.isPressed());
        button.onRelease(5, 5);
        assertFalse(button.isPressed());
        button.onClick(5, 5);
        button.setFocused(false);
        button.setFocused(true);
        assertFalse(button.isPressed());
        button.onClick(5, 5);
        button.cancelPress();
        assertFalse(button.isPressed());
        button.onClick(5, 5);
        button.active = false;
        assertFalse(button.isPressed());
    }
}
