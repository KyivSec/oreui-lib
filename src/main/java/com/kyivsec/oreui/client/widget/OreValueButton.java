package com.kyivsec.oreui.client.widget;

import com.kyivsec.oreui.client.style.OreDrawing;
import com.kyivsec.oreui.client.style.OreNativeIcons;
import com.kyivsec.oreui.client.style.OrePalette;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Compact label/value action row, with an optional color swatch and native button input. */
public class OreValueButton extends OreButton {
    private final Component label;
    private Component value;
    private Integer swatchColor;

    public OreValueButton(Component label, Component value, OnPress onPress) {
        super(0, 0, 200, 20, label, OrePalette.GENERAL, onPress);
        this.label = label;
        setValue(value);
    }

    public Component getValue() { return value; }
    public Integer getSwatchColor() { return swatchColor; }

    public void setValue(Component value) {
        this.value = java.util.Objects.requireNonNull(value);
        setMessage(Component.translatable("oreui.control.value", label, value));
    }

    public void setSwatchColor(Integer color) { swatchColor = color; }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!active || !isFocused()) cancelPress();
        boolean highlighted = active && (isHovered() || isFocused() || isSelected());
        if (highlighted) OreDrawing.bevel(graphics, getX(), getY(), width, height,
                isPressed() ? 0xFF275B1A : 0xFF387E24, 0xFF50913D, 0xFF1C4A12);
        else graphics.fill(getX(), getBottom() - 1, getRight(), getBottom(), 0x30515453);
        int color = active ? OreDrawing.TEXT : OreDrawing.MUTED;
        int valueRight = getRight() - 17;
        int split = getX() + width * 55 / 100;
        int labelRight = Math.min(split - 4, getX() + 6 + (int) Math.ceil(font().width(label) * OreDrawing.SMALL_TEXT));
        OreDrawing.scaledText(graphics, font(), label, getX() + 6, getY(), labelRight, getBottom(), color, OreDrawing.SMALL_TEXT);
        if (swatchColor != null) {
            OreDrawing.bevel(graphics, valueRight - 22, getY() + (height - 12) / 2,
                    22, 12, swatchColor, 0x88FFFFFF, 0x66000000);
        } else {
            int valueLeft = Math.max(split, valueRight - (int) Math.ceil(font().width(value) * OreDrawing.SMALL_TEXT));
            OreDrawing.scaledText(graphics, font(), value, valueLeft, getY(), valueRight, getBottom(), color, OreDrawing.SMALL_TEXT);
        }
        if (active) OreNativeIcons.CHEVRON_RIGHT.draw(graphics, getRight() - 13, getY() + (height - 10) / 2, 10, color);
        renderOutline(graphics);
    }
}
