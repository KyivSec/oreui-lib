package com.kyivsec.oreui.client.widget;

import com.kyivsec.oreui.client.style.OreDrawing;
import java.util.function.DoubleConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** A range divided into equal intervals, with optional visible segment separators. */
public class OreSteppedSlider extends OreSlider {
    private final Component rangeLabel;
    private final int steps;
    private final boolean segmented;

    public OreSteppedSlider(Component label, int steps, double progress, boolean segmented, DoubleConsumer onChange) {
        super(0, 0, 255, 29, label, snap(progress, steps), onChange);
        this.rangeLabel = label;
        this.steps = steps;
        this.segmented = segmented;
        updateMessage();
    }

    private static double snap(double value, int steps) {
        if (steps < 1 || steps > 1000) throw new IllegalArgumentException("Slider intervals must be in [1, 1000]");
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Slider progress must be finite");
        return Math.round(Math.clamp(value, 0, 1) * steps) / (double) steps;
    }

    public int getStep() {
        return (int) Math.round(getProgress() * steps);
    }

    public int getSteps() {
        return steps;
    }

    public boolean isSegmented() {
        return segmented;
    }

    @Override
    public void setProgress(double value) {
        super.setProgress(snap(value, steps));
    }

    @Override
    protected void updateMessage() {
        if (steps > 0) setMessage(Component.translatable("oreui.control.value", rangeLabel, getStep()));
        else super.updateMessage();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!active || !visible || !isFocused()) return false;
        switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_DOWN -> setProgress(getProgress() - 1.0 / steps);
            case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_UP -> setProgress(getProgress() + 1.0 / steps);
            case GLFW.GLFW_KEY_HOME -> setProgress(0);
            case GLFW.GLFW_KEY_END -> setProgress(1);
            default -> { return super.keyPressed(keyCode, scanCode, modifiers); }
        }
        return true;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        var font = Minecraft.getInstance().font;
        int color = active ? OreDrawing.TEXT : 0xFFD0D1D4;
        int labelRight = Math.min(getRight() - 28, getX() + (int) Math.ceil(font.width(rangeLabel) * OreDrawing.SMALL_TEXT));
        OreDrawing.scaledText(graphics, font, rangeLabel, getX(), getY(), labelRight, getY() + 10, color, OreDrawing.SMALL_TEXT);
        OreDrawing.scaledText(graphics, font, Component.literal(Integer.toString(getStep())), getRight() - 24, getY(),
                getRight(), getY() + 10, color, OreDrawing.SMALL_TEXT);
        int y = getY() + height - 11;
        int edge = active ? OreDrawing.BACKGROUND : 0xFF8C8D90;
        graphics.fill(getX(), y, getRight(), y + 6, edge);
        OreDrawing.bevel(graphics, getX() + 1, y + 1, width - 2, 4,
                active ? 0xFF8C8D90 : 0xFFD0D1D4, active ? 0xFFA3A4A6 : 0xFFD9DADD, active ? 0xFFA3A4A6 : 0xFFD9DADD);
        int fill = (int) Math.round((width - 2) * getProgress());
        if (fill > 0) OreDrawing.bevel(graphics, getX() + 1, y + 1, fill, 4,
                active ? 0xFF3C8527 : 0xFFD0D1D4, active ? 0xFF639D52 : 0xFFD5D6D8, active ? 0xFF639D52 : 0xFFD5D6D8);
        if (segmented) {
            for (int i = 1; i < steps; i++) {
                int x = getX() + (int) Math.round((width - 1) * i / (double) steps);
                graphics.fill(x, y, x + 1, y + 6, edge);
            }
        }
        int x = getX() + (int) Math.round(getProgress() * Math.max(0, width - 15));
        graphics.fill(x, y - 4, x + 15, y + 11, edge);
        graphics.fill(x + 1, y + 8, x + 14, y + 10, active ? 0xFF58585A : 0xFFB1B2B5);
        OreDrawing.bevel(graphics, x + 1, y - 3, 13, 11, 0xFFD0D1D4, 0xFFECEDEE, 0xFFECEDEE);
        renderOutline(graphics);
    }

    @Override
    public void renderOutline(GuiGraphics graphics) {
        if (visible && active && isFocused()) {
            int x = getX() + (int) Math.round(getProgress() * Math.max(0, width - 15));
            OreDrawing.buttonFocus(graphics, x, getY() + height - 15, 15, 15);
        }
    }
}
