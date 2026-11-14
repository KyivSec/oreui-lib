package com.kyivsec.oreui.client.style;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;

/** Native user glyphs and their separately supplied vertical animation strips. */
public final class OrePixelIcons {
    public enum Icon {
        TICK(OreNativeIcons.CHECKMARK), CROSS(OreNativeIcons.CROSS), OFF(OreNativeIcons.OFF), ON(OreNativeIcons.ON),
        CHEVRON_DOWN(OreNativeIcons.CHEVRON_DOWN), ARROW_DOWN(OreNativeIcons.ARROW_DOWN),
        ARROW_RIGHT(OreNativeIcons.ARROW_RIGHT), ARROW_UP(OreNativeIcons.ARROW_UP),
        ARROW_LEFT(OreNativeIcons.ARROW_LEFT), CHEVRON_UP(OreNativeIcons.CHEVRON_UP),
        CHEVRON_LEFT(OreNativeIcons.CHEVRON_LEFT), CHEVRON_RIGHT(OreNativeIcons.CHEVRON_RIGHT), CIRCLE(OreNativeIcons.CIRCLE);

        private final OreNativeIcons nativeIcon;

        Icon(OreNativeIcons nativeIcon) { this.nativeIcon = nativeIcon; }
        public OreNativeIcons nativeIcon() { return nativeIcon; }
    }

    public static final int FRAME_SIZE = 15;
    public static final int FRAME_COUNT = 16;
    public static final int FRAME_DURATION_MILLIS = 100;
    public static final ResourceLocation LOADING_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "oreui_lib", "textures/gui/loading.png");
    public static final ResourceLocation PROGRESS_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "oreui_lib", "textures/gui/progress.png");
    /** Compatibility name for the loading strip; the retired shared atlas layout is no longer available. */
    @Deprecated
    public static final ResourceLocation TEXTURE = LOADING_TEXTURE;

    private OrePixelIcons() {
    }

    public static void draw(GuiGraphics graphics, Icon icon, int x, int y, int size, int color) {
        if (size < 7 && (icon == Icon.ARROW_UP || icon == Icon.ARROW_DOWN)) {
            // A hand-snapped 3x2 counterpart for the table's four-pixel sort slots, not a resampled arrow.
            if (size < 3) return;
            int left = x + (size - 3) / 2, top = y + (size - 2) / 2;
            boolean up = icon == Icon.ARROW_UP;
            graphics.fill(left + 1, top + (up ? 0 : 1), left + 2, top + (up ? 1 : 2), color);
            graphics.fill(left, top + (up ? 1 : 0), left + 3, top + (up ? 2 : 1), color);
        } else icon.nativeIcon.draw(graphics, x, y, size, color);
    }

    public static int loadingFrame(long elapsedMillis) {
        return Math.floorMod(Math.floorDiv(elapsedMillis, FRAME_DURATION_MILLIS), FRAME_COUNT);
    }

    /** Returns -1 for empty/invalid progress; supplied frames contain one through sixteen ring pixels. */
    public static int progressFrame(double progress) {
        if (!Double.isFinite(progress) || progress <= 0) return -1;
        return (int) Math.ceil(Math.min(1, progress) * FRAME_COUNT) - 1;
    }

    public static void loader(GuiGraphics graphics, int frame, int x, int y, int size, int color) {
        drawFrame(graphics, LOADING_TEXTURE, Math.floorMod(frame, FRAME_COUNT), x, y, size, color);
    }

    public static void progressRing(GuiGraphics graphics, int frame, int x, int y, int size, int color) {
        if (frame < 0) return;
        drawFrame(graphics, PROGRESS_TEXTURE, Math.min(frame, FRAME_COUNT - 1), x, y, size, color);
    }

    record AnimationPlacement(int offset, int size, int sourceInset, int sourceSize) {}

    static AnimationPlacement animationPlacement(int slot) {
        if (slot <= 0) throw new IllegalArgumentException("Animation slot must be positive");
        // A single fixed frame/window for the entire sequence prevents frame-dependent scaling or jitter.
        if (slot < 7) return new AnimationPlacement(0, slot, 4, 7);
        int size = FRAME_SIZE * Math.max(1, slot / 16);
        return new AnimationPlacement(Math.floorDiv(slot - size, 2), size, 0, FRAME_SIZE);
    }

    private static void drawFrame(GuiGraphics graphics, ResourceLocation texture, int frame, int x, int y, int size, int color) {
        if (size <= 0) return;
        AnimationPlacement placement = animationPlacement(size);
        Minecraft.getInstance().getTextureManager().getTexture(texture).setFilter(false, false);
        float[] previous = RenderSystem.getShaderColor().clone();
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        graphics.setColor((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F,
                (color & 255) / 255F, (color >>> 24) / 255F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        try {
            graphics.blit(texture, x + placement.offset, y + placement.offset, placement.size, placement.size,
                    placement.sourceInset, frame * FRAME_SIZE + placement.sourceInset,
                    placement.sourceSize, placement.sourceSize, FRAME_SIZE, FRAME_SIZE * FRAME_COUNT);
        } finally {
            graphics.setColor(previous[0], previous[1], previous[2], previous[3]);
            if (!blend) RenderSystem.disableBlend();
        }
    }
}
