package com.kyivsec.oreui.client.style;

import java.util.Objects;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

/** Opt-in font resources; never overrides Minecraft's global default font. */
public final class OreFonts {
    private OreFonts() {}

    /** A native Font-compatible view using the selected resource for default-font text. */
    public static Font font(Font base, ResourceLocation fontId) {
        Objects.requireNonNull(base);
        Objects.requireNonNull(fontId);
        if (fontId.equals(Style.DEFAULT_FONT)) return base;
        return new OreResourceFont(base, fontId);
    }

    static Style resolveStyle(Style style, ResourceLocation defaultFont) {
        ResourceLocation selected = style.getFont().equals(Style.DEFAULT_FONT) ? defaultFont : style.getFont();
        return style.withFont(selected);
    }
}
