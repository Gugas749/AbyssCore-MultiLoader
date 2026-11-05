package com.gugas749.abysscore.api.gui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * One page of an AbyssPanelScreen.
 *
 * IMPORTANT: init() runs every time the tab is shown AND every time the window is resized,
 * and it creates NEW widgets each time. So keep the tab's STATE (typed text, selected
 * player, toggles...) in fields of the tab object — not only inside widgets — and give
 * the new widgets those values in init().
 *
 * Simple tab without a class:
 *   AbyssTab.of(Component.literal("Info"), ctx -> ctx.column(4).add(new AbyssButton(...)));
 */
public interface AbyssTab {

    Component title();

    /** Add this tab's widgets with ctx.add(...) / ctx.column(...). */
    void init(TabContext ctx);

    /** Once per client tick (20×/s) while the tab is visible — e.g. to refresh data. */
    default void tick(TabContext ctx) {}

    /** Draw extra things (text, section titles). Called BEFORE widgets, so widgets are on top. */
    default void render(GuiGraphics g, TabContext ctx, int mouseX, int mouseY, float partialTick) {}

    static AbyssTab of(Component title, Consumer<TabContext> init) {
        return new AbyssTab() {
            @Override public Component title() { return title; }
            @Override public void init(TabContext ctx) { init.accept(ctx); }
        };
    }
}
