package com.kyivsec.oreui.client.hud;

import com.kyivsec.oreui.client.layout.OreGrid;
import net.minecraft.client.gui.navigation.ScreenRectangle;

public enum OreHudAnchor {
    TOP_LEFT(0, 0), TOP(1, 0), TOP_RIGHT(2, 0),
    LEFT(0, 1), CENTER(1, 1), RIGHT(2, 1),
    BOTTOM_LEFT(0, 2), BOTTOM(1, 2), BOTTOM_RIGHT(2, 2);

    private final int horizontal, vertical;
    OreHudAnchor(int horizontal, int vertical) { this.horizontal = horizontal; this.vertical = vertical; }

    public ScreenRectangle place(int screenWidth, int screenHeight, int width, int height,
                                 OreGrid.Insets safeArea, int offsetX, int offsetY) {
        int left = Math.min(safeArea.left(), Math.max(0, screenWidth - 1));
        int top = Math.min(safeArea.top(), Math.max(0, screenHeight - 1));
        int availableWidth = Math.max(1, screenWidth - left - safeArea.right());
        int availableHeight = Math.max(1, screenHeight - top - safeArea.bottom());
        width = Math.clamp(width, 1, availableWidth); height = Math.clamp(height, 1, availableHeight);
        int x = left + (availableWidth - width) * horizontal / 2 + offsetX;
        int y = top + (availableHeight - height) * vertical / 2 + offsetY;
        return new ScreenRectangle(Math.clamp(x, left, left + availableWidth - width),
                Math.clamp(y, top, top + availableHeight - height), width, height);
    }
}
