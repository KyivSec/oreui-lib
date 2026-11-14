package com.kyivsec.oreui.client.widget;

import com.kyivsec.oreui.client.screen.OrePopupScreen;
import com.kyivsec.oreui.client.style.OrePalette;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class OreKeybindButton extends OreButton {
    private final Component label;
    private final Consumer<InputConstants.Key> onChange;
    private InputConstants.Key key;

    public OreKeybindButton(Component label, InputConstants.Key initialKey, Consumer<InputConstants.Key> onChange) {
        super(0, 0, 100, 28, label, OrePalette.GENERAL, button -> {});
        this.label = label;
        this.key = initialKey;
        this.onChange = onChange;
        updateMessage();
    }

    public InputConstants.Key getKey() {
        return key;
    }

    private void updateMessage() {
        setMessage(Component.translatable("oreui.control.value", label, key.getDisplayName()));
    }

    private void bind(InputConstants.Key next) {
        if (!key.equals(next)) {
            key = next;
            updateMessage();
            onChange.accept(next);
        }
    }

    @Override
    public void onPress() {
        Minecraft.getInstance().pushGuiLayer(new OrePopupScreen(label, popup -> popup.add(new OreLabel(popup.font(),
                Component.translatable("oreui.keybind.prompt"), 100))) {
            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                if (isOpeningKey(keyCode)) {
                    return true;
                }
                if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                    onClose();
                } else {
                    bind(keyCode == GLFW.GLFW_KEY_BACKSPACE ? InputConstants.UNKNOWN : InputConstants.getKey(keyCode, scanCode));
                    onClose();
                }
                return true;
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (getChildAt(mouseX, mouseY).orElse(null) instanceof OreButton) {
                    return super.mouseClicked(mouseX, mouseY, button);
                }
                bind(InputConstants.Type.MOUSE.getOrCreate(button));
                onClose();
                return true;
            }
        });
    }
}
