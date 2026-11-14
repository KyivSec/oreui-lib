package com.kyivsec.oreui.client.host;

import com.kyivsec.oreui.client.style.OreDrawing;
import com.kyivsec.oreui.client.widget.OreButton;
import com.kyivsec.oreui.client.widget.OreColorSquare;
import com.kyivsec.oreui.client.widget.OreEditBox;
import com.kyivsec.oreui.client.widget.OreHueSlider;
import com.kyivsec.oreui.client.widget.OreScrollbar;
import com.kyivsec.oreui.client.widget.OreSlider;
import com.kyivsec.oreui.client.widget.OreTable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;

/** Transient input/focus behavior shared by screen, grid and HUD hosts. */
public final class OreWidgetSupport {
    private OreWidgetSupport() {}

    public static void cancel(AbstractWidget widget) {
        if (widget instanceof OreButton button) button.cancelPress();
        if (widget instanceof OreColorSquare square) square.cancelDrag();
        if (widget instanceof OreHueSlider hue) hue.cancelDrag();
        if (widget instanceof OreScrollbar scrollbar) scrollbar.cancelDrag();
        if (widget instanceof OreSlider slider) slider.onRelease(0, 0);
    }

    public static void outline(AbstractWidget widget, GuiGraphics graphics) {
        if (!widget.visible || !widget.active) return;
        if (!widget.isFocused() && !(widget instanceof OreButton button && button.isSelected())) return;
        if (widget instanceof OreButton button) button.renderOutline(graphics);
        else if (widget instanceof OreSlider slider) slider.renderOutline(graphics);
        else if (widget instanceof OreColorSquare square) square.renderOutline(graphics);
        else if (widget instanceof OreHueSlider hue) hue.renderOutline(graphics);
        else if (widget instanceof OreEditBox field) field.renderOutline(graphics);
        else if (widget instanceof OreTable table) table.renderOutline(graphics);
        else if (!(widget instanceof OreScrollbar)) OreDrawing.focus(graphics, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight());
    }
}
