package com.kyivsec.oreui.client.hud;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.InputType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.DeltaTracker;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/** Native HUD registry and explicit cursor mode. Input hooks are no-ops until a visible layer opts in. */
public final class OreHudManager {
    private static final Map<ResourceLocation, OreHudLayer> LAYERS = new LinkedHashMap<>();
    private static final OreHudInputGuard INPUT = new OreHudInputGuard();
    private static KeyMapping interactionKey;
    private static OreHudLayer editing;
    private static boolean restoreMouse;
    private static Object lastWorld;
    private static double mouseX, mouseY;

    private OreHudManager() {}

    public static void register(RegisterGuiLayersEvent event, OreHudLayer layer) {
        registerRelative(event, layer, null, true);
    }
    public static void registerAbove(RegisterGuiLayersEvent event, ResourceLocation other, OreHudLayer layer) {
        registerRelative(event, layer, other, true);
    }
    public static void registerBelow(RegisterGuiLayersEvent event, ResourceLocation other, OreHudLayer layer) {
        registerRelative(event, layer, other, false);
    }
    private static void registerRelative(RegisterGuiLayersEvent event, OreHudLayer layer, ResourceLocation other, boolean above) {
        if (LAYERS.containsKey(layer.id())) throw new IllegalArgumentException("HUD layer already registered: " + layer.id());
        net.minecraft.client.gui.LayeredDraw.Layer renderer = (graphics, delta) -> render(layer, graphics, delta);
        if (other == null) {
            if (above) event.registerAboveAll(layer.id(), renderer); else event.registerBelowAll(layer.id(), renderer);
        } else if (above) event.registerAbove(other, layer.id(), renderer);
        else event.registerBelow(other, layer.id(), renderer);
        LAYERS.put(layer.id(), layer);
    }
    public static void registerInteractionKey(RegisterKeyMappingsEvent event) {
        interactionKey = new KeyMapping("key.oreui_lib.hud_interact", GLFW.GLFW_KEY_F7, "key.categories.oreui_lib");
        event.register(interactionKey);
    }
    public static boolean isInteracting() { return editing != null; }
    public static OreHudLayer find(ResourceLocation id) { return LAYERS.get(id); }

    private static OreHudContext context(Minecraft minecraft) {
        return new OreHudContext(minecraft.level != null && minecraft.player != null,
                screenType(minecraft.screen == null ? null : minecraft.screen.getClass()),
                minecraft.options.hideGui, minecraft.player != null && minecraft.player.isSpectator(),
                minecraft.isWindowActive(), minecraft.getOverlay() != null);
    }

    static OreHudScreen screenType(Class<? extends Screen> screen) {
        if (screen == null) return OreHudScreen.NONE;
        if (ChatScreen.class.isAssignableFrom(screen)) return OreHudScreen.CHAT;
        if (AbstractContainerScreen.class.isAssignableFrom(screen)) return OreHudScreen.INVENTORY;
        if (PauseScreen.class.isAssignableFrom(screen)) return OreHudScreen.PAUSE_MENU;
        if (TitleScreen.class.isAssignableFrom(screen)) return OreHudScreen.MAIN_MENU;
        return OreHudScreen.OTHER;
    }

