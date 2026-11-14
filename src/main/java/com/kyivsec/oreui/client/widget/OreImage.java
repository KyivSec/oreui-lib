package com.kyivsec.oreui.client.widget;

import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import com.kyivsec.oreui.client.style.OreDrawing;
import com.kyivsec.oreui.client.style.OreNativeIcons;
import com.kyivsec.oreui.client.style.OreIcons;

public class OreImage extends AbstractWidget {
    private ResourceLocation texture;
    private final int textureWidth;
    private final int textureHeight;

    public OreImage(Component label, ResourceLocation texture, int textureWidth, int textureHeight, int height) {
        super(0, 0, 100, height, label);
        if (textureWidth <= 0 || textureHeight <= 0 || height <= 0) {
            throw new IllegalArgumentException("Image dimensions must be positive");
        }
        this.texture = texture;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
    }

    public void setTexture(ResourceLocation texture) {
        this.texture = texture;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        OreIcons.Raster resolved = OreIcons.raster(texture, Math.min(width, height));
        ResourceLocation renderTexture = resolved == null ? texture : resolved.texture();
        OreNativeIcons nativeIcon = OreNativeIcons.fromTexture(renderTexture);
        if (nativeIcon != null) {
            int slot = Math.min(width, height);
            nativeIcon.draw(graphics, getX() + (width - slot) / 2, getY() + (height - slot) / 2, slot, OreDrawing.TEXT);
            return;
        }
        int imageHeight = Math.min(height, width * textureHeight / textureWidth);
        int imageWidth = imageHeight * textureWidth / textureHeight;
        OreDrawing.image(graphics, texture, getX() + (width - imageWidth) / 2, getY() + (height - imageHeight) / 2,
                imageWidth, imageHeight, textureWidth, textureHeight, OreDrawing.TEXT);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    @Override
    public ComponentPath nextFocusPath(FocusNavigationEvent event) {
        return null;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
    }
}
