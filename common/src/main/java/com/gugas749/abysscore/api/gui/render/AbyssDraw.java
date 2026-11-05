package com.gugas749.abysscore.api.gui.render;

import com.gugas749.abysscore.api.gui.theme.AbyssTheme;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.Random;

/**
 * Drawing helpers for the Abyss sheet (and, with the texture overloads, for your own textures).
 *
 * Why not GuiGraphics.blitNineSliced()? Its 1.20.1 version assumes 256 × 256 textures and
 * its 1.21.1 version expects a sprite atlas — these helpers behave the same on both.
 */
public final class AbyssDraw {

    private AbyssDraw() {}

    /** Sprite (from the Abyss sheet) at its own size. */
    public static void sprite(GuiGraphics g, AbyssTheme.Sprite s, int x, int y) {
        sprite(g, AbyssTheme.SHEET, AbyssTheme.SHEET_SIZE, AbyssTheme.SHEET_SIZE, s, x, y);
    }

    /**
     * Sprite from YOUR texture at its own size. {@code s} gives the (u, v, width, height) inside it.
     *   AbyssDraw.sprite(g, MyMod.id("textures/gui/icons.png"), 64, 64, new AbyssTheme.Sprite(0, 0, 16, 16), x, y);
     */
    public static void sprite(GuiGraphics g, ResourceLocation texture, int texWidth, int texHeight,
                              AbyssTheme.Sprite s, int x, int y) {
        region(g, texture, texWidth, texHeight, s.u(), s.v(), s.width(), s.height(), x, y);
    }

    /**
     * Nine-slice: corners copied once, edges + middle REPEATED to fill w × h.
     *
     *   ┌──┬────────┬──┐
     *   │1 │   2    │3 │   1,3,7,9  copied once
     *   ├──┼────────┼──┤   2,8      tiled horizontally
     *   │4 │   5    │6 │   4,6      tiled vertically
     *   ├──┼────────┼──┤   5        tiled both ways
     *   │7 │   8    │9 │
     *   └──┴────────┴──┘
     */
    public static void nineSlice(GuiGraphics g, AbyssTheme.Sprite s, int border, int x, int y, int w, int h) {
        nineSlice(g, AbyssTheme.SHEET, AbyssTheme.SHEET_SIZE, AbyssTheme.SHEET_SIZE, s, border, x, y, w, h);
    }

    /** Nine-slice from YOUR texture ({@code texWidth} × {@code texHeight} pixels). */
    public static void nineSlice(GuiGraphics g, ResourceLocation texture, int texWidth, int texHeight,
                                 AbyssTheme.Sprite s, int border, int x, int y, int w, int h) {
        ResourceLocation t = texture;
        int tw = texWidth, th = texHeight;
        int b = border, u = s.u(), v = s.v(), uw = s.width(), vh = s.height();
        int midU = uw - 2 * b, midV = vh - 2 * b, midW = w - 2 * b, midH = h - 2 * b;

        region(g, t, tw, th, u, v, b, b, x, y);
        region(g, t, tw, th, u + uw - b, v, b, b, x + w - b, y);
        region(g, t, tw, th, u, v + vh - b, b, b, x, y + h - b);
        region(g, t, tw, th, u + uw - b, v + vh - b, b, b, x + w - b, y + h - b);

        tile(g, t, tw, th, u + b, v, midU, b, x + b, y, midW, b);
        tile(g, t, tw, th, u + b, v + vh - b, midU, b, x + b, y + h - b, midW, b);
        tile(g, t, tw, th, u, v + b, b, midV, x, y + b, b, midH);
        tile(g, t, tw, th, u + uw - b, v + b, b, midV, x + w - b, y + b, b, midH);
        tile(g, t, tw, th, u + b, v + b, midU, midV, x + b, y + b, midW, midH);
    }

    /** 1px outline INSIDE the rectangle. */
    public static void outline(GuiGraphics g, int x, int y, int w, int h, int argb) {
        g.fill(x, y, x + w, y + 1, argb);
        g.fill(x, y + h - 1, x + w, y + h, argb);
        g.fill(x, y, x + 1, y + h, argb);
        g.fill(x + w - 1, y, x + w, y + h, argb);
    }

    /**
     * Faint stars scattered over an area — the "void" feel of the panels.
     * Drawn by code, not baked into the texture: a tiled texture would repeat the same
     * star pattern every few pixels. A fixed seed puts them in the same place every frame.
     */
    public static void stars(GuiGraphics g, int x, int y, int w, int h, long seed) {
        Random random = new Random(seed);
        int count = Math.max(1, w * h / 450);
        for (int i = 0; i < count; i++) {
            int sx = x + random.nextInt(Math.max(1, w));
            int sy = y + random.nextInt(Math.max(1, h));
            int brightness = 40 + random.nextInt(110);
            int color = 0xFF000000 | (brightness / 2) << 16 | (brightness / 2 + 10) << 8 | brightness;
            g.fill(sx, sy, sx + 1, sy + 1, color);
        }
    }

    /** Section heading in the accent color, e.g. "BUTTONS". */
    public static void sectionTitle(GuiGraphics g, Font font, Component text, int x, int y) {
        g.drawString(font, text, x, y, AbyssTheme.TITLE, true);
    }

    /** Text cut to maxWidth with "…" — keeps colors/styles (works on FormattedText, not String). */
    public static FormattedCharSequence trimmed(Font font, Component text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text.getVisualOrderText();
        FormattedText cut = font.substrByWidth(text, Math.max(0, maxWidth - font.width("…")));
        return Language.getInstance().getVisualOrder(FormattedText.composite(cut, FormattedText.of("…")));
    }

    //-----------------------------------------------------------------------------------

    /** Copies w × h pixels from (u, v) of the texture to (x, y). */
    private static void region(GuiGraphics g, ResourceLocation texture, int texW, int texH,
                               int u, int v, int w, int h, int x, int y) {
        if (w <= 0 || h <= 0) return;
        RenderSystem.enableBlend();            // textures may have transparent parts
        RenderSystem.defaultBlendFunc();
        g.blit(texture, x, y, (float) u, (float) v, w, h, texW, texH);
    }

    /** Repeats a (pieceW × pieceH) piece until it covers w × h. */
    private static void tile(GuiGraphics g, ResourceLocation texture, int texW, int texH,
                             int u, int v, int pieceW, int pieceH, int x, int y, int w, int h) {
        if (w <= 0 || h <= 0 || pieceW <= 0 || pieceH <= 0) return;
        for (int dy = 0; dy < h; dy += pieceH) {
            for (int dx = 0; dx < w; dx += pieceW) {
                region(g, texture, texW, texH, u, v, Math.min(pieceW, w - dx), Math.min(pieceH, h - dy), x + dx, y + dy);
            }
        }
    }
}
