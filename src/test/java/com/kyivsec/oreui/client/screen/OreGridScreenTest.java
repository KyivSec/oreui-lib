package com.kyivsec.oreui.client.screen;

import com.kyivsec.oreui.client.layout.OreMenuLayout;
import com.kyivsec.oreui.client.layout.OreMenuPresets;
import java.util.List;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OreGridScreenTest {
    @Test
    void defaultTitlesKeepEqualHorizontalInsetsAcrossPresetsAndResizes() {
        var title = OreGridScreen.defaultHeader(null, Component.literal("Title"));
        var body = new SpacerElement(0, 0);
        var footer = new SpacerElement(0, 0);
        for (var menu : List.of(
                OreMenuPresets.simple(title, body, footer),
                OreMenuPresets.settings(title, new SpacerElement(0, 0), body, footer),
                OreMenuPresets.split(title, new SpacerElement(0, 0), body, footer),
                OreMenuPresets.modal(title, body, footer))) {
            for (int width : new int[] {240, 480, 1104, 240}) {
                menu.setSize(width, 240);
                menu.setPosition(20, 30);
                var bounds = menu.bounds(OreMenuLayout.Region.HEADER);
                title.visitWidgets(label -> {
                    assertEquals(8, label.getX() - bounds.left());
                    assertEquals(8, bounds.right() - label.getRight());
                });
            }
        }
    }
}
