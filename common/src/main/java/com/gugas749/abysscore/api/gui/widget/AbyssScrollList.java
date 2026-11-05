package com.gugas749.abysscore.api.gui.widget;

import com.gugas749.abysscore.api.gui.render.AbyssDraw;
import com.gugas749.abysscore.api.gui.theme.AbyssTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A scrollable list of ANY kind of item. You decide how a row looks:
 *
 *   AbyssScrollList<String> list = new AbyssScrollList<>(x, y, 200, 120, 12,
 *       (g, name, rx, ry, rw, rh, hovered, selected) ->
 *           g.drawString(font, name, rx, ry + 2, AbyssTheme.TEXT, true));
 *   list.setItems(playerNames);
 *   list.onSelect(name -> selectedLabel.setText(Component.literal(name)));
 *
 * Hover + selection highlights, mouse wheel, draggable scrollbar and the "empty" text
 * are handled here; the row renderer only draws the row's content.
 */
public class AbyssScrollList<T> extends AbstractWidget {

    /** Draws one row's content. (x, y, width, height) = the row's area. */
    @FunctionalInterface
    public interface RowRenderer<T> {
        void render(GuiGraphics g, T item, int x, int y, int width, int height, boolean hovered, boolean selected);
    }

    private static final int SCROLL_GAP = 2;

    private final int rowHeight;
    private final RowRenderer<T> renderer;
    private List<T> items = new ArrayList<>();
    @Nullable private T selected;
    private boolean selectable = true;
    @Nullable private Consumer<T> onSelect;
    private Component emptyText = Component.translatable("gui.abysscore.list.empty");

    private int scroll = 0;              // index of the first visible row
    private boolean draggingScrollbar = false;

    public AbyssScrollList(int x, int y, int width, int height, int rowHeight, RowRenderer<T> renderer) {
        super(x, y, width, height, Component.empty());
        this.rowHeight = rowHeight;
        this.renderer = renderer;
    }

    // ── API ────────────────────────────────────────────────────────────────────

    /** Replaces the items. The selection is kept if that item is still in the new list. */
    public AbyssScrollList<T> setItems(List<? extends T> newItems) {
        this.items = new ArrayList<>(newItems);
        if (selected != null && !items.contains(selected)) selected = null;
        scroll = Mth.clamp(scroll, 0, maxScroll());
        return this;
    }

    public List<T> getItems() { return items; }

    @Nullable public T getSelected() { return selected; }

    public AbyssScrollList<T> setSelected(@Nullable T item) { this.selected = item; return this; }

    public AbyssScrollList<T> onSelect(Consumer<T> onSelect) { this.onSelect = onSelect; return this; }

    /** false = rows only highlight on hover, clicking does nothing. */
    public AbyssScrollList<T> selectable(boolean selectable) { this.selectable = selectable; return this; }

    public AbyssScrollList<T> emptyText(Component text) { this.emptyText = text; return this; }

    // ── Geometry ───────────────────────────────────────────────────────────────

    private int visibleRows() { return Math.max(1, getHeight() / rowHeight); }
    private int maxScroll()   { return Math.max(0, items.size() - visibleRows()); }
    private boolean scrollable() { return items.size() > visibleRows(); }
    private int rowWidth()    { return getWidth() - (scrollable() ? AbyssTheme.SCROLL_WIDTH + SCROLL_GAP : 0); }
    private int scrollbarX()  { return getX() + getWidth() - AbyssTheme.SCROLL_WIDTH; }

    /** Which item is under the mouse, or -1. */
    private int indexAt(double mouseX, double mouseY) {
        if (mouseX < getX() || mouseX >= getX() + rowWidth() || mouseY < getY()) return -1;
        int row = (int) ((mouseY - getY()) / rowHeight);
        int index = scroll + row;
        return row < visibleRows() && index < items.size() ? index : -1;
    }

