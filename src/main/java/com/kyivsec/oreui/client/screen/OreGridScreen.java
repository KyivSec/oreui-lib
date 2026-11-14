package com.kyivsec.oreui.client.screen;

import com.kyivsec.oreui.client.layout.OreGrid;
import com.kyivsec.oreui.client.layout.OreMenuLayout;
import com.kyivsec.oreui.client.layout.OreMenuPresets;
import com.kyivsec.oreui.client.style.OreDrawing;
import com.kyivsec.oreui.client.style.OrePalette;
import com.kyivsec.oreui.client.widget.OreButton;
import com.kyivsec.oreui.client.widget.OreLabel;
import com.kyivsec.oreui.client.widget.OreModelPreview;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Ready-to-use grid menu shell. Chrome is fixed; only the body viewport scrolls. */
public final class OreGridScreen extends OreScreen {
    private final OreMenuPresets.Kind kind;
    private final Consumer<OreGridScreen> build;
    private OreMenuPresets.Options options = OreMenuPresets.Options.DEFAULT;
    private OreMenuLayout menu;
    private OreGrid body = new OreGrid(1, 1);
    private LayoutElement header, sidebar, preview, footer;
    private final LayoutElement bodySlot = new SpacerElement(0, 0);
    private final List<AbstractWidget> bodyWidgets = new ArrayList<>();
    private final List<AbstractWidget> chromeWidgets = new ArrayList<>();
    private final Set<Integer> openingKeys = new HashSet<>();
    private ScreenRectangle panel = new ScreenRectangle(0, 0, 1, 1);
    private Supplier<ScreenRectangle> anchor;
    private int preferredWidth = 0, preferredHeight = 0;
    private boolean ready;

    public OreGridScreen(Component title, OreMenuPresets.Kind kind, Consumer<OreGridScreen> build) {
        super(title); this.kind = Objects.requireNonNull(kind); this.build = Objects.requireNonNull(build);
        if (kind == OreMenuPresets.Kind.MODAL) options = options.headerHeight(20).padding(8).gap(4);
    }
    public Font font() { return font; }
    public OreGrid body() { return body; }
    public OreMenuLayout layout() { return menu; }
    public OreGridScreen anchor(Supplier<ScreenRectangle> value) {
        anchor = Objects.requireNonNull(value); if (ready) repositionElements(); return this;
    }
    public OreGridScreen size(int width, int height) {
        if (width < 1 || height < 1) throw new IllegalArgumentException("Menu size must be positive");
        preferredWidth = width; preferredHeight = height; if (ready) repositionElements(); return this;
    }
    public OreGridScreen options(OreMenuPresets.Options value) {
        options = Objects.requireNonNull(value);
        if (ready) { makeLayout(); repositionElements(); }
        return this;
    }
    public void body(OreGrid value) { body = Objects.requireNonNull(value); if (ready) reflow(); }
    public void header(LayoutElement value) {
        beforeBuild(); if (kind == OreMenuPresets.Kind.ANCHORED) throw new IllegalStateException("Anchored menus have only a body");
        header = Objects.requireNonNull(value);
    }
    public void sidebar(LayoutElement value) {
        beforeBuild(); if (kind != OreMenuPresets.Kind.SETTINGS) throw new IllegalStateException("Only settings presets have a sidebar");
        sidebar = Objects.requireNonNull(value);
    }
    public void preview(LayoutElement value) {
        beforeBuild(); if (kind != OreMenuPresets.Kind.SPLIT) throw new IllegalStateException("Only split presets have a preview region");
        preview = Objects.requireNonNull(value);
    }
    public void footer(LayoutElement value) {
        beforeBuild(); if (kind == OreMenuPresets.Kind.ANCHORED) throw new IllegalStateException("Anchored menus have only a body");
        footer = Objects.requireNonNull(value);
    }
    private void beforeBuild() { if (ready) throw new IllegalStateException("Assign chrome in the build callback; its widgets remain mutable afterwards"); }

    @Override
    protected void init() {
        ready = false; bodyWidgets.clear(); chromeWidgets.clear(); openingKeys.clear();
        body = new OreGrid(1, 1);
        header = defaultHeader(font, title);
        sidebar = new SpacerElement(0, 0); preview = new SpacerElement(0, 0);
        footer = new OreButton(0, 0, 100, 28, Component.translatable("gui.back"), OrePalette.GENERAL, button -> onClose());
        for (int key : new int[] {GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_SPACE}) {
            if (InputConstants.isKeyDown(minecraft.getWindow().getWindow(), key)) openingKeys.add(key);
        }
        build.accept(this);
        makeLayout();
        menu.visitWidgets(widget -> { chromeWidgets.add(widget); addWidget(widget); });
        addRenderableOnly((graphics, mouseX, mouseY, partialTick) -> {
            graphics.enableScissor(panel.left(), panel.top(), panel.right(), panel.bottom());
            try { menu.render(graphics, mouseX, mouseY, partialTick); }
            finally { graphics.disableScissor(); }
        });
        ready = true;
        repositionElements();
    }

    static OreGrid defaultHeader(Font font, Component title) {
        OreGrid header = new OreGrid(1, 1).row(0, OreGrid.Track.flex(1))
                .padding(new OreGrid.Insets(6, 0, 6, 0));
        header.add(new OreLabel(font, title, 200), 0, 0,
                OreGrid.Cell.DEFAULT.align(OreGrid.Align.STRETCH, OreGrid.Align.STRETCH));
        return header;
    }

