package com.kyivsec.oreui.client.hud;

import com.kyivsec.oreui.client.layout.OreGrid;
import com.kyivsec.oreui.client.widget.OreSlider;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import static org.junit.jupiter.api.Assertions.*;

class OreHudTest {
    private static final class Action extends AbstractWidget {
        private final Runnable action;
        private boolean pressed;
        Action(Runnable action) { super(0, 0, 100, 20, Component.literal("Action")); this.action = action; }
        @Override public boolean keyPressed(int key, int scan, int modifiers) {
            if (key != GLFW.GLFW_KEY_ENTER) return false;
            if (pressed) return true;
            pressed = true;
            action.run(); return true;
        }
        @Override public boolean keyReleased(int key, int scan, int modifiers) {
            if (key != GLFW.GLFW_KEY_ENTER) return false;
            pressed = false; return true;
        }
        @Override protected void renderWidget(GuiGraphics graphics, int x, int y, float partial) {}
        @Override protected void updateWidgetNarration(NarrationElementOutput output) {}
    }
    private static final OreHudContext PLAYING = new OreHudContext(true, false, false, false, true, false);
    private static OreHudLayer layer() {
        return new OreHudLayer(ResourceLocation.fromNamespaceAndPath("example", "hud"), () -> new OreGrid(1, 1));
    }

    @Test
    void displayOnlyIsTheDefaultAndWorldScreenF1AndOverlayPoliciesAreExplicit() {
        var layer = layer();
        assertTrue(layer.isDisplayed(PLAYING));
        assertFalse(layer.canInteract(PLAYING));
        layer.interactive(true);
        assertTrue(layer.canInteract(PLAYING));
        assertFalse(layer.isDisplayed(new OreHudContext(false, false, false, false, true, false)));
        assertFalse(layer.isDisplayed(new OreHudContext(true, true, false, false, true, false)));
        assertFalse(layer.isDisplayed(new OreHudContext(true, false, true, false, true, false)));
        assertFalse(layer.isDisplayed(new OreHudContext(true, false, false, false, true, true)));
        assertFalse(layer.canInteract(new OreHudContext(true, false, false, false, false, false)));
        layer.showWithScreens(true).respectHideGui(false).showSpectator(false);
        assertTrue(layer.isDisplayed(new OreHudContext(true, true, true, false, true, false)));
        assertFalse(layer.canInteract(new OreHudContext(true, true, false, false, true, false)));
        assertFalse(layer.isDisplayed(new OreHudContext(true, false, false, true, true, false)));
        layer.enabled(false);
        assertFalse(layer.isDisplayed(PLAYING));
    }

    @Test
    void visibilityQueriesDoNotBuildWorldOrFontDependentContent() {
        AtomicInteger builds = new AtomicInteger();
        var layer = new OreHudLayer(ResourceLocation.fromNamespaceAndPath("example", "lazy"), () -> {
            builds.incrementAndGet(); return new OreGrid(1, 1);
        });
        assertTrue(layer.isDisplayed(PLAYING));
        assertEquals(0, builds.get());
        layer.content(); layer.content();
        assertEquals(1, builds.get());
        layer.detach(); layer.content();
        assertEquals(2, builds.get());
        layer.visibleWhen(() -> false);
        assertFalse(layer.isDisplayed(PLAYING));
    }

    @Test
    void anchorsClampToSafeAreaAcrossResizingWithoutNegativeDimensions() {
        var safe = OreGrid.Insets.all(8);
        assertEquals(new ScreenRectangle(212, 182, 100, 50), OreHudAnchor.BOTTOM_RIGHT.place(320, 240, 100, 50, safe, 0, 0));
        assertEquals(new ScreenRectangle(110, 95, 100, 50), OreHudAnchor.CENTER.place(320, 240, 100, 50, safe, 0, 0));
        assertEquals(new ScreenRectangle(8, 8, 304, 224), OreHudAnchor.TOP_LEFT.place(320, 240, 1000, 1000, safe, -100, -100));
        for (var anchor : OreHudAnchor.values()) {
            var bounds = anchor.place(80, 60, 30, 20, safe, 1000, -1000);
            assertTrue(bounds.left() >= 8 && bounds.top() >= 8);
            assertTrue(bounds.right() <= 72 && bounds.bottom() <= 52);
        }
    }

    @Test
    void consumedKeysCannotLeakRepeatsIntoGameplayAfterModeExit() {
        var guard = new OreHudInputGuard();
        guard.consumed(GLFW.GLFW_KEY_W, GLFW.GLFW_PRESS);
        guard.consumed(GLFW.GLFW_KEY_ENTER, GLFW.GLFW_PRESS);
        assertTrue(guard.suppressAfterExit(GLFW.GLFW_KEY_W, GLFW.GLFW_REPEAT));
        assertTrue(guard.suppressAfterExit(GLFW.GLFW_KEY_W, GLFW.GLFW_RELEASE));
        assertFalse(guard.suppressAfterExit(GLFW.GLFW_KEY_W, GLFW.GLFW_PRESS));
        assertTrue(guard.suppressAfterExit(GLFW.GLFW_KEY_ENTER, GLFW.GLFW_REPEAT));
        guard.clear();
        assertFalse(guard.suppressAfterExit(GLFW.GLFW_KEY_ENTER, GLFW.GLFW_REPEAT));
    }

