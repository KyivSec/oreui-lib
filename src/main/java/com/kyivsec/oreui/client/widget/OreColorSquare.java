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

/** A square saturation/brightness field. Hue is supplied separately; incoming alpha is preserved. */
public class OreColorSquare extends AbstractWidget {
    private final IntConsumer onChange;
    private int color;
    private double hue;
    private double saturation;
    private double brightness;
    private boolean dragging;

    public OreColorSquare(Component label, int initialColor, IntConsumer onChange) {
        super(0, 0, 128, 128, label);
        this.onChange = Objects.requireNonNull(onChange);
        color = initialColor;
        var hsv = OreColorMath.hsv(initialColor);
        hue = hsv.hue();
        saturation = hsv.saturation();
        brightness = hsv.brightness();
    }

    public int getColor() { return color; }
    public double getHue() { return hue; }
    public double getSaturation() { return saturation; }
    public double getBrightness() { return brightness; }

    /** Notifies only when the resulting ARGB color changes, like the other value widgets. */
    public void setColor(int color) {
        var hsv = OreColorMath.hsv(color);
        // Gray has no hue; black has neither hue nor saturation. Retain these for subsequent edits.
        if (hsv.saturation() > 0) hue = hsv.hue();
        if (hsv.brightness() > 0) saturation = hsv.saturation();
        brightness = hsv.brightness();
        updateColor(color);
    }

    public void setHue(double hue) {
        this.hue = OreColorMath.normalized(hue);
        applyCoordinates();
    }

    public void setSaturationBrightness(double saturation, double brightness) {
        double s = OreColorMath.normalized(saturation);
        double v = OreColorMath.normalized(brightness);
        this.saturation = s;
        this.brightness = v;
        applyCoordinates();
    }

    private void applyCoordinates() {
        updateColor(color & 0xFF000000 | OreColorMath.rgb(hue, saturation, brightness) & 0xFFFFFF);
    }

    private void updateColor(int next) {
        if (color != next) {
            color = next;
            onChange.accept(next);
        }
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(Math.max(8, width));
        super.setHeight(getWidth());
    }

    @Override
    public void setHeight(int height) {
        setWidth(height);
    }

    private int span() { return width - 7; }

    private void setFromPointer(double mouseX, double mouseY) {
        setSaturationBrightness((mouseX - getX() - 3) / span(), 1 - (mouseY - getY() - 3) / span());
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (!active || !visible) return;
        dragging = true;
        setFromPointer(mouseX, mouseY);
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (dragging && active && visible) setFromPointer(mouseX, mouseY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return active && visible && dragging && super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public void onRelease(double mouseX, double mouseY) { cancelDrag(); }

    public void cancelDrag() { dragging = false; }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused) cancelDrag();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!active || !visible || !isFocused()) return false;
        double step = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0 ? 0.1 : 0.01;
        switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT -> setSaturationBrightness(saturation - step, brightness);
            case GLFW.GLFW_KEY_RIGHT -> setSaturationBrightness(saturation + step, brightness);
            case GLFW.GLFW_KEY_UP -> setSaturationBrightness(saturation, brightness + step);
            case GLFW.GLFW_KEY_DOWN -> setSaturationBrightness(saturation, brightness - step);
            default -> { return false; }
        }
        return true;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        OreDrawing.field(graphics, getX(), getY(), width, height, active, false);
        int left = getX() + 3, top = getY() + 3, size = width - 6;
        for (int x = 0; x < size; x++) {
            graphics.fillGradient(left + x, top, left + x + 1, top + size - 1,
                    OreColorMath.rgb(hue, x / (double) span(), 1), 0xFF000000);
        }
        graphics.fill(left, top + size - 1, left + size, top + size, 0xFF000000);
        if (!active) graphics.fill(left, top, left + size, top + size, 0x90000000);
        int x = left + (int) Math.round(saturation * span());
        int y = top + (int) Math.round((1 - brightness) * span());
        OreDrawing.outline(graphics, x - 3, y - 3, 7, 7, OreDrawing.BACKGROUND);
        OreDrawing.outline(graphics, x - 2, y - 2, 5, 5, active ? OreDrawing.TEXT : OreDrawing.MUTED);
        renderOutline(graphics);
    }

    public void renderOutline(GuiGraphics graphics) {
        if (visible && active && isFocused()) OreDrawing.buttonFocus(graphics, getX(), getY(), width, height);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
        output.add(NarratedElementType.POSITION, Component.translatable("oreui.color.square.value",
                Math.round(saturation * 100), Math.round(brightness * 100), OreColorPicker.format(color)));
        output.add(NarratedElementType.USAGE, Component.translatable("oreui.color.square.usage"));
    }
}
