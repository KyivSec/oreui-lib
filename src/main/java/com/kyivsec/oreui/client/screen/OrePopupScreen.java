package com.kyivsec.oreui.client.screen;

import com.kyivsec.oreui.client.style.OreDrawing;
import com.kyivsec.oreui.client.style.OrePalette;
import com.kyivsec.oreui.client.style.OreIcons;
import com.kyivsec.oreui.client.widget.OreButton;
import com.kyivsec.oreui.client.widget.OreColumn;
import com.kyivsec.oreui.client.widget.OreLabel;
import com.kyivsec.oreui.client.widget.OrePixelButton;
import com.kyivsec.oreui.client.widget.OreToggle;
import com.kyivsec.oreui.client.style.OrePixelIcons;
import com.kyivsec.oreui.client.style.OreFonts;
import com.kyivsec.oreui.client.layout.OreGrid;
import com.kyivsec.oreui.client.layout.OreMenuLayout;
import com.kyivsec.oreui.client.layout.OreMenuPresets;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

public class OrePopupScreen extends OreScreen {
    private final Consumer<OrePopupScreen> buildContent;
    private final ResourceLocation fontId;
    private Font contentFont;
    private final OreColumn column = new OreColumn();
    private final OreColumn actionColumn = new OreColumn();
    private final Set<Integer> openingKeys = new HashSet<>();
    private final List<OreButton> actions = new ArrayList<>();
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int actionHeight;
    private int actionTop;
    private OreButton close;
    private OreButton back;
    private OreToggle confirmation;
    private Supplier<ScreenRectangle> anchor;

    public OrePopupScreen(Component title, Consumer<OrePopupScreen> buildContent) {
        this(title, Style.DEFAULT_FONT, buildContent);
    }

    public OrePopupScreen(Component title, ResourceLocation fontId, Consumer<OrePopupScreen> buildContent) {
        super(title);
        this.fontId = Objects.requireNonNull(fontId);
        this.buildContent = Objects.requireNonNull(buildContent);
    }

    public Font font() {
        return font;
    }

    protected boolean communityModal() {
        return false;
    }

    protected int preferredPanelWidth() {
        return communityModal() ? 200 : 178;
    }

    public OreToggle addConfirmation(Component label, boolean checked, Consumer<Boolean> onChange) {
        if (confirmation != null) throw new IllegalStateException("A modal has one confirmation row");
        confirmation = addRenderableWidget(new OreToggle(label, OreToggle.Kind.CHECKBOX, checked, onChange));
        confirmation.setFont(font);
        confirmation.setHeight(22);
        return confirmation;
    }

    public OrePopupScreen anchor(Supplier<ScreenRectangle> anchor) {
        this.anchor = anchor;
        return this;
    }

    public OrePopupScreen menu() {
        menu = true;
        return this;
    }

    private boolean menu;

    private boolean isMenu() {
        return menu || anchor != null;
    }

    public <T extends LayoutElement> T add(T element) {
        if (element instanceof OreLabel label) label.setPlainTextScale(OreDrawing.SMALL_TEXT);
        column.addChild(element);
        element.visitWidgets(widget -> {
            if (widget instanceof OreButton button) button.setFont(font);
            addContentWidget(widget);
        });
        return element;
    }

    public OreButton addAction(Component label, Runnable action) {
        return addAction(label, OrePalette.GENERAL, action);
    }

    public OreButton addAction(Component label, OrePalette palette, Runnable action) {
        OreButton button = new OreButton(0, 0, 100, isMenu() ? 28 : communityModal() ? 22 : 26, label, palette, clicked -> {
            onClose();
            action.run();
        });
        button.setFont(font);
        actions.add(button);
        if (isMenu()) {
            button.setAppearance(OreButton.Appearance.MENU);
            add(button);
        } else {
            actionColumn.addChild(button);
            addRenderableWidget(button);
        }
        return button;
    }

    @Override
    protected void init() {
        font = OreFonts.font(font, fontId);
        contentFont = font;
        column.spacing(isMenu() ? 0 : 4);
        actionColumn.spacing(4);
        for (int key : new int[] {GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_SPACE}) {
            if (InputConstants.isKeyDown(minecraft.getWindow().getWindow(), key)) {
                openingKeys.add(key);
            }
        }
        buildContent.accept(this);
        if (communityModal()) {
            close = addRenderableWidget(new OrePixelButton(Component.translatable("gui.cancel"), OrePixelIcons.Icon.CROSS, button -> onClose()));
            back = addRenderableWidget(new OrePixelButton(Component.translatable("gui.back"), OrePixelIcons.Icon.CHEVRON_LEFT, button -> onClose()));
        } else {
            close = addRenderableWidget(new OreButton(0, 0, 18, 18, Component.translatable("gui.cancel"), OrePalette.GENERAL, button -> onClose()));
            close.setAppearance(OreButton.Appearance.SEAMLESS);
            close.setIcon(OreIcons.CLOSE.texture(), true);
        }
        close.setTooltip(Tooltip.create(Component.translatable("gui.cancel")));
        repositionElements();
    }

