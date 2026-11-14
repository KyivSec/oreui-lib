package com.kyivsec.oreui.client.style;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;

public final class OreTooltip {
    /** The side on which the tooltip body sits relative to its target. */
    public enum Side { TOP, BOTTOM, LEFT, RIGHT }

    private OreTooltip() {
    }

    public static void render(GuiGraphics graphics, Font font, List<FormattedCharSequence> lines,
                              int x, int y, int width, Side side) {
        if (lines.isEmpty() || width < 12) return;
        int height = lines.size() * 8 + 4;
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(0, 0, 400);
            // Expand the actual stepped silhouette so its tail receives the same one-pixel outline.
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) silhouette(graphics, x + dx, y + dy, width, height, side, OreDrawing.BACKGROUND);
            }
            silhouette(graphics, x, y, width, height, side, OreDrawing.NOTIFICATION);
            graphics.enableScissor(x + 3, y, x + width - 3, y + height);
            try {
                graphics.pose().translate(x + 5, y + 3, 0);
                graphics.pose().scale(OreDrawing.DESCRIPTION_TEXT, OreDrawing.DESCRIPTION_TEXT, 1);
                for (int i = 0; i < lines.size(); i++) graphics.drawString(font, lines.get(i), 0, i * 12, OreDrawing.TEXT, false);
            } finally {
                graphics.disableScissor();
            }
        } finally {
            graphics.pose().popPose();
        }
    }

    private static void silhouette(GuiGraphics graphics, int x, int y, int width, int height, Side side, int color) {
        graphics.fill(x + 2, y, x + width - 2, y + height, color);
        graphics.fill(x, y + 2, x + width, y + height - 2, color);
        for (int i = 0; i < 3; i++) {
            int size = 6 - i * 2;
            switch (side) {
                case TOP -> graphics.fill(x + width - 12 + i * 2, y + height + i * 2,
                        x + width - 12 + i * 2 + size, y + height + i * 2 + 2, color);
                case BOTTOM -> graphics.fill(x + width - 12 + i * 2, y - 2 - i * 2,
                        x + width - 12 + i * 2 + size, y - i * 2, color);
                case LEFT -> graphics.fill(x + width + i * 2, y + 2, x + width + i * 2 + 2, y + 2 + size, color);
                case RIGHT -> graphics.fill(x - 2 - i * 2, y + 2, x - i * 2, y + 2 + size, color);
            }
        }
    }
}
