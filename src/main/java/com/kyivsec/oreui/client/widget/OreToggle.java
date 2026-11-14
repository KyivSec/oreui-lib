package com.kyivsec.oreui.client.widget;

import com.kyivsec.oreui.client.style.OreDrawing;
import com.kyivsec.oreui.client.style.OrePalette;
import com.kyivsec.oreui.client.style.OrePixelIcons;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import com.mojang.math.Axis;

public class OreToggle extends OreButton {
    public enum Kind {
        CHECKBOX, RADIO, SWITCH
    }

    private final Component label;
    private final Kind kind;
    private final Consumer<Boolean> onChange;
    private boolean checked;
    private OrePalette switchPalette = OrePalette.COLORED;
    private boolean switchSymbols;
    private boolean indeterminate;
    private OrePalette checkboxPalette = OrePalette.COLORED;

    public void setCheckboxPalette(OrePalette palette) {
        checkboxPalette = Objects.requireNonNull(palette);
    }

    public OrePalette getCheckboxPalette() {
        return checkboxPalette;
    }

    int checkboxFill() {
        if (!active) return 0xFFD0D1D4;
        if (!(checked || indeterminate)) return isPressed() ? 0xFF58585A : isHovered() ? 0xFFB1B2B5 : 0xFF8C8D90;
        return switch (checkboxPalette) {
            case COLORED -> isPressed() ? 0xFF2A641C : isHovered() ? 0xFF52A535 : 0xFF3C8527;
            case DESTRUCTIVE -> 0xFFAD1D1D;
            case WARNING -> 0xFFC09A0D;
            case REALMS -> 0xFF4A1CAC;
            case GENERAL -> 0xFFB1B2B6;
        };
    }

    int checkboxStroke() {
        if (!active) return 0xFFD0D1D4;
        if (!(checked || indeterminate)) return isPressed() ? 0xFF68686A : isHovered() ? 0xFFB9BABC : isFocused() ? 0xFFA3A4A6 : 0xFF97989B;
        return switch (checkboxPalette) {
            case COLORED -> isPressed() ? 0xFF3F7332 : isHovered() ? 0xFF75B75D : 0xFF639D52;
            case DESTRUCTIVE -> 0xFFD98180;
            case WARNING -> 0xFFE7C85B;
            case REALMS -> 0xFF9242E2;
            case GENERAL -> 0xFFE3E3E5;
        };
    }

    int checkboxHighlight() {
        return checkboxStroke();
    }

    public void setIndeterminate(boolean indeterminate) {
        this.indeterminate = kind == Kind.CHECKBOX && indeterminate;
    }

    public boolean isIndeterminate() {
        return indeterminate;
    }

    public OreToggle(Component label, Kind kind, boolean checked, Consumer<Boolean> onChange) {
        super(0, 0, 100, 28, label, OrePalette.COLORED, button -> {});
        this.label = label;
        this.kind = kind;
        this.onChange = onChange;
        this.checked = checked;
        updateMessage();
    }

    public boolean isChecked() {
        return checked;
    }

    public void setSwitchPalette(OrePalette palette) {
        switchPalette = Objects.requireNonNull(palette);
    }

    public OrePalette getSwitchPalette() {
        return switchPalette;
    }

    public void setSwitchSymbols(boolean symbols) {
        switchSymbols = symbols;
    }

    public boolean hasSwitchSymbols() {
        return switchSymbols;
    }

    ScreenRectangle switchBounds() {
        return new ScreenRectangle(getX() + 4, getY() + (height - 14) / 2, 25, 14);
    }

    ScreenRectangle switchHandleBounds() {
        ScreenRectangle bounds = switchBounds();
        return new ScreenRectangle(bounds.left() + (checked ? 11 : 0), bounds.top(), 14, 14);
    }

    @Override
    public void renderOutline(GuiGraphics graphics) {
        if (kind != Kind.SWITCH) {
            if (visible && active && isFocused()) {
                int size = kind == Kind.RADIO ? 24 : 12;
                int x = getX() + 4, y = getY() + (height - size) / 2;
                if (kind == Kind.RADIO) {
                    graphics.pose().pushPose();
                    graphics.pose().translate(x + 12, y + 12, 0);
                    graphics.pose().mulPose(Axis.ZP.rotationDegrees(45));
                    OreDrawing.outline(graphics, -10, -10, 20, 20, OreDrawing.TEXT);
                    graphics.pose().popPose();
                } else OreDrawing.buttonFocus(graphics, x, y, size, size);
            }
        } else if (visible && active && isFocused()) {
            ScreenRectangle handle = switchHandleBounds();
            OreDrawing.buttonFocus(graphics, handle.left(), handle.top(), handle.width(), handle.height());
        }
    }

    public void setChecked(boolean checked) {
        if (this.checked != checked) {
            this.checked = checked;
            updateMessage();
            onChange.accept(checked);
        }
    }

    private void updateMessage() {
        setMessage(Component.translatable("oreui.control.state", label, Component.translatable(checked ? "options.on" : "options.off")));
    }

