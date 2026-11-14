package com.kyivsec.oreui.client.widget;

import com.kyivsec.oreui.client.style.OreDrawing;
import java.util.Objects;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Labeled community variant; native EditBox still owns editing, selection and clipboard behavior. */
public class OreTextField extends OreEditBox {
    private final Font captionFont;
    private Component description;

    public OreTextField(Font font, int x, int y, int width, Component label, Component description) {
        super(font, x, y, width, 43, label);
        captionFont = font;
        this.description = Objects.requireNonNull(description);
    }

    public void setDescription(Component description) {
        this.description = Objects.requireNonNull(description);
    }

    @Override
    protected int padding() {
        return 4;
    }

    @Override
    protected int inputTop() {
        return getY() + 11;
    }

    @Override
    protected int inputHeight() {
        return 22;
    }

    @Override
    protected void renderField(GuiGraphics graphics) {
        int y = inputTop();
        graphics.fill(getX(), y, getRight(), y + 22, active ? OreDrawing.BACKGROUND : 0xFF8C8D90);
        graphics.fill(getX() + 1, y + 1, getRight() - 1, y + 21, active ? 0xFF242425 : 0xFFB1B2B5);
        graphics.fill(getX() + 1, y + 3, getRight() - 1, y + 21, active ? OreDrawing.PANEL : 0xFFD0D1D4);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
        caption(graphics, getMessage(), getY(), getY() + 10, active ? OreDrawing.TEXT : 0xFFD0D1D4, OreDrawing.SMALL_TEXT);
        caption(graphics, description, getY() + 35, getBottom(), 0xFFD0D1D4, OreDrawing.DESCRIPTION_TEXT);
    }

    private void caption(GuiGraphics graphics, Component text, int top, int bottom, int color, float scale) {
        OreDrawing.scaledText(graphics, captionFont, text, getX(), top,
                Math.min(getRight(), getX() + (int) Math.ceil(captionFont.width(text) * scale)), bottom, color, scale);
    }
}