    private void makeLayout() {
        menu = switch (kind) {
            case SETTINGS -> OreMenuPresets.settings(header, sidebar, bodySlot, footer, options);
            case SIMPLE -> OreMenuPresets.simple(header, bodySlot, footer, options);
            case SPLIT -> OreMenuPresets.split(header, preview, bodySlot, footer, options);
            case MODAL -> OreMenuPresets.modal(header, bodySlot, footer, options);
            case ANCHORED -> OreMenuPresets.anchored(bodySlot, options.padding(2).gap(0));
        };
    }

    /** Call after height/structure changes. Existing widgets, editing state and bounded scroll are retained. */
    public void reflow() {
        if (!ready) return;
        List<AbstractWidget> current = new ArrayList<>();
        body.visitWidgets(current::add);
        if (current.stream().anyMatch(chromeWidgets::contains)) throw new IllegalArgumentException("Body and chrome cannot share widgets");
        boolean structureChanged = !current.equals(bodyWidgets);
        if (structureChanged) cancelInteraction();
        for (var old : List.copyOf(bodyWidgets)) if (!current.contains(old)) removeContentWidget(old);
        for (var widget : current) if (!bodyWidgets.contains(widget)) {
            if (widget instanceof OreModelPreview model) model.setAnimationHostVisible(true);
            addContentWidget(widget);
        }
        bodyWidgets.clear(); bodyWidgets.addAll(current);
        if (structureChanged) orderWidgets();
        body.setSize(viewport.width(), viewport.height());
        contentHeight = body.getContentHeight();
        clampScroll(); arrangeContent();
    }

    private void orderWidgets() {
        // Register in visual region order, not 'all chrome then body', so Tab reaches the footer last.
        List<AbstractWidget> extra = children().stream().filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast)
                .filter(widget -> !chromeWidgets.contains(widget) && !bodyWidgets.contains(widget)).toList();
        chromeWidgets.forEach(this::removeWidget); bodyWidgets.forEach(this::removeWidget); extra.forEach(this::removeWidget);
        menu.visitChildren(element -> {
            if (element == bodySlot) bodyWidgets.forEach(this::addWidget);
            else element.visitWidgets(this::addWidget);
        });
        extra.forEach(this::addWidget);
    }

    @Override
    protected void repositionElements() {
        if (!ready) return;
        cancelInteraction();
        boolean compact = kind == OreMenuPresets.Kind.MODAL || kind == OreMenuPresets.Kind.ANCHORED;
        int pw = Math.min(Math.max(1, width - 24), preferredWidth > 0 ? preferredWidth : compact ? 240 : 1104);
        int ph = Math.min(Math.max(1, height - 24), preferredHeight > 0 ? preferredHeight : compact ? 200 : height - 24);
        if (kind == OreMenuPresets.Kind.ANCHORED) {
            if (anchor == null) throw new IllegalStateException("Anchored presets require an anchor supplier");
            panel = OreMenuPresets.anchoredBounds(anchor.get(), width, height, pw, ph);
        } else panel = new ScreenRectangle((width - pw) / 2, (height - ph) / 2, pw, ph);
        menu.setSize(panel.width(), panel.height());
        menu.setPosition(panel.left(), panel.top());
        var region = menu.contentBounds(OreMenuLayout.Region.BODY);
        int left = Math.clamp(region.left(), panel.left(), panel.right() - 1);
        int top = Math.clamp(region.top(), panel.top(), panel.bottom() - 1);
        // Stable gutter keeps the scrollbar inside the body region rather than over the footer/preview.
        viewport = new ScreenRectangle(left, top, Math.max(1, Math.min(region.right(), panel.right()) - left - 10),
                Math.max(1, Math.min(region.bottom(), panel.bottom()) - top));
        reflow(); revealFocused();
    }

    @Override protected void arrangeContent() { body.setPosition(viewport.left(), viewport.top() - scrollOffset); }

    @Override
    protected boolean acceptsInput(GuiEventListener child, double x, double y) {
        if (!OreGrid.contains(panel, x, y)) return false;
        if (child instanceof AbstractWidget widget) {
            if (bodyWidgets.contains(widget)) return OreGrid.contains(viewport, x, y) && body.acceptsInput(widget, x, y);
            if (chromeWidgets.contains(widget)) return menu.grid().acceptsInput(widget, x, y);
        }
        return super.acceptsInput(child, x, y);
    }

    @Override public boolean mouseClicked(double x, double y, int button) {
        if (kind == OreMenuPresets.Kind.ANCHORED && !OreGrid.contains(panel, x, y)) { onClose(); return true; }
        return super.mouseClicked(x, y, button);
    }
    @Override public boolean keyPressed(int key, int scan, int modifiers) { return openingKeys.contains(key) || super.keyPressed(key, scan, modifiers); }
    @Override public boolean keyReleased(int key, int scan, int modifiers) { return openingKeys.remove(key) || super.keyReleased(key, scan, modifiers); }
    @Override protected void renderContentWidgets(GuiGraphics graphics, int x, int y, float partialTick) { body.render(graphics, x, y, partialTick); }
    @Override protected void renderWidgetOutline(AbstractWidget widget, GuiGraphics graphics, boolean content) {
        // Both grids already draw their final outline pass using their own nested cell scissors.
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (kind != OreMenuPresets.Kind.ANCHORED) graphics.fill(0, 0, width, height, 0xB2000000);
        OreDrawing.panel(graphics, panel.left(), panel.top(), panel.width(), panel.height());
        if (menu != null && menu.has(OreMenuLayout.Region.HEADER)) {
            var header = menu.bounds(OreMenuLayout.Region.HEADER);
            OreDrawing.bevel(graphics, header.left(), header.top(), header.width(), header.height(),
                    OreDrawing.HEADER, 0xFF5A5B5C, OreDrawing.INSET);
        }
    }
}
