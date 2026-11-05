package com.gugas749.abysscore.api.gui.widget;

import com.gugas749.abysscore.api.gui.layout.AbyssSized;
import com.gugas749.abysscore.api.gui.render.AbyssDraw;
import com.gugas749.abysscore.api.gui.theme.AbyssTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/**
 * Text input with the Abyss frame. Everything from vanilla EditBox works:
 * getValue(), setValue(), setResponder(...), setFilter(...), setMaxLength(...).
 *
 *   AbyssTextField name = new AbyssTextField(x, y, 150, Component.literal("Region name..."));
 *   name.setResponder(text -> saveButton.active = !text.isBlank());
 *
 * How it works: vanilla EditBox either draws its OWN frame or none. We turn its frame off
 * (setBordered(false)) and make the EditBox only the INNER text area; our frame is drawn
 * around it. So:
 *   - x, y, width you pass = the whole frame (what you see)
 *   - setX / setY also take the FRAME position
 *   - getX / getY / getWidth return the inner text area (vanilla needs that internally)
 *   - layouts use outerWidth()/outerHeight() (AbyssSized) to get the frame size
 */
public class AbyssTextField extends EditBox implements AbyssSized {

    public static final int HEIGHT = 20;
    private static final int PAD_X = 5, PAD_Y = 6;   // frame → text

    public AbyssTextField(int x, int y, int width, Component hint) {
        super(Minecraft.getInstance().font, x + PAD_X, y + PAD_Y, width - 2 * PAD_X, 12, hint);
        setBordered(false);
        setTextColor(AbyssTheme.TEXT & 0xFFFFFF);
        setTextColorUneditable(AbyssTheme.TEXT_OFF & 0xFFFFFF);
        setHint(hint.copy().withStyle(s -> s.withColor(AbyssTheme.TEXT_DIM & 0xFFFFFF)));
    }

    @Override
    public void setX(int frameX) {
        super.setX(frameX + PAD_X);
    }

    @Override
    public void setY(int frameY) {
        super.setY(frameY + PAD_Y);
    }

    public int frameX() { return getX() - PAD_X; }
    public int frameY() { return getY() - PAD_Y; }

    @Override public int outerWidth()  { return getWidth() + 2 * PAD_X; }
    @Override public int outerHeight() { return HEIGHT; }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (!isVisible()) return;
        AbyssDraw.nineSlice(g, isFocused() ? AbyssTheme.FIELD_FOCUSED : AbyssTheme.FIELD, AbyssTheme.FIELD_BORDER,
                frameX(), frameY(), outerWidth(), HEIGHT);
        super.renderWidget(g, mouseX, mouseY, partialTick);   // vanilla draws text, cursor, selection
    }
}
