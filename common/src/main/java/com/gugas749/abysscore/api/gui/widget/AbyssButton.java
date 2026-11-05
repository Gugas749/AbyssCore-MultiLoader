package com.gugas749.abysscore.api.gui.widget;

import com.gugas749.abysscore.api.gui.render.AbyssDraw;
import com.gugas749.abysscore.api.gui.theme.AbyssTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Abyss button. Any width, always 20 tall.
 *
 *   new AbyssButton(x, y, 80, Component.literal("Save"), b -> save());
 *   new AbyssButton(x, y, 80, Component.literal("Delete"), AbyssButton.Style.DANGER, b -> delete());
 *
 * Style.DANGER is red — use it for destructive staff actions (delete, reset, ban) so they
 * never look like an ordinary button. Pair it with AbyssDialog.confirmDanger(...).
 */
public class AbyssButton extends Button {

    public static final int HEIGHT = 20;

    public enum Style { NORMAL, DANGER }

    private final Style style;

    public AbyssButton(int x, int y, int width, Component message, OnPress onPress) {
        this(x, y, width, message, Style.NORMAL, onPress);
    }

    public AbyssButton(int x, int y, int width, Component message, Style style, OnPress onPress) {
        super(x, y, width, HEIGHT, message, onPress, DEFAULT_NARRATION);
        this.style = style;
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        boolean hovered = this.isHoveredOrFocused();
        AbyssTheme.Sprite sprite;
        int color;
        if (!this.active) {
            sprite = AbyssTheme.BUTTON_DISABLED;
            color = AbyssTheme.TEXT_OFF;
        } else if (style == Style.DANGER) {
            sprite = hovered ? AbyssTheme.BUTTON_DANGER_HOVER : AbyssTheme.BUTTON_DANGER;
            color = hovered ? AbyssTheme.TEXT_BRIGHT : AbyssTheme.DANGER_TEXT;
        } else {
            sprite = hovered ? AbyssTheme.BUTTON_HOVER : AbyssTheme.BUTTON;
            color = hovered ? AbyssTheme.TEXT_BRIGHT : AbyssTheme.TEXT;
        }
        AbyssDraw.nineSlice(g, sprite, AbyssTheme.BUTTON_BORDER, getX(), getY(), getWidth(), getHeight());
        // renderString centers the label and SCROLLS it if it doesn't fit (long translations)
        this.renderString(g, Minecraft.getInstance().font, (color & 0xFFFFFF) | Mth.ceil(this.alpha * 255.0F) << 24);
    }
}
