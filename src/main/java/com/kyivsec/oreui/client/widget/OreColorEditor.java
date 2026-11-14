package com.kyivsec.oreui.client.widget;

import java.util.Objects;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.network.chat.Component;

/** Inline color selection with immediate updates; does not open a screen or GUI layer. */
public class OreColorEditor extends LinearLayout {
    private final OreColorSquare square;
    private final OreHueSlider hue;
    private final OreLabel preview;
    private final OreTextField hex;
    private final IntConsumer onChange;
    private boolean refreshing;

    public OreColorEditor(Font font, Component label, int initialColor, IntConsumer onChange) {
        super(0, 0, Orientation.VERTICAL);
        this.onChange = Objects.requireNonNull(onChange);
        spacing(8);
        square = addChild(new OreColorSquare(label, initialColor, color -> {
            if (!refreshing) {
                refresh(true);
                this.onChange.accept(color);
            }
        }));
        hue = addChild(new OreHueSlider(Component.translatable("oreui.color.hue"), square.getHue(), value -> {
            if (!refreshing) square.setHue(value);
        }));
        preview = addChild(new OreLabel(font, Component.literal(OreColorPicker.format(initialColor)), 128)
                .decorate(OreLabel.Decoration.BANNER, initialColor).colorSwatch());
        hex = addChild(new OreTextField(font, 0, 0, 128, Component.translatable("oreui.color.hex"), Component.empty()));
        hex.setMaxLength(9);
        hex.setValue(OreColorPicker.format(initialColor));
        hex.setFilter(value -> value.matches("#?[0-9a-fA-F]{0,8}"));
        hex.setResponder(value -> {
            if (!refreshing && value.matches("#?([0-9a-fA-F]{6}|[0-9a-fA-F]{8})")) {
                int previous = getColor();
                refreshing = true;
                try {
                    square.setColor(OreColorPicker.parse(value));
                } finally {
                    refreshing = false;
                }
                refresh(false); // Preserve the user's text/cursor, including a lowercase or six-digit entry.
                if (getColor() != previous) this.onChange.accept(getColor());
            }
        });
        setWidth(128);
    }

    public int getColor() { return square.getColor(); }

    /** Updates all controls and notifies once if the ARGB color changes. */
    public void setColor(int color) {
        int previous = getColor();
        refreshing = true;
        try {
            square.setColor(color);
        } finally {
            refreshing = false;
        }
        refresh(true);
        if (getColor() != previous) onChange.accept(getColor());
    }

    private void refresh(boolean updateHex) {
        refreshing = true;
        try {
            hue.setHue(square.getHue());
            preview.decorate(OreLabel.Decoration.BANNER, getColor());
            preview.setMessage(Component.literal(OreColorPicker.format(getColor())));
            if (updateHex) hex.setValue(OreColorPicker.format(getColor()));
        } finally {
            refreshing = false;
        }
    }

    public void setWidth(int width) {
        // Keep the square usable in the gallery's short scroll viewport, even on very wide windows.
        int size = Math.clamp(width, 32, 128);
        square.setWidth(size);
        hue.setWidth(size);
        preview.setWidth(size);
        hex.setWidth(size);
        arrangeElements();
    }
}
