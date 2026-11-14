package com.kyivsec.oreui.client.screen;

import com.kyivsec.oreui.client.style.OreDrawing;
import com.kyivsec.oreui.client.style.OrePalette;
import com.kyivsec.oreui.client.style.OrePixelIcons;
import com.kyivsec.oreui.client.widget.OreButton;
import com.kyivsec.oreui.client.widget.OreDropdown;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Anchored list without a popup header or screen-dimming overlay. */
public final class OreDropdownScreen extends OreScreen {
    private final OreDropdown owner;
    private final List<OreButton> rows = new ArrayList<>();
    private final Set<Integer> openingKeys = new HashSet<>();
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;

    public OreDropdownScreen(OreDropdown owner) {
        super(owner.getMessage());
        this.owner = owner;
    }

    @Override
    protected void init() {
        rows.clear();
        for (int key : new int[] {GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_SPACE}) {
            if (InputConstants.isKeyDown(minecraft.getWindow().getWindow(), key)) openingKeys.add(key);
        }
        for (int i = 0; i < owner.getOptions().size(); i++) {
            int index = i;
            rows.add(addContentWidget(new OreButton(0, 0, 100, 18, owner.getOptions().get(i), OrePalette.GENERAL, button -> {
                onClose();
                owner.select(index);
            }) {
                @Override
                protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                    graphics.fill(getX(), getY(), getRight(), getBottom(), isHovered() ? OreDrawing.INSET : OreDrawing.PANEL);
                    if (isHovered() || isFocused()) OreDrawing.outline(graphics, getX(), getY(), width, height, 0xFF5A5B5C);
                    OreDrawing.scaledText(graphics, font, getMessage(), getX() + 11, getY(),
                            Math.min(getRight() - 19, getX() + 11 + (int) Math.ceil(font.width(getMessage()) * OreDrawing.SMALL_TEXT)),
                            getBottom(), OreDrawing.TEXT, OreDrawing.SMALL_TEXT);
                    if (index == owner.getSelectedIndex()) OrePixelIcons.draw(graphics, OrePixelIcons.Icon.TICK,
                            getRight() - 15, getY() + 5, 8, OreDrawing.TEXT);
                }
            }));
        }
        repositionElements();
        setInitialFocus(rows.get(owner.getSelectedIndex()));
        revealFocused();
    }

    @Override
    protected void repositionElements() {
        ScreenRectangle anchor = owner.inputBounds();
        panelWidth = Math.max(24, Math.min(anchor.width(), width - 24));
        panelX = Math.clamp(anchor.left(), 8, Math.max(8, width - panelWidth - 12));
        int below = height - anchor.bottom() - 8;
        int above = anchor.top() - 8;
        boolean useAbove = below < Math.min(74, owner.getOptions().size() * 18 + 2) && above > below;
        int available = Math.max(20, useAbove ? above : below);
        panelHeight = Math.min(owner.getOptions().size() * 18 + 2, available);
        panelY = useAbove ? anchor.top() - panelHeight : anchor.bottom() - 2;
        panelY = Math.clamp(panelY, 8, Math.max(8, height - panelHeight - 8));
        contentHeight = rows.size() * 18;
        viewport = new ScreenRectangle(panelX + 1, panelY + 1, panelWidth - 2, panelHeight - 2);
        clampScroll();
        arrangeContent();
    }

    @Override
    protected void arrangeContent() {
        for (int i = 0; i < rows.size(); i++) {
            rows.get(i).setWidth(viewport.width());
            rows.get(i).setPosition(viewport.left(), viewport.top() + i * 18 - scrollOffset);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, OreDrawing.BACKGROUND);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX < panelX || mouseX >= panelX + panelWidth + 10 || mouseY < panelY || mouseY >= panelY + panelHeight) {
            onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return openingKeys.contains(keyCode) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return openingKeys.remove(keyCode) || super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        owner.closeMenu();
        super.removed();
    }
}
