package com.kyivsec.oreui.client.widget;

import com.kyivsec.oreui.client.style.OreDrawing;
import com.kyivsec.oreui.client.style.OreIcons;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.network.chat.Component;

public class OreLabel extends AbstractWidget {
    public enum Decoration {
        PLAIN, TAG, BADGE, INDICATOR, DIVIDER, BANNER
    }

    private final Font font;
    private MultiLineLabel lines;
    private Decoration decoration = Decoration.PLAIN;
    private int color = 0xFF9DDF77;
    private boolean colorSwatch;
    private float plainTextScale = 1;
    private boolean textCentered;

    /** Centers the plain text block within the label, independently of layout-cell alignment. */
    public void setTextCentered(boolean centered) { textCentered = centered; }

    int plainTextX(int textWidth) {
        return getX() + (textCentered ? (width - textWidth) / 2 : 0);
    }

    public void setPlainTextScale(float scale) {
        if (!Float.isFinite(scale) || scale <= 0) throw new IllegalArgumentException("Text scale must be positive and finite");
        plainTextScale = scale;
        remeasure();
    }

    public OreLabel colorSwatch() {
        colorSwatch = true;
        return this;
    }

    public OreLabel(Font font, Component text, int width) {
        super(0, 0, width, 1, text);
        this.font = font;
        remeasure();
    }

    public OreLabel decorate(Decoration decoration, int color) {
        this.decoration = decoration;
        this.color = color;
        remeasure();
        return this;
    }

    private void remeasure() {
        if (font != null) {
            int padding = switch (decoration) {
                case PLAIN -> 0;
                case INDICATOR -> 12;
                case BANNER -> 34;
                default -> 10;
            };
            lines = MultiLineLabel.create(font, getMessage(), Math.max(1, (int) ((width - padding) / textScale())));
            int textHeight = (int) Math.ceil(lines.getLineCount() * font.lineHeight * textScale());
            height = switch (decoration) {
                case PLAIN, INDICATOR -> Math.max(4, textHeight);
                case BANNER -> Math.max(32, textHeight + 14);
                case DIVIDER -> getMessage().getString().isEmpty() ? 2 : Math.max(15, textHeight + 7);
                default -> Math.max(15, textHeight + 7);
            };
        }
    }

    private float textScale() {
        return decoration == Decoration.PLAIN ? plainTextScale : decoration == Decoration.BANNER ? 1 : OreDrawing.SMALL_TEXT;
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        remeasure();
    }

    @Override
    public void setMessage(Component message) {
        super.setMessage(message);
        remeasure();
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int textWidth = (int) Math.ceil(lines.getWidth() * textScale());
        int tagWidth = Math.min(width, Math.max(15, textWidth + 10));
        boolean filled = decoration == Decoration.TAG || decoration == Decoration.BADGE || decoration == Decoration.DIVIDER;
        if (decoration == Decoration.DIVIDER) {
            graphics.fill(getX(), getBottom() - 2, getRight(), getBottom(), color);
            if (getMessage().getString().isEmpty()) return;
        }
        if (filled) {
            graphics.fill(getX(), getY(), getX() + tagWidth, getBottom(), color);
            if (decoration == Decoration.TAG) OreDrawing.outline(graphics, getX(), getY(), tagWidth, height, OreDrawing.BACKGROUND);
        } else if (decoration == Decoration.BANNER) {
            OreDrawing.field(graphics, getX(), getY(), width, height, true, false);
            int iconY = getY() + (height - 12) / 2;
            if (colorSwatch) {
                for (int row = 0; row < 3; row++) {
                    for (int col = 0; col < 3; col++) {
                        graphics.fill(getX() + 7 + col * 4, iconY + row * 4, getX() + 11 + col * 4, iconY + 4 + row * 4,
                                (row + col) % 2 == 0 ? 0xFFD2D4D6 : 0xFF8C8D90);
                    }
                }
                graphics.fill(getX() + 7, iconY, getX() + 19, iconY + 12, color);
            } else OreDrawing.icon(graphics, OreIcons.INFO_CIRCLE.texture(), getX() + 7, iconY, 12, OreDrawing.TEXT);
        } else if (decoration == Decoration.INDICATOR) {
            int y = getY() + (height - 4) / 2;
            graphics.fill(getX(), y, getX() + 4, y + 4, color);
        }
        int inset = switch (decoration) {
            case PLAIN -> 0;
            case INDICATOR -> 12;
            case BANNER -> 27;
            default -> 5;
        };
        int brightness = (color >> 16 & 255) * 299 + (color >> 8 & 255) * 587 + (color & 255) * 114;
        int textColor = filled && color >>> 24 >= 128 && brightness >= 150000 ? 0xFF1E1E20 : OreDrawing.TEXT;
        int textHeight = (int) Math.ceil(lines.getLineCount() * font.lineHeight * textScale());
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(decoration == Decoration.PLAIN ? plainTextX(textWidth) : getX() + inset,
                    getY() + (height - textHeight) / 2, 0);
            graphics.pose().scale(textScale(), textScale(), 1);
            lines.renderLeftAlignedNoShadow(graphics, 0, 0, font.lineHeight, textColor);
        } finally {
            graphics.pose().popPose();
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
    }
}
