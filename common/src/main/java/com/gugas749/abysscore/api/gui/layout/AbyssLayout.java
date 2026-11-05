package com.gugas749.abysscore.api.gui.layout;

import net.minecraft.client.gui.components.AbstractWidget;

/**
 * Positions widgets so you don't compute coordinates by hand.
 *
 *   // Stack vertically, 4px apart
 *   var col = AbyssLayout.column(x, y, 4);
 *   col.add(new AbyssButton(0, 0, 100, text("Save"), b -> save()));      // x/y are overwritten
 *   col.add(new AbyssToggle(0, 0, text("Vanish"), false, v -> {}));
 *   col.space(8);                                                          // extra gap
 *   col.add(...);
 *
 *   // Side by side
 *   var row = AbyssLayout.row(x, y, 4);
 *   row.add(okButton); row.add(cancelButton);
 *
 *   // Grid: 3 columns of 60 × 20 cells (widgets are resized to the cell width)
 *   var grid = AbyssLayout.grid(x, y, 3, 60, 20, 4, 4);
 *   for (...) grid.add(new AbyssButton(0, 0, 0, name, b -> ...));
 *
 * Widgets are positioned immediately when added — there is no "arrange later" step.
 * Our own helper instead of vanilla's LinearLayout/GridLayout, because vanilla's layouts were
 * rewritten in 1.20.2 and behave differently between our Forge and NeoForge versions.
 */
public final class AbyssLayout {

    private AbyssLayout() {}

    public static Flow column(int x, int y, int gap) { return new Flow(x, y, gap, true); }
    public static Flow row(int x, int y, int gap)    { return new Flow(x, y, gap, false); }

    public static Grid grid(int x, int y, int columns, int cellWidth, int cellHeight, int gapX, int gapY) {
        return new Grid(x, y, columns, cellWidth, cellHeight, gapX, gapY);
    }

    /** x that centers something of width w inside [areaX, areaX + areaWidth). */
    public static int center(int areaX, int areaWidth, int w) {
        return areaX + (areaWidth - w) / 2;
    }

    static int widthOf(AbstractWidget w)  { return w instanceof AbyssSized s ? s.outerWidth()  : w.getWidth(); }
    static int heightOf(AbstractWidget w) { return w instanceof AbyssSized s ? s.outerHeight() : w.getHeight(); }

    /** A column (vertical) or row (horizontal). */
    public static final class Flow {
        private final int startX, startY, gap;
        private final boolean vertical;
        private int cursorX, cursorY;

        private Flow(int x, int y, int gap, boolean vertical) {
            this.startX = this.cursorX = x;
            this.startY = this.cursorY = y;
            this.gap = gap;
            this.vertical = vertical;
        }

        /** Places the widget at the cursor, moves the cursor past it, returns the SAME widget. */
        public <W extends AbstractWidget> W add(W widget) {
            widget.setX(cursorX);
            widget.setY(cursorY);
            if (vertical) cursorY += heightOf(widget) + gap;
            else          cursorX += widthOf(widget) + gap;
            return widget;
        }

        /** Extra empty space (in the flow's direction). */
        public Flow space(int pixels) {
            if (vertical) cursorY += pixels; else cursorX += pixels;
            return this;
        }

        /** Where the next widget would go — handy to draw a label there first. */
        public int x() { return cursorX; }
        public int y() { return cursorY; }

        /** Total size used so far. */
        public int usedWidth()  { return cursorX - startX; }
        public int usedHeight() { return cursorY - startY; }
    }

    /** Fixed-size cells, left to right, then the next line. */
    public static final class Grid {
        private final int x, y, columns, cellW, cellH, gapX, gapY;
        private int index = 0;

        private Grid(int x, int y, int columns, int cellW, int cellH, int gapX, int gapY) {
            this.x = x; this.y = y; this.columns = Math.max(1, columns);
            this.cellW = cellW; this.cellH = cellH; this.gapX = gapX; this.gapY = gapY;
        }

        public <W extends AbstractWidget> W add(W widget) {
            int col = index % columns, row = index / columns;
            widget.setX(x + col * (cellW + gapX));
            widget.setY(y + row * (cellH + gapY));
            if (!(widget instanceof AbyssSized)) widget.setWidth(cellW);   // fill the cell
            index++;
            return widget;
        }

        /** Leave a cell empty. */
        public Grid skip() { index++; return this; }

        public int rowsUsed() { return (index + columns - 1) / columns; }
    }
}