    public static boolean startInteraction(ResourceLocation id) {
        Minecraft minecraft = Minecraft.getInstance();
        var layer = LAYERS.get(id);
        if (layer == null || !layer.canInteract(context(minecraft))) return false;
        stopInteraction();
        layer.layout(minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
        editing = layer;
        restoreMouse = minecraft.mouseHandler.isMouseGrabbed();
        KeyMapping.releaseAll();
        minecraft.mouseHandler.releaseMouse();
        mouseX = guiX(minecraft); mouseY = guiY(minecraft);
        return true;
    }

    public static void stopInteraction() {
        stopInteraction(true);
    }

    /** A screen is about to open: yield without re-grabbing the mouse or calling setScreen(null). */
    static void screenOpening() {
        stopInteraction(false);
        LAYERS.values().forEach(OreHudLayer::cancelInteraction);
    }

    private static void stopInteraction(boolean restoreCapture) {
        if (editing == null) return;
        OreHudLayer old = editing;
        editing = null;
        old.cancelInteraction();
        KeyMapping.releaseAll();
        Minecraft minecraft = Minecraft.getInstance();
        if (restoreCapture && restoreMouse && minecraft.screen == null && minecraft.level != null && minecraft.isWindowActive()
                && minecraft.getOverlay() == null) minecraft.mouseHandler.grabMouse();
        // grabMouse refreshes physical key states; do not revive keys that were typed into the HUD.
        KeyMapping.releaseAll();
        restoreMouse = false;
    }

    public static void tick() {
        if (LAYERS.isEmpty()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (lastWorld != minecraft.level) {
            stopInteraction();
            LAYERS.values().forEach(OreHudLayer::detach);
            INPUT.clear(); lastWorld = minecraft.level;
        }
        OreHudContext context = context(minecraft);
        if (editing != null && !editing.canInteract(context)) stopInteraction();
        if (!context.windowActive()) INPUT.clear();
        for (var layer : LAYERS.values()) layer.tick(layer.isDisplayed(context));
    }

    private static void render(OreHudLayer layer, GuiGraphics graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        var context = context(minecraft);
        if (editing != null && !editing.canInteract(context)) stopInteraction();
        layer.render(graphics, delta, context, editing == layer, (int) guiX(minecraft), (int) guiY(minecraft));
    }

    /** Called at the native keyboard callback head, before vanilla gameplay bindings or Escape handling. */
    public static boolean key(long window, int key, int scan, int action, int modifiers) {
        boolean toggle = interactionKey != null && interactionKey.matches(key, scan);
        if (editing == null && !toggle) {
            boolean blocked = INPUT.suppressAfterExit(key, action);
            return blocked && Minecraft.getInstance().screen == null;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (window != minecraft.getWindow().getWindow()) return false;
        var context = context(minecraft);
        if (editing != null && !editing.canInteract(context)) stopInteraction();
        if (toggle && action == GLFW.GLFW_PRESS && !context.screenOpen()) {
            boolean closing = editing != null;
            if (closing) stopInteraction();
            else {
                var layers = LAYERS.values().stream().toList();
                for (int i = layers.size() - 1; i >= 0; i--) if (startInteraction(layers.get(i).id())) break;
            }
            if (closing || editing != null) {
                INPUT.consumed(key, action); return true;
            }
        }
        if (editing == null) return INPUT.suppressAfterExit(key, action) && minecraft.screen == null;
        if (key == GLFW.GLFW_KEY_F1) { stopInteraction(); return false; }
        if (key == GLFW.GLFW_KEY_F2) return false; // Keep native screenshots available.
        INPUT.consumed(key, action);
        if (action == GLFW.GLFW_RELEASE) editing.keyReleased(key, scan, modifiers);
        else {
            minecraft.setLastInputType(key == GLFW.GLFW_KEY_TAB ? InputType.KEYBOARD_TAB : InputType.KEYBOARD_ARROW);
            if (key == GLFW.GLFW_KEY_ESCAPE && !editing.hasPopover()) stopInteraction();
            else {
                editing.keyPressed(key, scan, modifiers);
                if (editing != null && editing.focusedWidget() != null) minecraft.getNarrator().sayNow(editing.focusedWidget().getMessage());
            }
        }
        if (editing != null && minecraft.screen != null) stopInteraction();
        return true;
    }

    public static boolean character(long window, int codepoint, int modifiers) {
        if (editing == null) return false;
        Minecraft minecraft = Minecraft.getInstance();
        if (window != minecraft.getWindow().getWindow() || !editing.canInteract(context(minecraft))) return false;
        if (Character.isValidCodePoint(codepoint)) for (char character : Character.toChars(codepoint)) editing.charTyped(character, modifiers);
        return true;
    }

    public static boolean mouseButton(long window, int button, int action, int modifiers) {
        if (editing == null) return false;
        Minecraft minecraft = Minecraft.getInstance();
        if (window != minecraft.getWindow().getWindow()) return false;
        if (!editing.canInteract(context(minecraft))) { stopInteraction(); return false; }
        minecraft.setLastInputType(InputType.MOUSE);
        if (action == GLFW.GLFW_RELEASE) editing.mouseReleased(guiX(minecraft), guiY(minecraft), button);
        else editing.mouseClicked(guiX(minecraft), guiY(minecraft), button);
        if (minecraft.screen != null) stopInteraction();
        return true; // Cursor mode owns pointer input, including clicks outside panels; never attack/regrab underneath.
    }

    public static boolean scroll(long window, double horizontal, double vertical) {
        if (editing == null) return false;
        Minecraft minecraft = Minecraft.getInstance();
        if (window != minecraft.getWindow().getWindow()) return false;
        if (!editing.canInteract(context(minecraft))) { stopInteraction(); return false; }
        editing.mouseScrolled(guiX(minecraft), guiY(minecraft), horizontal, vertical);
        return true;
    }

    public static void move(long window, double rawX, double rawY) {
        if (editing == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (window != minecraft.getWindow().getWindow() || !editing.canInteract(context(minecraft))) return;
        double x = rawX * minecraft.getWindow().getGuiScaledWidth() / minecraft.getWindow().getScreenWidth();
        double y = rawY * minecraft.getWindow().getGuiScaledHeight() / minecraft.getWindow().getScreenHeight();
        editing.mouseMoved(x, y, x - mouseX, y - mouseY);
        mouseX = x; mouseY = y;
    }
    private static double guiX(Minecraft minecraft) {
        return minecraft.mouseHandler.xpos() * minecraft.getWindow().getGuiScaledWidth() / minecraft.getWindow().getScreenWidth();
    }
    private static double guiY(Minecraft minecraft) {
        return minecraft.mouseHandler.ypos() * minecraft.getWindow().getGuiScaledHeight() / minecraft.getWindow().getScreenHeight();
    }
}
