package com.kyivsec.oreui.client.widget;

import com.kyivsec.oreui.client.style.OreDrawing;
import java.util.Locale;
import java.util.Objects;
import java.util.function.DoubleSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.network.chat.Component;

/** Compact, display-only value/maximum bar; suppliers are sampled live without modifying game state. */
public final class OreStatBar extends AbstractWidget {
    public record Values(double current, double maximum) {
        public Values {
            maximum = Double.isFinite(maximum) && maximum > 0 ? maximum : 0;
            current = Double.isFinite(current) ? Math.clamp(current, 0, maximum) : 0;
        }
        public double fraction() { return maximum > 0 ? current / maximum : 0; }
    }

    private final DoubleSupplier current, maximum;
    private final int color;

    public OreStatBar(Component label, int color, DoubleSupplier current, DoubleSupplier maximum) {
        super(0, 0, 100, 20, label);
        this.color = color | 0xFF000000;
        this.current = Objects.requireNonNull(current);
        this.maximum = Objects.requireNonNull(maximum);
    }

    public Values values() { return new Values(current.getAsDouble(), maximum.getAsDouble()); }
    public int color() { return color; }

    private Component caption(Values values) {
        return Component.translatable("oreui.stat.value", getMessage(), number(values.current), number(values.maximum));
    }

    private static String number(double value) {
        return value == Math.rint(value) ? String.format(Locale.ROOT, "%.0f", value) : String.format(Locale.ROOT, "%.1f", value);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (width < 3 || height < 3) return;
        Values values = values();
        OreDrawing.inset(graphics, getX(), getY(), width, height);
        int filled = (int) Math.round((width - 2) * values.fraction());
        if (filled > 0) OreDrawing.bevel(graphics, getX() + 1, getY() + 1, filled, height - 2,
                color, shade(1.2), shade(0.75));
        var font = Minecraft.getInstance().font;
        Component text = caption(values);
        float scale = Math.min(OreDrawing.SMALL_TEXT, Math.min((width - 6.0F) / Math.max(1, font.width(text)),
                (height - 4.0F) / font.lineHeight));
        if (scale <= 0) return;
        graphics.enableScissor(getX() + 1, getY() + 1, getRight() - 1, getBottom() - 1);
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(getX() + (width - font.width(text) * scale) / 2,
                    getY() + (height - font.lineHeight * scale) / 2, 0);
            graphics.pose().scale(scale, scale, 1);
            graphics.drawString(font, text, 0, 0, OreDrawing.TEXT, true);
        } finally {
            graphics.pose().popPose();
            graphics.disableScissor();
        }
    }

    private int shade(double factor) {
        int red = Math.min(255, (int) ((color >> 16 & 255) * factor));
        int green = Math.min(255, (int) ((color >> 8 & 255) * factor));
        int blue = Math.min(255, (int) ((color & 255) * factor));
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    @Override public boolean mouseClicked(double x, double y, int button) { return false; }
    @Override public ComponentPath nextFocusPath(FocusNavigationEvent event) { return null; }
    @Override protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, caption(values()));
    }
}
