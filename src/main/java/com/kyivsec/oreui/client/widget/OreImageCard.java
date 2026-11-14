package com.kyivsec.oreui.client.widget;

import com.kyivsec.oreui.client.style.OreDrawing;
import com.kyivsec.oreui.client.style.OrePalette;
import com.kyivsec.oreui.client.style.OrePixelIcons;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Image/caption card with an optional independent footer action. Image content is supplied by the caller. */
public class OreImageCard extends LinearLayout {
    private final OreButton image;
    private final OreButton caption;
    private final OreButton action;
    private Component count = Component.empty();
    private Component topTag = Component.empty();
    private Component bottomTag = Component.empty();
    private Component bottomInfoTag = Component.empty();

    public OreImageCard(Font font, Component title, Component description, ResourceLocation texture,
                        int textureWidth, int textureHeight, Runnable onOpen, Runnable onAction) {
        super(0, 0, Orientation.VERTICAL);
        if (textureWidth <= 0 || textureHeight <= 0) throw new IllegalArgumentException("Image dimensions must be positive");
        spacing(0);
        image = addChild(new OreButton(0, 0, 150, 85, title, OrePalette.GENERAL, button -> onOpen.run()) {
            @Override
            protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                graphics.fill(getX(), getY(), getRight(), getBottom(), OreDrawing.BACKGROUND);
                float scale = Math.max((width - 2F) / textureWidth, (height - 1F) / textureHeight);
                int dw = (int) Math.ceil(textureWidth * scale), dh = (int) Math.ceil(textureHeight * scale);
                graphics.enableScissor(getX() + 1, getY() + 1, getRight() - 1, getBottom());
                try {
                    OreDrawing.image(graphics, texture, getX() + (width - dw) / 2, getY() + (height - dh) / 2,
                            dw, dh, textureWidth, textureHeight, OreDrawing.TEXT);
                } finally {
                    graphics.disableScissor();
                }
                tag(graphics, font, topTag, getX() + 1, getY() + 1, 0xFF8CB3FF, width - 22);
                int firstTag = tag(graphics, font, bottomInfoTag, getX() + 1, getBottom() - 10, 0xFF8CB3FF, width - 2);
                tag(graphics, font, bottomTag, getX() + 1 + firstTag, getBottom() - 10, 0xFFFFE866, width - 2 - firstTag);
                graphics.fill(getRight() - 20, getY() + 1, getRight() - 1, getY() + 20, OreDrawing.BACKGROUND);
                graphics.fill(getRight() - 19, getY() + 1, getRight() - 1, getY() + 19, OreDrawing.PANEL);
                OrePixelIcons.draw(graphics, OrePixelIcons.Icon.CHEVRON_RIGHT, getRight() - 14, getY() + 6, 8, OreDrawing.TEXT);
                renderOutline(graphics);
            }
        });
        LinearLayout footer = addChild(new LinearLayout(0, 0, Orientation.HORIZONTAL));
        caption = footer.addChild(new OreButton(0, 0, 150, 32, title, OrePalette.GENERAL, button -> onOpen.run()) {
            @Override
            protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                footerSurface(graphics, getX(), getY(), width, height, active && isHovered());
                int reserved = onAction == null && !count.getString().isEmpty() ? 28 : 7;
                text(graphics, font, title, getX() + 7, getY() + 6, getRight() - reserved, getY() + 16, OreDrawing.SMALL_TEXT);
                text(graphics, font, description, getX() + 7, getY() + 17, getRight() - reserved, getBottom() - 5, OreDrawing.DESCRIPTION_TEXT);
                if (onAction == null) OreDrawing.scaledText(graphics, font, count, getRight() - 27, getY(),
                        getRight() - 6, getBottom(), OreDrawing.TEXT, OreDrawing.DESCRIPTION_TEXT);
                renderOutline(graphics);
            }
        });
        action = onAction == null ? null : footer.addChild(new OreButton(0, 0, 32, 32,
                Component.translatable("oreui.card.action"), OrePalette.GENERAL, button -> onAction.run()) {
            @Override
            protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                footerSurface(graphics, getX(), getY(), width, height, active && isHovered());
                OrePixelIcons.draw(graphics, OrePixelIcons.Icon.TICK, getX() + 11, getY() + 10, 10, OreDrawing.TEXT);
                renderOutline(graphics);
            }
        });
        setWidth(150);
    }

    public void setWidth(int width) {
        width = Math.max(48, width);
        image.setWidth(width);
        image.setHeight(Math.round((width - 2) * 9F / 16) + 2);
        caption.setWidth(width - (action == null ? 0 : 31));
        if (action != null) action.setWidth(31);
        arrangeElements();
    }

    public void setCount(Component count) { this.count = count; }
    public void setTopTag(Component tag) { topTag = tag; }
    public void setBottomTag(Component tag) { bottomTag = tag; }
    public void setBottomInfoTag(Component tag) { bottomInfoTag = tag; }

    private static void footerSurface(GuiGraphics graphics, int x, int y, int width, int height, boolean hot) {
        graphics.fill(x, y, x + width, y + height, OreDrawing.BACKGROUND);
        OreDrawing.bevel(graphics, x + 1, y, width - 2, height - 1,
                hot ? OreDrawing.PANEL : OreDrawing.HEADER, 0xFF5A5B5C, 0xFF333334);
    }

    private static void text(GuiGraphics graphics, Font font, Component text, int left, int top, int right, int bottom, float scale) {
        OreDrawing.scaledText(graphics, font, text, left, top,
                Math.min(right, left + (int) Math.ceil(font.width(text) * scale)), bottom, OreDrawing.TEXT, scale);
    }

    private static int tag(GuiGraphics graphics, Font font, Component text, int x, int y, int color, int maxWidth) {
        if (text.getString().isEmpty() || maxWidth < 4) return 0;
        int width = Math.min(maxWidth, (int) Math.ceil(font.width(text) * OreDrawing.DESCRIPTION_TEXT) + 4);
        graphics.fill(x, y, x + width, y + 10, OreDrawing.BACKGROUND);
        graphics.fill(x, y + 1, x + width - 1, y + 9, color);
        OreDrawing.scaledText(graphics, font, text, x + 2, y + 1, x + width - 2, y + 9,
                OreDrawing.BACKGROUND, OreDrawing.DESCRIPTION_TEXT);
        return width;
    }
}
