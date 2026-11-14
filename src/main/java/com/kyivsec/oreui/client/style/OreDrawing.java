package com.kyivsec.oreui.client.style;

import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.systems.RenderSystem;

public final class OreDrawing {
    public static final int BACKGROUND = 0xFF1E1E1F;
    public static final int PANEL = 0xFF313233;
    public static final int HEADER = 0xFF48494A;
    public static final int INSET = 0xFF242425;
    public static final int TEXT = 0xFFFFFFFF;
    public static final int MUTED = 0xFFBFC0C3;
    public static final int NOTIFICATION = 0xFF2E6BE5;
    public static final float SMALL_TEXT = 8.0F / 9.0F;
    public static final float DESCRIPTION_TEXT = 6.0F / 9.0F;
    public static final float PAGINATION_TEXT = 7.5F / 9.0F;

    private OreDrawing() {
    }

    public static void scrollingText(GuiGraphics graphics, Font font, Component text,
                                     int left, int top, int right, int bottom, int color) {
        if (right <= left || bottom <= top) {
            return;
        }
        int textWidth = font.width(text);
        int overflow = textWidth - (right - left);
        int y = top + (bottom - top - font.lineHeight) / 2 + 1;
        if (overflow <= 0) {
            graphics.drawString(font, text, left + (right - left - textWidth) / 2, y, color, false);
            return;
        }
        int offset = scrollingOffset(overflow);
        graphics.enableScissor(left, top, right, bottom);
        try {
            graphics.drawString(font, text, left - offset, y, color, false);
        } finally {
            graphics.disableScissor();
        }
    }

    public static void outline(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        if (width <= 0 || height <= 0) {
            return;
        }
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    public static void panel(GuiGraphics graphics, int x, int y, int width, int height) {
        surface(graphics, x, y, width, height, PANEL, 0xFF515453, 0x20FFFFFF);
    }

    public static void surface(GuiGraphics graphics, int x, int y, int width, int height, int fill, int stroke, int highlight) {
        graphics.fill(x, y, x + width, y + height, fill);
        outline(graphics, x, y, width, height, BACKGROUND);
        outline(graphics, x + 1, y + 1, width - 2, height - 2, stroke);
        if (width > 4 && height > 4) {
            graphics.fill(x + 2, y + 2, x + width - 2, y + 3, highlight);
            graphics.fill(x + 2, y + 2, x + 3, y + height - 2, highlight);
        }
    }

    public static void inset(GuiGraphics graphics, int x, int y, int width, int height) {
        field(graphics, x, y, width, height, true, false);
    }

    // Figma surfaces use directional edges, not concentric generic panel borders.
    public static void bevel(GuiGraphics graphics, int x, int y, int width, int height,
                             int fill, int light, int dark) {
        if (width < 1 || height < 1) return;
        graphics.fill(x, y, x + width, y + height, fill);
        graphics.fill(x, y, x + width, y + 1, light);
        graphics.fill(x, y + 1, x + 1, y + height, light);
        graphics.fill(x + 1, y + height - 1, x + width, y + height, dark);
        graphics.fill(x + width - 1, y + 1, x + width, y + height, dark);
    }

    public static void field(GuiGraphics graphics, int x, int y, int width, int height,
                             boolean enabled, boolean focused) {
        graphics.fill(x, y, x + width, y + height, enabled ? BACKGROUND : 0xFF5A5A5A);
        bevel(graphics, x + 1, y + 1, width - 2, height - 2,
                enabled ? PANEL : 0xFFC2C4C6, enabled ? INSET : 0xFF8F9194,
                enabled ? INSET : 0xFF8F9194);
        if (enabled && focused) buttonFocus(graphics, x, y, width, height);
    }

    public static void track(GuiGraphics graphics, int x, int y, int width, double value, boolean enabled) {
        if (width < 3) return;
        graphics.fill(x, y, x + width, y + 7, BACKGROUND);
        bevel(graphics, x + 1, y + 1, width - 2, 5, 0xFF8C8D90, 0xFFA3A4A6, 0xFF6D6D6E);
        int filled = (int) Math.round((width - 2) * Math.max(0, Math.min(1, value)));
        if (filled > 0) bevel(graphics, x + 1, y + 1, filled, 5,
                enabled ? 0xFF387E24 : 0xFFB1B2B5, enabled ? 0xFF50913D : 0xFF8C8D90,
                enabled ? 0xFF50913D : 0xFF8C8D90);
    }

    public static void scaledText(GuiGraphics graphics, Font font, Component text,
                                  int left, int top, int right, int bottom, int color, float scale) {
        if (right <= left || bottom <= top) return;
        int logicalWidth = (int) ((right - left) / scale);
        int logicalHeight = (int) ((bottom - top) / scale);
        int textWidth = font.width(text);
        int overflow = textWidth - logicalWidth;
        // Scissors use screen coordinates, regardless of the current pose transform.
        if (overflow > 0) graphics.enableScissor(left, top, right, bottom);
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(left, top, 0);
            graphics.pose().scale(scale, scale, 1);
            int x = overflow > 0 ? -scrollingOffset(overflow) : (logicalWidth - textWidth) / 2;
            int y = (logicalHeight - font.lineHeight) / 2 + 1;
            graphics.drawString(font, text, x, y, color, false);
        } finally {
            graphics.pose().popPose();
            if (overflow > 0) graphics.disableScissor();
        }
    }

