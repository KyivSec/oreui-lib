package com.kyivsec.oreui.client.widget;

import com.kyivsec.oreui.client.style.OreDrawing;
import com.kyivsec.oreui.client.style.OrePalette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class OreButton extends Button {
    public enum Appearance {
        RAISED, SEAMLESS, NAVIGATION, SELECTION, HORIZONTAL_NAVIGATION, SELECTABLE_ICON, MENU, PAGINATION
    }

    private final OrePalette palette;
    private boolean pointerPressed;
    private int pressedKey = -1;
    private boolean selected;
    private Appearance appearance = Appearance.RAISED;
    private ResourceLocation icon;
    private boolean iconOnly;
    private boolean verticalContent;
    private Component badge;
    private int iconSize = 16;
    private boolean explicitIconSize;
    private Font textFont;

    /** Optional per-widget font; null restores Minecraft's default renderer. */
    public void setFont(Font font) {
        textFont = font;
    }

    protected Font font() {
        return textFont == null ? Minecraft.getInstance().font : textFont;
    }

    /** Explicit draw size for both raised and reference appearances, including seamless icon-only buttons. */
    public void setIconSize(int size) {
        if (size < 1) throw new IllegalArgumentException("Icon size must be positive");
        iconSize = size;
        explicitIconSize = true;
    }

    int referenceIconSize() {
        return explicitIconSize ? iconSize : appearance == Appearance.NAVIGATION ? Math.min(14, height) : 8;
    }

    ReferenceContent referenceIconContent(int left, int right, int labelWidth, int size, int gap, boolean alignLeft) {
        return referenceContent(getX() + width / 2, iconOnly ? getX() : left, iconOnly ? getRight() : right,
                iconOnly ? 0 : labelWidth, size, iconOnly ? 0 : gap, alignLeft && !iconOnly);
    }

    public void setBadge(Component badge) {
        this.badge = badge;
    }

    public OreButton(int x, int y, int width, int height, Component message, OrePalette palette, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.palette = palette;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        if (appearance == Appearance.RAISED) appearance = Appearance.SELECTION;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setAppearance(Appearance appearance) {
        this.appearance = appearance;
    }

    /** Accepts a 16x16 PNG from any resource namespace, or a built-in OreUI icon. Null removes the icon. */
    public void setIcon(ResourceLocation icon, boolean iconOnly) {
        this.icon = icon;
        this.iconOnly = iconOnly;
    }

    public void setVerticalContent(boolean verticalContent) {
        this.verticalContent = verticalContent;
    }

    public void cancelPress() {
        pointerPressed = false;
        pressedKey = -1;
    }

    static int contentStart(int width, int textWidth) {
        return Math.max(6, (width - 22 - textWidth) / 2);
    }

    record ReferenceContent(int iconX, int textLeft, int textRight) {}

    static ReferenceContent referenceContent(int centerX, int left, int right, int textWidth,
                                             int iconSize, int gap, boolean alignLeft) {
        int labelWidth = Math.max(0, Math.min(textWidth, right - left - iconSize - gap));
        int groupWidth = iconSize + gap + labelWidth;
        int groupX = alignLeft ? left : Math.max(left, Math.min(centerX - groupWidth / 2, right - groupWidth));
        return new ReferenceContent(groupX, groupX + iconSize + gap, groupX + groupWidth);
    }

    static ScreenRectangle referenceFaceBounds(int x, int y, int width, int height, Appearance appearance,
                                               boolean selected, boolean pressed, boolean enabled) {
        int top = selected ? appearance == Appearance.SELECTION ? 4 : appearance == Appearance.SELECTABLE_ICON ? 3 : 2 : 1;
        int bottom = selected || pressed || !enabled ? 1 : 3;
        if (appearance == Appearance.SELECTABLE_ICON) bottom = selected ? 1 : 3;
        return new ScreenRectangle(x + 1, y + top, Math.max(0, width - 2), Math.max(0, height - top - bottom));
    }

    static ScreenRectangle referenceMarkerBounds(int x, int y, int width, int height, Appearance appearance) {
        int marker = Math.max(0, Math.min(width - 4, appearance == Appearance.PAGINATION ? 8 : 26));
        return new ScreenRectangle(x + (width - marker) / 2,
                y + height - (appearance == Appearance.PAGINATION ? 1 : 2), marker, 1);
    }

    public void renderOutline(GuiGraphics graphics) {
        if (!visible || !active) {
            return;
        }
        if (isFocused() && (appearance != Appearance.RAISED || !isPressed())) {
            OreDrawing.buttonFocus(graphics, getX(), getY(), width, height);
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        pointerPressed = true;
        super.onClick(mouseX, mouseY);
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        pointerPressed = false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!active || !visible || !isFocused() || !CommonInputs.selected(keyCode)) {
            return false;
        }
        if (pressedKey == -1) {
            pressedKey = keyCode;
            super.keyPressed(keyCode, scanCode, modifiers);
        }
        return true;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (keyCode == pressedKey) {
            pressedKey = -1;
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused) {
            cancelPress();
        }
    }

    protected boolean isPressed() {
        return active && visible && isFocused() && (pointerPressed || pressedKey != -1);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!active || !isFocused()) {
            cancelPress();
        }
        boolean pressed = isPressed();
        if (appearance != Appearance.RAISED) {
            renderReferenceButton(graphics, pressed);
            return;
        }
        OreDrawing.raisedButton(graphics, getX(), getY(), width, height, palette, active, isHovered(), pressed);
        ScreenRectangle face = OreDrawing.buttonFaceBounds(getX(), getY(), width, height, active, pressed);
        int color = palette.text(active);
        int textLeft = getX() + 6;
        int textRight = getRight() - 6;
        int textTop = face.top();
        int textBottom = face.bottom();
        if (icon != null) {
            var font = font();
            int labelWidth = Math.max(0, Math.min(font.width(getMessage()), width - iconSize - 18));
            int iconX = iconOnly || verticalContent ? getX() + (width - iconSize) / 2
                    : getX() + Math.max(6, (width - iconSize - 6 - labelWidth) / 2);
            int contentHeight = verticalContent && !iconOnly ? iconSize + 4 + font.lineHeight : iconSize;
            int iconY = face.top() + (face.height() - contentHeight) / 2;
            OreDrawing.icon(graphics, icon, iconX, iconY, iconSize, color);
            if (verticalContent && !iconOnly) {
                textTop = iconY + iconSize + 4;
                textBottom = textTop + font.lineHeight;
            } else if (!iconOnly) {
                textLeft = iconX + iconSize + 6;
                textRight = textLeft + labelWidth;
            }
        }
        if (!iconOnly) {
            OreDrawing.scrollingText(graphics, font(), getMessage(),
                    textLeft, textTop, textRight, textBottom, color);
        }
        renderOutline(graphics);
    }

    private void renderReferenceButton(GuiGraphics graphics, boolean pressed) {
        int x = getX(), y = getY();
        boolean hot = active && (isHovered() || isFocused());
        int top = 0, bottom = 0;
        int color = active ? OreDrawing.TEXT : 0xFF8C8D90;
        switch (appearance) {
            case SELECTION, HORIZONTAL_NAVIGATION, SELECTABLE_ICON -> {
                boolean selection = appearance == Appearance.SELECTION;
                ScreenRectangle face = referenceFaceBounds(x, y, width, height, appearance, selected, pressed, active);
                top = face.top() - y;
                bottom = getBottom() - face.bottom();
                int fill = selection ? selected ? hot ? 0xFF275B1A : 0xFF387E24
                        : hot || pressed || !active ? 0xFFB1B2B6 : 0xFFD0D1D5
                        : selected ? appearance == Appearance.SELECTABLE_ICON ? 0xFF1C4A12
                        : hot ? 0xFF444545 : 0xFF373A39 : hot || pressed ? 0xFF585959 : 0xFF515453;
                int edge = selection ? selected ? 0xFF50913D : 0xFFE3E3E5
                        : selected ? appearance == Appearance.SELECTABLE_ICON ? 0xFF3C8527 : 0xFF4B4E4C : 0xFF636562;
                graphics.fill(x, y + top - 1, getRight(), getBottom(), OreDrawing.BACKGROUND);
                if (!selected && !pressed && active) {
                    graphics.fill(x + 1, getBottom() - bottom, getRight() - 1, getBottom() - 1,
                            selection || appearance == Appearance.HORIZONTAL_NAVIGATION ? 0xFF59595B : 0xFF313332);
                }
                graphics.fill(face.left(), face.top(), face.right(), face.bottom(), fill);
                graphics.fill(face.left(), face.bottom() - 1, face.right(), face.bottom(), edge);
                graphics.fill(face.right() - 1, face.top(), face.right(), face.bottom(), edge);
                int light = selected && appearance != Appearance.HORIZONTAL_NAVIGATION ? edge : 0x33FFFFFF;
                graphics.fill(face.left(), face.top(), face.right() - 1, face.top() + 1, light);
                graphics.fill(face.left(), face.top() + 1, face.left() + 1, face.bottom() - 1, light);
                if (selection && !selected) color = active ? 0xFF1E1E20 : 0x801E1E20;
                else if (!active) color = 0x80FFFFFF;
            }
            case NAVIGATION -> {
                if (selected || hot) {
                    graphics.fill(x, y, getRight(), getBottom(), OreDrawing.HEADER);
                    graphics.fill(x, y, getRight(), y + 1, 0xFF2B2C2C);
                    graphics.fill(x, getBottom() - 1, getRight(), getBottom(), 0xFF5A5B5C);
                }
            }
            case SEAMLESS -> {
                if (hot || pressed) graphics.fill(x + 2, y + 2, getRight() - 2, getBottom() - 2,
                        pressed ? 0x33000000 : 0x1AFFFFFF);
            }
            case MENU -> {
                OreDrawing.bevel(graphics, x, y, width, height, hot || pressed ? 0xFF373A39 : 0xFF636365,
                        0xFF999999, 0xFF999999);
            }
            case PAGINATION -> {
                color = OreDrawing.TEXT;
                if (selected || pressed) {
                    graphics.fill(x, y, getRight(), getBottom(), 0xFF48494A);
                } else {
                    OreDrawing.bevel(graphics, x, y, width, height, 0xFF48494A, 0xFF6D6D6E, 0x80000000);
                }
                graphics.fill(x, getBottom() - 1, getRight(), getBottom(), 0xFF37383A);
            }
            default -> { }
        }
        var font = font();
        int padding = appearance == Appearance.PAGINATION ? 2 : appearance == Appearance.NAVIGATION ? 7 : 6;
        int left = x + padding, right = getRight() - padding;
        int contentTop = y + top, contentBottom = getBottom() - bottom;
        float textScale = appearance == Appearance.PAGINATION ? OreDrawing.PAGINATION_TEXT : OreDrawing.SMALL_TEXT;
        if (badge != null) {
            int badgeWidth = Math.max(15, (int) Math.ceil(font.width(badge) * OreDrawing.SMALL_TEXT) + 10);
            int badgeX = getRight() - badgeWidth - 5;
            int badgeY = contentTop + (contentBottom - contentTop - 15) / 2;
            graphics.fill(badgeX, badgeY, badgeX + badgeWidth, badgeY + 15, selected ? 0xFFD2D4D6 : 0xFF37373A);
            OreDrawing.scaledText(graphics, font, badge, badgeX, badgeY, badgeX + badgeWidth, badgeY + 15,
                    selected ? 0xFF1E1E20 : OreDrawing.TEXT, OreDrawing.SMALL_TEXT);
            right = badgeX - 4;
        }
        boolean alignLeft = appearance == Appearance.NAVIGATION || appearance == Appearance.MENU;
        int labelWidth = (int) Math.ceil(font.width(getMessage()) * textScale);
        boolean stacked = verticalContent || appearance == Appearance.SELECTABLE_ICON && !iconOnly;
        if (icon != null && stacked && !iconOnly) {
            int size = referenceIconSize();
            int iconY = contentTop + (contentBottom - contentTop - size - 12) / 2;
            OreDrawing.icon(graphics, icon, x + (width - size) / 2, iconY, size, color);
            contentTop = iconY + size + 4;
            contentBottom = contentTop + 8;
            ReferenceContent content = referenceContent(x + width / 2, left, right, labelWidth, 0, 0, false);
            left = content.textLeft();
            right = content.textRight();
        } else if (icon != null) {
            int size = referenceIconSize();
            int gap = appearance == Appearance.NAVIGATION ? 6 : 4;
            ReferenceContent content = referenceIconContent(left, right, labelWidth, size, gap, alignLeft);
            int iconY = contentTop + (contentBottom - contentTop - size) / 2;
            OreDrawing.icon(graphics, icon, content.iconX(), iconY, size, color);
            left = content.textLeft();
            right = content.textRight();
        } else {
            ReferenceContent content = referenceContent(x + width / 2, left, right, labelWidth, 0, 0, alignLeft);
            left = content.textLeft();
            right = content.textRight();
        }
        if (!iconOnly) {
            OreDrawing.scaledText(graphics, font, getMessage(), left, contentTop, right, contentBottom, color, textScale);
        }
        if (selected && (appearance == Appearance.SELECTION || appearance == Appearance.HORIZONTAL_NAVIGATION || appearance == Appearance.PAGINATION)) {
            ScreenRectangle marker = referenceMarkerBounds(x, y, width, height, appearance);
            graphics.fill(marker.left(), marker.top(), marker.right(), marker.bottom(), active ? OreDrawing.TEXT : 0x80FFFFFF);
        }
        renderOutline(graphics);
    }
}
