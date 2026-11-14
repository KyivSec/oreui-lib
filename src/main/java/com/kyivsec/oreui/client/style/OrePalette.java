package com.kyivsec.oreui.client.style;

public enum OrePalette {
    COLORED(0xFF387E24, 0xFF275B1A, 0xFF1C4A12, 0xFF50913D, 0xFF1D4D13, 0xFFFFFFFF),
    GENERAL(0xFFD0D1D5, 0xFFB1B2B6, 0xFFB1B2B6, 0xFFE3E3E5, 0xFF59595B, 0xFF1E1E20),
    DESTRUCTIVE(0xFFC93636, 0xFFC02D2D, 0xFFAD1D1D, 0xFFD04A4B, 0xFFAD1D1D, 0xFFFFFFFF),
    WARNING(0xFFE1B922, 0xFFC09A0D, 0xFFC09A0D, 0xFFE7C85B, 0xFFB87919, 0xFF1E1E20),
    REALMS(0xFF7345E5, 0xFF5D2CC6, 0xFF4A1CAC, 0xFF8E4AEB, 0xFF4A1CAC, 0xFFFFFFFF);

    private final int background;
    private final int hovered;
    private final int pressed;
    private final int stroke;
    private final int shadow;
    private final int text;

    OrePalette(int background, int hovered, int pressed, int stroke, int shadow, int text) {
        this.background = background;
        this.hovered = hovered;
        this.pressed = pressed;
        this.stroke = stroke;
        this.shadow = shadow;
        this.text = text;
    }

    public int background(boolean enabled, boolean hovered, boolean pressed) {
        return !enabled ? this.hovered : pressed ? this.pressed : hovered ? this.hovered : background;
    }

    public int stroke() {
        return stroke;
    }

    public int stroke(boolean hovered, boolean pressed) {
        if (this == COLORED && pressed) {
            return 0xFF6B935F;
        }
        if (hovered) {
            return switch (this) {
                case COLORED -> 0xFF6B935F;
                case DESTRUCTIVE -> 0xFFD98180;
                case REALMS -> 0xFF9242E2;
                default -> stroke;
            };
        }
        return stroke;
    }

    public int highlight(boolean hovered) {
        return this == REALMS ? hovered ? 0x66FAB0FF : 0x33FAB0FF : hovered ? 0x66FFFFFF : 0x33FFFFFF;
    }

    public int buttonHighlight(boolean hovered, boolean pressed) {
        return this == GENERAL ? 0x33FFFFFF : highlight(hovered || this == COLORED && pressed);
    }

    public int shadow() {
        return shadow;
    }

    public int text(boolean enabled) {
        return enabled ? text : (text & 0x00FFFFFF) | 0x80000000;
    }
}
