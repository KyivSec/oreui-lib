package com.kyivsec.oreui.client.hud;

import com.kyivsec.oreui.client.host.OreWidgetHost;
import com.kyivsec.oreui.client.layout.OreGrid;
import com.kyivsec.oreui.client.style.OreDrawing;
import com.kyivsec.oreui.client.style.OrePalette;
import com.kyivsec.oreui.client.style.OrePixelIcons;
import com.kyivsec.oreui.client.style.OreTooltip;
import com.kyivsec.oreui.client.widget.OreButton;
import com.kyivsec.oreui.client.widget.OreDropdown;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

/** Persistent native HUD content. Registration does not create a Screen, pause the world or capture input. */
public final class OreHudLayer {
    private final ResourceLocation id;
    private final Supplier<OreGrid> buildContent;
    private OreWidgetHost host, popover, pointerCapture;
    private Supplier<ScreenRectangle> popoverAnchor;
    private Runnable closePopover;
    private final Set<Integer> openingKeys = new HashSet<>();
    private OreHudAnchor anchor = OreHudAnchor.TOP_RIGHT;
    private OreGrid.Insets safeArea = OreGrid.Insets.all(8);
    private BooleanSupplier visibleWhen = () -> true;
    private boolean enabled = true, interactive, showWithScreens, showSpectator = true, respectHideGui = true, panel = true;
    private int width = 180, height = 120, offsetX, offsetY;
    private int screenWidth, screenHeight;
    private boolean dirty = true;

    public OreHudLayer(ResourceLocation id, Supplier<OreGrid> buildContent) {
        this.id = Objects.requireNonNull(id); this.buildContent = Objects.requireNonNull(buildContent);
    }
    public ResourceLocation id() { return id; }
    public boolean isEnabled() { return enabled; }
    public OreHudLayer enabled(boolean value) { enabled = value; return this; }
    public OreHudLayer interactive(boolean value) { interactive = value; return this; }
    public OreHudLayer anchor(OreHudAnchor value) { anchor = Objects.requireNonNull(value); invalidateLayout(); return this; }
    public OreHudLayer safeArea(OreGrid.Insets value) { safeArea = Objects.requireNonNull(value); invalidateLayout(); return this; }
    public OreHudLayer offset(int x, int y) { offsetX = x; offsetY = y; invalidateLayout(); return this; }
    public OreHudLayer size(int width, int height) {
        if (width < 1 || height < 1) throw new IllegalArgumentException("HUD dimensions must be positive");
        this.width = width; this.height = height; invalidateLayout(); return this;
    }
    public OreHudLayer showWithScreens(boolean value) { showWithScreens = value; return this; }
    public OreHudLayer showSpectator(boolean value) { showSpectator = value; return this; }
    public OreHudLayer respectHideGui(boolean value) { respectHideGui = value; return this; }
    public OreHudLayer panel(boolean value) { panel = value; return this; }
    public OreHudLayer visibleWhen(BooleanSupplier value) { visibleWhen = Objects.requireNonNull(value); return this; }
    public void invalidateLayout() { dirty = true; }

    public boolean isDisplayed(OreHudContext context) {
        return enabled && context.worldAvailable() && !context.overlayOpen()
                && (showWithScreens || !context.screenOpen()) && (!respectHideGui || !context.hideGui())
                && (showSpectator || !context.spectator()) && visibleWhen.getAsBoolean();
    }
    public boolean canInteract(OreHudContext context) {
        return interactive && isDisplayed(context) && !context.screenOpen() && context.windowActive();
    }

    /** Lazily built on the client thread after resources/world are available; rebuilt for a different world. */
    public OreGrid content() { return host().grid(); }
    private OreWidgetHost host() {
        if (host == null) { host = new OreWidgetHost(Objects.requireNonNull(buildContent.get())); dirty = true; }
        return host;
    }
    private void bindDropdowns(OreWidgetHost owner) {
        for (AbstractWidget widget : owner.widgets()) {
            if (widget instanceof OreDropdown dropdown) dropdown.setMenuOpener(this::openDropdown);
        }
    }

