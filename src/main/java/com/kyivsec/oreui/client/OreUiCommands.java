package com.kyivsec.oreui.client;

import com.kyivsec.oreui.OreUi;
import com.kyivsec.oreui.client.screen.OreUiTestScreen;
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
                })));
    }
}
