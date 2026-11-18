package com.kyivsec.oreui.client.widget;

import com.kyivsec.oreui.client.style.OreDrawing;
import com.kyivsec.oreui.client.style.OrePalette;
import java.util.Objects;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;

public class OreCard extends OreButton {
    public static final int PREVIEW_CAPTION_HEIGHT = 22;

    @FunctionalInterface
    public interface PreviewRenderer {
        void render(GuiGraphics graphics, ScreenRectangle bounds, int mouseX, int mouseY, float partialTick);
    }

    private final Font font;
    private final Component body;
    private MultiLineLabel lines;
    private PreviewRenderer previewRenderer;
    private int preferredPreviewHeight;

    public OreCard(Font font, Component title, Component body, int accent, OnPress onPress) {
        super(0, 0, 100, 80, title, OrePalette.GENERAL, onPress);
        this.font = font;
        this.body = body;
        // Accent is retained in the public constructor for compatibility. Figma cards are neutral.
        setWidth(100);
    }

    /** One selectable/focusable card with clipped caller content above a caption, suitable for future 3D previews. */
    public OreCard(Font font, Component title, int previewHeight, PreviewRenderer previewRenderer, OnPress onPress) {
        this(font, title, Component.empty(), 0, onPress);
        if (previewHeight < 1) throw new IllegalArgumentException("Preview height must be positive");
        this.previewRenderer = Objects.requireNonNull(previewRenderer);
        preferredPreviewHeight = previewHeight;
        setWidth(getWidth());
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        if (previewRenderer != null) {
            setHeight(preferredPreviewHeight + PREVIEW_CAPTION_HEIGHT + 2);
            return;
        }
        lines = MultiLineLabel.create(font, body, Math.max(1, (int) ((width - 14) / OreDrawing.DESCRIPTION_TEXT)));
        setHeight(Math.max(32, 22 + lines.getLineCount() * 6));
    }

    /** Screen-space bounds recomputed from the current card size, not cached at construction. */
    public ScreenRectangle previewBounds() {
        return new ScreenRectangle(getX() + 1, getY() + 1, Math.max(0, width - 2),
                Math.max(0, height - PREVIEW_CAPTION_HEIGHT - 2));
    }

    public ScreenRectangle captionBounds() {
        var preview = previewBounds();
        return new ScreenRectangle(preview.left(), preview.bottom(), preview.width(),
                Math.max(0, getBottom() - 1 - preview.bottom()));
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!active || !isFocused()) cancelPress();
        if (previewRenderer != null) {
            renderPreviewCard(graphics, mouseX, mouseY, partialTick);
            return;
        }
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

    private void renderPreviewCard(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean hot = active && (isHovered() || isFocused());
        int edge = previewBorderColor();
        graphics.fill(getX(), getY(), getRight(), getBottom(), OreDrawing.INSET);
        var preview = previewBounds();
        if (preview.width() > 0 && preview.height() > 0) {
            graphics.enableScissor(preview.left(), preview.top(), preview.right(), preview.bottom());
            graphics.pose().pushPose();
            try {
                previewRenderer.render(graphics, preview, mouseX, mouseY, partialTick);
            } finally {
                try { graphics.flush(); }
                finally { graphics.pose().popPose(); graphics.disableScissor(); }
            }
        }
        var caption = captionBounds();
        int fill = isSelected() ? isPressed() ? 0xFF275B1A : 0xFF387E24 : hot ? 0xFF515453 : OreDrawing.HEADER;
        graphics.fill(caption.left(), caption.top(), caption.right(), caption.bottom(), fill);
        graphics.fill(caption.left(), caption.top(), caption.right(), caption.top() + 1, edge);
        OreDrawing.scaledText(graphics, font, getMessage(), caption.left() + 4, caption.top() + 1,
                caption.right() - 4, caption.bottom(), active ? OreDrawing.TEXT : OreDrawing.MUTED, OreDrawing.DESCRIPTION_TEXT);
        // This is the card's permanent frame, not its separate keyboard-focus outline.
        // Paint last, with the same visible color on every edge; bevel shading hid the right/bottom edges.
        paintPreviewBorder(graphics::fill);
        renderOutline(graphics);
    }

    private int previewBorderColor() {
        return isSelected() ? 0xFF50913D : active && (isHovered() || isFocused()) ? 0xFF8C8D90 : 0xFF5A5B5C;
    }

    @FunctionalInterface
    interface RectangleFill {
        void fill(int left, int top, int right, int bottom, int color);
    }

    /** Shared by the actual draw path and headless pixel-coverage tests. */
    void paintPreviewBorder(RectangleFill painter) {
        if (width <= 0 || height <= 0) return;
        int color = previewBorderColor();
        painter.fill(getX(), getY(), getRight(), getY() + 1, color);
        painter.fill(getX(), getBottom() - 1, getRight(), getBottom(), color);
        painter.fill(getX(), getY(), getX() + 1, getBottom(), color);
        painter.fill(getRight() - 1, getY(), getRight(), getBottom(), color);
    }
}