    void layout(int width, int height) {
        if (!dirty && screenWidth == width && screenHeight == height) return;
        screenWidth = width; screenHeight = height;
        closePopover();
        var viewport = anchor.place(width, height, this.width, this.height, safeArea, offsetX, offsetY);
        host().setViewport(viewport);
        bindDropdowns(host);
        dirty = false;
    }

    void tick(boolean displayed) {
        if (host != null) host.tick(displayed);
        if (popover != null) popover.tick(displayed);
        if (!displayed) cancelInteraction();
    }
    void detach() {
        cancelInteraction();
        if (host != null) host.tick(false);
        host = null; dirty = true;
    }
    void cancelInteraction() {
        closePopover();
        if (host != null) host.cancelInteraction();
        pointerCapture = null;
    }

    void render(GuiGraphics graphics, DeltaTracker delta, OreHudContext context, boolean editing, int mouseX, int mouseY) {
        if (!isDisplayed(context)) return;
        layout(graphics.guiWidth(), graphics.guiHeight());
        if (panel) OreDrawing.panel(graphics, host.viewport().left(), host.viewport().top(), host.viewport().width(), host.viewport().height());
        host.render(graphics, editing ? mouseX : Integer.MIN_VALUE, editing ? mouseY : Integer.MIN_VALUE, delta.getGameTimeDeltaPartialTick(false));
        if (popover != null) {
            OreDrawing.panel(graphics, popover.viewport().left(), popover.viewport().top(), popover.viewport().width(), popover.viewport().height());
            popover.render(graphics, mouseX, mouseY, delta.getGameTimeDeltaPartialTick(false));
        }
        if (editing) renderTooltip(graphics, mouseX, mouseY);
    }

    public void openPopover(OreGrid content, Supplier<ScreenRectangle> anchor, Runnable onClose) {
        closePopover();
        popover = new OreWidgetHost(Objects.requireNonNull(content));
        popoverAnchor = Objects.requireNonNull(anchor);
        closePopover = Objects.requireNonNull(onClose);
        bindDropdowns(popover);
        content.fitContent();
        ScreenRectangle source = popoverAnchor.get();
        int availableWidth = Math.max(1, screenWidth - 16), availableHeight = Math.max(1, screenHeight - 16);
        int pw = Math.min(availableWidth, Math.max(24, source.width()));
        int ph = Math.min(availableHeight, Math.max(1, content.getContentHeight()));
        int px = Math.clamp(source.left(), 8, Math.max(8, screenWidth - pw - 8));
        int below = screenHeight - source.bottom() - 8;
        int above = source.top() - 8;
        boolean useAbove = below < Math.min(ph, 74) && above > below;
        ph = Math.min(ph, Math.max(1, useAbove ? above : below));
        int py = useAbove ? source.top() - ph : source.bottom();
        py = Math.clamp(py, 8, Math.max(8, screenHeight - ph - 8));
        popover.setViewport(new ScreenRectangle(px, py, pw, ph));
    }

    public void closePopover() {
        OreWidgetHost old = popover;
        Runnable callback = closePopover;
        popover = null; closePopover = null; popoverAnchor = null; openingKeys.clear();
        if (old != null) { old.cancelInteraction(); old.tick(false); }
        if (callback != null) callback.run();
    }

