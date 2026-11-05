package com.gugas749.abysscore.api.gui.widget;

import com.gugas749.abysscore.api.gui.render.AbyssDraw;
import com.gugas749.abysscore.api.gui.theme.AbyssTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * A line of text as a widget — so it can go into a layout next to buttons.
 * Too long for its width → cut with "…". Not clickable.
 *
 *   AbyssLabel status = ctx.add(new AbyssLabel(0, 0, 150, Component.literal("Ready")));
 *   status.setText(Component.literal("Saved!"));
 */
public class AbyssLabel extends AbstractWidget {

    public enum Align { LEFT, CENTER, RIGHT }

    private Component text;
    private int color = AbyssTheme.TEXT;
    private Align align = Align.LEFT;

    public AbyssLabel(int x, int y, int width, Component text) {
        super(x, y, width, 9, text);
        this.text = text;
        this.active = false;   // display only
    }

    public AbyssLabel setText(Component text) { this.text = text; setMessage(text); return this; }
    public AbyssLabel color(int argb) { this.color = argb; return this; }
    public AbyssLabel align(Align align) { this.align = align; return this; }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        var font = Minecraft.getInstance().font;
        var line = AbyssDraw.trimmed(font, text, getWidth());
        int w = font.width(line);
        int x = switch (align) {
            case LEFT -> getX();
            case CENTER -> getX() + (getWidth() - w) / 2;
            case RIGHT -> getX() + getWidth() - w;
        };
        g.drawString(font, line, x, getY(), color, true);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, text);
    }
}
