package com.kyivsec.oreui.client.hud;

import com.kyivsec.oreui.client.layout.OreGrid;
import com.kyivsec.oreui.client.widget.OreStatBar;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OreHudStatLayoutTest {
    @Test
    void bothBarsStayInTheFirstOfTwentyFiveResponsiveCells() {
        var grid = OreHudStatTest.content(() -> 14, () -> 20, () -> 8);
        List<AbstractWidget> widgets = new ArrayList<>();
        grid.visitWidgets(widgets::add);
        assertEquals(2, widgets.size());
        OreStatBar health = (OreStatBar) widgets.getFirst(), hunger = (OreStatBar) widgets.getLast();
        assertEquals(OreHudStatTest.HEALTH_COLOR, health.color());
        assertEquals(OreHudStatTest.HUNGER_COLOR, hunger.color());
        assertEquals(0.7, health.values().fraction());
        assertEquals(0.4, hunger.values().fraction());
        for (int[] size : new int[][] {{1920, 1080}, {480, 270}, {320, 240}, {320, 180}, {853, 479}}) {
            grid.setSize(size[0], size[1]);
            OreGrid[] first = {null};
            grid.visitChildren(child -> first[0] = (OreGrid) child);
            ScreenRectangle cell = grid.cellBounds(first[0]);
            assertEquals(0, cell.left()); assertEquals(0, cell.top());
            assertTrue(Math.abs(cell.width() - size[0] / 5.0) <= 1);
            assertTrue(Math.abs(cell.height() - size[1] / 5.0) <= 1);
            for (var bar : widgets) {
                assertEquals(4, bar.getX());
                assertTrue(bar.getY() >= 4 && bar.getBottom() <= cell.bottom() - 4);
                assertEquals(cell.width() - 8, bar.getWidth());
                assertTrue(bar.getHeight() > 0 && bar.getHeight() <= 20);
            }
            assertTrue(hunger.getY() >= health.getBottom() + 4);
            assertEquals(size[1], grid.getContentHeight());
        }
    }

    @Test
    void fullScreenLayerResizesAndRemainsDisplayOnlyWithNormalVisibilityPolicies() {
        var layer = new OreHudLayer(ResourceLocation.fromNamespaceAndPath("example", "stats"),
                () -> OreHudStatTest.content(() -> 20, () -> 20, () -> 20)).fillScreen().panel(false);
        layer.layout(480, 270);
        assertEquals(480, layer.content().getWidth());
        assertEquals(270, layer.content().getHeight());
        assertEquals(0, layer.content().getX()); assertEquals(0, layer.content().getY());
        layer.layout(320, 180);
        assertEquals(320, layer.content().getWidth()); assertEquals(180, layer.content().getHeight());
        assertFalse(layer.canInteract(new OreHudContext(true, false, false, false, true, false)));
        assertFalse(layer.isDisplayed(new OreHudContext(true, true, false, false, true, false)));
        assertFalse(layer.isDisplayed(new OreHudContext(true, false, true, false, true, false)));
        layer.size(100, 80).anchor(OreHudAnchor.TOP_LEFT);
        layer.layout(320, 180);
        assertEquals(100, layer.content().getWidth());
        assertEquals(8, layer.content().getX());
    }
}
