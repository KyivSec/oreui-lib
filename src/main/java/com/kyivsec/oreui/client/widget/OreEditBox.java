package com.kyivsec.oreui.client.widget;

import com.kyivsec.oreui.client.style.OreDrawing;
import com.kyivsec.oreui.client.style.OreIcons;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class OreEditBox extends EditBox {
    private static final int PADDING = 7;
    private boolean search;

    public OreEditBox search() {
        search = true;
        return this;
    }

    protected int padding() {
        return PADDING;
    }

    protected int inputTop() {
        return getY();
    }

    protected int inputHeight() {
        return height;
    }

    private int textInset() {
        return search ? 27 : padding();
    }

    public OreEditBox(Font font, int x, int y, int width, int height, Component label) {
        super(font, x, y, width, height, label);
        setBordered(false);
        setTextShadow(false);
        setTextColor(0xFFB1B2B5);
        setTextColorUneditable(0xFF3F4143);
    }

    @Override
    public int getInnerWidth() {
        return Math.max(0, width - textInset() - padding());
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        super.onClick(mouseX - textInset(), mouseY);
    }

    @Override
    public int getScreenX(int position) {
        return super.getScreenX(position) + textInset();
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderField(graphics);
        setTextColor(active ? 0xFFB1B2B5 : 0xFF3F4143);
        if (search) OreDrawing.icon(graphics, OreIcons.SEARCH.texture(), getX() + PADDING,
                inputTop() + (inputHeight() - 12) / 2, 12, active ? OreDrawing.TEXT : 0xFF3F4143);
        graphics.enableScissor(getX() + textInset(), inputTop() + 3, getRight() - padding(), inputTop() + inputHeight() - 3);
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(textInset(), inputTop() - getY() + (inputHeight() - 8) / 2, 0);
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
        } finally {
            graphics.pose().popPose();
            graphics.disableScissor();
        }
        renderOutline(graphics);
    }

    protected void renderField(GuiGraphics graphics) {
        OreDrawing.field(graphics, getX(), inputTop(), width, inputHeight(), active, isFocused());
    }

    public void renderOutline(GuiGraphics graphics) {
        if (visible && isFocused() && active) OreDrawing.buttonFocus(graphics, getX(), inputTop(), width, inputHeight());
    }
}
