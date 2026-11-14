package com.kyivsec.oreui.client.layout;

import com.kyivsec.oreui.client.widget.OreButton;
import com.kyivsec.oreui.client.widget.OreColumn;
import com.kyivsec.oreui.client.style.OrePalette;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OreGridTest {
    private static final class Wrapped extends AbstractWidget {
        Wrapped() { super(0, 0, 200, 10, Component.empty()); }
        @Override public void setWidth(int width) { super.setWidth(width); setHeight((int) Math.ceil(200.0 / Math.max(1, width)) * 10); }
        @Override protected void renderWidget(GuiGraphics graphics, int x, int y, float partial) {}
        @Override protected void updateWidgetNarration(NarrationElementOutput output) {}
    }
    private static OreButton button(int width, int height) {
        return new OreButton(0, 0, width, height, Component.literal("Button"), OrePalette.GENERAL, ignored -> {});
    }

    @Test
    void fixedContentAndWeightedFlexTracksUseIntegerPixelBounds() {
        OreGrid grid = new OreGrid(1, 3).gap(4).padding(2)
                .column(0, OreGrid.Track.fixed(30)).column(1, OreGrid.Track.flex(1)).column(2, OreGrid.Track.flex(2));
        var a = grid.add(button(100, 20), 0, 0);
        var b = grid.add(button(100, 20), 0, 1);
        var c = grid.add(button(100, 20), 0, 2);
        grid.setWidth(342);
        grid.setPosition(10, 20);
        assertEquals(new ScreenRectangle(12, 22, 30, 20), grid.cellBounds(a));
        assertEquals(100, b.getWidth());
        assertEquals(200, c.getWidth());
        assertEquals(342, grid.getContentWidth());
        assertEquals(24, grid.getHeight());
        assertEquals(grid.cellBounds(c).right() + 2, grid.getX() + grid.getWidth());
    }

    @Test
    void flexMaximaRedistributeSurplusWithoutUnexplainedGaps() {
        OreGrid grid = new OreGrid(1, 2).column(0, OreGrid.Track.flex(1, 20, 40));
        var a = grid.add(button(1, 20), 0, 0);
        var b = grid.add(button(1, 20), 0, 1);
        grid.setWidth(101);
        assertEquals(40, a.getWidth());
        assertEquals(61, b.getWidth());
        assertEquals(101, grid.getContentWidth());
    }

    @Test
    void mergedCellsCoverGapsAndAreVisitedOnceInVisualOrder() {
        OreGrid grid = new OreGrid(2, 3).gap(5);
        var last = grid.add(button(10, 20), 1, 2);
        var merged = grid.add(button(10, 30), 0, 0, 2, 2, OreGrid.Cell.DEFAULT);
        var top = grid.add(button(10, 15), 0, 2);
        grid.setWidth(310);
        List<AbstractWidget> visited = new ArrayList<>();
        grid.visitWidgets(visited::add);
        assertEquals(List.of(merged, top, last), visited);
        assertEquals(205, grid.cellBounds(merged).width());
        assertEquals(grid.getContentHeight(), grid.cellBounds(merged).height());
        assertTrue(grid.cellBounds(last).top() >= grid.cellBounds(top).bottom() + 5);
    }

    @Test
    void cellsRespectLimitsInsetsAndAutomaticAlignment() {
        OreGrid grid = new OreGrid(1, 1).row(0, OreGrid.Track.flex(1));
        var child = grid.add(button(100, 28), 0, 0, OreGrid.Cell.DEFAULT
                .width(20, 60).height(10, 20).padding(4).margin(2).align(OreGrid.Align.CENTER, OreGrid.Align.END));
        grid.setSize(120, 80);
        assertEquals(60, child.getWidth());
        assertEquals(20, child.getHeight());
        assertEquals(30, child.getX());
        assertEquals(54, child.getY());
    }

    @Test
    void widthIsMeasuredBeforeWrappedHeightAndResizesReflowRows() {
        OreGrid grid = new OreGrid(2, 1).gap(4);
        Wrapped text = grid.add(new Wrapped(), 0, 0);
        var below = grid.add(button(100, 20), 1, 0);
        grid.setWidth(50);
        assertEquals(40, text.getHeight());
        assertEquals(44, below.getY());
        grid.setWidth(100);
        assertEquals(20, text.getHeight());
        assertEquals(24, below.getY());
        assertEquals(44, grid.getContentHeight());
    }

    @Test
    void minimumSizesOverflowRatherThanSilentlyShrinkingAndHitTestsMatchClipping() {
        OreGrid grid = new OreGrid(1, 1).column(0, OreGrid.Track.fixed(40));
        var child = grid.add(button(10, 20), 0, 0, OreGrid.Cell.DEFAULT.width(80, 100));
        grid.setWidth(30);
        assertEquals(80, child.getWidth());
        assertEquals(40, grid.getContentWidth());
        assertEquals(30, grid.getWidth());
        assertTrue(grid.acceptsInput(child, 39, 10));
        assertFalse(grid.acceptsInput(child, 40, 10));
        OreGrid visible = new OreGrid(1, 1).column(0, OreGrid.Track.fixed(40));
        var overflowing = visible.add(button(80, 20), 0, 0, OreGrid.Cell.DEFAULT.width(80, 80).overflow(OreGrid.Overflow.VISIBLE));
        visible.setWidth(40);
        assertTrue(visible.acceptsInput(overflowing, 70, 10));
    }

    @Test
    void nestedGridClipsComposeAndColumnSizingReachesTheGrid() {
        OreGrid nested = new OreGrid(1, 1).column(0, OreGrid.Track.fixed(40));
        var child = nested.add(button(80, 20), 0, 0, OreGrid.Cell.DEFAULT.width(80, 80));
        OreGrid root = new OreGrid(1, 1);
        root.add(nested, 0, 0);
        OreColumn column = new OreColumn();
        column.addChild(root);
        column.setWidth(100);
        column.setPosition(25, 30);
        assertEquals(100, root.getWidth());
        assertTrue(root.acceptsInput(child, 64, 35));
        assertFalse(root.acceptsInput(child, 66, 35));
        assertEquals(25, child.getX());
    }

    @Test
    void rejectsOverlappingInvalidAndCyclicPlacementsWithoutRegisteringExtraWidgets() {
        OreGrid grid = new OreGrid(2, 2);
        var merged = grid.add(button(20, 20), 0, 0, 2, 1, OreGrid.Cell.DEFAULT);
        assertThrows(IllegalArgumentException.class, () -> grid.add(button(1, 1), 1, 0));
        assertThrows(IllegalArgumentException.class, () -> grid.add(merged, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> grid.add(button(1, 1), 0, 1, 1, 2, OreGrid.Cell.DEFAULT));
        assertThrows(IllegalArgumentException.class, () -> grid.add(button(1, 1), -1, 0));
        OreGrid nested = new OreGrid(1, 1);
        grid.add(nested, 0, 1);
        assertThrows(IllegalArgumentException.class, () -> nested.add(grid, 0, 0));
        List<AbstractWidget> visited = new ArrayList<>();
        grid.visitWidgets(visited::add);
        assertEquals(List.of(merged), visited);
        assertThrows(IllegalArgumentException.class, () -> OreGrid.Cell.DEFAULT.width(20, 10));
        assertThrows(IllegalArgumentException.class, () -> OreGrid.Track.flex(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> grid.gap(-1));
    }

    @Test
    void layoutChangesDoNotFireControlCallbacksAndIntrinsicTracksRemainContentSized() {
        AtomicInteger changes = new AtomicInteger();
        var button = new OreButton(0, 0, 48, 20, Component.literal("Test"), OrePalette.GENERAL, ignored -> changes.incrementAndGet());
        OreGrid grid = new OreGrid(1, 2).column(0, OreGrid.Track.content()).column(1, OreGrid.Track.fixed(10)).padding(3);
        grid.add(button, 0, 0);
        grid.add(new SpacerElement(10, 5), 0, 1);
        grid.fitContent();
        assertEquals(64, grid.getWidth());
        grid.setPosition(100, 200);
        grid.setWidth(300);
        assertEquals(48, button.getWidth());
        assertEquals(0, changes.get());
    }
}
