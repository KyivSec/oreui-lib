package com.kyivsec.oreui.client.widget;

import com.kyivsec.oreui.client.style.OreDrawing;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;

public record OreToast(Component title, Component message) implements Toast {
    @Override
    public int width() {
        return 160;
    }

    @Override
    public int height() {
        return 20;
    }

    @Override
    public Visibility render(GuiGraphics graphics, ToastComponent component, long elapsed) {
        graphics.fill(0, 0, width(), height(), OreDrawing.NOTIFICATION);
        // The API has two lines; both use the reference's compact 24-source-unit typography.
        OreDrawing.scaledText(graphics, component.getMinecraft().font, title, 2, 2, width() - 2, 10,
                OreDrawing.TEXT, OreDrawing.DESCRIPTION_TEXT);
        OreDrawing.scaledText(graphics, component.getMinecraft().font, message, 2, 10, width() - 2, 18,
                OreDrawing.TEXT, OreDrawing.DESCRIPTION_TEXT);
        return elapsed >= 4000 * component.getNotificationDisplayTimeMultiplier() ? Visibility.HIDE : Visibility.SHOW;
    }
}
