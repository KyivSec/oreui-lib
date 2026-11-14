package com.kyivsec.oreui.client.widget;

import com.kyivsec.oreui.client.style.OreDrawing;
import java.util.function.DoubleConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class OreSlider extends AbstractSliderButton {
    private final Component label;
    private final DoubleConsumer onChange;
    private boolean dragging;

    @Override
    public void onClick(double mouseX, double mouseY) {
        dragging = true;
        setFromPointer(mouseX);
    }

    private void setFromPointer(double mouseX) {
        setProgress((mouseX - getX() - 7.5) / Math.max(1, width - 15));
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
        setFromPointer(mouseX);
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        boolean wasDragging = dragging;
        dragging = false;
        if (wasDragging) playReleaseSound();
    }

    /** Aborts pointer capture without playing the native slider's release sound or changing its value. */
    public void cancelDrag() {
        dragging = false;
    }

    protected void playReleaseSound() {
        super.onRelease(0, 0);
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused) dragging = false;
    }

    public OreSlider(int x, int y, int width, int height, Component label, double progress, DoubleConsumer onChange) {
        super(x, y, width, height, label, normalized(progress));
        this.label = label;
        this.onChange = onChange;
        updateMessage();
    }

    private static double normalized(double progress) {
        if (!Double.isFinite(progress)) {
            throw new IllegalArgumentException("Slider progress must be finite");
        }
        return Mth.clamp(progress, 0.0, 1.0);
    }

    public double getProgress() {
        return value;
    }

    public void setProgress(double progress) {
        double next = normalized(progress);
        if (next != value) {
            value = next;
            updateMessage();
            applyValue();
        }
    }

    @Override
    protected void updateMessage() {
        setMessage(Component.translatable("oreui.test.slider.value", label, Math.round(value * 100)));
    }

    @Override
    protected void applyValue() {
        onChange.accept(value);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return active && visible && super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return active && visible && super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        OreDrawing.scrollingText(graphics, Minecraft.getInstance().font, getMessage(),
                getX() + 5, getY() + 2, getX() + width - 5, getY() + 16, active ? OreDrawing.TEXT : OreDrawing.MUTED);
        int trackY = getY() + height - 12;
        int handleX = getX() + (int) Math.round(value * Math.max(0, width - 15));
        OreDrawing.track(graphics, getX(), trackY, width, value, active);
        int handleY = trackY - 4;
        graphics.fill(handleX, handleY, handleX + 15, handleY + 15, OreDrawing.BACKGROUND);
        if (active && !dragging) graphics.fill(handleX + 1, handleY + 12, handleX + 14, handleY + 14, 0xFF59595B);
        OreDrawing.bevel(graphics, handleX + 1, handleY + 1, 13, active && !dragging ? 11 : 13,
                !active ? 0xFFB1B2B5 : isHovered() || dragging ? 0xFFB1B2B6 : 0xFFD0D1D5,
                0xFFE3E3E5, 0xFFE3E3E5);
        renderOutline(graphics);
    }

    public void renderOutline(GuiGraphics graphics) {
        if (visible && active && isFocused()) {
            int handleX = getX() + (int) Math.round(value * Math.max(0, width - 15));
            OreDrawing.buttonFocus(graphics, handleX, getY() + height - 16, 15, 15);
        }
    }
}
