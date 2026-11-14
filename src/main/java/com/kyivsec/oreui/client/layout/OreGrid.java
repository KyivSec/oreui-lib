package com.kyivsec.oreui.client.layout;

import com.kyivsec.oreui.client.host.OreWidgetSupport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.navigation.ScreenRectangle;

/** Bounded, integer-pixel grid with native widget ownership, content measurement and merged cells. */
public final class OreGrid implements Layout, Renderable {
    public enum Align { START, CENTER, END, STRETCH }
    public enum Overflow { CLIP, VISIBLE }
    public enum TrackKind { FIXED, CONTENT, FLEX }

    public record Insets(int left, int top, int right, int bottom) {
        public static final Insets NONE = new Insets(0, 0, 0, 0);
        public Insets {
            if (left < 0 || top < 0 || right < 0 || bottom < 0) throw new IllegalArgumentException("Insets cannot be negative");
        }
        public static Insets all(int pixels) { return new Insets(pixels, pixels, pixels, pixels); }
        public int horizontal() { return Math.addExact(left, right); }
        public int vertical() { return Math.addExact(top, bottom); }
    }

    public record Track(TrackKind kind, int min, int max, double weight) {
        public Track {
            Objects.requireNonNull(kind);
            limits(min, max);
            if (!Double.isFinite(weight) || weight <= 0) throw new IllegalArgumentException("Track weight must be positive and finite");
            if (kind == TrackKind.FIXED && min != max) throw new IllegalArgumentException("Fixed tracks have one size");
        }
        public static Track fixed(int size) { return new Track(TrackKind.FIXED, size, size, 1); }
        public static Track content() { return content(0, Integer.MAX_VALUE); }
        public static Track content(int min, int max) { return new Track(TrackKind.CONTENT, min, max, 1); }
        public static Track flex(double weight) { return flex(weight, 0, Integer.MAX_VALUE); }
        public static Track flex(double weight, int min, int max) { return new Track(TrackKind.FLEX, min, max, weight); }
    }

    public record Cell(int minWidth, int maxWidth, int minHeight, int maxHeight, Insets padding, Insets margin,
                       Align horizontal, Align vertical, Overflow overflow) {
        public static final Cell DEFAULT = new Cell(0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE,
                Insets.NONE, Insets.NONE, Align.STRETCH, Align.START, Overflow.CLIP);
        public Cell {
            limits(minWidth, maxWidth);
            limits(minHeight, maxHeight);
            Objects.requireNonNull(padding); Objects.requireNonNull(margin);
            Objects.requireNonNull(horizontal); Objects.requireNonNull(vertical); Objects.requireNonNull(overflow);
        }
        public Cell width(int min, int max) { return new Cell(min, max, minHeight, maxHeight, padding, margin, horizontal, vertical, overflow); }
        public Cell height(int min, int max) { return new Cell(minWidth, maxWidth, min, max, padding, margin, horizontal, vertical, overflow); }
        public Cell padding(int pixels) { return padding(Insets.all(pixels)); }
        public Cell padding(Insets value) { return new Cell(minWidth, maxWidth, minHeight, maxHeight, value, margin, horizontal, vertical, overflow); }
        public Cell margin(int pixels) { return new Cell(minWidth, maxWidth, minHeight, maxHeight, padding, Insets.all(pixels), horizontal, vertical, overflow); }
        public Cell align(Align horizontal, Align vertical) { return new Cell(minWidth, maxWidth, minHeight, maxHeight, padding, margin, horizontal, vertical, overflow); }
        public Cell overflow(Overflow value) { return new Cell(minWidth, maxWidth, minHeight, maxHeight, padding, margin, horizontal, vertical, value); }
    }

    private static final class Entry {
        final LayoutElement element;
        final int row, column, rowSpan, columnSpan;
        final Cell cell;
        ScreenRectangle bounds = new ScreenRectangle(0, 0, 0, 0);
        Entry(LayoutElement element, int row, int column, int rowSpan, int columnSpan, Cell cell) {
            this.element = element; this.row = row; this.column = column;
            this.rowSpan = rowSpan; this.columnSpan = columnSpan; this.cell = cell;
        }
    }

    private final Track[] rows, columns;
    private final List<Entry> entries = new ArrayList<>();
    private int x, y, width = -1, height = -1, contentWidth, contentHeight;
    private int columnGap, rowGap;
    private Insets padding = Insets.NONE;

    public OreGrid(int rowCount, int columnCount) {
        if (rowCount < 1 || columnCount < 1) throw new IllegalArgumentException("Grid dimensions must be positive");
        rows = new Track[rowCount]; columns = new Track[columnCount];
        Arrays.fill(rows, Track.content()); Arrays.fill(columns, Track.flex(1));
    }

