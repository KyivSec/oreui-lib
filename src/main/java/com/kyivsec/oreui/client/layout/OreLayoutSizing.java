package com.kyivsec.oreui.client.layout;

import com.kyivsec.oreui.client.widget.OreColorEditor;
import com.kyivsec.oreui.client.widget.OreColumn;
import com.kyivsec.oreui.client.widget.OreImageCard;
import com.kyivsec.oreui.client.widget.OreNavBar;
import com.kyivsec.oreui.client.widget.OreNavigationBar;
import com.kyivsec.oreui.client.widget.OrePagination;
import com.kyivsec.oreui.client.widget.OreSection;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutElement;

/** Width propagation for native widgets and Ore composite layouts; unknown layouts keep their intrinsic size. */
public final class OreLayoutSizing {
    private OreLayoutSizing() {}

    public static void width(LayoutElement element, int width) {
        width = Math.max(0, width);
        if (element instanceof AbstractWidget widget) widget.setWidth(width);
        else if (element instanceof OreGrid grid) grid.setWidth(width);
        else if (element instanceof OreMenuLayout menu) menu.setWidth(width);
        else if (element instanceof OreColumn column) column.setWidth(width);
        else if (element instanceof OreSection section) section.setWidth(width);
        else if (element instanceof OreNavigationBar navigation) navigation.setWidth(width);
        else if (element instanceof OrePagination pagination) pagination.setWidth(width);
        else if (element instanceof OreImageCard card) card.setWidth(width);
        else if (element instanceof OreColorEditor color) color.setWidth(width);
        else if (element instanceof OreNavBar navbar) navbar.setWidth(width);
        else if (element instanceof Layout layout) layout.arrangeElements();
    }

    public static void height(LayoutElement element, int height) {
        if (element instanceof AbstractWidget widget) widget.setHeight(Math.max(0, height));
        else if (element instanceof OreGrid grid) grid.setHeight(height);
        else if (element instanceof OreMenuLayout menu) menu.setHeight(height);
        else if (element instanceof OreNavigationBar navigation) navigation.fitHeight(Math.max(0, height));
    }
}
