package com.kyivsec.oreui.client.screen;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Community modal: compact back/title/close header, scrollable body and fixed confirmation/actions. */
public final class OreModalScreen extends OrePopupScreen {
    public OreModalScreen(Component title, Consumer<OreModalScreen> buildContent) {
        super(title, popup -> buildContent.accept((OreModalScreen) popup));
    }

    public OreModalScreen(Component title, ResourceLocation fontId, Consumer<OreModalScreen> buildContent) {
        super(title, fontId, popup -> buildContent.accept((OreModalScreen) popup));
    }

    @Override
    protected boolean communityModal() {
        return true;
    }
}
