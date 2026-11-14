package com.kyivsec.oreui.client;

import com.kyivsec.oreui.OreUi;
import com.kyivsec.oreui.client.screen.OreUiTestScreen;
import com.kyivsec.oreui.client.hud.OreHudDemo;
import com.kyivsec.oreui.client.hud.OreHudManager;
import com.kyivsec.oreui.client.hud.OreHudStatTest;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

@EventBusSubscriber(modid = OreUi.ID, value = Dist.CLIENT)
public final class OreUiCommands {
    private OreUiCommands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("oreui")
                .then(Commands.literal("test").executes(context -> {
                    Minecraft minecraft = Minecraft.getInstance();
                    minecraft.tell(() -> minecraft.setScreen(new OreUiTestScreen()));
                    return 1;
                }))
                .then(Commands.literal("hud").executes(context -> hud(OreHudDemo::toggle))
                        .then(Commands.literal("on").executes(context -> hud(() -> OreHudDemo.LAYER.enabled(true))))
                         .then(Commands.literal("off").executes(context -> hud(() -> {
                            OreHudDemo.LAYER.enabled(false); OreHudStatTest.LAYER.enabled(false); OreHudManager.stopInteraction();
                         })))
                        .then(Commands.literal("test").executes(context -> hud(OreHudStatTest::toggle)))
                        .then(Commands.literal("edit").executes(context -> hud(OreHudDemo::edit)))));
    }

    private static int hud(Runnable action) {
        Minecraft.getInstance().tell(action);
        return 1;
    }
}
