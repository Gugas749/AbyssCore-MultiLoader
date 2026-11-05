package com.gugas749.abysscore.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Shared design language for all AbyssCore screens.
 *
 * Aesthetic: deep ocean abyss — near-black backgrounds, dark blue panels,
 * sharp corners, thin cyan-blue borders with subtle glow on active elements.
 */
public final class AbyssUI {

    private AbyssUI() {}

    // ── Color palette ─────────────────────────────────────────────────────────

    /** Full-screen background — near void black */
    public static final int BG              = 0xF0050810;
    /** Panel / card background */
    public static final int PANEL           = 0xEE0A1628;
    /** Panel background — slightly lighter, for nested elements */
    public static final int PANEL_LIGHT     = 0xEE0D1F3C;
    /** Sidebar background — darkest panel */
    public static final int SIDEBAR         = 0xF0060E1E;

    /** Default border — dark teal */
    public static final int BORDER          = 0xFF1A3A5C;
    /** Active / selected border — bright cyan */
    public static final int BORDER_ACTIVE   = 0xFF2B7FBF;
    /** Hover border */
    public static final int BORDER_HOVER    = 0xFF1F5A8A;

    /** Primary text — cold white */
    public static final int TEXT            = 0xFFC8E8F5;
    /** Secondary / muted text */
    public static final int TEXT_MUTED      = 0xFF5A8BA8;
    /** Accent text — aqua highlight */
    public static final int TEXT_ACCENT     = 0xFF4DB8E8;
    /** Disabled text */
    public static final int TEXT_DISABLED   = 0xFF2A4A5E;

    /** Row hover tint */
    public static final int ROW_HOVER       = 0x220A2040;
    /** Row selected tint */
    public static final int ROW_SELECTED    = 0x3A1A4B7A;

    /** Button — default */
    public static final int BTN             = 0xFF0D1F3C;
    /** Button — hover */
    public static final int BTN_HOVER       = 0xFF1A3A5C;
    /** Button — active/pressed */
    public static final int BTN_ACTIVE      = 0xFF1F5A8A;
    /** Button — danger (delete, red tint) */
    public static final int BTN_DANGER      = 0xFF2A0808;
    /** Button — danger border */
    public static final int BTN_DANGER_BDR  = 0xFF8B1A1A;

    /** ON state — green tint */
    public static final int STATE_ON        = 0xFF0A2A0A;
    public static final int STATE_ON_BDR    = 0xFF1A7A1A;
    /** OFF state — dark */
    public static final int STATE_OFF       = 0xFF1A1A1A;
    public static final int STATE_OFF_BDR   = 0xFF3A3A3A;

    // ── Layout constants ──────────────────────────────────────────────────────

    public static final int PAD         = 8;
    public static final int ROW_H       = 20;
    public static final int SIDEBAR_W   = 116;
    public static final int HEADER_H    = 22;
    public static final int BTN_H       = 16;
    public static final int BTN_W_SM    = 38;
    public static final int BTN_W_MD    = 60;
    public static final int BTN_W_LG    = 90;

    // ── Draw helpers ──────────────────────────────────────────────────────────

