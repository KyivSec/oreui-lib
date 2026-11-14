package com.kyivsec.oreui.client.widget;

import com.kyivsec.oreui.client.screen.OreDropdownScreen;
import com.kyivsec.oreui.client.style.OreDrawing;
import com.kyivsec.oreui.client.style.OrePixelIcons;
import com.kyivsec.oreui.client.style.OrePalette;
import java.util.List;
import java.util.function.IntConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;

public class OreDropdown extends OreButton {
    private final Component label;
    private final List<Component> options;
    private final IntConsumer onChange;
    private int selectedIndex;
    private boolean opened;

    public OreDropdown(Component label, List<Component> options, int selectedIndex, IntConsumer onChange) {
        super(0, 0, 100, 33, label, OrePalette.GENERAL, button -> {});
        this.label = label;
        this.options = List.copyOf(options);
        this.onChange = onChange;
        if (selectedIndex < 0 || selectedIndex >= options.size()) {
            throw new IndexOutOfBoundsException(selectedIndex);
        }
        this.selectedIndex = selectedIndex;
        updateMessage();
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public List<Component> getOptions() {
        return options;
    }

    public ScreenRectangle inputBounds() {
        return new ScreenRectangle(getX(), getY() + 11, width, 22);
    }

    public void closeMenu() {
        opened = false;
    }

    @Override
    public void renderOutline(GuiGraphics graphics) {
        if (visible && active && isFocused() && !opened) OreDrawing.buttonFocus(graphics, getX(), getY() + 11, width, 22);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        var font = Minecraft.getInstance().font;
        OreDrawing.scaledText(graphics, font, label, getX(), getY(),
                Math.min(getRight(), getX() + (int) Math.ceil(font.width(label) * OreDrawing.SMALL_TEXT)),
                getY() + 10, active ? OreDrawing.TEXT : OreDrawing.MUTED, OreDrawing.SMALL_TEXT);
        int y = getY() + 11;
        int controlHeight = opened ? 20 : 22;
        graphics.fill(getX(), y, getRight(), y + controlHeight, active ? OreDrawing.BACKGROUND : 0xFF8C8D90);
        if (!opened) graphics.fill(getX() + 1, y + 19, getRight() - 1, y + 21, 0xFF58585A);
        OreDrawing.bevel(graphics, getX() + 1, y + 1, width - 2, 18,
                opened || isPressed() || !active ? 0xFFB1B2B5 : 0xFFD0D1D4,
                opened ? 0xFFD0D1D3 : 0xFFE3E3E5, opened ? 0xFFD0D1D3 : 0xFFE3E3E5);
        Component selected = options.get(selectedIndex);
        OreDrawing.scaledText(graphics, font, selected, getX() + 8, y + 1,
                Math.min(getRight() - 22, getX() + 8 + (int) Math.ceil(font.width(selected) * OreDrawing.SMALL_TEXT)),
                y + 19, OreDrawing.BACKGROUND, OreDrawing.SMALL_TEXT);
        OrePixelIcons.draw(graphics, opened ? OrePixelIcons.Icon.CHEVRON_UP : OrePixelIcons.Icon.CHEVRON_DOWN,
                getRight() - 17, y + 6, 8, OreDrawing.BACKGROUND);
        renderOutline(graphics);
    }

    public void select(int index) {
        if (index < 0 || index >= options.size()) {
            throw new IndexOutOfBoundsException(index);
        }
        if (selectedIndex != index) {
            selectedIndex = index;
            updateMessage();
            onChange.accept(index);
        }
    }

    private void updateMessage() {
        setMessage(Component.translatable("oreui.control.value", label, options.get(selectedIndex)));
    }

    @Override
    public void onPress() {
        if (!active || !visible || opened) return;
        opened = true;
        Minecraft.getInstance().pushGuiLayer(new OreDropdownScreen(this));
    }
}
