package com.kyivsec.oreui.client.hud;

import com.kyivsec.oreui.client.layout.OreGrid;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OreHudScreenPolicyTest {
    private static OreHudLayer layer() {
        return new OreHudLayer(ResourceLocation.fromNamespaceAndPath("example", "screen_policy"), () -> new OreGrid(1, 1));
    }

    private static OreHudContext context(OreHudScreen screen) {
        return new OreHudContext(true, screen, false, false, true, false);
    }

    @Test
    void chatOnlyPolicyPersistsInChatButNotInventoriesOrMenusAndNeverStealsChatInput() {
        var layer = layer().interactive(true).showWithScreens(OreHudScreen.CHAT);
        for (var screen : OreHudScreen.values()) {
            assertEquals(screen == OreHudScreen.NONE || screen == OreHudScreen.CHAT, layer.isDisplayed(context(screen)), screen.name());
            assertEquals(screen == OreHudScreen.NONE, layer.canInteract(context(screen)), screen.name());
        }
        assertFalse(layer.isDisplayed(new OreHudContext(false, OreHudScreen.MAIN_MENU, false, false, true, false)));
        assertFalse(layer.isDisplayed(new OreHudContext(true, OreHudScreen.CHAT, true, false, true, false)));
        assertFalse(layer.isDisplayed(new OreHudContext(true, OreHudScreen.CHAT, false, false, true, true)));
    }

    @Test
    void explicitCategoriesReplacePreviousPoliciesAndBooleanCompatibilityStillWorks() {
        var layer = layer();
        for (var screen : OreHudScreen.values()) assertEquals(screen == OreHudScreen.NONE, layer.isDisplayed(context(screen)));
        layer.showWithScreens(true);
        for (var screen : OreHudScreen.values()) assertTrue(layer.isDisplayed(context(screen)));
        assertFalse(layer.isDisplayed(new OreHudContext(false, OreHudScreen.MAIN_MENU, false, false, true, false)));
        layer.showWithScreens(OreHudScreen.CHAT, OreHudScreen.INVENTORY);
        assertTrue(layer.isDisplayed(context(OreHudScreen.INVENTORY)));
        assertFalse(layer.isDisplayed(context(OreHudScreen.PAUSE_MENU)));
        layer.showWithScreens(OreHudScreen.CHAT);
        assertFalse(layer.isDisplayed(context(OreHudScreen.INVENTORY)));
        layer.showWithScreens(false);
        assertFalse(layer.isDisplayed(context(OreHudScreen.CHAT)));
        layer.showWithScreens();
        assertTrue(layer.isDisplayed(context(OreHudScreen.NONE)));
        assertThrows(NullPointerException.class, () -> layer.showWithScreens(OreHudScreen.CHAT, null));
    }

    @Test
    void nativeScreensAndTheirSubclassesMapToTheExpectedCategories() {
        class CustomChat extends ChatScreen { CustomChat() { super(""); } }
        assertEquals(OreHudScreen.NONE, OreHudManager.screenType(null));
        assertEquals(OreHudScreen.CHAT, OreHudManager.screenType(ChatScreen.class));
        assertEquals(OreHudScreen.CHAT, OreHudManager.screenType(CustomChat.class));
        assertEquals(OreHudScreen.INVENTORY, OreHudManager.screenType(InventoryScreen.class));
        assertEquals(OreHudScreen.INVENTORY, OreHudManager.screenType(CreativeModeInventoryScreen.class));
        assertEquals(OreHudScreen.INVENTORY, OreHudManager.screenType(ContainerScreen.class));
        assertEquals(OreHudScreen.PAUSE_MENU, OreHudManager.screenType(PauseScreen.class));
        assertEquals(OreHudScreen.MAIN_MENU, OreHudManager.screenType(TitleScreen.class));
        assertEquals(OreHudScreen.OTHER, OreHudManager.screenType(OptionsScreen.class));
    }

    @Test
    void compatibilityContextsTreatUnspecifiedScreensAsOtherAndRejectInconsistentCategories() {
        assertEquals(OreHudScreen.NONE, new OreHudContext(true, false, false, false, true, false).screen());
        assertEquals(OreHudScreen.OTHER, new OreHudContext(true, true, false, false, true, false).screen());
        assertThrows(IllegalArgumentException.class,
                () -> new OreHudContext(true, false, false, false, true, false, OreHudScreen.CHAT));
    }

    @Test
    void liveStatTestUsesTheChatOnlyPolicyByDefault() {
        boolean enabled = OreHudStatTest.LAYER.isEnabled();
        try {
            OreHudStatTest.LAYER.enabled(true);
            assertTrue(OreHudStatTest.LAYER.isDisplayed(context(OreHudScreen.CHAT)));
            assertFalse(OreHudStatTest.LAYER.isDisplayed(context(OreHudScreen.INVENTORY)));
            assertFalse(OreHudStatTest.LAYER.isDisplayed(context(OreHudScreen.PAUSE_MENU)));
            assertFalse(OreHudStatTest.LAYER.isDisplayed(context(OreHudScreen.MAIN_MENU)));
            assertFalse(OreHudStatTest.LAYER.canInteract(context(OreHudScreen.CHAT)));
        } finally {
            OreHudStatTest.LAYER.enabled(enabled);
        }
    }
}