    public OreGrid row(int index, Track track) { rows[index] = Objects.requireNonNull(track); return this; }
    public OreGrid column(int index, Track track) { columns[index] = Objects.requireNonNull(track); return this; }
    public OreGrid gap(int pixels) { return gaps(pixels, pixels); }
    public OreGrid gaps(int horizontal, int vertical) {
        if (horizontal < 0 || vertical < 0) throw new IllegalArgumentException("Gaps cannot be negative");
        columnGap = horizontal; rowGap = vertical; return this;
    }
    public OreGrid padding(int pixels) { return padding(Insets.all(pixels)); }
    public OreGrid padding(Insets value) { padding = Objects.requireNonNull(value); return this; }

    public <T extends LayoutElement> T add(T element, int row, int column) {
        return add(element, row, column, 1, 1, Cell.DEFAULT);
    }
    public <T extends LayoutElement> T add(T element, int row, int column, Cell cell) {
        return add(element, row, column, 1, 1, cell);
    }
    public <T extends LayoutElement> T add(T element, int row, int column, int rowSpan, int columnSpan, Cell cell) {
        Objects.requireNonNull(element); Objects.requireNonNull(cell);
        if (containsLayout(element, this)) throw new IllegalArgumentException("Layouts cannot contain a cycle");
        if (row < 0 || column < 0 || rowSpan < 1 || columnSpan < 1
                || rowSpan > rows.length - row || columnSpan > columns.length - column) {
            throw new IllegalArgumentException("Cell/span lies outside the grid");
        }
        Map<AbstractWidget, Boolean> widgets = new IdentityHashMap<>();
        element.visitWidgets(widget -> widgets.put(widget, true));
        for (Entry entry : entries) {
            if (entry.element == element) throw new IllegalArgumentException("An element can only be placed once");
            if (row < entry.row + entry.rowSpan && row + rowSpan > entry.row
                    && column < entry.column + entry.columnSpan && column + columnSpan > entry.column) {
                throw new IllegalArgumentException("Grid cells overlap");
            }
            entry.element.visitWidgets(widget -> {
                if (widgets.containsKey(widget)) throw new IllegalArgumentException("A widget cannot have two grid owners");
            });
        }
        entries.add(new Entry(element, row, column, rowSpan, columnSpan, cell));
        entries.sort(Comparator.comparingInt((Entry entry) -> entry.row).thenComparingInt(entry -> entry.column));
        return element;
    }

    public void setWidth(int width) { nonnegative(width); this.width = width; arrangeElements(); }
    public void setHeight(int height) { nonnegative(height); this.height = height; arrangeElements(); }
    public void setSize(int width, int height) {
        nonnegative(width); nonnegative(height); this.width = width; this.height = height; arrangeElements();
    }
    /** Returns to intrinsic measurement for both axes. */
    public void fitContent() { width = height = -1; arrangeElements(); }
    public int getContentWidth() { return contentWidth; }
    public int getContentHeight() { return contentHeight; }
    public ScreenRectangle cellBounds(LayoutElement element) { return entry(element).bounds; }

    private Entry entry(LayoutElement element) {
        return entries.stream().filter(entry -> entry.element == element).findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown grid element"));
    }

    private static boolean containsLayout(LayoutElement element, LayoutElement target) {
        if (element == target) return true;
        if (!(element instanceof Layout layout)) return false;
        boolean[] found = {false};
        layout.visitChildren(child -> { if (containsLayout(child, target)) found[0] = true; });
        return found[0];
    }

    @Override
    public void arrangeElements() {
        int[] widths = measure(columns, true);
        // Width must be applied before measuring height: labels, accordions and cards can reflow.
        for (Entry entry : entries) {
            int available = Math.max(0, span(widths, entry.column, entry.columnSpan, columnGap) - horizontalInsets(entry.cell));
            int preferred = entry.cell.horizontal == Align.STRETCH ? available : entry.element.getWidth();
            OreLayoutSizing.width(entry.element, Math.clamp(preferred, entry.cell.minWidth, entry.cell.maxWidth));
        }
        int[] heights = measure(rows, false);
        contentWidth = Math.addExact(total(widths, columnGap), padding.horizontal());
        contentHeight = Math.addExact(total(heights, rowGap), padding.vertical());
        for (Entry entry : entries) {
            int left = x + padding.left + offset(widths, entry.column, columnGap);
            int top = y + padding.top + offset(heights, entry.row, rowGap);
            int cellWidth = span(widths, entry.column, entry.columnSpan, columnGap);
            int cellHeight = span(heights, entry.row, entry.rowSpan, rowGap);
            entry.bounds = new ScreenRectangle(left, top, cellWidth, cellHeight);
            int innerHeight = Math.max(0, cellHeight - verticalInsets(entry.cell));
            int preferred = entry.cell.vertical == Align.STRETCH ? innerHeight : entry.element.getHeight();
            OreLayoutSizing.height(entry.element, Math.clamp(preferred, entry.cell.minHeight, entry.cell.maxHeight));
            int innerWidth = Math.max(0, cellWidth - horizontalInsets(entry.cell));
            entry.element.setPosition(left + entry.cell.margin.left + entry.cell.padding.left
                            + aligned(innerWidth - entry.element.getWidth(), entry.cell.horizontal),
                    top + entry.cell.margin.top + entry.cell.padding.top
                            + aligned(innerHeight - entry.element.getHeight(), entry.cell.vertical));
        }
    }

