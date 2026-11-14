package com.kyivsec.oreui.client.style;

import com.kyivsec.oreui.OreUi;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public enum OreIcons {
    ADD, ADJUST_HORIZONTAL3, ARROW_COUNTER_CLOCKWISE2, ARROW_DOWN_FILL, BINARY_CODE,
    BULLETPOINTED_LIST3, BUNDLE, CHECK, CHEVRON_DOWN, CHEVRON_LEFT, CHEVRON_RIGHT,
    CHEVRON_RIGTH_FILL, CHEVRON_UP, CLOSE, CURLY_BRACKETS, DOUBLE_DOTS2_GRID,
    EXCLAMATION_MARK_TRIANGLE, EXPORT, FILTER, FOREIGN_CHARACTERS, GARDROBE, HAMMER,
    HASHTAG, HOUSE, IMPORT, INFO_CIRCLE, KEY, LANDSCAPE, LANDSCAPE2, LAYOUT_HORIZONTAL2,
    LETTER, LOG_IN, LOG_OUT, MINUS, PACKAGE, PENCIL, PERSON, PERSON2, PERSON_ADD, PLAY,
    PRESENT, QUESTION_MARK, SEARCH, SPARKLES3, TRIPLE_DOTS_HORIZONTAL, TRIPLE_DOTS_VERTICAL,
    TRIPLE_DOTS_VERTICAL2;

    private static final Map<String, OreIcons> BY_NAME = new HashMap<>();

    static {
        for (OreIcons icon : values()) BY_NAME.put(icon.name().toLowerCase(Locale.ROOT), icon);
    }

    public record Raster(ResourceLocation texture, int size) {}

    public static Raster raster(ResourceLocation texture, int pixelSize) {
        OreNativeIcons nativeIcon = OreNativeIcons.fromTexture(texture);
        if (nativeIcon != null) return new Raster(nativeIcon.texture(), nativeIcon.canvasSize());
        if (!texture.getNamespace().equals(OreUi.ID)) return null;
        String path = texture.getPath();
        if (!path.startsWith("textures/icons/") || !path.endsWith(".png")) return null;
        String name = path.substring("textures/icons/".length(), path.length() - 4);
        int suffix = name.lastIndexOf('_');
        if (suffix >= 0 && name.substring(suffix + 1).matches("8|16|24|32|48")) name = name.substring(0, suffix);
        OreIcons icon = BY_NAME.get(name);
        if (icon == null) return null;
        OreNativeIcons replacement = icon.nativeIcon();
        return new Raster(icon.texture(), replacement == null ? 16 : replacement.canvasSize());
    }

    public ResourceLocation texture() {
        OreNativeIcons icon = nativeIcon();
        return icon == null ? ResourceLocation.fromNamespaceAndPath(OreUi.ID, "textures/icons/package_16.png") : icon.texture();
    }

    /** Preserves established names while routing defaults to the user's native pixel artwork. */
    public OreNativeIcons nativeIcon() {
        return switch (this) {
            case ADD -> OreNativeIcons.PLUS;
            case ADJUST_HORIZONTAL3 -> OreNativeIcons.SLIDERS;
            case ARROW_COUNTER_CLOCKWISE2 -> OreNativeIcons.LOOP;
            case ARROW_DOWN_FILL -> OreNativeIcons.ARROW_DOWN;
            case BINARY_CODE -> OreNativeIcons.BYTECODE;
            case BULLETPOINTED_LIST3 -> OreNativeIcons.MENU;
            case BUNDLE -> OreNativeIcons.BAG;
            case CHECK -> OreNativeIcons.CHECKMARK;
            case CHEVRON_DOWN -> OreNativeIcons.CHEVRON_DOWN;
            case CHEVRON_LEFT -> OreNativeIcons.CHEVRON_LEFT;
            case CHEVRON_RIGHT -> OreNativeIcons.CHEVRON_RIGHT;
            case CHEVRON_RIGTH_FILL -> OreNativeIcons.ARROW_RIGHT;
            case CHEVRON_UP -> OreNativeIcons.CHEVRON_UP;
            case CLOSE -> OreNativeIcons.CROSS;
            case CURLY_BRACKETS -> OreNativeIcons.BRACKETS;
            case DOUBLE_DOTS2_GRID -> OreNativeIcons.GRID;
            case EXCLAMATION_MARK_TRIANGLE -> OreNativeIcons.WARNING;
            case EXPORT -> OreNativeIcons.EXPORT;
            case FILTER -> OreNativeIcons.FILTER;
            case FOREIGN_CHARACTERS -> OreNativeIcons.LANGUAGE;
            case GARDROBE -> OreNativeIcons.HANGER;
            case HAMMER -> OreNativeIcons.HAMMER;
            case HASHTAG -> OreNativeIcons.HASHTAG;
            case HOUSE -> OreNativeIcons.HOME;
            case IMPORT -> OreNativeIcons.IMPORT;
            case INFO_CIRCLE -> OreNativeIcons.INFO;
            case KEY -> OreNativeIcons.KEY;
            case LANDSCAPE -> OreNativeIcons.IMAGE;
            case LANDSCAPE2 -> OreNativeIcons.IMAGES;
            case LAYOUT_HORIZONTAL2 -> OreNativeIcons.CARDS;
            case LETTER -> OreNativeIcons.MAIL;
            case LOG_IN -> OreNativeIcons.EXIT_LEFT;
            case LOG_OUT -> OreNativeIcons.EXIT_RIGHT;
            case MINUS -> OreNativeIcons.MINUS;
            case PACKAGE -> null; // No replacement box glyph was supplied; retain the existing asset.
            case PENCIL -> OreNativeIcons.PENCIL;
            case PERSON -> OreNativeIcons.USER;
            case PERSON2 -> OreNativeIcons.USERS;
            case PERSON_ADD -> OreNativeIcons.USER_ADD;
            case PLAY -> OreNativeIcons.PLAY;
            case PRESENT -> OreNativeIcons.PRESENT;
            case QUESTION_MARK -> OreNativeIcons.QUESTION;
            case SEARCH -> OreNativeIcons.SEARCH;
            case SPARKLES3 -> OreNativeIcons.DROPLETS;
            case TRIPLE_DOTS_HORIZONTAL -> OreNativeIcons.ELLIPSIS_HORIZONTAL;
            case TRIPLE_DOTS_VERTICAL -> OreNativeIcons.ELLIPSIS_VERTICAL;
            case TRIPLE_DOTS_VERTICAL2 -> OreNativeIcons.DRAG_HANDLE;
        };
    }

    /** Compatibility overload: drawing size is now separate from the native texture's canvas. */
    public ResourceLocation texture(int size) {
        if (size != 8 && size != 16 && size != 24 && size != 32 && size != 48) {
            throw new IllegalArgumentException("Icon size must be 8, 16, 24, 32, or 48");
        }
        return texture();
    }

    public Component label() {
        return Component.literal(name().toLowerCase(Locale.ROOT).replace('_', ' '));
    }
}
