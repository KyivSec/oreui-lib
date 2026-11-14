package com.kyivsec.oreui.client.layout;

import com.kyivsec.oreui.client.style.OrePalette;
import com.kyivsec.oreui.client.widget.OreButton;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static com.kyivsec.oreui.client.layout.OreMenuLayout.Region.*;
import static org.junit.jupiter.api.Assertions.*;

class OreMenuPresetsTest {
    private static SpacerElement slot() { return new SpacerElement(0, 0); }
    private static OreButton button() { return new OreButton(0, 0, 100, 28, Component.literal("Test"), OrePalette.GENERAL, ignored -> {}); }

    @Test
    void settingsRegionsMergeHeaderAndFooterAndKeepSidebarBesideBody() {
        var menu = OreMenuPresets.settings(slot(), slot(), slot(), slot());
        menu.setSize(480, 320); menu.setPosition(20, 30);
        assertEquals(456, menu.bounds(HEADER).width());
        assertEquals(menu.bounds(HEADER).left(), menu.bounds(FOOTER).left());
        assertEquals(menu.bounds(HEADER).width(), menu.bounds(FOOTER).width());
        assertEquals(112, menu.bounds(SIDEBAR).width());
        assertEquals(menu.bounds(SIDEBAR).right() + 8, menu.bounds(BODY).left());
        assertEquals(menu.bounds(SIDEBAR).top(), menu.bounds(BODY).top());
        assertEquals(menu.bounds(SIDEBAR).height(), menu.bounds(BODY).height());
        assertFalse(menu.has(PREVIEW));
    }

    @Test
    void simpleAndModalUseThreeRowsWithBodyTakingTheRemainingHeight() {
        for (var menu : List.of(OreMenuPresets.simple(slot(), slot(), slot()), OreMenuPresets.modal(slot(), slot(), slot()))) {
            menu.setSize(240, 200);
            assertTrue(menu.bounds(BODY).top() >= menu.bounds(HEADER).bottom());
            assertTrue(menu.bounds(FOOTER).top() >= menu.bounds(BODY).bottom());
            assertTrue(menu.contentBounds(BODY).height() > 0);
            assertFalse(menu.has(SIDEBAR));
            assertThrows(IllegalArgumentException.class, () -> menu.bounds(SIDEBAR));
        }
        var modal = OreMenuPresets.modal(slot(), slot(), slot());
        modal.setSize(240, 200);
        assertEquals(20, modal.bounds(HEADER).height());
    }

    @Test
    void splitSharesFlexibleSpaceWhileKeepingPreviewAndControlsSeparate() {
        var menu = OreMenuPresets.split(slot(), slot(), slot(), slot());
        menu.setSize(640, 360);
        assertTrue(menu.bounds(BODY).width() > menu.bounds(PREVIEW).width());
        assertEquals(menu.bounds(PREVIEW).right() + 8, menu.bounds(BODY).left());
        assertEquals(menu.bounds(BODY).height(), menu.bounds(PREVIEW).height());
        assertEquals(menu.bounds(HEADER).width(), menu.bounds(PREVIEW).width() + 8 + menu.bounds(BODY).width());
    }

    @Test
    void longBodyContentCannotMoveTheFixedFooter() {
        var shortMenu = OreMenuPresets.simple(slot(), new SpacerElement(100, 10), slot());
        var longMenu = OreMenuPresets.simple(slot(), new SpacerElement(100, 2000), slot());
        shortMenu.setSize(320, 240); longMenu.setSize(320, 240);
        assertEquals(shortMenu.bounds(FOOTER), longMenu.bounds(FOOTER));
        assertEquals(shortMenu.bounds(BODY), longMenu.bounds(BODY));
        assertEquals(34, longMenu.bounds(FOOTER).height());
    }

