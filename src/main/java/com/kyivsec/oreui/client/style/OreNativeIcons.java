package com.kyivsec.oreui.client.style;

import com.kyivsec.oreui.OreUi;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;

/** User-authored pixel icons. Canvas size and ink bounds are deliberately separate. */
public enum OreNativeIcons {
    CHECKMARK(15, 4, 4, 8, 6), CROSS(15, 4, 4, 7, 7), CIRCLE(15, 4, 4, 7, 7),
    ON(15, 7, 5, 1, 5), OFF(15, 5, 5, 5, 5),
    CHEVRON_DOWN(15, 4, 6, 7, 4), CHEVRON_UP(15, 4, 6, 7, 4),
    CHEVRON_LEFT(15, 5, 4, 4, 7), CHEVRON_RIGHT(15, 6, 4, 4, 7),
    ARROW_DOWN(15, 4, 6, 7, 4), ARROW_UP(15, 4, 6, 7, 4),
    ARROW_LEFT(15, 5, 4, 4, 7), ARROW_RIGHT(15, 6, 4, 4, 7),
    PLUS(15, 4, 4, 7, 7), PLUS_SMALL(15, 5, 5, 5, 5),
    MINUS(15, 4, 7, 7, 1), MINUS_SMALL(15, 5, 7, 5, 1),
    SLIDERS(15, 3, 3, 9, 9), CARDS(15, 2, 2, 11, 11), MAIL(15, 2, 3, 11, 9),
    IMAGE(15, 2, 2, 11, 11), IMAGES(15, 1, 1, 13, 13),
    EXIT_RIGHT(15, 4, 3, 8, 9), EXIT_LEFT(15, 3, 3, 8, 9),
    WARNING(15, 3, 2, 9, 10), INFO(15, 3, 3, 9, 9),
    EXPORT(15, 2, 3, 11, 9), IMPORT(15, 2, 3, 11, 9),
    CLIPBOARD(15, 3, 3, 9, 9), BYTECODE(15, 3, 3, 9, 9), LANGUAGE(15, 3, 3, 9, 9),
    BRACKETS(15, 3, 4, 9, 7), HASHTAG(15, 5, 4, 5, 7), DRAG_HANDLE(15, 6, 5, 3, 5),
    QUESTION(15, 5, 4, 5, 7), ELLIPSIS_HORIZONTAL(15, 5, 7, 5, 1),
    PRESENT(15, 3, 2, 9, 11), LOOP(15, 3, 3, 9, 9), MENU(15, 4, 5, 7, 5),
    ELLIPSIS_VERTICAL(15, 7, 5, 1, 5), GRID(15, 6, 6, 3, 3), GRID_LARGE(15, 5, 5, 5, 5),
    HOME(16, 2, 2, 12, 12), USER(16, 4, 4, 8, 9), USERS(16, 2, 3, 12, 11),
    USER_ADD(16, 4, 4, 10, 11), SEARCH(16, 3, 3, 11, 11), PLAY(16, 5, 3, 7, 10),
    PENCIL(16, 4, 4, 8, 8), KEY(16, 4, 2, 8, 12), HAMMER(16, 4, 3, 8, 10),
    HANGER(16, 2, 2, 12, 12), BAG(16, 1, 1, 14, 14), HASHTAG_LARGE(16, 4, 3, 8, 10),
    DROPLETS(16, 5, 4, 6, 8), FILTER(16, 2, 2, 12, 12),
    ELLIPSIS_HORIZONTAL_LARGE(16, 3, 7, 10, 2), DRAG_HANDLE_LARGE(16, 5, 3, 6, 10),
    ELLIPSIS_VERTICAL_LARGE(16, 7, 3, 2, 10);

    public record Bounds(int x, int y, int width, int height) {}
    public record Placement(int x, int y, int width, int height) {}

    private static final Map<String, OreNativeIcons> BY_PATH = new HashMap<>();
    static {
        for (OreNativeIcons icon : values()) BY_PATH.put(icon.texture.getPath(), icon);
    }

    private final int canvasSize;
    private final Bounds ink;
    private final ResourceLocation texture;

    OreNativeIcons(int canvasSize, int x, int y, int width, int height) {
        this.canvasSize = canvasSize;
        ink = new Bounds(x, y, width, height);
        texture = ResourceLocation.fromNamespaceAndPath(OreUi.ID,
                "textures/icons/native/" + name().toLowerCase(Locale.ROOT) + ".png");
    }

    public int canvasSize() { return canvasSize; }
    public Bounds ink() { return ink; }
    public ResourceLocation texture() { return texture; }

    public Component label() {
        return Component.literal(name().toLowerCase(Locale.ROOT).replace('_', ' '));
    }

    public static OreNativeIcons fromTexture(ResourceLocation texture) {
        return texture.getNamespace().equals(OreUi.ID) ? BY_PATH.get(texture.getPath()) : null;
    }

    /** Relative ink placement in a square slot. Never enlarges individual glyphs to fill their padding. */
    public Placement placement(int slot) {
        if (slot <= 0) throw new IllegalArgumentException("Icon slot must be positive");
        // Both canvas families share one pixel pitch; e.g. a 30-pixel slot must not double only 15px icons.
        int scale = Math.max(1, slot / 16);
        int width = ink.width * scale, height = ink.height * scale;
        if (width <= slot && height <= slot) {
            int origin = Math.floorDiv(slot - canvasSize * scale, 2);
            int x = Math.clamp(origin + ink.x * scale, 0, slot - width);
            int y = Math.clamp(origin + ink.y * scale, 0, slot - height);
            return new Placement(x, y, width, height);
        }
        // Compatibility fallback for explicitly undersized slots; standard library slots avoid this path.
        double fit = slot / (double) Math.max(ink.width, ink.height);
        width = Math.max(1, (int) Math.round(ink.width * fit));
        height = Math.max(1, (int) Math.round(ink.height * fit));
        return new Placement((slot - width) / 2, (slot - height) / 2, width, height);
    }

    public void draw(GuiGraphics graphics, int x, int y, int slot, int color) {
        if (slot <= 0) return;
        Placement placement = placement(slot);
        Minecraft.getInstance().getTextureManager().getTexture(texture).setFilter(false, false);
        float[] previous = RenderSystem.getShaderColor().clone();
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        graphics.setColor((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F,
                (color & 255) / 255F, (color >>> 24) / 255F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        try {
            graphics.blit(texture, x + placement.x, y + placement.y, placement.width, placement.height,
                    ink.x, ink.y, ink.width, ink.height, canvasSize, canvasSize);
        } finally {
            graphics.setColor(previous[0], previous[1], previous[2], previous[3]);
            if (!blend) RenderSystem.disableBlend();
        }
    }
}
