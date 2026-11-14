package com.kyivsec.oreui.client.host;

import com.kyivsec.oreui.client.layout.OreGrid;
import com.kyivsec.oreui.client.widget.OreModelPreview;
import com.kyivsec.oreui.client.widget.OreScrollbar;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** A screen-independent native widget container with clipping, scrolling, drag capture and keyboard focus. */
public final class OreWidgetHost extends AbstractContainerEventHandler {
    private final OreGrid grid;
    private final List<AbstractWidget> widgets = new ArrayList<>();
    private ScreenRectangle viewport = new ScreenRectangle(0, 0, 1, 1);
    private int scrollOffset;
    private int contentWidth;
    private final OreScrollbar scrollbar = new OreScrollbar(Component.translatable("oreui.scrollbar"),
            OreScrollbar.Orientation.VERTICAL, value -> { scrollOffset = value; positionContent(); });

    public OreWidgetHost(OreGrid grid) {
        this.grid = grid;
        refreshWidgets();
    }

    public OreGrid grid() { return grid; }
    public List<AbstractWidget> widgets() { return List.copyOf(widgets); }
    public ScreenRectangle viewport() { return viewport; }
    public int scrollOffset() { return scrollOffset; }
    @Override public ScreenRectangle getRectangle() { return viewport; }
    @Override public List<? extends GuiEventListener> children() {
        List<GuiEventListener> children = new ArrayList<>(widgets);
        if (scrollbar.visible) children.add(scrollbar);
        return children;
    }

    public void refreshWidgets() {
        widgets.clear();
        grid.visitWidgets(widgets::add);
        if (getFocused() != null && !widgets.contains(getFocused()) && getFocused() != scrollbar) cancelInteraction();
    }

    public void setViewport(ScreenRectangle viewport) {
        if (viewport.width() < 1 || viewport.height() < 1) throw new IllegalArgumentException("Viewport must be positive");
        if (!viewport.equals(this.viewport)) cancelPointerCapture();
        this.viewport = viewport;
        refreshWidgets();
        grid.setSize(viewport.width(), viewport.height());
        scrollbar.visible = grid.getContentHeight() > viewport.height();
        contentWidth = Math.max(1, viewport.width() - (scrollbar.visible ? 10 : 0));
        if (scrollbar.visible) grid.setSize(contentWidth, viewport.height());
        scrollOffset = Math.clamp(scrollOffset, 0, maximumScroll());
        scrollbar.setPosition(viewport.right() - 6, viewport.top());
        scrollbar.setHeight(viewport.height());
        scrollbar.setMetrics(grid.getContentHeight(), viewport.height(), scrollOffset);
        positionContent();
    }

    private int maximumScroll() { return Math.max(0, grid.getContentHeight() - viewport.height()); }
    private void positionContent() { grid.setPosition(viewport.left(), viewport.top() - scrollOffset); }

    private boolean accepts(AbstractWidget widget, double x, double y) {
        return widget.visible && widget.active && OreGrid.contains(viewport, x, y)
                && (widget == scrollbar || x < viewport.left() + contentWidth && grid.acceptsInput(widget, x, y));
    }

    @Override
    public Optional<GuiEventListener> getChildAt(double x, double y) {
        var children = children();
        for (int i = children.size() - 1; i >= 0; i--) {
            var widget = (AbstractWidget) children.get(i);
            if (accepts(widget, x, y) && widget.isMouseOver(x, y)) return Optional.of(widget);
        }
        return Optional.empty();
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        var child = getChildAt(x, y);
        if (child.isPresent() && child.get().mouseClicked(x, y, button)) {
            if (getFocused() != child.get()) setFocused(child.get());
            setDragging(button == 0);
            return true;
        }
        if (OreGrid.contains(viewport, x, y)) { cancelInteraction(); return true; }
        return false;
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        boolean captured = isDragging();
        setDragging(false);
        if (getFocused() != null) getFocused().mouseReleased(x, y, button);
        return captured || OreGrid.contains(viewport, x, y);
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        return isDragging() && getFocused() != null && getFocused().mouseDragged(x, y, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (!OreGrid.contains(viewport, x, y)) return false;
        var hovered = getChildAt(x, y);
        if (hovered.isPresent() && hovered.get().mouseScrolled(x, y, horizontal, vertical)) return true;
        if (maximumScroll() == 0) return false;
        scrollOffset = Math.clamp(scrollOffset - (int) Math.round(vertical * 24), 0, maximumScroll());
        scrollbar.setMetrics(grid.getContentHeight(), viewport.height(), scrollOffset);
        positionContent();
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == GLFW.GLFW_KEY_TAB) {
            cancelPointerCapture();
            var candidates = children();
            boolean forward = (modifiers & GLFW.GLFW_MOD_SHIFT) == 0;
            int start = candidates.indexOf(getFocused());
            if (start < 0 && !forward) start = 0;
            for (int step = 1; step <= candidates.size(); step++) {
                int index = Math.floorMod(start + (forward ? step : -step), candidates.size());
                var candidate = (AbstractWidget) candidates.get(index);
                if (!candidate.visible || !candidate.active || candidate.nextFocusPath(new FocusNavigationEvent.TabNavigation(forward)) == null) continue;
                setFocused(candidate);
                revealFocused();
                return true;
            }
            return false;
        }
        return availableFocus() && getFocused().keyPressed(key, scan, modifiers);
    }

    private boolean availableFocus() {
        if (getFocused() instanceof AbstractWidget widget && widget.visible && widget.active) return true;
        if (getFocused() != null) cancelInteraction();
        return false;
    }

    @Override public boolean charTyped(char character, int modifiers) {
        return availableFocus() && getFocused().charTyped(character, modifiers);
    }

    @Override public boolean keyReleased(int key, int scan, int modifiers) {
        return availableFocus() && getFocused().keyReleased(key, scan, modifiers);
    }

    public void revealFocused() {
        if (!(getFocused() instanceof AbstractWidget widget) || widget == scrollbar) return;
        if (widget.getY() < viewport.top()) scrollOffset -= viewport.top() - widget.getY();
        else if (widget.getBottom() > viewport.bottom()) scrollOffset += widget.getBottom() - viewport.bottom();
        scrollOffset = Math.clamp(scrollOffset, 0, maximumScroll());
        scrollbar.setMetrics(grid.getContentHeight(), viewport.height(), scrollOffset);
        positionContent();
    }

    public void cancelInteraction() {
        cancelPointerCapture();
        setFocused(null);
    }

    private void cancelPointerCapture() {
        setDragging(false);
        widgets.forEach(OreWidgetSupport::cancel);
        scrollbar.cancelDrag();
    }

    public void tick(boolean displayed) {
        for (AbstractWidget widget : widgets) {
            if (widget instanceof OreModelPreview preview) {
                preview.setAnimationHostVisible(displayed);
                preview.tick();
            }
        }
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.enableScissor(viewport.left(), viewport.top(), viewport.right(), viewport.bottom());
        try {
            grid.render(graphics, mouseX, mouseY, partialTick);
            if (scrollbar.visible) scrollbar.render(graphics, mouseX, mouseY, partialTick);
        } finally { graphics.disableScissor(); }
    }
}
