package com.kyivsec.oreui.client.widget;

import com.kyivsec.oreui.client.style.OreIcons;
import com.kyivsec.oreui.client.style.OrePalette;
import com.kyivsec.oreui.client.style.OreDrawing;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class OrePagination extends LinearLayout {
    private final List<OreButton> items = new ArrayList<>();
    private final IntConsumer onSelect;
    private final OreButton previous;
    private final OreButton next;
    private final int pageCount;
    private final boolean bounded;
    private final int[] itemPages;
    private int page;

    public OrePagination(int page, IntConsumer onSelect) {
        this(Integer.MAX_VALUE, page, false, onSelect);
    }

    public OrePagination(int pageCount, int page, IntConsumer onSelect) {
        this(pageCount, page, true, onSelect);
    }

    private OrePagination(int pageCount, int page, boolean bounded, IntConsumer onSelect) {
        super(0, 0, Orientation.HORIZONTAL);
        if (pageCount < 1) throw new IllegalArgumentException("Pagination requires at least one page");
        this.onSelect = onSelect;
        this.pageCount = pageCount;
        this.bounded = bounded;
        itemPages = new int[bounded ? Math.min(7, pageCount) : 5];
        spacing(0);
        previous = new OreButton(0, 0, 21, 21, Component.translatable("gui.back"), OrePalette.GENERAL,
                button -> select(this.page - 1));
        previous.setIcon(OreIcons.CHEVRON_LEFT.texture(), true);
        previous.setIconSize(8);
        addChild(previous, settings -> settings.paddingRight(6));
        for (int i = 0; i < itemPages.length; i++) {
            int index = i;
            OreButton item = new OreButton(0, 0, 17, 17, Component.empty(), OrePalette.GENERAL,
                    button -> {
                        if (itemPages[index] > 0) select(itemPages[index]);
                    }) {
                @Override
                protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                    if (index == 0) {
                        OreDrawing.outline(graphics, getX() - 1, getY() - 1,
                                items.getLast().getRight() - getX() + 2, getHeight() + 2, 0xFF000000);
                    }
                    super.renderWidget(graphics, mouseX, mouseY, partialTick);
                }
            };
            item.setAppearance(OreButton.Appearance.PAGINATION);
            items.add(addChild(item, settings -> settings.paddingTop(2)));
        }
        next = new OreButton(0, 0, 21, 21, Component.translatable("gui.next"), OrePalette.GENERAL,
                button -> select(this.page < pageCount ? this.page + 1 : this.page));
        next.setIcon(OreIcons.CHEVRON_RIGHT.texture(), true);
        next.setIconSize(8);
        addChild(next, settings -> settings.paddingLeft(6));
        this.page = Math.max(1, Math.min(pageCount, page));
        refresh();
        arrangeElements();
    }

    public void select(int page) {
        int next = Math.max(1, Math.min(pageCount, page));
        if (next != this.page) {
            this.page = next;
            refresh();
            onSelect.accept(next);
        }
    }

    public int getPage() {
        return page;
    }

    public int getPageCount() {
        return pageCount;
    }

    public void setWidth(int width) {
        int itemWidth = Math.max(12, Math.min(17, (width - 54) / items.size()));
        items.forEach(item -> item.setWidth(itemWidth));
        arrangeElements();
    }

    private void refresh() {
        int slots = itemPages.length;
        if (!bounded) {
            int first = Math.min(Integer.MAX_VALUE - slots + 1, ((page - 1) / slots) * slots + 1);
            for (int i = 0; i < slots; i++) itemPages[i] = first + i;
        } else if (pageCount <= slots) {
            for (int i = 0; i < slots; i++) itemPages[i] = i + 1;
        } else {
            itemPages[0] = 1;
            itemPages[slots - 1] = pageCount;
            if (page <= slots - 3) {
                for (int i = 1; i < slots - 2; i++) itemPages[i] = i + 1;
                itemPages[slots - 2] = 0;
            } else if (page >= pageCount - slots + 4) {
                itemPages[1] = 0;
                for (int i = 2; i < slots - 1; i++) itemPages[i] = pageCount - slots + i + 1;
            } else {
                itemPages[1] = itemPages[slots - 2] = 0;
                int first = page - (slots - 4) / 2;
                for (int i = 2; i < slots - 2; i++) itemPages[i] = first + i - 2;
            }
        }
        previous.active = page > 1;
        next.active = page < pageCount;
        for (int i = 0; i < items.size(); i++) {
            items.get(i).setMessage(Component.literal(itemPages[i] == 0 ? "..." : Integer.toString(itemPages[i])));
            items.get(i).setSelected(itemPages[i] == page);
            items.get(i).active = itemPages[i] != 0;
        }
    }
}
