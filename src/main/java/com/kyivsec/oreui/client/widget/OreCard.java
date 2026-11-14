package com.kyivsec.oreui.client.widget;

import com.kyivsec.oreui.client.style.OreDrawing;
import com.kyivsec.oreui.client.style.OrePalette;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.network.chat.Component;

public class OreCard extends OreButton {
    private final Font font;
    private final Component body;
    private MultiLineLabel lines;

    public OreCard(Font font, Component title, Component body, int accent, OnPress onPress) {
        super(0, 0, 100, 80, title, OrePalette.GENERAL, onPress);
        this.font = font;
        this.body = body;
        // Accent is retained in the public constructor for compatibility. Figma cards are neutral.
        setWidth(100);
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        lines = MultiLineLabel.create(font, body, Math.max(1, (int) ((width - 14) / OreDrawing.DESCRIPTION_TEXT)));
        setHeight(Math.max(32, 22 + lines.getLineCount() * 6));
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int fill = isSelected() ? 0xFF6D6D6E : active && isHovered() ? 0xFF2F2F2F : 0xFF48494A;
        int edge = isSelected() ? 0xFFB1B2B5 : active && isHovered() ? 0xFF3F3F3F : 0xFF6D6D6E;
        graphics.fill(getX(), getY(), getRight(), getBottom(), OreDrawing.BACKGROUND);
        OreDrawing.bevel(graphics, getX() + 1, getY() + 1, width - 2, height - 2, fill, edge, 0x80000000);
        int titleWidth = (int) Math.ceil(font.width(getMessage()) * OreDrawing.SMALL_TEXT);
        OreDrawing.scaledText(graphics, font, getMessage(), getX() + 7, getY() + 7,
                Math.min(getRight() - 7, getX() + 7 + titleWidth), getY() + 15,
                active ? OreDrawing.TEXT : 0xFF8C8D90, OreDrawing.SMALL_TEXT);
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(getX() + 7, getY() + 17, 0);
            graphics.pose().scale(OreDrawing.DESCRIPTION_TEXT, OreDrawing.DESCRIPTION_TEXT, 1);
            lines.renderLeftAlignedNoShadow(graphics, 0, 0, font.lineHeight, active ? 0xFFD0D1D4 : 0xFF8C8D90);
        } finally {
            graphics.pose().popPose();
        }
        renderOutline(graphics);
    }
}
