package com.kyivsec.oreui.client.screen;

import com.kyivsec.oreui.OreUi;
import com.kyivsec.oreui.client.layout.OreGrid;
import com.kyivsec.oreui.client.layout.OreMenuPresets;
import com.kyivsec.oreui.client.preview.OrePlayerAppearance;
import com.kyivsec.oreui.client.style.OrePalette;
import com.kyivsec.oreui.client.widget.OreButton;
import com.kyivsec.oreui.client.widget.OreLabel;
import com.kyivsec.oreui.client.widget.OrePlayerPreview;
import com.kyivsec.oreui.client.widget.OreToggle;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/** Generic model/animation demonstration, available before joining any world. */
@EventBusSubscriber(modid = OreUi.ID, value = Dist.CLIENT)
public final class OrePlayerPreviewDemo {
    private OrePlayerPreviewDemo() {}
    private static Component text(String key) { return Component.translatable("oreui.preview.demo." + key); }

    @SubscribeEvent public static void titleScreen(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof TitleScreen screen)) return;
        int width = Math.min(104, Math.max(1, screen.width - 16));
        event.addListener(new OreButton(screen.width - width - 8, 8, width, 22, text("open"), OrePalette.GENERAL, button -> open()));
    }

    public static void open() {
        Minecraft.getInstance().pushGuiLayer(new OreGridScreen(text("title"), OreMenuPresets.Kind.SIMPLE, screen -> {
            OreGrid body = new OreGrid(3, 2).gap(6).padding(2).row(1, OreGrid.Track.flex(1));
            var stretch = OreGrid.Cell.DEFAULT.align(OreGrid.Align.STRETCH, OreGrid.Align.STRETCH);
            body.add(new OreLabel(screen.font(), text("description"), 200), 0, 0, 1, 2, OreGrid.Cell.DEFAULT);
            OrePlayerPreview classic = body.add(new OrePlayerPreview(text("classic"), 160, new OrePlayerAppearance(
                    ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png"), OrePlayerAppearance.Model.CLASSIC)), 1, 0, stretch);
            OrePlayerPreview slim = body.add(new OrePlayerPreview(text("slim"), 160, new OrePlayerAppearance(
                    ResourceLocation.withDefaultNamespace("textures/entity/player/slim/alex.png"), OrePlayerAppearance.Model.SLIM)), 1, 1, stretch);
            var previews = List.of(classic, slim);
            previews.forEach(preview -> { preview.setWalking(true); preview.setFollowMouse(true); });
            body.add(new OreLabel(screen.font(), text("classic"), 100), 2, 0);
            body.add(new OreLabel(screen.font(), text("slim"), 100), 2, 1);
            OreGrid footer = new OreGrid(1, 3).gap(6).row(0, OreGrid.Track.flex(1));
            footer.add(new OreButton(0, 0, 100, 28, Component.translatable("gui.back"), OrePalette.GENERAL, button -> screen.onClose()), 0, 0, stretch);
            footer.add(new OreToggle(text("animation"), OreToggle.Kind.SWITCH, true,
                    running -> previews.forEach(preview -> preview.setAnimationRunning(running))), 0, 1, stretch);
            footer.add(new OreButton(0, 0, 100, 28, text("swing"), OrePalette.COLORED,
                    button -> previews.forEach(preview -> preview.playSwing(InteractionHand.MAIN_HAND))), 0, 2, stretch);
            screen.footer(footer);
            screen.body(body);
        }).size(480, 400));
    }
}
