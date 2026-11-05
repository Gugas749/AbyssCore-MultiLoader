package com.gugas749.abysscore.api.gui.widget;

import com.gugas749.abysscore.api.gui.render.AbyssDraw;
import com.gugas749.abysscore.api.gui.theme.AbyssTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;

/**
 * A sidebar tab (used by AbyssPanelScreen). 18 tall, any width.
 * Asks "am I selected?" every frame, so the screen is the only owner of that state.
 */
public class AbyssTabButton extends AbstractButton {

    public static final int HEIGHT = 18;

    private final BooleanSupplier selected;
    private final Runnable onSelect;

    public AbyssTabButton(int x, int y, int width, Component label, BooleanSupplier selected, Runnable onSelect) {
        super(x, y, width, HEIGHT, label);
        this.selected = selected;
        this.onSelect = onSelect;
    }

    @Override
    public void onPress() {
        if (!selected.getAsBoolean()) onSelect.run();
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        boolean isSelected = selected.getAsBoolean();
        boolean hover = isHoveredOrFocused();
        AbyssDraw.nineSlice(g, isSelected ? AbyssTheme.TAB_SELECTED : hover ? AbyssTheme.TAB_HOVER : AbyssTheme.TAB,
                AbyssTheme.TAB_BORDER, getX(), getY(), getWidth(), getHeight());

        int color = isSelected ? AbyssTheme.TEXT_BRIGHT : hover ? AbyssTheme.TEXT : AbyssTheme.TEXT_DIM;
        var font = Minecraft.getInstance().font;
        g.drawString(font, AbyssDraw.trimmed(font, getMessage(), getWidth() - 12), getX() + 8, getY() + 5, color, true);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
