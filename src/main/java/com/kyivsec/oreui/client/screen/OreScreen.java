package com.kyivsec.oreui.client.screen;

import com.kyivsec.oreui.client.widget.OreButton;
import com.kyivsec.oreui.client.widget.OreModelPreview;
import com.kyivsec.oreui.client.widget.OreScrollbar;
import com.kyivsec.oreui.client.style.OreDrawing;
import com.kyivsec.oreui.client.style.OreTooltip;
import com.kyivsec.oreui.client.host.OreWidgetSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public abstract class OreScreen extends Screen {
    private final List<AbstractWidget> contentWidgets = new ArrayList<>();
    protected ScreenRectangle viewport = new ScreenRectangle(0, 0, 1, 1);
    protected int scrollOffset;
    protected int contentHeight;
    private List<FormattedCharSequence> oreTooltip;
    private ClientTooltipPositioner tooltipPositioner;
    private final OreScrollbar scrollbar = new OreScrollbar(Component.translatable("oreui.scrollbar"),
            OreScrollbar.Orientation.VERTICAL, value -> {
                scrollOffset = value;
                arrangeContent();
            });

    @Override
    public void setTooltipForNextRenderPass(List<FormattedCharSequence> lines,
                                            ClientTooltipPositioner positioner, boolean override) {
        if (oreTooltip == null || override) {
            oreTooltip = lines;
            tooltipPositioner = positioner;
        }
    }

    private void renderOreTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (oreTooltip == null || oreTooltip.isEmpty()) {
            oreTooltip = null;
            return;
        }
        float scale = OreDrawing.DESCRIPTION_TEXT;
        int textWidth = oreTooltip.stream().mapToInt(font::width).max().orElse(0);
        int bodyWidth = Math.min(Math.max(12, width - 16), (int) Math.ceil(textWidth * scale) + 10);
        int bodyHeight = oreTooltip.size() * 8 + 4;
        var position = tooltipPositioner.positionTooltip(width, height, mouseX, mouseY, bodyWidth, bodyHeight + 6);
        int x = Math.clamp(position.x(), 7, Math.max(7, width - bodyWidth - 7));
        int y = Math.clamp(position.y(), 7, Math.max(7, height - bodyHeight - 7));
        OreTooltip.render(graphics, font, oreTooltip, x, y, bodyWidth,
                y < mouseY ? OreTooltip.Side.TOP : OreTooltip.Side.BOTTOM);
        oreTooltip = null;
    }

    protected OreScreen(Component title) {
        super(title);
    }

    protected <T extends AbstractWidget> T addContentWidget(T widget) {
        contentWidgets.add(widget);
        return addWidget(widget);
    }

    protected void removeContentWidget(AbstractWidget widget) {
        OreWidgetSupport.cancel(widget);
        if (widget instanceof OreModelPreview preview) preview.setAnimationHostVisible(false);
        if (getFocused() == widget) clearFocus();
        contentWidgets.remove(widget);
        removeWidget(widget);
    }

    protected abstract void arrangeContent();

    @Override
    protected void clearWidgets() {
        super.clearWidgets();
        contentWidgets.clear();
    }

    private void syncScrollbar() {
        if (!children().contains(scrollbar)) addWidget(scrollbar);
        scrollbar.setPosition(viewport.right() + 4, viewport.top());
        scrollbar.setHeight(viewport.height());
        scrollbar.setMetrics(contentHeight, viewport.height(), scrollOffset);
        scrollbar.visible = maxScroll() > 0;
    }

    protected void renderContentBackground(GuiGraphics graphics) {
    }

    protected int maxScroll() {
        return Math.max(0, contentHeight - viewport.height());
    }

    protected void clampScroll() {
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll()));
    }

    private boolean inside(double x, double y) {
        return x >= viewport.left() && x < viewport.right() && y >= viewport.top() && y < viewport.bottom();
    }

    protected boolean acceptsInput(GuiEventListener child, double x, double y) {
        return !contentWidgets.contains(child) || inside(x, y);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        syncScrollbar();
        for (GuiEventListener child : children()) {
            if (acceptsInput(child, mouseX, mouseY) && child.mouseClicked(mouseX, mouseY, button)) {
                if (minecraft.screen != this) {
                    cancelInteraction();
                    return true;
                }
                setFocused(child);
                setDragging(button == 0);
                return true;
            }
        }
        cancelInteraction();
        clearFocus();
        return true;
    }

    @Override
    public Optional<GuiEventListener> getChildAt(double mouseX, double mouseY) {
        return super.getChildAt(mouseX, mouseY).filter(child -> acceptsInput(child, mouseX, mouseY));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (!inside(mouseX, mouseY)) {
            return false;
        }
        if (super.mouseScrolled(mouseX, mouseY, horizontal, vertical)) {
            return true;
        }
        scrollOffset -= (int) Math.round(vertical * 24);
        clampScroll();
        arrangeContent();
        return true;
    }

    @Override
    public void setFocused(GuiEventListener listener) {
        if (getFocused() != listener) {
            super.setFocused(listener);
        }
        revealFocused();
    }

    protected void revealFocused() {
        if (getFocused() instanceof AbstractWidget widget && widget.visible && contentWidgets.contains(widget)) {
            int previous = scrollOffset;
            if (widget.getY() - 3 < viewport.top()) {
                scrollOffset += widget.getY() - 3 - viewport.top();
            } else if (widget.getBottom() + 3 > viewport.bottom()) {
                scrollOffset += widget.getBottom() + 3 - viewport.bottom();
            }
            clampScroll();
            if (previous != scrollOffset) {
                arrangeContent();
            }
        }
    }

    protected void cancelInteraction() {
        setDragging(false);
        scrollbar.cancelDrag();
        for (GuiEventListener child : children()) {
            if (child instanceof AbstractWidget widget) OreWidgetSupport.cancel(widget);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_TAB && isDragging()) {
            cancelInteraction();
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void tick() {
        if (!minecraft.isWindowActive()) {
            cancelInteraction();
        }
        for (GuiEventListener child : children()) {
            // Hidden previews sample the paused clock without advancing their model state.
            if (child instanceof OreModelPreview preview) preview.tick();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        syncScrollbar();
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.enableScissor(viewport.left() - 3, viewport.top(), viewport.right() + 3, viewport.bottom());
        try {
            renderContentBackground(graphics);
            int contentMouseY = inside(mouseX, mouseY) ? mouseY : Integer.MIN_VALUE;
            renderContentWidgets(graphics, mouseX, contentMouseY, partialTick);
        } finally {
            graphics.disableScissor();
        }
        if (scrollbar.visible) scrollbar.render(graphics, mouseX, mouseY, partialTick);
        for (GuiEventListener child : children()) {
            if (!(child instanceof AbstractWidget widget) || !widget.visible || !widget.active) {
                continue;
            }
            if (!widget.isFocused() && !(widget instanceof OreButton button && button.isSelected())) {
                continue;
            }
            boolean clipped = contentWidgets.contains(widget);
            if (clipped) {
                graphics.enableScissor(viewport.left() - 3, viewport.top(), viewport.right() + 3, viewport.bottom());
            }
            try {
                renderWidgetOutline(widget, graphics, clipped);
            } finally {
                if (clipped) {
                    graphics.disableScissor();
                }
            }
        }
        renderOreTooltip(graphics, mouseX, mouseY);
    }

    protected void renderContentWidgets(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        for (AbstractWidget widget : contentWidgets) {
            if (widget.visible && widget.getBottom() >= viewport.top() && widget.getY() <= viewport.bottom()) {
                widget.render(graphics, mouseX, mouseY, partialTick);
            }
        }
    }

    protected void renderWidgetOutline(AbstractWidget widget, GuiGraphics graphics, boolean content) {
        OreWidgetSupport.outline(widget, graphics);
    }

    @Override
    public void removed() {
        cancelInteraction();
        clearFocus();
        super.removed();
    }
}