    /**
     * Draws a panel with the Abyss style — dark fill + thin border.
     */
    public static void drawPanel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, PANEL);
        drawBorder(g, x, y, w, h, BORDER);
    }

    /**
     * Draws a panel with an active (glowing) border.
     */
    public static void drawActivePanel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, PANEL_LIGHT);
        drawBorder(g, x, y, w, h, BORDER_ACTIVE);
        // Inner glow — one pixel inside the border, semi-transparent accent
        g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x222B7FBF);
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, 0x222B7FBF);
    }

    /**
     * Draws the sidebar background — slightly darker than panels.
     */
    public static void drawSidebar(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, SIDEBAR);
        drawBorder(g, x, y, w, h, BORDER);
    }

    /**
     * Draws a thin 1px border around a rectangle.
     */
    public static void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x,         y,         x + w,     y + 1,     color); // top
        g.fill(x,         y + h - 1, x + w,     y + h,     color); // bottom
        g.fill(x,         y,         x + 1,     y + h,     color); // left
        g.fill(x + w - 1, y,         x + w,     y + h,     color); // right
    }

    /**
     * Draws a header bar inside a panel — title text + bottom divider.
     */
    public static void drawHeader(GuiGraphics g, Font font, int x, int y, int w, String title) {
        g.fill(x, y, x + w, y + HEADER_H, 0xFF060E1E);
        g.fill(x, y + HEADER_H - 1, x + w, y + HEADER_H, BORDER_ACTIVE);
        g.drawString(font, title, x + PAD, y + (HEADER_H - 8) / 2, TEXT_ACCENT, false);
    }

    /**
     * Draws a sidebar category row — highlighted if selected.
     */
    public static void drawCategoryRow(GuiGraphics g, Font font,
            int x, int y, int w, String label, boolean selected, boolean hovered) {
        if (selected) {
            g.fill(x, y, x + w, y + ROW_H, ROW_SELECTED);
            drawBorder(g, x, y, w, ROW_H, BORDER_ACTIVE);
            // Left accent bar
            g.fill(x, y, x + 2, y + ROW_H, BORDER_ACTIVE);
        } else if (hovered) {
            g.fill(x, y, x + w, y + ROW_H, ROW_HOVER);
            drawBorder(g, x, y, w, ROW_H, BORDER_HOVER);
        }
        int textColor = selected ? TEXT_ACCENT : (hovered ? TEXT : TEXT_MUTED);
        g.drawString(font, label, x + PAD + (selected ? 4 : 2), y + (ROW_H - 8) / 2, textColor, false);
    }

    /**
     * Draws a data row — zebra-striped with hover highlight.
     */
    public static void drawRow(GuiGraphics g, int x, int y, int w,
            boolean hovered, boolean even) {
        int bg = even ? 0x110A1628 : 0x00000000;
        if (hovered) bg = ROW_HOVER;
        if (bg != 0) g.fill(x, y, x + w, y + ROW_H, bg);
    }

    /**
     * Draws an inline button — sharp corners, bordered.
     */
    public static void drawButton(GuiGraphics g, Font font,
            int x, int y, int w, int h, String label, boolean hovered, boolean danger) {
        int bg  = danger ? BTN_DANGER      : (hovered ? BTN_HOVER : BTN);
        int bdr = danger ? BTN_DANGER_BDR  : (hovered ? BORDER_ACTIVE : BORDER);
        g.fill(x, y, x + w, y + h, bg);
        drawBorder(g, x, y, w, h, bdr);
        g.drawCenteredString(font, label, x + w / 2, y + (h - 8) / 2, TEXT);
    }

    /**
     * Draws a toggle indicator — ON (green) or OFF (dark).
     */
    public static void drawToggle(GuiGraphics g, Font font,
            int x, int y, int w, int h, boolean on) {
        int bg  = on ? STATE_ON  : STATE_OFF;
        int bdr = on ? STATE_ON_BDR : STATE_OFF_BDR;
        g.fill(x, y, x + w, y + h, bg);
        drawBorder(g, x, y, w, h, bdr);
        String label = on ? "\u00a7aON" : "\u00a78OFF";
        g.drawCenteredString(font, label, x + w / 2, y + (h - 8) / 2, TEXT);
    }

    /**
     * Draws the full-screen background gradient — top dark blue to near-black at bottom.
     */
    public static void drawBackground(GuiGraphics g, int screenW, int screenH) {
        // Vertical gradient: dark navy top → near void bottom
        g.fillGradient(0, 0, screenW, screenH, 0xFF080D1A, 0xFF020508);
    }

    /**
     * Clips rendering to a rectangle — call g.disableScissor() after.
     */
    public static void scissor(GuiGraphics g, int x, int y, int w, int h) {
        g.enableScissor(x, y, x + w, y + h);
    }

    /**
     * Returns true if the mouse is inside the given rectangle.
     */
    public static boolean isHovered(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
