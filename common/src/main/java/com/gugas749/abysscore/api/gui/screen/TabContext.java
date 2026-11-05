package com.gugas749.abysscore.api.gui.screen;

import com.gugas749.abysscore.api.gui.layout.AbyssLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;

/**
 * What a tab gets to build itself: its content area and a way to add widgets.
 * x, y, width, height = the free area right of the sidebar, below the header.
 */
public final class TabContext {

    public final int x, y, width, height;
    private final AbyssPanelScreen screen;

    TabContext(AbyssPanelScreen screen, int x, int y, int width, int height) {
        this.screen = screen;
        this.x = x; this.y = y; this.width = width; this.height = height;
    }

    /** Adds a widget to the screen as part of THIS tab (removed when switching tabs). */
    public <W extends AbstractWidget> W add(W widget) {
        return screen.addTabWidget(widget);
    }

    /** A column starting at the top-left of the content area. */
    public AbyssLayout.Flow column(int gap) { return AbyssLayout.column(x, y, gap); }

    /** A row starting at the top-left of the content area. */
    public AbyssLayout.Flow row(int gap) { return AbyssLayout.row(x, y, gap); }

    public Font font() { return Minecraft.getInstance().font; }

    public AbyssPanelScreen screen() { return screen; }

    /** Opens a dialog (or any screen); closing it comes back to this panel on the same tab. */
    public void open(Screen dialog) { Minecraft.getInstance().setScreen(dialog); }

    /** Rebuilds this tab (e.g. after the data it shows changed). */
    public void rebuild() { screen.rebuildTab(); }
}
