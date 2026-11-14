package com.kyivsec.oreui.client.widget;

/** RGB/HSV conversion without a desktop graphics or rendering dependency. */
final class OreColorMath {
    record Hsv(double hue, double saturation, double brightness) {}

    private OreColorMath() {}

    static double normalized(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Color coordinates must be finite");
        return Math.clamp(value, 0, 1);
    }

    static Hsv hsv(int color) {
        double r = (color >> 16 & 255) / 255.0;
        double g = (color >> 8 & 255) / 255.0;
        double b = (color & 255) / 255.0;
        double max = Math.max(r, Math.max(g, b));
        double min = Math.min(r, Math.min(g, b));
        double delta = max - min;
        double hue = delta == 0 ? 0 : max == r ? (g - b) / delta
                : max == g ? (b - r) / delta + 2 : (r - g) / delta + 4;
        hue /= 6;
        if (hue < 0) hue += 1;
        return new Hsv(hue, max == 0 ? 0 : delta / max, max);
    }

    static int rgb(double hue, double saturation, double brightness) {
        double sector = normalized(hue) * 6;
        double s = normalized(saturation), v = normalized(brightness);
        double chroma = v * s;
        double x = chroma * (1 - Math.abs(sector % 2 - 1));
        double m = v - chroma;
        double r = 0, g = 0, b = 0;
        switch ((int) sector % 6) {
            case 0 -> { r = chroma; g = x; }
            case 1 -> { r = x; g = chroma; }
            case 2 -> { g = chroma; b = x; }
            case 3 -> { g = x; b = chroma; }
            case 4 -> { r = x; b = chroma; }
            case 5 -> { r = chroma; b = x; }
        }
        return 0xFF000000 | (int) Math.round((r + m) * 255) << 16
                | (int) Math.round((g + m) * 255) << 8 | (int) Math.round((b + m) * 255);
    }
}
