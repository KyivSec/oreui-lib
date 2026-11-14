package com.kyivsec.oreui.client.widget;

import com.kyivsec.oreui.client.screen.OrePopupScreen;
import com.kyivsec.oreui.client.style.OrePalette;
import java.util.Locale;
import java.util.function.IntConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class OreColorPicker extends OreButton {
    private final Component label;
    private final IntConsumer onChange;
    private int color;

    public OreColorPicker(Component label, int initialColor, IntConsumer onChange) {
        super(0, 0, 100, 28, label, OrePalette.GENERAL, button -> {});
        this.label = label;
        this.color = initialColor;
        this.onChange = onChange;
        updateMessage();
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        if (this.color != color) {
            this.color = color;
            updateMessage();
            onChange.accept(color);
        }
    }

    public static String format(int color) {
        return String.format(Locale.ROOT, "#%08X", color);
    }

    public static int parse(String value) {
        String hex = value.startsWith("#") ? value.substring(1) : value;
        if (!hex.matches("[0-9a-fA-F]{6}([0-9a-fA-F]{2})?")) {
            throw new IllegalArgumentException("Expected RRGGBB or AARRGGBB");
        }
        return (int) Long.parseLong(hex.length() == 6 ? "FF" + hex : hex, 16);
    }

    private void updateMessage() {
        setMessage(Component.translatable("oreui.control.value", label, Component.literal(format(color))));
    }

    @Override
    public void onPress() {
        Selection selection = new Selection(color);
        Minecraft.getInstance().pushGuiLayer(new OrePopupScreen(label, selection::build) {
            @Override
            protected int preferredPanelWidth() {
                // Keep the square and hue strip together in the viewport on short GUI layouts.
                return Math.min(super.preferredPanelWidth(), Math.max(112, height - 110));
            }
        });
    }

    private final class Selection {
        private final OreColorSquare square;
        private final OreHueSlider hue;
        private boolean refreshing;
        private EditBox hex;
        private OreLabel preview;

        private Selection(int color) {
            square = new OreColorSquare(Component.translatable("oreui.color.square"), color, value -> {
                if (!refreshing) refresh();
            });
            hue = new OreHueSlider(Component.translatable("oreui.color.hue"), square.getHue(), value -> {
                if (!refreshing) square.setHue(value);
            });
        }

        private void build(OrePopupScreen popup) {
            popup.add(square);
            popup.add(hue);
            preview = popup.add(new OreLabel(popup.font(), Component.literal(format(square.getColor())), 100)
                    .decorate(OreLabel.Decoration.BANNER, square.getColor()).colorSwatch());
            hex = popup.add(new OreTextField(popup.font(), 0, 0, 100, Component.translatable("oreui.color.hex"), Component.empty()));
            hex.setTextShadow(false);
            hex.setMaxLength(9);
            hex.setValue(format(square.getColor()));
            hex.setFilter(value -> value.matches("#?[0-9a-fA-F]{0,8}"));
            hex.setResponder(value -> {
                if (!refreshing && value.matches("#?([0-9a-fA-F]{6}|[0-9a-fA-F]{8})")) {
                    refreshing = true;
                    try {
                        square.setColor(parse(value));
                        hue.setHue(square.getHue());
                        refreshPreview();
                    } finally {
                        refreshing = false;
                    }
                }
            });
            popup.addAction(Component.translatable("gui.done"), OrePalette.COLORED, () -> {
                if (hex.getValue().matches("#?([0-9a-fA-F]{6}|[0-9a-fA-F]{8})")) {
                    setColor(parse(hex.getValue()));
                } else {
                    setColor(square.getColor());
                }
            });
        }

        private void refreshPreview() {
            preview.decorate(OreLabel.Decoration.BANNER, square.getColor());
            preview.setMessage(Component.literal(format(square.getColor())));
        }

        private void refresh() {
            refreshPreview();
            if (hex != null) {
                refreshing = true;
                try {
                    hex.setValue(format(square.getColor()));
                } finally {
                    refreshing = false;
                }
            }
        }
    }
}
