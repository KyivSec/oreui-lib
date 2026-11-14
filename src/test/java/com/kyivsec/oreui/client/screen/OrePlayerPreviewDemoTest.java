package com.kyivsec.oreui.client.screen;

import com.kyivsec.oreui.client.widget.OreButton;
import java.util.ArrayList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrePlayerPreviewDemoTest {
    @Test
    void mainMenuEntryIsAddedWithoutAWorldAndDoesNotModifyOtherScreens() {
        var title = new TitleScreen(); title.width = 320; title.height = 240;
        var widgets = new ArrayList<GuiEventListener>();
        OrePlayerPreviewDemo.titleScreen(new ScreenEvent.Init.Post(title, widgets, widgets::add, widgets::remove));
        assertEquals(1, widgets.size());
        var button = (OreButton) widgets.getFirst();
        assertEquals(8, title.width - button.getRight());
        assertEquals(8, button.getY());
        widgets.clear();
        OrePlayerPreviewDemo.titleScreen(new ScreenEvent.Init.Post(new PauseScreen(true), widgets, widgets::add, widgets::remove));
        assertTrue(widgets.isEmpty());
    }
}
