package com.kyivsec.oreui.client.widget;

import com.kyivsec.oreui.client.layout.OreLayoutSizing;
import net.minecraft.client.gui.layouts.LinearLayout;

public final class OreColumn extends LinearLayout {
    public OreColumn() {
        super(0, 0, Orientation.VERTICAL);
        spacing(12);
    }

    public void setWidth(int width) {
        visitChildren(child -> OreLayoutSizing.width(child, width));
        arrangeElements();
    }
}