    @Test
    void popoversSuppressTheOpeningKeyAndDismissOutsideWithoutClickingTheParent() {
        AtomicInteger activations = new AtomicInteger();
        OreHudLayer[] layer = new OreHudLayer[1];
        OreGrid body = new OreGrid(1, 1);
        var opener = body.add(new Action(() -> {
            OreGrid menu = new OreGrid(1, 1);
            menu.add(new Action(activations::incrementAndGet), 0, 0);
            layer[0].openPopover(menu, () -> new ScreenRectangle(100, 30, 100, 20), () -> {});
        }), 0, 0);
        layer[0] = new OreHudLayer(ResourceLocation.fromNamespaceAndPath("example", "popover"), () -> body);
        layer[0].layout(320, 240);
        layer[0].keyPressed(GLFW.GLFW_KEY_TAB, 0, 0);
        assertSame(opener, layer[0].focusedWidget());
        layer[0].keyPressed(GLFW.GLFW_KEY_ENTER, 0, 0);
        assertTrue(layer[0].hasPopover());
        layer[0].keyPressed(GLFW.GLFW_KEY_TAB, 0, 0);
        layer[0].keyPressed(GLFW.GLFW_KEY_ENTER, 0, 0);
        assertEquals(0, activations.get());
        layer[0].keyReleased(GLFW.GLFW_KEY_ENTER, 0, 0);
        layer[0].keyPressed(GLFW.GLFW_KEY_ENTER, 0, 0);
        assertEquals(1, activations.get());
        assertTrue(layer[0].mouseClicked(0, 0, 0));
        assertFalse(layer[0].hasPopover());
        assertEquals(1, activations.get());
    }

    @Test
    void releasingAnOpeningKeyWhileInAPopoverAlsoReleasesTheParentControl() {
        OreHudLayer[] layer = new OreHudLayer[1];
        OreGrid body = new OreGrid(1, 1);
        body.add(new Action(() -> layer[0].openPopover(new OreGrid(1, 1),
                () -> new ScreenRectangle(100, 30, 100, 20), () -> {})), 0, 0);
        layer[0] = new OreHudLayer(ResourceLocation.fromNamespaceAndPath("example", "release"), () -> body);
        layer[0].layout(320, 240);
        layer[0].keyPressed(GLFW.GLFW_KEY_TAB, 0, 0);
        layer[0].keyPressed(GLFW.GLFW_KEY_ENTER, 0, 0);
        assertTrue(layer[0].hasPopover());
        layer[0].keyReleased(GLFW.GLFW_KEY_ENTER, 0, 0);
        layer[0].keyPressed(GLFW.GLFW_KEY_ESCAPE, 0, 0);
        assertFalse(layer[0].hasPopover());
        layer[0].keyPressed(GLFW.GLFW_KEY_ENTER, 0, 0);
        assertTrue(layer[0].hasPopover());
    }

    @Test
    void hidingOrDisablingAHudDismissesItsPopoverAndNeverSynthesizesSliderReleases() {
        AtomicInteger releases = new AtomicInteger();
        OreGrid body = new OreGrid(1, 1);
        var slider = body.add(new OreSlider(0, 0, 100, 34, Component.literal("Slider"), 0.5, value -> {}) {
            @Override protected void playReleaseSound() { releases.incrementAndGet(); }
        }, 0, 0);
        var layer = new OreHudLayer(ResourceLocation.fromNamespaceAndPath("example", "silent_cleanup"), () -> body);
        layer.layout(320, 240);
        slider.onClick(slider.getX() + 50, slider.getY() + 10);
        AtomicInteger closed = new AtomicInteger();
        layer.openPopover(new OreGrid(1, 1), () -> slider.getRectangle(), closed::incrementAndGet);
        layer.keyPressed(GLFW.GLFW_KEY_ESCAPE, 0, 0);
        assertFalse(layer.hasPopover());
        assertEquals(1, closed.get());
        layer.openPopover(new OreGrid(1, 1), () -> slider.getRectangle(), closed::incrementAndGet);
        layer.tick(false); // Real screen opened / HUD hidden.
        assertFalse(layer.hasPopover());
        assertNull(layer.focusedWidget());
        layer.openPopover(new OreGrid(1, 1), () -> slider.getRectangle(), closed::incrementAndGet);
        layer.enabled(false); // Explicitly hiding the layer also ends interaction.
        assertFalse(layer.hasPopover());
        for (int tick = 0; tick < 100; tick++) layer.tick(false);
        slider.onRelease(0, 0); // A late native release must also remain silent after cancellation.
        assertEquals(3, closed.get());
        assertEquals(0, releases.get());
    }
}
