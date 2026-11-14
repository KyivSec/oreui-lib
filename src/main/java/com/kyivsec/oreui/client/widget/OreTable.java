package com.kyivsec.oreui.client.widget;

import com.kyivsec.oreui.client.style.OreDrawing;
import com.kyivsec.oreui.client.style.OrePixelIcons;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Striped data table with row headings, a grouped heading and keyboard-accessible column sorting. */
public class OreTable extends AbstractWidget {
    public record Row(Component heading, List<Component> cells) {
        public Row {
            Objects.requireNonNull(heading);
            cells = List.copyOf(cells);
        }
    }

    private final List<Component> columns;
    private final List<Row> rows = new ArrayList<>();
    private final Map<Integer, Comparator<Component>> comparators = new HashMap<>();
    private int sortColumn = -1;
    private int focusedColumn;
    private boolean ascending = true;
    private int activationKey = -1;

    public OreTable(Component title, List<Component> columns, List<Row> rows) {
        super(0, 0, 256, 106, title);
        if (columns.isEmpty()) throw new IllegalArgumentException("A table needs at least one data column");
        this.columns = List.copyOf(columns);
        setRows(rows);
    }

    public void setRows(List<Row> rows) {
        List<Row> copied = List.copyOf(rows);
        for (Row row : copied) {
            if (row.cells().size() != columns.size()) throw new IllegalArgumentException("Row must match the column count");
        }
        this.rows.clear();
        this.rows.addAll(copied);
        setHeight(43 + rows.size() * 21);
        applySort();
    }

    public List<Row> getRows() { return List.copyOf(rows); }

    public void setComparator(int column, Comparator<Component> comparator) {
        checkColumn(column);
        comparators.put(column, Objects.requireNonNull(comparator));
        applySort();
    }

    public int getSortColumn() { return sortColumn; }
    public boolean isAscending() { return ascending; }

    public void sortBy(int column) {
        checkColumn(column);
        ascending = sortColumn != column || !ascending;
        sortColumn = focusedColumn = column;
        applySort();
    }

    private void checkColumn(int column) {
        if (column < 0 || column >= columns.size()) throw new IndexOutOfBoundsException(column);
    }

    private void applySort() {
        if (sortColumn < 0) return;
        Comparator<Component> values = comparators.getOrDefault(sortColumn,
                Comparator.comparing(Component::getString, String.CASE_INSENSITIVE_ORDER));
        Comparator<Row> comparator = (left, right) -> values.compare(left.cells().get(sortColumn), right.cells().get(sortColumn));
        rows.sort(ascending ? comparator : comparator.reversed());
    }

    private int rowHeadingWidth() { return Math.min(60, Math.max(12, width / (columns.size() + 1))); }

    private int columnX(int column) {
        return getX() + rowHeadingWidth() + (width - rowHeadingWidth() - 1) * column / columns.size();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!active || !visible || button != 0 || !isMouseOver(mouseX, mouseY)) return false;
        if (mouseY >= getY() + 21 && mouseY < getY() + 42 && mouseX >= columnX(0)) {
            int column = Math.min(columns.size() - 1,
                    (int) ((mouseX - columnX(0)) * columns.size() / Math.max(1, width - rowHeadingWidth())));
            sortBy(column);
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!active || !visible || !isFocused()) return false;
        switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT -> focusedColumn = Math.max(0, focusedColumn - 1);
            case GLFW.GLFW_KEY_RIGHT -> focusedColumn = Math.min(columns.size() - 1, focusedColumn + 1);
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_SPACE -> {
                if (activationKey == -1) { activationKey = keyCode; sortBy(focusedColumn); }
            }
            default -> { return false; }
        }
        return true;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (activationKey == keyCode) { activationKey = -1; return true; }
        return false;
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused) activationKey = -1;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        cell(graphics, columnX(0), getY(), columnX(columns.size()) - columnX(0), getMessage(), OreDrawing.HEADER, false);
        for (int column = 0; column < columns.size(); column++) {
            int x = columnX(column), cellWidth = columnX(column + 1) - x;
            cell(graphics, x, getY() + 21, cellWidth, columns.get(column), OreDrawing.HEADER, true);
            OrePixelIcons.draw(graphics, OrePixelIcons.Icon.ARROW_UP, x + cellWidth - 9, getY() + 27, 4,
                    sortColumn == column && ascending ? OreDrawing.TEXT : 0xFF8C8D90);
            OrePixelIcons.draw(graphics, OrePixelIcons.Icon.ARROW_DOWN, x + cellWidth - 9, getY() + 31, 4,
                    sortColumn == column && !ascending ? OreDrawing.TEXT : 0xFF8C8D90);
        }
        for (int row = 0; row < rows.size(); row++) {
            int y = getY() + 42 + row * 21;
            cell(graphics, getX(), y, rowHeadingWidth() + 1, rows.get(row).heading(), OreDrawing.HEADER, false);
            for (int column = 0; column < columns.size(); column++) {
                cell(graphics, columnX(column), y, columnX(column + 1) - columnX(column), rows.get(row).cells().get(column),
                        row % 2 == 0 ? OreDrawing.PANEL : OreDrawing.INSET, false);
            }
        }
        renderOutline(graphics);
    }

    public void renderOutline(GuiGraphics graphics) {
        if (active && visible && isFocused()) OreDrawing.buttonFocus(graphics,
                columnX(focusedColumn), getY() + 21, columnX(focusedColumn + 1) - columnX(focusedColumn), 22);
    }

    private void cell(GuiGraphics graphics, int x, int y, int width, Component text, int fill, boolean sort) {
        graphics.fill(x, y, x + width + 1, y + 22, OreDrawing.BACKGROUND);
        graphics.fill(x + 1, y + 1, x + width, y + 21, fill);
        OreDrawing.scaledText(graphics, Minecraft.getInstance().font, text, x + 3, y + 1,
                x + width - (sort ? 11 : 3), y + 21, active ? OreDrawing.TEXT : OreDrawing.MUTED, OreDrawing.SMALL_TEXT);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
        output.add(NarratedElementType.POSITION, Component.translatable("oreui.table.summary", rows.size(), columns.get(focusedColumn)));
        if (sortColumn >= 0) output.add(NarratedElementType.USAGE,
                Component.translatable(ascending ? "oreui.table.ascending" : "oreui.table.descending", columns.get(sortColumn)));
    }
}
