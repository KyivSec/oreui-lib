package com.kyivsec.oreui.client.widget;

import com.kyivsec.oreui.client.style.OrePalette;
import com.kyivsec.oreui.client.style.OreDrawing;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class OreNavigationBar extends LinearLayout implements Renderable {
    private final List<OreButton> buttons = new ArrayList<>();
    private final Orientation orientation;
    private int selectedIndex;
    private final LinearLayout headerSlot;
    private final List<LinearLayout> groupSlots = new ArrayList<>();
    private final Map<Integer, GroupHeading> groupHeadings = new HashMap<>();
    private OreLabel header;

    public OreNavigationBar(List<Component> labels, Orientation orientation, IntConsumer onSelect) {
        super(0, 0, orientation);
        if (labels.isEmpty()) {
            throw new IllegalArgumentException("Navigation requires at least one entry");
        }
        this.orientation = orientation;
        spacing(0);
        headerSlot = orientation == Orientation.VERTICAL ? addChild(new LinearLayout(0, 0, Orientation.VERTICAL)) : null;
        LinearLayout entries = addChild(new LinearLayout(0, 0, orientation));
        for (int i = 0; i < labels.size(); i++) {
            int index = i;
            LinearLayout row = orientation == Orientation.VERTICAL
                    ? entries.addChild(new LinearLayout(0, 0, Orientation.VERTICAL)) : entries;
            if (orientation == Orientation.VERTICAL) groupSlots.add(row.addChild(new LinearLayout(0, 0, Orientation.VERTICAL)));
            OreButton button = row.addChild(new OreButton(0, 0, 100, orientation == Orientation.HORIZONTAL ? 28 : 25,
                    labels.get(i), OrePalette.GENERAL, clicked -> {
                        if (selectedIndex != index) {
                            select(index);
                            onSelect.accept(index);
                        }
                    }));
            button.setAppearance(orientation == Orientation.HORIZONTAL ? OreButton.Appearance.HORIZONTAL_NAVIGATION : OreButton.Appearance.NAVIGATION);
            buttons.add(button);
        }
        select(0);
    }

    public void setHeader(Font font, Component title) {
        if (headerSlot == null) throw new IllegalStateException("Only vertical navigation has a header");
        if (header == null) header = headerSlot.addChild(new OreLabel(font, title, buttons.getFirst().getWidth())
                .decorate(OreLabel.Decoration.DIVIDER, 0xFF89AFFF));
        else header.setMessage(title);
        arrangeElements();
    }

    public void setGroupHeading(int index, Font font, Component title) {
        if (orientation != Orientation.VERTICAL) throw new IllegalStateException("Only vertical navigation has groups");
        GroupHeading heading = groupHeadings.get(index);
        if (heading == null) {
            heading = groupSlots.get(index).addChild(new GroupHeading(font, title, buttons.get(index).getWidth()));
            groupHeadings.put(index, heading);
        } else heading.setMessage(title);
        arrangeElements();
    }

    public void setIcon(int index, ResourceLocation icon) {
        buttons.get(index).setIcon(icon, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (orientation == Orientation.VERTICAL) {
            int top = getY() + (header == null ? 0 : header.getHeight());
            graphics.fill(getX(), top, getX() + getWidth(), getY() + getHeight(), OreDrawing.PANEL);
        }
    }

    public void setWidth(int width) {
        int buttonWidth = orientation == Orientation.HORIZONTAL ? width / buttons.size() : width;
        buttons.forEach(button -> button.setWidth(Math.max(16, buttonWidth)));
        if (header != null) header.setWidth(width);
        groupHeadings.values().forEach(heading -> {
            int height = heading.getHeight();
            heading.setWidth(width);
            heading.setHeight(height);
        });
        arrangeElements();
    }

    public void fitHeight(int height) {
        if (orientation == Orientation.VERTICAL) {
            int available = height - (header == null ? 0 : header.getHeight());
            int rowHeight = Math.max(12, Math.min(25, available / (buttons.size() + groupHeadings.size())));
            buttons.forEach(button -> button.setHeight(rowHeight));
            groupHeadings.values().forEach(heading -> heading.setHeight(rowHeight));
            arrangeElements();
        }
    }

    public void select(int index) {
        if (index < 0 || index >= buttons.size()) {
            throw new IndexOutOfBoundsException(index);
        }
        selectedIndex = index;
        for (int i = 0; i < buttons.size(); i++) {
            buttons.get(i).setSelected(i == index);
        }
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    private static final class GroupHeading extends OreLabel {
        private final Font font;

        private GroupHeading(Font font, Component title, int width) {
            super(font, title, width);
            this.font = font;
            setHeight(25);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(getX(), getBottom() - 2, getRight(), getBottom() - 1, 0xFF1D1E1F);
            graphics.fill(getX(), getBottom() - 1, getRight(), getBottom(), 0xFF454647);
            int textRight = Math.min(getRight() - 7, getX() + 7 + (int) Math.ceil(font.width(getMessage()) * OreDrawing.SMALL_TEXT));
            OreDrawing.scaledText(graphics, font, getMessage(), getX() + 7, getY(), textRight, getBottom(),
                    0xFFD0D1D4, OreDrawing.SMALL_TEXT);
        }
    }
}
