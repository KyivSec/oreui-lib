package com.kyivsec.oreui.client.widget;

import com.kyivsec.oreui.client.style.OreDrawing;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.network.chat.Component;

public final class OreSection extends LinearLayout implements Renderable {
    private final OreColumn content = new OreColumn();

    public OreSection(Font font, Component title) {
        super(0, 0, Orientation.VERTICAL);
        content.spacing(8);
        addChild(content, settings -> settings.padding(12));
        content.addChild(new OreLabel(font, title, 100));
    }

    public <T extends LayoutElement> T add(T element) {
        return content.addChild(element);
    }

    public void setWidth(int width) {
        content.setWidth(Math.max(16, width - 24));
        arrangeElements();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        OreDrawing.inset(graphics, getX(), getY(), getWidth(), getHeight());
    }
}
