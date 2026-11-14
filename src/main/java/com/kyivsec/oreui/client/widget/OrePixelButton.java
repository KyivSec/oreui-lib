package com.kyivsec.oreui.client.widget;

import com.kyivsec.oreui.client.style.OreDrawing;
import com.kyivsec.oreui.client.style.OrePalette;
import com.kyivsec.oreui.client.style.OrePixelIcons;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class OrePixelButton extends OreButton {
    private final OrePixelIcons.Icon glyph;
    private int color = OreDrawing.TEXT;

    public OrePixelButton(Component label, OrePixelIcons.Icon glyph, OnPress action) {
        super(0, 0, 20, 18, label, OrePalette.GENERAL, action);
        this.glyph = glyph;
    }

    public void setGlyphColor(int color) {
        this.color = color;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (active && (isHovered() || isPressed())) {
            graphics.fill(getX(), getY(), getRight(), getBottom(), isPressed() ? 0x33000000 : 0x1AFFFFFF);
        }
        OrePixelIcons.draw(graphics, glyph, getX() + (width - 8) / 2, getY() + (height - 8) / 2,
                8, active ? color : 0xFF8C8D90);
        renderOutline(graphics);
    }
}
