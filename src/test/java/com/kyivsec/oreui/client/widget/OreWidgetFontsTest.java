package com.kyivsec.oreui.client.widget;

import com.kyivsec.oreui.client.screen.OrePopupScreen;
import com.kyivsec.oreui.client.style.OreFonts;
import com.kyivsec.oreui.client.style.OrePalette;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OreWidgetFontsTest {
    private static final ResourceLocation CUSTOM_FONT = ResourceLocation.fromNamespaceAndPath("example", "ui_body");

    private static final class HeadlessPopup extends OrePopupScreen {
        private HeadlessPopup(Font selected) {
            super(Component.literal("Popup"), CUSTOM_FONT, popup -> {});
            font = selected;
        }
    }

    @Test
    void perButtonFontsDoNotActivateOrRestyleSiblingControls() {
        Font first = new Font(resource -> null, false), second = new Font(resource -> null, false);
        AtomicInteger changes = new AtomicInteger();
        OreButton a = new OreButton(0, 0, 100, 28, Component.literal("A"), OrePalette.GENERAL, button -> changes.incrementAndGet());
        OreButton b = new OreButton(0, 0, 100, 28, Component.literal("B"), OrePalette.GENERAL, button -> changes.incrementAndGet());
        a.setFont(first);
        b.setFont(second);
        assertSame(first, a.font());
        assertSame(second, b.font());
        a.setFont(second);
        assertSame(second, a.font());
        assertEquals(0, changes.get());
        a.setFont(null); // Restoring the native default must not invoke the action either.
        assertEquals(0, changes.get());
    }

    @Test
    void popupButtonsActionsAndConfirmationInheritItsSelectedFont() {
        Font font = OreFonts.font(new Font(resource -> null, false), CUSTOM_FONT);
        HeadlessPopup popup = new HeadlessPopup(font);
        AtomicInteger changes = new AtomicInteger();
        OreButton body = popup.add(new OreButton(0, 0, 100, 28, Component.literal("Body"), OrePalette.GENERAL,
                button -> changes.incrementAndGet()));
        OreButton action = popup.addAction(Component.literal("Confirm"), changes::incrementAndGet);
        OreToggle confirmation = popup.addConfirmation(Component.literal("Agree"), false, checked -> changes.incrementAndGet());
        assertSame(font, popup.font());
        assertSame(font, body.font());
        assertSame(font, action.font());
        assertSame(font, confirmation.font());
        assertEquals(0, changes.get());
        confirmation.setChecked(true);
        assertEquals(1, changes.get());
    }
}
