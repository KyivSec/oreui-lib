package com.kyivsec.oreui.client.layout;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.navigation.ScreenRectangle;

/** Grid structure only: presets never choose application content, inventory behavior or callbacks. */
public final class OreMenuPresets {
    public enum Kind { SETTINGS, SIMPLE, SPLIT, MODAL, ANCHORED }

    public record Options(OreGrid.Insets padding, int gap, int headerHeight, int footerHeight,
                          int sidebarWidth, int minimumBodyWidth, int cellPadding) {
        public static final Options DEFAULT = new Options(OreGrid.Insets.all(12), 8, 26, 34, 112, 64, 2);
        public Options {
            Objects.requireNonNull(padding);
            if (gap < 0 || headerHeight < 0 || footerHeight < 0 || sidebarWidth < 0 || minimumBodyWidth < 0 || cellPadding < 0) {
                throw new IllegalArgumentException("Preset dimensions cannot be negative");
            }
        }
        public Options padding(int pixels) { return new Options(OreGrid.Insets.all(pixels), gap, headerHeight, footerHeight, sidebarWidth, minimumBodyWidth, cellPadding); }
        public Options gap(int pixels) { return new Options(padding, pixels, headerHeight, footerHeight, sidebarWidth, minimumBodyWidth, cellPadding); }
        public Options headerHeight(int pixels) { return new Options(padding, gap, pixels, footerHeight, sidebarWidth, minimumBodyWidth, cellPadding); }
        public Options footerHeight(int pixels) { return new Options(padding, gap, headerHeight, pixels, sidebarWidth, minimumBodyWidth, cellPadding); }
        public Options sidebarWidth(int pixels) { return new Options(padding, gap, headerHeight, footerHeight, pixels, minimumBodyWidth, cellPadding); }
    }
    private OreMenuPresets() {}

    public static OreMenuLayout simple(LayoutElement header, LayoutElement body, LayoutElement footer) {
        return simple(header, body, footer, Options.DEFAULT);
    }
    public static OreMenuLayout simple(LayoutElement header, LayoutElement body, LayoutElement footer, Options options) {
        return compose(Kind.SIMPLE, header, null, body, footer, options);
    }
    public static OreMenuLayout settings(LayoutElement header, LayoutElement sidebar, LayoutElement body, LayoutElement footer) {
        return settings(header, sidebar, body, footer, Options.DEFAULT);
    }
    public static OreMenuLayout settings(LayoutElement header, LayoutElement sidebar, LayoutElement body, LayoutElement footer, Options options) {
        return compose(Kind.SETTINGS, header, sidebar, body, footer, options);
    }
    public static OreMenuLayout split(LayoutElement header, LayoutElement preview, LayoutElement body, LayoutElement footer) {
        return split(header, preview, body, footer, Options.DEFAULT);
    }
    public static OreMenuLayout split(LayoutElement header, LayoutElement preview, LayoutElement body, LayoutElement footer, Options options) {
        return compose(Kind.SPLIT, header, preview, body, footer, options);
    }
    public static OreMenuLayout modal(LayoutElement header, LayoutElement body, LayoutElement footer) {
        return modal(header, body, footer, Options.DEFAULT.headerHeight(20).padding(8).gap(4));
    }
    public static OreMenuLayout modal(LayoutElement header, LayoutElement body, LayoutElement footer, Options options) {
        return compose(Kind.MODAL, header, null, body, footer, options);
    }
    public static OreMenuLayout anchored(LayoutElement body) {
        return anchored(body, Options.DEFAULT.padding(2).gap(0));
    }
    public static OreMenuLayout anchored(LayoutElement body, Options options) {
        return compose(Kind.ANCHORED, null, null, body, null, options);
    }

    private static OreMenuLayout compose(Kind kind, LayoutElement header, LayoutElement side, LayoutElement body,
                                          LayoutElement footer, Options options) {
        Objects.requireNonNull(body); Objects.requireNonNull(options);
        boolean twoColumns = kind == Kind.SETTINGS || kind == Kind.SPLIT;
        boolean compact = kind == Kind.ANCHORED;
        OreGrid grid = new OreGrid(compact ? 1 : 3, twoColumns ? 2 : 1).gap(options.gap).padding(options.padding);
        var cell = OreGrid.Cell.DEFAULT.padding(options.cellPadding).align(OreGrid.Align.STRETCH, OreGrid.Align.STRETCH);
        Map<OreMenuLayout.Region, LayoutElement> regions = new EnumMap<>(OreMenuLayout.Region.class);
        if (compact) {
            grid.row(0, OreGrid.Track.flex(1)); grid.add(body, 0, 0, cell); regions.put(OreMenuLayout.Region.BODY, body);
        } else {
            Objects.requireNonNull(header); Objects.requireNonNull(footer);
            grid.row(0, OreGrid.Track.fixed(options.headerHeight)).row(1, OreGrid.Track.flex(1))
                    .row(2, OreGrid.Track.fixed(options.footerHeight));
            grid.column(twoColumns ? 1 : 0, OreGrid.Track.flex(1, options.minimumBodyWidth, Integer.MAX_VALUE));
            grid.add(header, 0, 0, 1, twoColumns ? 2 : 1, cell);
            grid.add(body, 1, twoColumns ? 1 : 0, cell);
            grid.add(footer, 2, 0, 1, twoColumns ? 2 : 1, cell);
            regions.put(OreMenuLayout.Region.HEADER, header); regions.put(OreMenuLayout.Region.BODY, body);
            regions.put(OreMenuLayout.Region.FOOTER, footer);
            if (twoColumns) {
                Objects.requireNonNull(side);
                if (kind == Kind.SETTINGS) grid.column(0, OreGrid.Track.fixed(options.sidebarWidth));
                else {
                    grid.column(0, OreGrid.Track.flex(1, options.minimumBodyWidth, Integer.MAX_VALUE));
                    grid.column(1, OreGrid.Track.flex(2, options.minimumBodyWidth, Integer.MAX_VALUE));
                }
                grid.add(side, 1, 0, cell);
                regions.put(kind == Kind.SETTINGS ? OreMenuLayout.Region.SIDEBAR : OreMenuLayout.Region.PREVIEW, side);
            }
        }
        return new OreMenuLayout(grid, regions, options.cellPadding);
    }

    /** Prefer below the trigger; flip above when that side has more room, then clamp to the GUI safe edge. */
    public static ScreenRectangle anchoredBounds(ScreenRectangle anchor, int screenWidth, int screenHeight, int width, int height) {
        Objects.requireNonNull(anchor);
        width = Math.clamp(width, 1, Math.max(1, screenWidth - 16));
        int below = Math.max(1, screenHeight - anchor.bottom() - 8), above = Math.max(1, anchor.top() - 8);
        boolean flip = below < Math.min(height, 74) && above > below;
        height = Math.clamp(height, 1, flip ? above : below);
        int left = Math.clamp(anchor.left(), 8, Math.max(8, screenWidth - width - 8));
        int top = Math.clamp(flip ? anchor.top() - height : anchor.bottom() + 2, 8, Math.max(8, screenHeight - height - 8));
        return new ScreenRectangle(left, top, width, height);
    }
}
