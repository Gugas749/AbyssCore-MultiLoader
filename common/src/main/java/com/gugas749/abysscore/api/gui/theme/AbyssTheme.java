package com.gugas749.abysscore.api.gui.theme;

import com.gugas749.abysscore.Constants;
import net.minecraft.resources.ResourceLocation;

/**
 * The "Abyss" look: every sprite of abyss_gui.png (256 × 256) and every color.
 *
 * This is the ONLY class that knows where things are in the texture. Editing the PNG in
 * Photoshop? Keep parts at the same place, or update the numbers here — nowhere else.
 *
 * Borders: how many pixels on each side are the "edge" in nine-slicing. The rest (the middle)
 * is repeated to fill any size, so nothing that should appear only once may be in the middle.
 */
public final class AbyssTheme {

    private AbyssTheme() {}

    public static final ResourceLocation SHEET = rl("textures/gui/abyss_gui.png");
    public static final int SHEET_SIZE = 256;

    /** A part of the sheet: position (u, v) and size. */
    public record Sprite(int u, int v, int width, int height) {
        public ResourceLocation texture() { return SHEET; }
    }

    // ── Panel & header ─────────────────────────────────────────────────────────
    public static final Sprite PANEL  = new Sprite(0, 0, 32, 32);   public static final int PANEL_BORDER  = 6;
    public static final Sprite HEADER = new Sprite(32, 0, 32, 16);  public static final int HEADER_BORDER = 4;

    // ── Buttons (20 tall) ──────────────────────────────────────────────────────
    public static final Sprite BUTTON              = new Sprite(0, 32, 64, 20);
    public static final Sprite BUTTON_HOVER        = new Sprite(0, 52, 64, 20);
    public static final Sprite BUTTON_DISABLED     = new Sprite(0, 72, 64, 20);
    public static final Sprite BUTTON_DANGER       = new Sprite(64, 32, 64, 20);
    public static final Sprite BUTTON_DANGER_HOVER = new Sprite(64, 52, 64, 20);
    public static final int BUTTON_BORDER = 3;

    // ── Sidebar tabs (18 tall) ─────────────────────────────────────────────────
    public static final Sprite TAB          = new Sprite(128, 32, 64, 18);
    public static final Sprite TAB_HOVER    = new Sprite(128, 50, 64, 18);
    public static final Sprite TAB_SELECTED = new Sprite(128, 68, 64, 18);
    public static final int TAB_BORDER = 4;

    // ── Slider ─────────────────────────────────────────────────────────────────
    public static final Sprite SLIDER_TRACK        = new Sprite(192, 32, 64, 20);  public static final int SLIDER_BORDER = 3;
    public static final Sprite SLIDER_HANDLE       = new Sprite(192, 52, 6, 20);
    public static final Sprite SLIDER_HANDLE_HOVER = new Sprite(198, 52, 6, 20);

    // ── Toggle switch 24 × 12 ──────────────────────────────────────────────────
    public static final Sprite TOGGLE_OFF       = new Sprite(0, 96, 24, 12);
    public static final Sprite TOGGLE_OFF_HOVER = new Sprite(24, 96, 24, 12);
    public static final Sprite TOGGLE_ON        = new Sprite(48, 96, 24, 12);
    public static final Sprite TOGGLE_ON_HOVER  = new Sprite(72, 96, 24, 12);

    // ── Text field (20 tall) ───────────────────────────────────────────────────
    public static final Sprite FIELD         = new Sprite(0, 112, 64, 20);
    public static final Sprite FIELD_FOCUSED = new Sprite(64, 112, 64, 20);
    public static final int FIELD_BORDER = 3;

    // ── Scrollbar (vertical nine-slice) ────────────────────────────────────────
    public static final Sprite SCROLL_TRACK        = new Sprite(160, 96, 6, 32);
    public static final Sprite SCROLL_HANDLE       = new Sprite(168, 96, 6, 16);
    public static final Sprite SCROLL_HANDLE_HOVER = new Sprite(176, 96, 6, 16);
    public static final int SCROLL_BORDER = 2;
    public static final int SCROLL_WIDTH = 6;

    // ── Icons 9 × 9 ────────────────────────────────────────────────────────────
    public static final Sprite ICON_CLOSE       = new Sprite(128, 96, 9, 9);
    public static final Sprite ICON_CLOSE_HOVER = new Sprite(137, 96, 9, 9);

    // ── Colors (ARGB) ──────────────────────────────────────────────────────────
    public static final int TEXT         = 0xFFDCE4FF;   // normal text
    public static final int TEXT_BRIGHT  = 0xFFF0F5FF;   // hovered / selected
    public static final int TEXT_DIM     = 0xFF7887AF;   // secondary
    public static final int TEXT_OFF     = 0xFF46506E;   // disabled
    public static final int TITLE        = 0xFF78B4FF;   // headings, section titles
    public static final int ACCENT       = 0xFF60D4FF;   // cyan highlights
    public static final int DANGER_TEXT  = 0xFFFF96AA;
    public static final int BADGE        = 0xFFE4466E;   // "STAFF"
    public static final int DIVIDER      = 0xFF1D2B58;
    public static final int ROW_HOVER    = 0x22507CE0;   // translucent: the panel shows through
    public static final int ROW_SELECTED = 0x3860D4FF;
    public static final int DIM_OVERLAY  = 0xB0000000;   // behind dialogs

    /** abysscore:path. Constants.id works on both versions (ResourceLocation.tryBuild). */
    public static ResourceLocation rl(String path) {
        return Constants.asResource(path);
    }
}
