package com.kyivsec.oreui.client.widget;

import java.util.function.DoubleConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class OreNumberField extends OreTextField {
    private final DoubleConsumer onCommit;
    private double committedValue;

    public OreNumberField(Font font, Component label, double initialValue, DoubleConsumer onCommit) {
        super(font, 0, 0, 100, label, Component.empty());
        if (!Double.isFinite(initialValue)) {
            throw new IllegalArgumentException("Initial numeric value must be finite");
        }
        this.onCommit = onCommit;
        committedValue = initialValue;
        setMaxLength(32);
        setTextShadow(false);
        setFilter(OreNumberField::isIntermediateValue);
        setValue(Double.toString(initialValue));
    }

    public static boolean isIntermediateValue(String value) {
        return value.matches("[+-]?[0-9]*(\\.[0-9]*)?([eE][+-]?[0-9]*)?");
    }

    public double getCommittedValue() {
        return committedValue;
    }

    public boolean commit() {
        try {
            double next = Double.parseDouble(getValue());
            if (Double.isFinite(next)) {
                boolean changed = next != committedValue;
                committedValue = next;
                setValue(Double.toString(next));
                if (changed) {
                    onCommit.accept(next);
                }
                return true;
            }
        } catch (NumberFormatException ignored) {
        }
        setValue(Double.toString(committedValue));
        return false;
    }

    @Override
    public void setFocused(boolean focused) {
        if (!focused && isFocused()) {
            commit();
        }
        super.setFocused(focused);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (active && isFocused() && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            commit();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