    private void openDropdown(OreDropdown dropdown) {
        OreGrid menu = new OreGrid(dropdown.getOptions().size(), 1).padding(1);
        OreButton selected = null;
        for (int index = 0; index < dropdown.getOptions().size(); index++) {
            int option = index;
            menu.row(index, OreGrid.Track.fixed(18));
            OreButton row = new OreButton(0, 0, Math.max(24, dropdown.getWidth()), 18, dropdown.getOptions().get(index),
                    OrePalette.GENERAL, button -> { closePopover(); dropdown.select(option); }) {
                @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
                    graphics.fill(getX(), getY(), getRight(), getBottom(), isHovered() ? OreDrawing.INSET : OreDrawing.PANEL);
                    var font = font();
                    OreDrawing.scaledText(graphics, font, getMessage(), getX() + 8, getY(), getRight() - 18,
                            getBottom(), OreDrawing.TEXT, OreDrawing.SMALL_TEXT);
                    if (option == dropdown.getSelectedIndex()) OrePixelIcons.draw(graphics, OrePixelIcons.Icon.TICK,
                            getRight() - 13, getY() + 5, 8, OreDrawing.TEXT);
                    renderOutline(graphics);
                }
            };
            menu.add(row, index, 0);
            if (index == dropdown.getSelectedIndex()) selected = row;
        }
        openPopover(menu, dropdown::inputBounds, dropdown::closeMenu);
        if (selected != null) { popover.setFocused(selected); popover.revealFocused(); }
    }

    boolean mouseClicked(double x, double y, int button) {
        if (popover != null && !OreGrid.contains(popover.viewport(), x, y)) { closePopover(); return true; }
        pointerCapture = popover == null ? host() : popover;
        return pointerCapture.mouseClicked(x, y, button);
    }
    void mouseReleased(double x, double y, int button) {
        if (pointerCapture != null) pointerCapture.mouseReleased(x, y, button);
        pointerCapture = null;
    }
    void mouseMoved(double x, double y, double dx, double dy) {
        if (pointerCapture != null) pointerCapture.mouseDragged(x, y, 0, dx, dy);
    }
    void mouseScrolled(double x, double y, double horizontal, double vertical) {
        (popover == null ? host() : popover).mouseScrolled(x, y, horizontal, vertical);
    }
    boolean keyPressed(int key, int scan, int modifiers) {
        if (popover != null && key == GLFW.GLFW_KEY_ESCAPE) { closePopover(); return true; }
        if (openingKeys.contains(key)) return true;
        OreWidgetHost before = popover;
        var target = popover == null ? host() : popover;
        if (popover != null && (key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_DOWN)) {
            target.keyPressed(GLFW.GLFW_KEY_TAB, scan, key == GLFW.GLFW_KEY_UP ? GLFW.GLFW_MOD_SHIFT : 0);
            return true;
        }
        boolean handled = target.keyPressed(key, scan, modifiers);
        if (before != popover && popover != null) openingKeys.add(key);
        return handled;
    }
    void keyReleased(int key, int scan, int modifiers) {
        openingKeys.remove(key);
        // The opener still owns the original press, even while focus is in its popover.
        // Release both hosts so Escape-dismissed menus can be opened again on the very next press.
        host().keyReleased(key, scan, modifiers);
        if (popover != null) popover.keyReleased(key, scan, modifiers);
    }
    void charTyped(char character, int modifiers) { (popover == null ? host() : popover).charTyped(character, modifiers); }
    AbstractWidget focusedWidget() {
        var focused = (popover == null ? host() : popover).getFocused();
        return focused instanceof AbstractWidget widget ? widget : null;
    }
    boolean hasPopover() { return popover != null; }

    private void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        var target = popover == null ? host : popover;
        var hovered = target.getChildAt(mouseX, mouseY).orElse(target.getFocused());
        if (!(hovered instanceof AbstractWidget widget) || widget.getTooltip() == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        var lines = widget.getTooltip().toCharSequence(minecraft);
        int tw = Math.min(Math.max(12, screenWidth - 16), (int) Math.ceil(lines.stream().mapToInt(minecraft.font::width).max().orElse(0)
                * OreDrawing.DESCRIPTION_TEXT) + 10);
        int th = lines.size() * 8 + 4;
        int tx = Math.clamp(mouseX + 8, 8, Math.max(8, screenWidth - tw - 8));
        int ty = Math.clamp(mouseY + 12, 8, Math.max(8, screenHeight - th - 8));
        OreTooltip.render(graphics, minecraft.font, lines, tx, ty, tw, OreTooltip.Side.TOP);
    }
}
