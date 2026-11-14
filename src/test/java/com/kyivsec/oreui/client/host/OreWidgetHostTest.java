package com.kyivsec.oreui.client.host;

import com.kyivsec.oreui.client.layout.OreGrid;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import static org.junit.jupiter.api.Assertions.*;

class OreWidgetHostTest {
    private static final class Control extends AbstractWidget {
        int presses, releases, drags, keys;
        String typed = "";
        Control() { super(0, 0, 80, 20, Component.literal("Control")); }
        @Override public boolean mouseClicked(double x, double y, int button) {
            if (!active || !visible || button != 0 || !isMouseOver(x, y)) return false;
            presses++; return true;
        }
        @Override public void onRelease(double x, double y) { releases++; }
        @Override protected void onDrag(double x, double y, double dx, double dy) { drags++; }
        @Override public boolean keyPressed(int key, int scan, int modifiers) { keys++; return true; }
        @Override public boolean charTyped(char character, int modifiers) { typed += character; return true; }
        @Override protected void renderWidget(GuiGraphics graphics, int x, int y, float partial) {}
        @Override protected void updateWidgetNarration(NarrationElementOutput output) {}
    }

    @Test
    void clippingRejectsInvisibleCellRegionsWhileCapturedDragsAndReleaseCanLeaveTheViewport() {
        OreGrid grid = new OreGrid(1, 1).column(0, OreGrid.Track.fixed(40));
        Control control = grid.add(new Control(), 0, 0, OreGrid.Cell.DEFAULT.width(80, 80));
        OreWidgetHost host = new OreWidgetHost(grid);
        host.setViewport(new ScreenRectangle(10, 20, 100, 40));
        assertTrue(host.getChildAt(49, 25).isPresent());
        assertTrue(host.getChildAt(51, 25).isEmpty());
        assertFalse(host.mouseClicked(500, 500, 0));
        assertTrue(host.mouseClicked(20, 25, 0));
        assertSame(control, host.getFocused());
        assertTrue(host.mouseDragged(500, 500, 0, 480, 475));
        assertTrue(host.mouseReleased(500, 500, 0));
        assertEquals(1, control.presses); assertEquals(1, control.drags); assertEquals(1, control.releases);
        assertFalse(host.isDragging());
    }

    @Test
    void tabTraversesVisualOrderSkipsUnavailableControlsAndWrapsInBothDirections() {
        OreGrid grid = new OreGrid(3, 1);
        Control last = grid.add(new Control(), 2, 0);
        Control first = grid.add(new Control(), 0, 0);
        Control disabled = grid.add(new Control(), 1, 0);
        disabled.active = false;
        OreWidgetHost host = new OreWidgetHost(grid);
        host.setViewport(new ScreenRectangle(0, 0, 100, 100));
        assertTrue(host.keyPressed(GLFW.GLFW_KEY_TAB, 0, 0));
        assertSame(first, host.getFocused());
        host.keyPressed(GLFW.GLFW_KEY_TAB, 0, 0);
        assertSame(last, host.getFocused());
        host.keyPressed(GLFW.GLFW_KEY_TAB, 0, 0);
        assertSame(first, host.getFocused());
        host.keyPressed(GLFW.GLFW_KEY_TAB, 0, GLFW.GLFW_MOD_SHIFT);
        assertSame(last, host.getFocused());
        host.cancelInteraction();
        host.keyPressed(GLFW.GLFW_KEY_TAB, 0, GLFW.GLFW_MOD_SHIFT);
        assertSame(last, host.getFocused());
    }

    @Test
    void keyboardAndCharactersOnlyReachAnAvailableFocusedControl() {
        OreGrid grid = new OreGrid(1, 1);
        Control control = grid.add(new Control(), 0, 0);
        OreWidgetHost host = new OreWidgetHost(grid);
        host.setViewport(new ScreenRectangle(0, 0, 100, 40));
        assertFalse(host.charTyped('a', 0));
        host.setFocused(control);
        assertTrue(host.charTyped('a', 0));
        assertTrue(host.keyPressed(GLFW.GLFW_KEY_LEFT, 0, 0));
        assertEquals("a", control.typed);
        control.visible = false;
        assertFalse(host.charTyped('b', 0));
        assertNull(host.getFocused());
        assertEquals(1, control.keys);
    }

    @Test
    void scrollingIsBoundedAndFocusRevealsOffscreenRows() {
        OreGrid grid = new OreGrid(10, 1);
        Control last = null;
        for (int row = 0; row < 10; row++) last = grid.add(new Control(), row, 0);
        OreWidgetHost host = new OreWidgetHost(grid);
        host.setViewport(new ScreenRectangle(10, 20, 100, 60));
        assertFalse(host.mouseScrolled(0, 0, 0, -1));
        assertTrue(host.mouseScrolled(20, 30, 0, -100));
        assertEquals(140, host.scrollOffset());
        host.mouseScrolled(20, 30, 0, 100);
        assertEquals(0, host.scrollOffset());
        host.setFocused(last);
        host.revealFocused();
        assertEquals(140, host.scrollOffset());
        assertTrue(last.getBottom() <= host.viewport().bottom());
    }

    @Test
    void resizingCancelsDragButPreservesKeyboardFocusAndDoesNotActivateControls() {
        OreGrid grid = new OreGrid(1, 1);
        Control control = grid.add(new Control(), 0, 0);
        OreWidgetHost host = new OreWidgetHost(grid);
        host.setViewport(new ScreenRectangle(0, 0, 100, 40));
        host.mouseClicked(10, 10, 0);
        host.setViewport(new ScreenRectangle(10, 20, 120, 50));
        assertFalse(host.isDragging());
        assertSame(control, host.getFocused());
        assertEquals(1, control.presses);
        host.cancelInteraction();
        assertNull(host.getFocused());
    }
}
