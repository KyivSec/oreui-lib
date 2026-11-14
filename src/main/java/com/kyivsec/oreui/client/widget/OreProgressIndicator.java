package com.kyivsec.oreui.client.widget;

import com.kyivsec.oreui.client.style.OreDrawing;
import com.kyivsec.oreui.client.style.OrePixelIcons;
import java.util.function.DoubleSupplier;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class OreProgressIndicator extends AbstractWidget {
    public enum Kind {
        BAR, SPINNER, OUTLINED_BAR, PIXEL_SPINNER, PIXEL_RING
    }

    private final Kind kind;
    private final DoubleSupplier progress;
    private boolean running = true;

    public OreProgressIndicator(Component label, Kind kind, DoubleSupplier progress) {
        super(0, 0, 100, 34, label);
        this.kind = kind;
        this.progress = progress;
        if (kind == Kind.OUTLINED_BAR) setHeight(18);
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (kind == Kind.PIXEL_SPINNER || kind == Kind.PIXEL_RING) {
            if (kind == Kind.PIXEL_SPINNER) OrePixelIcons.loader(graphics, running ? OrePixelIcons.loadingFrame(Util.getMillis()) : 0,
                    getX() + 2, getY() + 9, 16, OreDrawing.TEXT);
            else {
                double raw = progress.getAsDouble();
                OrePixelIcons.progressRing(graphics, OrePixelIcons.progressFrame(raw),
                        getX() + 2, getY() + 9, 16, OreDrawing.TEXT);
            }
            OreDrawing.scrollingText(graphics, Minecraft.getInstance().font, getMessage(), getX() + 26, getY(), getRight(), getBottom(), OreDrawing.TEXT);
        } else if (kind == Kind.OUTLINED_BAR) {
            double raw = progress.getAsDouble();
            double value = Double.isFinite(raw) ? Mth.clamp(raw, 0, 1) : 0;
            OreDrawing.outline(graphics, getX(), getY(), width, 8, OreDrawing.TEXT);
            int fill = (int) Math.round(Math.max(0, width - 4) * value);
            graphics.fill(getX() + 2, getY() + 2, getX() + 2 + fill, getY() + 6, OreDrawing.TEXT);
            OreDrawing.scaledText(graphics, Minecraft.getInstance().font, Component.literal(Math.round(value * 100) + "%"),
                    getX(), getY() + 10, getRight(), getBottom(), OreDrawing.TEXT, OreDrawing.DESCRIPTION_TEXT);
        } else if (kind == Kind.BAR) {
            OreDrawing.scrollingText(graphics, Minecraft.getInstance().font, getMessage(), getX(), getY(), getRight(), getY() + 16, OreDrawing.TEXT);
            double value = progress.getAsDouble();
            OreDrawing.track(graphics, getX(), getY() + 23, width,
                    Double.isFinite(value) ? Mth.clamp(value, 0, 1) : 0, active);
        } else {
            int step = running ? (int) (Util.getMillis() / 100 % 8) : 0;
            for (int i = 0; i < 8; i++) {
                double angle = i * Math.PI / 4;
                int x = getX() + 15 + (int) (Math.cos(angle) * 10);
                int y = getY() + 15 + (int) (Math.sin(angle) * 10);
                graphics.fill(x - 2, y - 2, x + 2, y + 2, running && i == step ? 0xFFD2D4D6 : 0xFF48494A);
            }
            OreDrawing.scrollingText(graphics, Minecraft.getInstance().font, getMessage(), getX() + 36, getY(), getRight(), getBottom(), OreDrawing.TEXT);
        }
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
        if (kind == Kind.BAR || kind == Kind.OUTLINED_BAR || kind == Kind.PIXEL_RING) {
            double raw = progress.getAsDouble();
            long percent = Math.round((Double.isFinite(raw) ? Mth.clamp(raw, 0, 1) : 0) * 100);
            output.add(NarratedElementType.POSITION, Component.translatable("oreui.progress.value", percent));
        }
    }
}
