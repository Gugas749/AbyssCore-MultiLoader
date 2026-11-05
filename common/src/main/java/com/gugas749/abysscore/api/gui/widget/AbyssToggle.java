package com.gugas749.abysscore.api.gui.widget;

import com.gugas749.abysscore.api.gui.render.AbyssDraw;
import com.gugas749.abysscore.api.gui.theme.AbyssTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * On/off switch with a label on its right. 20 tall so it lines up with buttons.
 *
 *   new AbyssToggle(x, y, Component.literal("Vanish"), isVanished, on -> sendVanish(on));
 *
 * The width is computed from the label (switch + gap + text).
 */
public class AbyssToggle extends AbstractButton {

    private static final int SWITCH_W = 24, SWITCH_H = 12, GAP = 4;

    private boolean value;
    private final Consumer<Boolean> onChange;

    public AbyssToggle(int x, int y, Component label, boolean initial, Consumer<Boolean> onChange) {
        super(x, y, SWITCH_W + GAP + Minecraft.getInstance().font.width(label), 20, label);
        this.value = initial;
        this.onChange = onChange;
    }

    public boolean getValue() {
        return value;
    }

    /** Change it from code (does NOT call onChange — that's only for the user's clicks). */
    public void setValue(boolean value) {
        this.value = value;
    }

    @Override
    public void onPress() {
        value = !value;
        onChange.accept(value);
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        boolean hover = isHoveredOrFocused() && this.active;
        AbyssTheme.Sprite sprite = value
                ? (hover ? AbyssTheme.TOGGLE_ON_HOVER : AbyssTheme.TOGGLE_ON)
                : (hover ? AbyssTheme.TOGGLE_OFF_HOVER : AbyssTheme.TOGGLE_OFF);
        AbyssDraw.sprite(g, sprite, getX(), getY() + (getHeight() - SWITCH_H) / 2);

        int color = !this.active ? AbyssTheme.TEXT_OFF : value ? AbyssTheme.TEXT : AbyssTheme.TEXT_DIM;
        g.drawString(Minecraft.getInstance().font, getMessage(), getX() + SWITCH_W + GAP, getY() + 6, color, true);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, Component.empty().append(getMessage())
                .append(": ").append(Component.translatable(value ? "options.on" : "options.off")));
    }
}
