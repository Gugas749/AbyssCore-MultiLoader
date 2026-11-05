package com.gugas749.abysscore.api.gui.widget;

import com.gugas749.abysscore.api.gui.render.AbyssDraw;
import com.gugas749.abysscore.api.gui.theme.AbyssTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * A small button that is just an icon (e.g. the panel's close "X").
 * The label is never drawn — it's used as tooltip and for narration.
 */
public class AbyssIconButton extends AbstractButton {

    private final ResourceLocation texture;
    private final int texWidth, texHeight;
    private final AbyssTheme.Sprite icon, iconHover;
    private final Runnable onPress;

    /** Icon from the Abyss sheet (e.g. AbyssTheme.ICON_CLOSE / ICON_CLOSE_HOVER). */
    public AbyssIconButton(int x, int y, AbyssTheme.Sprite icon, AbyssTheme.Sprite iconHover,
                           Component label, Runnable onPress) {
        this(x, y, AbyssTheme.SHEET, AbyssTheme.SHEET_SIZE, AbyssTheme.SHEET_SIZE, icon, iconHover, label, onPress);
    }

    /**
     * Icon from YOUR texture ({@code texWidth} × {@code texHeight} pixels); the sprites give
     * the icon's position in it:
     *   new AbyssIconButton(x, y, MyMod.id("textures/gui/icons.png"), 64, 64,
     *           new AbyssTheme.Sprite(0, 0, 12, 12), new AbyssTheme.Sprite(12, 0, 12, 12),
     *           Component.literal("Refresh"), this::refresh);
     */
    public AbyssIconButton(int x, int y, ResourceLocation texture, int texWidth, int texHeight,
                           AbyssTheme.Sprite icon, AbyssTheme.Sprite iconHover,
                           Component label, Runnable onPress) {
        super(x, y, icon.width(), icon.height(), label);
        this.texture = texture;
        this.texWidth = texWidth;
        this.texHeight = texHeight;
        this.icon = icon;
        this.iconHover = iconHover;
        this.onPress = onPress;
        setTooltip(Tooltip.create(label));
    }

    @Override
    public void onPress() {
        onPress.run();
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        AbyssDraw.sprite(g, texture, texWidth, texHeight, isHoveredOrFocused() ? iconHover : icon, getX(), getY());
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
