package com.kyivsec.oreui.client.widget;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.layouts.LinearLayout;

public final class OreColumn extends LinearLayout {
    public OreColumn() {
        super(0, 0, Orientation.VERTICAL);
        spacing(12);
    }

    public void setWidth(int width) {
        visitChildren(child -> {
            if (child instanceof AbstractWidget widget) {
                widget.setWidth(width);
            } else if (child instanceof OreNavigationBar navigation) {
                navigation.setWidth(width);
            } else if (child instanceof OrePagination pagination) {
                pagination.setWidth(width);
            } else if (child instanceof OreSection section) {
                section.setWidth(width);
            } else if (child instanceof OreImageCard card) {
                card.setWidth(width);
            } else if (child instanceof OreColorEditor color) {
                color.setWidth(width);
            } else if (child instanceof OreNavBar navbar) {
                navbar.setWidth(width);
            }
        });
        arrangeElements();
    }
}