    // ── Render ─────────────────────────────────────────────────────────────────

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (items.isEmpty()) {
            var font = Minecraft.getInstance().font;
            g.drawCenteredString(font, emptyText, getX() + getWidth() / 2, getY() + getHeight() / 2 - 4, AbyssTheme.TEXT_DIM);
            return;
        }
        scroll = Mth.clamp(scroll, 0, maxScroll());
        int hoveredIndex = isHovered() ? indexAt(mouseX, mouseY) : -1;
        int w = rowWidth();

        for (int row = 0; row < visibleRows() && scroll + row < items.size(); row++) {
            int index = scroll + row;
            T item = items.get(index);
            int y = getY() + row * rowHeight;
            boolean isSelected = item.equals(selected);
            boolean isHovered = index == hoveredIndex;

            if (isSelected) {
                g.fill(getX(), y, getX() + w, y + rowHeight, AbyssTheme.ROW_SELECTED);
                g.fill(getX(), y, getX() + 1, y + rowHeight, AbyssTheme.ACCENT);   // accent bar on the left
            } else if (isHovered) {
                g.fill(getX(), y, getX() + w, y + rowHeight, AbyssTheme.ROW_HOVER);
            }
            renderer.render(g, item, getX() + 3, y, w - 3, rowHeight, isHovered, isSelected);
        }

        if (scrollable()) drawScrollbar(g, mouseX, mouseY);
    }

    private void drawScrollbar(GuiGraphics g, int mouseX, int mouseY) {
        int trackH = visibleRows() * rowHeight;
        AbyssDraw.nineSlice(g, AbyssTheme.SCROLL_TRACK, AbyssTheme.SCROLL_BORDER, scrollbarX(), getY(), AbyssTheme.SCROLL_WIDTH, trackH);
        int handleH = Math.max(10, trackH * visibleRows() / items.size());
        int handleY = getY() + (maxScroll() == 0 ? 0 : (trackH - handleH) * scroll / maxScroll());
        boolean hover = draggingScrollbar || (mouseX >= scrollbarX() && mouseY >= handleY && mouseY < handleY + handleH
                && mouseX < scrollbarX() + AbyssTheme.SCROLL_WIDTH);
        AbyssDraw.nineSlice(g, hover ? AbyssTheme.SCROLL_HANDLE_HOVER : AbyssTheme.SCROLL_HANDLE, AbyssTheme.SCROLL_BORDER,
                scrollbarX(), handleY, AbyssTheme.SCROLL_WIDTH, handleH);
    }

    // ── Input ──────────────────────────────────────────────────────────────────

    /** Shared by both versions' mouseScrolled (their signatures differ). */
    protected boolean scrollBy(double amount) {
        scroll = Mth.clamp(scroll - (int) Math.signum(amount), 0, maxScroll());
        return true;
    }

    /*
     * mouseScrolled has 4 parameters on 1.21.1 (horizontal + vertical) and 3 on 1.20.1
     * (vertical only). We write both WITHOUT @Override: on each version one of them overrides
     * the real method, the other is an extra method nobody calls.
     */
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return scrollBy(scrollY);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
        return scrollBy(scroll);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        draggingScrollbar = scrollable() && mouseX >= scrollbarX();
        if (draggingScrollbar) {
            scrollToMouse(mouseY);
            return;
        }
        int index = indexAt(mouseX, mouseY);
        if (selectable && index >= 0) {
            selected = items.get(index);
            if (onSelect != null) onSelect.accept(selected);
        }
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
        if (draggingScrollbar) scrollToMouse(mouseY);
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        draggingScrollbar = false;
    }

    private void scrollToMouse(double mouseY) {
        double fraction = (mouseY - getY()) / (double) (visibleRows() * rowHeight);
        scroll = Mth.clamp((int) Math.round(fraction * maxScroll()), 0, maxScroll());
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        if (selected != null) output.add(NarratedElementType.TITLE, Component.literal(String.valueOf(selected)));
    }
}
