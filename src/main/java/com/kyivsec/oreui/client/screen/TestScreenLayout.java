package com.kyivsec.oreui.client.screen;

import com.kyivsec.oreui.client.layout.OreGrid;
import net.minecraft.client.gui.layouts.SpacerElement;

record TestScreenLayout(int navigationX, int navigationWidth, int x, int width, int top, int height) {
    static TestScreenLayout of(int screenWidth, int screenHeight) {
        int groupWidth = Math.max(1, Math.min(1104, screenWidth - 24));
        int navigationWidth = Math.max(88, Math.min(132, screenWidth / 5));
        OreGrid grid = new OreGrid(1, 2).gaps(12, 0)
                .column(0, OreGrid.Track.fixed(navigationWidth)).column(1, OreGrid.Track.flex(1, 1, Integer.MAX_VALUE))
                .row(0, OreGrid.Track.flex(1));
        var navigation = grid.add(new SpacerElement(0, 0), 0, 0);
        var content = grid.add(new SpacerElement(0, 0), 0, 1);
        grid.setSize(groupWidth, Math.max(1, screenHeight - 84));
        grid.setPosition((screenWidth - groupWidth) / 2, 54);
        var navBounds = grid.cellBounds(navigation);
        var bodyBounds = grid.cellBounds(content);
        return new TestScreenLayout(navBounds.left(), navBounds.width(), bodyBounds.left(), bodyBounds.width(), bodyBounds.top(), bodyBounds.height());
    }

}
