package com.kyivsec.oreui.client.widget;

import com.kyivsec.oreui.client.style.OreDrawing;
import com.kyivsec.oreui.client.style.OrePalette;
import com.kyivsec.oreui.client.style.OrePixelIcons;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.network.chat.Component;

/** Light top header, distinct from the existing sidebar/tab navigation control. */
public final class OreNavBar implements LayoutElement, Renderable {
    private final Component title;
    private final OrePixelButton back;
    private final OreButton status;
    private int x;
    private int y;
    private int width = 400;

    public OreNavBar(Component title, Component statusLabel, Runnable onBack, Runnable onStatus) {
        this.title = title;
        back = new OrePixelButton(Component.translatable("gui.back"), OrePixelIcons.Icon.CHEVRON_LEFT, button -> onBack.run());
        back.setGlyphColor(OreDrawing.BACKGROUND);
        status = new OreButton(0, 0, 70, 18, statusLabel, OrePalette.GENERAL, button -> onStatus.run()) {
            @Override
            protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                graphics.fill(getX(), getY(), getRight(), getBottom(), isPressed() ? 0xFFB1B2B5 : 0xFFE7E8EC);
                graphics.fill(getX(), getY(), getX() + 1, getBottom(), 0xFFA1A2A4);
                graphics.fill(getX() + 1, getY(), getX() + 2, getBottom(), OreDrawing.TEXT);
                OrePixelIcons.draw(graphics, OrePixelIcons.Icon.CIRCLE, getX() + 8, getY() + 5, 8, OreDrawing.BACKGROUND);
                OreDrawing.scaledText(graphics, Minecraft.getInstance().font, getMessage(), getX() + 18,
                        getY(), getRight() - 5, getBottom(), OreDrawing.BACKGROUND, OreDrawing.SMALL_TEXT);
                renderOutline(graphics);
            }
        };
        arrange();
    }

    public void setStatus(Component label) {
        status.setMessage(label);
    }

    public void setWidth(int width) {
        this.width = Math.max(60, width);
        arrange();
    }

    private void arrange() {
        back.setPosition(x, y);
        status.setWidth(Math.min(90, width / 3));
        status.setPosition(x + width - status.getWidth(), y);
    }

    @Override public int getX() { return x; }
    @Override public int getY() { return y; }
    @Override public int getWidth() { return width; }
    @Override public int getHeight() { return 20; }
    @Override public void setX(int x) { this.x = x; arrange(); }
    @Override public void setY(int y) { this.y = y; arrange(); }

    @Override
    public void visitWidgets(Consumer<AbstractWidget> visitor) {
        visitor.accept(back);
        visitor.accept(status);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(x, y, x + width, y + 18, 0xFFE7E8EC);
        graphics.fill(x, y + 18, x + width, y + 20, 0xFFB1B2B6);
        int margin = status.getWidth() + 4;
        OreDrawing.scaledText(graphics, Minecraft.getInstance().font, title, x + margin, y,
                x + width - margin, y + 18, OreDrawing.BACKGROUND, OreDrawing.SMALL_TEXT);
    }
}