    @Override
    public void onPress() {
        indeterminate = false;
        setChecked(kind == Kind.RADIO || !checked);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (kind == Kind.SWITCH) {
            renderSwitch(graphics);
            return;
        }
        int x = getX() + 4;
        int size = kind == Kind.RADIO ? 24 : 12;
        int y = getY() + (height - size) / 2;
        if (kind == Kind.RADIO) {
            graphics.pose().pushPose();
            graphics.pose().translate(x + 12, y + 12, 0);
            graphics.pose().mulPose(Axis.ZP.rotationDegrees(45));
            graphics.fill(-8, -8, 8, 8, OreDrawing.BACKGROUND);
            OreDrawing.bevel(graphics, -7, -7, 14, 14,
                    !active ? 0xFFB1B2B5 : checked ? isHovered() ? 0xFF275B1A : 0xFF387E24 : 0xFF8C8D90,
                    !active ? 0xFF8C8D90 : checked ? 0xFF50913D : 0xFFA3A4A6,
                    !active ? 0xFF8C8D90 : checked ? 0xFF50913D : 0xFFA3A4A6);
            if (checked) {
                int half = isPressed() ? 3 : 4;
                graphics.fill(-half, -half, 0, 0, active ? 0xFFE6E8EB : 0xFF8C8D90);
                graphics.fill(0, -half, half, 0, active ? OreDrawing.TEXT : 0xFFA3A4A6);
                graphics.fill(-half, 0, 0, half, active ? 0xFFD0D1D4 : 0xFF666666);
                graphics.fill(0, 0, half, half, active ? 0xFFE6E8EB : 0xFF8C8D90);
            }
            graphics.pose().popPose();
        } else {
            graphics.fill(x, y, x + 12, y + 12, active ? OreDrawing.BACKGROUND : 0xFF8C8D90);
            graphics.fill(x + 1, y + 1, x + 11, y + 11, checkboxStroke());
            graphics.fill(x + 2, y + 2, x + 10, y + 10, checkboxFill());
            int symbol = active ? checkboxPalette.text(true) : 0xFF8C8D90;
            if (indeterminate) graphics.fill(x + 3, y + 5, x + 9, y + 7, symbol);
            else if (checked) OrePixelIcons.draw(graphics, OrePixelIcons.Icon.TICK, x + 2, y + 2, 8, symbol);
        }
        var font = font();
        int labelLeft = x + size + (kind == Kind.CHECKBOX ? 5 : 8);
        int labelRight = kind == Kind.CHECKBOX
                ? Math.min(getRight() - 6, labelLeft + (int) Math.ceil(font.width(label) * OreDrawing.SMALL_TEXT)) : getRight() - 6;
        OreDrawing.scaledText(graphics, font, label, labelLeft, getY(), labelRight, getBottom(),
                active ? OreDrawing.TEXT : OreDrawing.MUTED, OreDrawing.SMALL_TEXT);
        renderOutline(graphics);
    }

    private void renderSwitch(GuiGraphics graphics) {
        ScreenRectangle bounds = switchBounds();
        int x = bounds.left();
        int y = bounds.top();
        int track = !active ? 0xFFB1B2B5 : checked ? switchPalette.background(true, false, false) : 0xFF8C8D90;
        int stroke = !active ? 0xFF8C8D90 : checked ? switchPalette.stroke() : 0xFFA3A4A6;
        graphics.fill(x, y + 2, x + 25, y + 14, active ? OreDrawing.BACKGROUND : stroke);
        graphics.fill(x + 1, y + 3, x + 24, y + 13, track);
        if (active) {
            OreDrawing.outline(graphics, x + 1, y + 3, 23, 10, stroke);
        }
        if (switchSymbols) {
            int symbol = !active ? 0xFF77787A : checked ? switchPalette.text(true) : 0xFF1E1E20;
            if (checked) {
                graphics.fill(x + 6, y + 5, x + 7, y + 11, symbol);
            } else {
                graphics.fill(x + 18, y + 5, x + 21, y + 6, symbol);
                graphics.fill(x + 18, y + 9, x + 21, y + 10, symbol);
                graphics.fill(x + 17, y + 6, x + 18, y + 9, symbol);
                graphics.fill(x + 21, y + 6, x + 22, y + 9, symbol);
            }
        }
        ScreenRectangle handle = switchHandleBounds();
        if (active) {
            OreDrawing.raisedButton(graphics, handle.left(), handle.top(), 14, 14,
                    OrePalette.GENERAL, true, isHovered(), false);
        } else {
            graphics.fill(handle.left(), handle.top(), handle.right(), handle.bottom(), 0xFF5A5A5A);
            graphics.fill(handle.left() + 1, handle.top() + 1, handle.right() - 1, handle.bottom() - 1, 0xFF8C8D90);
            graphics.fill(handle.left() + 1, handle.top() + 1, handle.right() - 1, handle.bottom() - 3, 0xFFB1B2B5);
        }
        OreDrawing.scrollingText(graphics, font(), label,
                x + 33, getY(), getRight() - 6, getBottom(), active ? OreDrawing.TEXT : OreDrawing.MUTED);
        renderOutline(graphics);
    }
}