    @Test
    void namedRegionsFollowVisualWidgetOrderAndMergedChromeIsVisitedOnce() {
        var header = button(); var sidebar = button(); var body = button(); var footer = button();
        var menu = OreMenuPresets.settings(header, sidebar, body, footer);
        menu.setSize(480, 320);
        List<AbstractWidget> widgets = new ArrayList<>();
        menu.visitWidgets(widgets::add);
        assertEquals(List.of(header, sidebar, body, footer), widgets);
        assertEquals(1, widgets.stream().filter(widget -> widget == header).count());
    }

    @Test
    void anchoredBoundsFlipClampAndRemainUsableAtSmallGuiSizes() {
        var bounds = OreMenuPresets.anchoredBounds(new ScreenRectangle(290, 200, 20, 20), 320, 240, 180, 120);
        assertEquals(132, bounds.left());
        assertTrue(bounds.bottom() <= 200);
        assertTrue(bounds.top() >= 8 && bounds.right() <= 312);
        var below = OreMenuPresets.anchoredBounds(new ScreenRectangle(20, 20, 80, 20), 320, 240, 100, 100);
        assertEquals(42, below.top());
        var menu = OreMenuPresets.anchored(slot());
        menu.setSize(bounds.width(), bounds.height()); menu.setPosition(bounds.left(), bounds.top());
        assertTrue(menu.has(BODY)); assertFalse(menu.has(HEADER)); assertFalse(menu.has(FOOTER));
        assertTrue(menu.contentBounds(BODY).width() > 0);
    }

    @Test
    void popupCompatibilityGeometryRetainsTheOriginalHeaderBodyAndFooterBounds() {
        for (int header : new int[] {20, 26}) {
            int panelWidth = 178, panelHeight = 216, actionHeight = 40, x = 10, y = 12;
            var menu = OreMenuPresets.modal(slot(), slot(), slot(),
                    new OreMenuPresets.Options(OreGrid.Insets.NONE, 0, header - 1, actionHeight, 0, 0, 0));
            menu.setSize(panelWidth - 2, panelHeight - 2); menu.setPosition(x + 1, y + 1);
            assertEquals(new ScreenRectangle(x + 1, y + header, panelWidth - 2, panelHeight - header - 1 - actionHeight), menu.bounds(BODY));
            assertEquals(y + panelHeight - 1 - actionHeight, menu.bounds(FOOTER).top());
        }
    }

    @Test
    void resizingRecomputesRegionsWithoutChangingExplicitTrackLimits() {
        var options = OreMenuPresets.Options.DEFAULT.sidebarWidth(88).footerHeight(50);
        var menu = OreMenuPresets.settings(slot(), slot(), slot(), slot(), options);
        for (int width : new int[] {320, 480, 1104, 320}) {
            menu.setSize(width, 240);
            assertEquals(88, menu.bounds(SIDEBAR).width());
            assertEquals(50, menu.bounds(FOOTER).height());
            assertTrue(menu.bounds(BODY).right() <= width);
        }
        assertThrows(IllegalArgumentException.class, () -> options.sidebarWidth(-1));
        assertThrows(IllegalArgumentException.class, () -> OreMenuPresets.simple(button(), button(), button(), options.gap(-1)));
    }

    @Test
    void nestingAPresetInsideAGridRetainsThePresetCellClips() {
        var header = button();
        var menu = OreMenuPresets.simple(header, slot(), slot());
        OreGrid outer = new OreGrid(1, 1).row(0, OreGrid.Track.flex(1));
        outer.add(menu, 0, 0, OreGrid.Cell.DEFAULT.align(OreGrid.Align.STRETCH, OreGrid.Align.STRETCH));
        outer.setSize(320, 240);
        var bounds = menu.bounds(HEADER);
        header.setHeight(80); // Content can overflow after an asynchronous update, before reflow.
        assertTrue(outer.acceptsInput(header, header.getX() + 1, bounds.top() + 5));
        assertFalse(outer.acceptsInput(header, header.getX() + 1, bounds.bottom() + 1));
    }
}
