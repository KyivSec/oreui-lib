package com.kyivsec.oreui.client.layout;

import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.navigation.ScreenRectangle;

/** A preset's named regions remain ordinary grid cells; content and track constraints are caller-owned. */
public final class OreMenuLayout implements Layout, Renderable {
    public enum Region { HEADER, SIDEBAR, PREVIEW, BODY, FOOTER }

    private final OreGrid grid;
    private final Map<Region, LayoutElement> regions;
    private final int cellPadding;

    OreMenuLayout(OreGrid grid, Map<Region, LayoutElement> regions, int cellPadding) {
        this.grid = grid; this.regions = Map.copyOf(regions); this.cellPadding = cellPadding;
    }
    public OreGrid grid() { return grid; }
    public boolean has(Region region) { return regions.containsKey(region); }
    public LayoutElement element(Region region) {
        var element = regions.get(region);
        if (element == null) throw new IllegalArgumentException("This preset has no " + region + " region");
        return element;
    }
    public ScreenRectangle bounds(Region region) { return grid.cellBounds(element(region)); }
    public ScreenRectangle contentBounds(Region region) {
        var bounds = bounds(region);
        int px = Math.min(cellPadding, bounds.width() / 2), py = Math.min(cellPadding, bounds.height() / 2);
        return new ScreenRectangle(bounds.left() + px, bounds.top() + py,
                Math.max(0, bounds.width() - 2 * cellPadding), Math.max(0, bounds.height() - 2 * cellPadding));
    }
    public void setSize(int width, int height) { grid.setSize(width, height); }
    public void setWidth(int width) { grid.setWidth(width); }
    public void setHeight(int height) { grid.setHeight(height); }
    @Override public void arrangeElements() { grid.arrangeElements(); }
    @Override public int getX() { return grid.getX(); }
    @Override public int getY() { return grid.getY(); }
    @Override public int getWidth() { return grid.getWidth(); }
    @Override public int getHeight() { return grid.getHeight(); }
    @Override public void setX(int x) { grid.setX(x); }
    @Override public void setY(int y) { grid.setY(y); }
    @Override public void visitChildren(Consumer<LayoutElement> visitor) { grid.visitChildren(visitor); }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        grid.render(graphics, mouseX, mouseY, partialTick);
    }
}
