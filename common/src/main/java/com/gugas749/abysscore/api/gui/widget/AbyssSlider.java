package com.gugas749.abysscore.api.gui.widget;

import com.gugas749.abysscore.api.gui.render.AbyssDraw;
import com.gugas749.abysscore.api.gui.theme.AbyssTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

/**
 * Slider. Internally always 0.0 – 1.0; convert in your two functions:
 *
 *   new AbyssSlider(x, y, 150, radius / 128.0,
 *       v -> Component.literal("Radius: " + (int) (v * 128)),   // label on the slider
 *       v -> radius = (int) (v * 128));                          // on every change
 */
public class AbyssSlider extends AbstractSliderButton {

    private static final int HANDLE_W = 6;

    private final DoubleFunction<Component> label;
    private final DoubleConsumer onChange;

    public AbyssSlider(int x, int y, int width, double initial,
                       DoubleFunction<Component> label, DoubleConsumer onChange) {
        super(x, y, width, 20, Component.empty(), initial);
        this.label = label;
        this.onChange = onChange;
        updateMessage();   // super() ran before our fields existed → build the label now
    }

    public double getValue() {
        return this.value;
    }

    @Override
    protected void updateMessage() {
        setMessage(label.apply(this.value));
    }

    @Override
    protected void applyValue() {
        onChange.accept(this.value);
    }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        AbyssDraw.nineSlice(g, AbyssTheme.SLIDER_TRACK, AbyssTheme.SLIDER_BORDER, getX(), getY(), getWidth(), getHeight());
        int handleX = getX() + (int) (this.value * (getWidth() - HANDLE_W));
        AbyssDraw.sprite(g, isHoveredOrFocused() ? AbyssTheme.SLIDER_HANDLE_HOVER : AbyssTheme.SLIDER_HANDLE, handleX, getY());
        this.renderScrollingString(g, Minecraft.getInstance().font, 2,
                this.active ? AbyssTheme.TEXT : AbyssTheme.TEXT_OFF);
    }
}