    private int[] measure(Track[] tracks, boolean horizontal) {
        int[] sizes = Arrays.stream(tracks).mapToInt(Track::min).toArray();
        int gap = horizontal ? columnGap : rowGap;
        // Shorter spans first makes measurement deterministic and honors single-cell demands first.
        var ordered = entries.stream().sorted(Comparator.comparingInt(entry -> horizontal ? entry.columnSpan : entry.rowSpan)).toList();
        for (Entry entry : ordered) {
            int start = horizontal ? entry.column : entry.row, count = horizontal ? entry.columnSpan : entry.rowSpan;
            Cell cell = entry.cell;
            int min = horizontal ? cell.minWidth : cell.minHeight, max = horizontal ? cell.maxWidth : cell.maxHeight;
            int intrinsic = horizontal ? entry.element.getWidth() : entry.element.getHeight();
            int insets = horizontal ? horizontalInsets(cell) : verticalInsets(cell);
            boolean content = false;
            for (int i = start; i < start + count; i++) content |= tracks[i].kind == TrackKind.CONTENT;
            // Unbounded grids also measure flex tracks intrinsically; bounded flex tracks can shrink to their minima.
            int constraint = horizontal ? width : height;
            int demand = (content || constraint < 0 ? Math.clamp(intrinsic, min, max) : min) + insets;
            grow(sizes, tracks, start, count, Math.max(0, demand - span(sizes, start, count, gap)), false);
        }
        int bound = horizontal ? width : height;
        if (bound >= 0) {
            int available = Math.max(0, bound - (horizontal ? padding.horizontal() : padding.vertical()));
            grow(sizes, tracks, 0, tracks.length, Math.max(0, available - total(sizes, gap)), true);
        }
        return sizes;
    }

    private static void grow(int[] sizes, Track[] tracks, int start, int count, int amount, boolean flexOnly) {
        while (amount > 0) {
            double weights = 0;
            for (int i = start; i < start + count; i++) {
                if (eligible(tracks[i], sizes[i], flexOnly)) weights += flexOnly ? tracks[i].weight : 1;
            }
            if (weights == 0) return; // Fixed/max-limited tracks cannot satisfy the demand: overflow remains explicit.
            int budget = amount, spent = 0;
            for (int i = start; i < start + count && spent < budget; i++) {
                if (!eligible(tracks[i], sizes[i], flexOnly)) continue;
                int share = Math.max(1, (int) Math.floor(budget * (flexOnly ? tracks[i].weight : 1) / weights));
                int add = Math.min(budget - spent, Math.min(share, tracks[i].max - sizes[i]));
                sizes[i] += add; spent += add;
            }
            if (spent == 0) return;
            amount -= spent;
        }
    }

    private static boolean eligible(Track track, int size, boolean flexOnly) {
        return size < track.max && track.kind != TrackKind.FIXED && (!flexOnly || track.kind == TrackKind.FLEX);
    }
    private static int aligned(int extra, Align alignment) {
        return alignment == Align.END ? extra : alignment == Align.CENTER ? Math.floorDiv(extra, 2) : 0;
    }
    private static int horizontalInsets(Cell cell) { return Math.addExact(cell.padding.horizontal(), cell.margin.horizontal()); }
    private static int verticalInsets(Cell cell) { return Math.addExact(cell.padding.vertical(), cell.margin.vertical()); }
    private static int total(int[] values, int gap) { return span(values, 0, values.length, gap); }
    private static int span(int[] values, int start, int count, int gap) {
        int size = Math.multiplyExact(Math.max(0, count - 1), gap);
        for (int i = start; i < start + count; i++) size = Math.addExact(size, values[i]);
        return size;
    }
    private static int offset(int[] values, int index, int gap) { return index == 0 ? 0 : Math.addExact(span(values, 0, index, gap), gap); }
    private static void nonnegative(int value) { if (value < 0) throw new IllegalArgumentException("Size cannot be negative"); }
    private static void limits(int min, int max) { if (min < 0 || max < min) throw new IllegalArgumentException("Invalid size limits"); }

