package com.kyivsec.oreui.client.widget;

import com.kyivsec.oreui.client.style.OreDrawing;
import com.kyivsec.oreui.client.style.OrePalette;
import com.kyivsec.oreui.client.style.OrePixelIcons;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class OreAccordion extends OreButton {
    private final Font font;
    private final Component body;
    private final Consumer<Boolean> onChange;
    private MultiLineLabel lines;
    private boolean expanded;

    public OreAccordion(Font font, Component title, Component body, boolean expanded, Consumer<Boolean> onChange) {
        super(0, 0, 200, 20, title, OrePalette.GENERAL, button -> {});
        this.font = Objects.requireNonNull(font);
        this.body = Objects.requireNonNull(body);
        this.onChange = Objects.requireNonNull(onChange);
        this.expanded = expanded;
        setWidth(200);
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        if (this.expanded == expanded) return;
        this.expanded = expanded;
        resizeBody();
        onChange.accept(expanded);
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        if (font != null) {
            lines = MultiLineLabel.create(font, body, Math.max(1, (int) ((width - 16) / OreDrawing.DESCRIPTION_TEXT)));
            resizeBody();
        }
    }

    private void resizeBody() {
        setHeight(20 + (expanded ? Math.max(16, lines.getLineCount() * 6 + 10) : 0));
    }

    @Override
    public void onPress() {
        setExpanded(!expanded);
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, Component.translatable("oreui.accordion.state", getMessage(),
                Component.translatable(expanded ? "oreui.expanded" : "oreui.collapsed")));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return mouseY < getY() + 20 && super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void renderOutline(GuiGraphics graphics) {
        if (active && visible && isFocused()) OreDrawing.buttonFocus(graphics, getX(), getY(), width, 20);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = getX(), y = getY();
        graphics.fill(x, y, getRight(), getBottom(), OreDrawing.BACKGROUND);
        OreDrawing.bevel(graphics, x + 1, y + 1, width - 2, 18,
                isPressed() ? OreDrawing.PANEL : OreDrawing.HEADER, 0xFF5A5B5C, 0xFF333334);
        OreDrawing.scaledText(graphics, font, getMessage(), x + 7, y + 1,
                Math.min(getRight() - 22, x + 7 + (int) Math.ceil(font.width(getMessage()) * OreDrawing.SMALL_TEXT)),
                y + 19, active ? OreDrawing.TEXT : OreDrawing.MUTED, OreDrawing.SMALL_TEXT);
        OrePixelIcons.draw(graphics, expanded ? OrePixelIcons.Icon.CHEVRON_UP : OrePixelIcons.Icon.CHEVRON_DOWN,
                getRight() - 16, y + 6, 8, active ? OreDrawing.TEXT : OreDrawing.MUTED);
        if (expanded) {
            graphics.fill(x + 1, y + 20, getRight() - 1, getBottom() - 1, OreDrawing.HEADER);
            graphics.pose().pushPose();
            graphics.enableScissor(x + 1, y + 20, getRight() - 1, getBottom() - 1);
            try {
                graphics.pose().translate(x + 8, y + 25, 0);
                graphics.pose().scale(OreDrawing.DESCRIPTION_TEXT, OreDrawing.DESCRIPTION_TEXT, 1);
                lines.renderLeftAlignedNoShadow(graphics, 0, 0, 9, 0xFFD0D1D4);
            } finally {
                graphics.pose().popPose();
                graphics.disableScissor();
            }
        }
        renderOutline(graphics);
    }
}
