package com.kyivsec.oreui.client.widget;

import com.kyivsec.oreui.client.style.OreDrawing;
import java.util.Objects;
import java.util.function.DoubleConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** A rainbow hue strip, paired with {@link OreColorSquare}. Hue is normalized to [0,1]. */
public class OreHueSlider extends AbstractWidget {
    private final DoubleConsumer onChange;
    private double hue;
    private boolean dragging;

    public OreHueSlider(Component label, double hue, DoubleConsumer onChange) {
        super(0, 0, 128, 28, label);
        this.hue = OreColorMath.normalized(hue);
        this.onChange = Objects.requireNonNull(onChange);
    }

    public double getHue() { return hue; }

    public void setHue(double hue) {
        double next = OreColorMath.normalized(hue);
        if (this.hue != next) {
            this.hue = next;
            onChange.accept(next);
        }
    }

    private void setFromPointer(double mouseX) {
        setHue((mouseX - getX() - 3) / Math.max(1, width - 7));
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (!active || !visible) return;
        dragging = true;
        setFromPointer(mouseX);
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (dragging && active && visible) setFromPointer(mouseX);
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
        double step = ((modifiers & GLFW.GLFW_MOD_SHIFT) != 0 ? 10 : 1) / 360.0;
        switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_DOWN -> setHue(hue - step);
            case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_UP -> setHue(hue + step);
            case GLFW.GLFW_KEY_HOME -> setHue(0);
            case GLFW.GLFW_KEY_END -> setHue(1);
            default -> { return false; }
        }
        return true;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        OreDrawing.scaledText(graphics, Minecraft.getInstance().font, getMessage(), getX(), getY(), getRight(), getY() + 10,
                active ? OreDrawing.TEXT : OreDrawing.MUTED, OreDrawing.SMALL_TEXT);
        int top = getY() + 14;
        OreDrawing.field(graphics, getX(), top, width, 12, active, false);
        for (int x = 0; x < width - 6; x++) {
            graphics.fill(getX() + 3 + x, top + 3, getX() + 4 + x, top + 9,
                    OreColorMath.rgb(x / (double) Math.max(1, width - 7), 1, 1));
        }
        if (!active) graphics.fill(getX() + 3, top + 3, getRight() - 3, top + 9, 0x90000000);
        int x = getX() + (int) Math.round(hue * Math.max(0, width - 7));
        graphics.fill(x, top - 2, x + 7, top + 14, OreDrawing.BACKGROUND);
        graphics.fill(x + 1, top - 1, x + 6, top + 13, 0xFF59595B);
        OreDrawing.bevel(graphics, x + 1, top - 1, 5, dragging ? 14 : 12,
                !active || isHovered() || dragging ? 0xFFB1B2B5 : 0xFFD0D1D5, 0xFFE3E3E5, 0xFFE3E3E5);
        renderOutline(graphics);
    }

    public void renderOutline(GuiGraphics graphics) {
        if (visible && active && isFocused()) {
            int x = getX() + (int) Math.round(hue * Math.max(0, width - 7));
            OreDrawing.buttonFocus(graphics, x, getY() + 12, 7, 16);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
        output.add(NarratedElementType.POSITION, Component.translatable("oreui.color.hue.value", Math.round(hue * 360)));
        output.add(NarratedElementType.USAGE, Component.translatable("oreui.color.hue.usage"));
    }
}
