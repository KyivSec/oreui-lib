package com.kyivsec.oreui.client.screen;

import com.kyivsec.oreui.client.style.OrePalette;
import com.kyivsec.oreui.client.widget.OreButton;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OreScreenTest {
    private static final class TestScreen extends OreScreen {
        private final OreButton button;

        private TestScreen() {
            super(Component.literal("Test"));
            viewport = new ScreenRectangle(100, 54, 196, 156);
            contentHeight = 400;
            button = addContentWidget(new OreButton(110, 300, 100, 28, Component.literal("Button"), OrePalette.GENERAL, ignored -> {}));
        }

        @Override
        protected void arrangeContent() {
            button.setPosition(110, 300 - scrollOffset);
        }
    }

    @Test
    void scrollingClampsAndOnlyConsumesEventsInsideTheViewport() {
        TestScreen screen = new TestScreen();
        screen.scrollOffset = -24;
        screen.clampScroll();
        assertEquals(0, screen.scrollOffset);
        assertFalse(screen.mouseScrolled(20, 60, 0, -1));
        assertFalse(screen.mouseScrolled(296, 54, 0, -1));
        assertFalse(screen.mouseScrolled(100, 210, 0, -1));
        assertTrue(screen.mouseScrolled(110, 60, 0, -100));
        assertEquals(244, screen.scrollOffset);
        screen.viewport = new ScreenRectangle(100, 54, 196, 636);
        screen.clampScroll();
        assertEquals(0, screen.scrollOffset);
    }

    @Test
    void focusRevealsContentAndRemovalReleasesFocusAndCapture() {
        TestScreen screen = new TestScreen();
        screen.setFocused(screen.button);
        assertEquals(121, screen.scrollOffset);
        assertTrue(screen.button.getY() >= screen.viewport.top());
        assertTrue(screen.button.getBottom() < screen.viewport.bottom());
        screen.setDragging(true);
        screen.removed();
        assertFalse(screen.isDragging());
        assertNull(screen.getFocused());
        assertFalse(screen.button.isFocused());
    }
}
