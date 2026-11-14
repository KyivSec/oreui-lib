package com.kyivsec.oreui.client.screen;

import com.kyivsec.oreui.client.layout.OreGrid;
import com.kyivsec.oreui.client.layout.OreMenuPresets;
import com.kyivsec.oreui.client.style.OrePalette;
import com.kyivsec.oreui.client.widget.OreButton;
import com.kyivsec.oreui.client.widget.OreItemPreview;
import com.kyivsec.oreui.client.widget.OreLabel;
import com.kyivsec.oreui.client.widget.OreNavigationBar;
import com.kyivsec.oreui.client.widget.OreSlider;
import com.kyivsec.oreui.client.widget.OreTextField;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Local, non-configuration examples of the public preset shells. */
final class OreMenuDemo {
    private OreMenuDemo() {}
    private static Component text(String key, Object... args) { return Component.translatable("oreui.test.presets." + key, args); }

    static void open(OreMenuPresets.Kind kind, Supplier<ScreenRectangle> anchor) {
        OreGridScreen screen = new OreGridScreen(text(kind.name().toLowerCase(java.util.Locale.ROOT)), kind, menu -> {
            if (kind == OreMenuPresets.Kind.ANCHORED) {
                OreGrid body = new OreGrid(3, 1).gap(4).padding(2);
                for (int row = 0; row < 3; row++) {
                    body.add(new OreButton(0, 0, 140, 22, text("choice", row + 1), OrePalette.GENERAL, button -> menu.onClose()), row, 0,
                            OreGrid.Cell.DEFAULT.padding(2));
                }
                menu.body(body);
                return;
            }
            OreLabel status = new OreLabel(menu.font(), text("body"), 200);
            OreGrid body = new OreGrid(7, 2).gap(8).padding(2);
            body.add(status, 0, 0, 1, 2, OreGrid.Cell.DEFAULT);
            body.add(new OreTextField(menu.font(), 0, 0, 180, text("field"), text("local")), 1, 0, 1, 2, OreGrid.Cell.DEFAULT.padding(2));
            body.add(new OreSlider(0, 0, 180, 34, text("slider"), 0.5,
                    value -> status.setMessage(text("value", Math.round(value * 100)))), 2, 0, 1, 2, OreGrid.Cell.DEFAULT.padding(2));
            for (int row = 3; row < 7; row++) {
                body.add(new OreLabel(menu.font(), text("cell", row, 0), 100), row, 0);
                body.add(new OreLabel(menu.font(), text("cell", row, 1), 100), row, 1);
            }
            menu.body(body);
            if (kind == OreMenuPresets.Kind.SETTINGS) {
                var sidebar = new OreNavigationBar(List.of(text("general"), text("display"), text("controls")),
                        LinearLayout.Orientation.VERTICAL, index -> status.setMessage(text("page", index + 1)));
                menu.sidebar(sidebar);
            }
            if (kind == OreMenuPresets.Kind.SPLIT) {
                OreItemPreview preview = new OreItemPreview(text("preview"), new ItemStack(Items.DIAMOND_SWORD), 140);
                preview.setFloating(true);
                menu.preview(preview);
            }
            OreGrid footer = new OreGrid(1, 2).gap(4).row(0, OreGrid.Track.flex(1));
            footer.add(new OreButton(0, 0, 100, 26, Component.translatable("gui.back"), OrePalette.GENERAL, button -> menu.onClose()), 0, 0,
                    OreGrid.Cell.DEFAULT.padding(2));
            footer.add(new OreButton(0, 0, 100, 26, text("apply"), OrePalette.COLORED,
                    button -> status.setMessage(text("applied"))), 0, 1, OreGrid.Cell.DEFAULT.padding(2));
            menu.footer(footer);
        });
        if (kind == OreMenuPresets.Kind.ANCHORED) screen.anchor(anchor).size(180, 112);
        Minecraft.getInstance().pushGuiLayer(screen);
    }
}
