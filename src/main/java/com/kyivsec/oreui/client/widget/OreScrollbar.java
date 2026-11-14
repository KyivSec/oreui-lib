package com.kyivsec.oreui.client.widget;

import com.kyivsec.oreui.client.style.OreDrawing;
import java.util.Objects;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class OreScrollbar extends AbstractWidget {
    public enum Orientation { VERTICAL, HORIZONTAL }

    private final Orientation orientation;
    private final IntConsumer onChange;
    private int contentSize = 1;
    private int viewSize = 1;
    private int value;
    private double grabOffset;
    private boolean dragging;

    public OreScrollbar(Component label, Orientation orientation, IntConsumer onChange) {
        super(0, 0, orientation == Orientation.VERTICAL ? 6 : 135, orientation == Orientation.VERTICAL ? 137 : 8, label);
        this.orientation = Objects.requireNonNull(orientation);
        this.onChange = Objects.requireNonNull(onChange);
    }

    /** Updates layout state without invoking the user callback. */
    public void setMetrics(int contentSize, int viewSize, int value) {
        if (contentSize < 0 || viewSize < 0) throw new IllegalArgumentException("Scroll sizes cannot be negative");
        this.contentSize = contentSize;
        this.viewSize = viewSize;
        this.value = Math.clamp(value, 0, getMaximum());
    }

    public int getValue() {
        return value;
    }

    public int getMaximum() {
        return Math.max(0, contentSize - viewSize);
    }

    public void setValue(int value) {
        int next = Math.clamp(value, 0, getMaximum());
        if (this.value != next) {
            this.value = next;
            onChange.accept(next);
        }
    }

    public int thumbLength() {
        int length = length();
        return Math.min(length, Math.max(8, (int) Math.round(length * Math.min(1, viewSize / (double) Math.max(1, contentSize)))));
    }

    private int length() {
        return Math.max(1, orientation == Orientation.VERTICAL ? height : width);
    }

    private int thumbPosition() {
        return getMaximum() == 0 ? 0 : (int) Math.round((length() - thumbLength()) * value / (double) getMaximum());
    }

    private double pointer(double mouseX, double mouseY) {
        return orientation == Orientation.VERTICAL ? mouseY - getY() : mouseX - getX();
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        double pointer = pointer(mouseX, mouseY);
        int start = thumbPosition();
        grabOffset = pointer >= start && pointer < start + thumbLength() ? pointer - start : thumbLength() / 2.0;
        dragging = true;
        moveThumb(pointer);
    }

    private void moveThumb(double pointer) {
        int travel = length() - thumbLength();
        if (travel > 0) setValue((int) Math.round((pointer - grabOffset) / travel * getMaximum()));
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (dragging && active && visible) moveThumb(pointer(mouseX, mouseY));
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        dragging = false;
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused) dragging = false;
    }

    public void cancelDrag() {
        dragging = false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!active || !visible || !isFocused()) return false;
        switch (keyCode) {
            case GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_LEFT -> setValue(value - 1);
            case GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_RIGHT -> setValue(value + 1);
            case GLFW.GLFW_KEY_PAGE_UP -> setValue(value - viewSize);
            case GLFW.GLFW_KEY_PAGE_DOWN -> setValue(value + viewSize);
            case GLFW.GLFW_KEY_HOME -> setValue(0);
            case GLFW.GLFW_KEY_END -> setValue(getMaximum());
            default -> { return false; }
        }
        return true;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = getX(), y = getY();
        if (orientation == Orientation.VERTICAL) graphics.fill(x + 2, y, x + 4, getBottom(), 0xFF5A5B5C);
        else graphics.fill(x, y + 3, getRight(), y + 5, 0xFF5A5B5C);
        int tw = orientation == Orientation.VERTICAL ? 6 : thumbLength();
        int th = orientation == Orientation.VERTICAL ? thumbLength() : 8;
        if (tw < 4 || th < 4) return;
        if (orientation == Orientation.VERTICAL) y += thumbPosition();
        else x += thumbPosition();
        graphics.fill(x, y, x + tw, y + th, OreDrawing.BACKGROUND);
        graphics.fill(x + 1, y + 1, x + tw - 1, y + th - 1, 0xFF58585A);
        graphics.fill(x + 1, y + 1, x + tw - 1, y + th - 3, 0xFFE3E3E5);
        graphics.fill(x + 2, y + 2, x + tw - 2, y + th - 3, active ? 0xFFD0D1D4 : 0xFFB1B2B5);
        if (active && isFocused()) OreDrawing.buttonFocus(graphics, x, y, tw, th);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
        output.add(NarratedElementType.POSITION, Component.translatable("oreui.scroll.position", value, getMaximum()));
    }
}