    private static int scrollingOffset(int overflow) {
        double period = Math.max(3.0, overflow * 0.5);
        double phase = Util.getMillis() / 1000.0 * Math.PI * 2.0 / period;
        return (int) (overflow * (0.5 + 0.5 * Math.sin(Math.PI * 0.5 * Math.cos(phase))));
    }

    public static void icon(GuiGraphics graphics, ResourceLocation texture, int x, int y, int size, int color) {
        OreIcons.Raster resolved = OreIcons.raster(texture, size);
        if (resolved != null) texture = resolved.texture();
        OreNativeIcons nativeIcon = OreNativeIcons.fromTexture(texture);
        if (nativeIcon != null) {
            nativeIcon.draw(graphics, x, y, size, color);
            return;
        }
        image(graphics, texture, x, y, size, size, 16, 16, color);
    }

    public static void image(GuiGraphics graphics, ResourceLocation texture, int x, int y,
                             int width, int height, int sourceWidth, int sourceHeight, int color) {
        Minecraft minecraft = Minecraft.getInstance();
        OreIcons.Raster raster = OreIcons.raster(texture,
                (int) Math.ceil(Math.max(width, height) * minecraft.getWindow().getGuiScale()));
        if (raster != null) {
            texture = raster.texture();
            sourceWidth = sourceHeight = raster.size();
            minecraft.getTextureManager().getTexture(texture).setFilter(false, false);
        }
        graphics.setColor((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F,
                (color & 255) / 255F, (color >>> 24) / 255F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        try {
            graphics.blit(texture, x, y, width, height, 0, 0, sourceWidth, sourceHeight, sourceWidth, sourceHeight);
        } finally {
            RenderSystem.disableBlend();
            graphics.setColor(1, 1, 1, 1);
        }
    }

    public static void focus(GuiGraphics graphics, int x, int y, int width, int height) {
        outline(graphics, x - 2, y - 2, width + 4, height + 4, TEXT);
    }

    public static void button(GuiGraphics graphics, int x, int y, int width, int height,
                              OrePalette palette, boolean enabled, boolean hovered, boolean pressed, boolean focused) {
        int depth = pressed ? 0 : 2;
        graphics.fill(x, y, x + width, y + height, BACKGROUND);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, enabled ? palette.shadow() : 0xFF373737);
        surface(graphics, x, y, width, height - depth, enabled ? palette.background(true, hovered, pressed) : 0xFF5A5A5A,
                enabled ? palette.stroke(hovered, pressed) : 0xFF777777, enabled ? palette.highlight(hovered) : 0x22FFFFFF);
        if (focused && enabled) {
            focus(graphics, x, y, width, height);
        }
    }

    public static ScreenRectangle buttonFaceBounds(int x, int y, int width, int height, boolean enabled, boolean pressed) {
        int top = enabled && pressed ? 4 : 1;
        int bottom = enabled && !pressed ? 3 : 1;
        return new ScreenRectangle(x + 1, y + top, Math.max(0, width - 2), Math.max(0, height - top - bottom));
    }

    public static void buttonFocus(GuiGraphics graphics, int x, int y, int width, int height) {
        outline(graphics, x - 1, y - 1, width + 2, height + 2, TEXT);
    }

    public static void raisedButton(GuiGraphics graphics, int x, int y, int width, int height,
                                    OrePalette palette, boolean enabled, boolean hovered, boolean pressed) {
        if (width < 3 || height < 6) {
            return;
        }
        ScreenRectangle face = buttonFaceBounds(x, y, width, height, enabled, pressed);
        graphics.fill(x, enabled && pressed ? y + 3 : y, x + width, y + height, BACKGROUND);
        if (enabled && !pressed) {
            graphics.fill(face.left(), face.bottom(), face.right(), y + height - 1, palette.shadow());
        }
        graphics.fill(face.left(), face.top(), face.right(), face.bottom(), palette.background(enabled, hovered, pressed));
        int stroke = palette.stroke(enabled && hovered, enabled && pressed);
        graphics.fill(face.left(), face.bottom() - 1, face.right(), face.bottom(), stroke);
        graphics.fill(face.right() - 1, face.top(), face.right(), face.bottom(), stroke);
        int highlight = palette.buttonHighlight(enabled && hovered, enabled && pressed);
        graphics.fill(face.left(), face.top(), face.right() - 1, face.top() + 1, highlight);
        graphics.fill(face.left(), face.top() + 1, face.left() + 1, face.bottom() - 1, highlight);
    }
}