    @Override
    protected void repositionElements() {
        // Screen.init may restore Minecraft's font when an existing GUI layer is reattached.
        if (contentFont != null) font = contentFont;
        cancelInteraction();
        ScreenRectangle source = anchor == null ? null : anchor.get();
        int headerHeight = communityModal() ? 20 : 26;
        panelWidth = Math.max(80, Math.min(source == null ? preferredPanelWidth() : source.width(), width - 32));
        column.setWidth(panelWidth - (isMenu() ? 2 : 24));
        actionColumn.setWidth(panelWidth - (communityModal() ? 24 : 10));
        int confirmationHeight = confirmation == null ? 0 : 22;
        actionHeight = (isMenu() || actions.isEmpty() ? 0 : actionColumn.getHeight() + 14) + confirmationHeight;
        contentHeight = column.getHeight() + (isMenu() ? 0 : 22);
        panelHeight = Math.min(height - 24, contentHeight + headerHeight + 1 + actionHeight);
        panelX = source == null ? (width - panelWidth) / 2 : Mth.clamp(source.left(), 12, width - panelWidth - 12);
        panelY = source == null ? (height - panelHeight) / 2 : Mth.clamp(source.bottom() + 2, 12, height - panelHeight - 12);
        var shell = OreMenuPresets.modal(new SpacerElement(0, 0), new SpacerElement(0, 0), new SpacerElement(0, 0),
                new OreMenuPresets.Options(OreGrid.Insets.NONE, 0, headerHeight - 1, actionHeight, 0, 0, 0));
        shell.setSize(Math.max(1, panelWidth - 2), Math.max(1, panelHeight - 2));
        shell.setPosition(panelX + 1, panelY + 1);
        actionTop = shell.bounds(OreMenuLayout.Region.FOOTER).top();
        var body = shell.bounds(OreMenuLayout.Region.BODY);
        viewport = new ScreenRectangle(body.left(), body.top(), body.width(), Math.max(1, body.height()));
        actionColumn.setPosition(panelX + (communityModal() ? 12 : 5), actionTop + confirmationHeight + 7);
        if (confirmation != null) {
            confirmation.setWidth(panelWidth - 2);
            confirmation.setPosition(panelX + 1, actionTop);
        }
        close.setPosition(panelX + panelWidth - 21, panelY + (communityModal() ? 1 : 4));
        if (back != null) back.setPosition(panelX + 2, panelY + 1);
        clampScroll();
        arrangeContent();
        revealFocused();
    }

    @Override
    protected void arrangeContent() {
        column.setPosition(viewport.left() + (isMenu() ? 0 : 11), viewport.top() + (isMenu() ? 0 : 11) - scrollOffset);
    }

    protected boolean isOpeningKey(int keyCode) {
        return openingKeys.contains(keyCode);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (anchor != null && (mouseX < panelX || mouseX >= panelX + panelWidth || mouseY < panelY || mouseY >= panelY + panelHeight)) {
            onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return isOpeningKey(keyCode) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return openingKeys.remove(keyCode) || super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, isMenu() ? 0x80000000 : 0xB2000000);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, OreDrawing.BACKGROUND);
        if (communityModal()) {
            graphics.fill(panelX + 1, panelY + 20, panelX + panelWidth - 1, panelY + panelHeight - 1, OreDrawing.PANEL);
            OreDrawing.outline(graphics, panelX + 1, panelY + 1, panelWidth - 2, 19, 0xFF5A5B5C);
            graphics.fill(panelX + 2, panelY + 2, panelX + panelWidth - 2, panelY + 19, OreDrawing.HEADER);
            if (actionHeight > 0) {
                graphics.fill(panelX + 1, actionTop, panelX + panelWidth - 1, panelY + panelHeight - 1, OreDrawing.HEADER);
                divider(graphics, actionTop);
                if (confirmation != null) divider(graphics, actionTop + 22);
            }
            OreDrawing.scaledText(graphics, font, title, panelX + 23, panelY + 1,
                    panelX + panelWidth - 23, panelY + 19, OreDrawing.TEXT, OreDrawing.SMALL_TEXT);
            return;
        }
        graphics.fill(panelX + 1, panelY + 26, panelX + panelWidth - 1, panelY + panelHeight - 1, 0xFF373A39);
        OreDrawing.bevel(graphics, panelX + 1, panelY + 1, panelWidth - 2, 25, 0xFF585959, 0xFF797A7A, 0xFF636562);
        if (actionHeight > 0) {
            OreDrawing.bevel(graphics, panelX + 1, actionTop, panelWidth - 2, actionHeight,
                    0xFF585959, 0xFF797A7A, 0xFF636562);
        }
        OreDrawing.scaledText(graphics, font, title, panelX + 8, panelY + 1, panelX + panelWidth - 16, panelY + 26,
                OreDrawing.TEXT, OreDrawing.SMALL_TEXT);
    }

    private void divider(GuiGraphics graphics, int y) {
        graphics.fill(panelX + 1, y, panelX + panelWidth - 1, y + 1, 0xFF5A5B5C);
        graphics.fill(panelX + 1, y + 1, panelX + panelWidth - 1, y + 2, 0xFF333334);
    }
}