    @Override public int getX() { return x; }
    @Override public int getY() { return y; }
    @Override public int getWidth() { return width < 0 ? contentWidth : width; }
    @Override public int getHeight() { return height < 0 ? contentHeight : height; }
    @Override public void setX(int x) { int delta = x - this.x; this.x = x; translate(delta, 0); }
    @Override public void setY(int y) { int delta = y - this.y; this.y = y; translate(0, delta); }
    private void translate(int dx, int dy) {
        for (Entry entry : entries) {
            entry.element.setPosition(entry.element.getX() + dx, entry.element.getY() + dy);
            entry.bounds = new ScreenRectangle(entry.bounds.left() + dx, entry.bounds.top() + dy, entry.bounds.width(), entry.bounds.height());
        }
    }
    @Override public void visitChildren(Consumer<LayoutElement> visitor) { entries.forEach(entry -> visitor.accept(entry.element)); }

    /** Same clipping policy used by rendering; hosts must consult this before routing pointer input. */
    public boolean acceptsInput(AbstractWidget widget, double mouseX, double mouseY) {
        for (Entry entry : entries) {
            boolean[] contains = {false};
            entry.element.visitWidgets(child -> { if (child == widget) contains[0] = true; });
            if (!contains[0]) continue;
            if (entry.cell.overflow == Overflow.CLIP && !contains(entry.bounds, mouseX, mouseY)) return false;
            return acceptsNested(entry.element, widget, mouseX, mouseY);
        }
        return false;
    }
    private static boolean acceptsNested(LayoutElement element, AbstractWidget widget, double x, double y) {
        if (element instanceof OreGrid grid) return grid.acceptsInput(widget, x, y);
        if (element instanceof OreMenuLayout menu) return menu.grid().acceptsInput(widget, x, y);
        if (element instanceof Layout layout) {
            boolean[] allowed = {false};
            layout.visitChildren(child -> { if (acceptsNested(child, widget, x, y)) allowed[0] = true; });
            return allowed[0];
        }
        boolean[] found = {false};
        element.visitWidgets(child -> { if (child == widget) found[0] = true; });
        return found[0];
    }
    public static boolean contains(ScreenRectangle bounds, double x, double y) {
        return x >= bounds.left() && y >= bounds.top() && x < bounds.right() && y < bounds.bottom();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        for (Entry entry : entries) {
            boolean clip = entry.cell.overflow == Overflow.CLIP;
            if (clip) graphics.enableScissor(entry.bounds.left(), entry.bounds.top(), entry.bounds.right(), entry.bounds.bottom());
            boolean hovered = !clip || contains(entry.bounds, mouseX, mouseY);
            try { renderElement(entry.element, graphics, hovered ? mouseX : Integer.MIN_VALUE,
                    hovered ? mouseY : Integer.MIN_VALUE, partialTick); }
            finally { if (clip) graphics.disableScissor(); }
        }
        renderOutlines(graphics);
    }

    public void renderOutlines(GuiGraphics graphics) {
        for (Entry entry : entries) {
            boolean clip = entry.cell.overflow == Overflow.CLIP;
            if (clip) graphics.enableScissor(entry.bounds.left(), entry.bounds.top(), entry.bounds.right(), entry.bounds.bottom());
            try {
                outlineElement(entry.element, graphics);
            } finally { if (clip) graphics.disableScissor(); }
        }
    }
    private static void outlineElement(LayoutElement element, GuiGraphics graphics) {
        if (element instanceof OreGrid grid) grid.renderOutlines(graphics);
        else if (element instanceof OreMenuLayout menu) menu.grid().renderOutlines(graphics);
        else if (element instanceof Layout layout) layout.visitChildren(child -> outlineElement(child, graphics));
        else element.visitWidgets(widget -> OreWidgetSupport.outline(widget, graphics));
    }
    private static void renderElement(LayoutElement element, GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (element instanceof OreGrid grid) grid.render(graphics, mouseX, mouseY, partialTick);
        else if (element instanceof OreMenuLayout menu) menu.render(graphics, mouseX, mouseY, partialTick);
        else {
            if (element instanceof Renderable renderable) renderable.render(graphics, mouseX, mouseY, partialTick);
            if (element instanceof Layout layout) layout.visitChildren(child -> renderElement(child, graphics, mouseX, mouseY, partialTick));
            else if (!(element instanceof AbstractWidget)) element.visitWidgets(widget -> widget.render(graphics, mouseX, mouseY, partialTick));
        }
    }
}
