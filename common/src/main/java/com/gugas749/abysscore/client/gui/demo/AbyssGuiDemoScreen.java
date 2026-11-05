package com.gugas749.abysscore.client.gui.demo;

import com.gugas749.abysscore.api.gui.dialog.AbyssDialog;
import com.gugas749.abysscore.api.gui.layout.AbyssLayout;
import com.gugas749.abysscore.api.gui.render.AbyssDraw;
import com.gugas749.abysscore.api.gui.screen.AbyssPanelScreen;
import com.gugas749.abysscore.api.gui.screen.AbyssTab;
import com.gugas749.abysscore.api.gui.screen.TabContext;
import com.gugas749.abysscore.api.gui.theme.AbyssTheme;
import com.gugas749.abysscore.api.gui.widget.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Shows every part of the AbyssCore GUI API. Open with /abyssgui (staff only).
 * Also a reference: each tab demonstrates one way of building a tab.
 */
public class AbyssGuiDemoScreen extends AbyssPanelScreen {

    public AbyssGuiDemoScreen() {
        super(Component.literal("ABYSS GUI DEMO"));
    }

    @Override
    protected void addTabs(List<AbyssTab> tabs) {
        tabs.add(new WidgetsTab());
        tabs.add(new PlayersTab());
        tabs.add(new DialogsTab());
        tabs.add(AbyssTab.of(Component.literal("Grid"), GridTab::build));   // a tab without its own class
    }

    // ── Widgets: buttons, toggle, slider, text field. State lives in FIELDS (see AbyssTab). ──
    private static class WidgetsTab implements AbyssTab {
        private boolean vanish = true;
        private double radius = 0.5;
        private String name = "spawn_region_01";

        @Override public Component title() { return Component.literal("Widgets"); }

        // Section title y-positions (relative to the content area), used by init() AND render()
        private static final int BUTTONS_Y = 0, TOGGLES_Y = 38, FIELD_Y = 100;

        @Override
        public void init(TabContext ctx) {
            // Row of buttons under the first title
            var buttons = AbyssLayout.row(ctx.x, ctx.y + BUTTONS_Y + 12, 4);
            buttons.add(ctx.add(new AbyssButton(0, 0, 64, Component.literal("Save"), b -> {})));
            buttons.add(ctx.add(new AbyssButton(0, 0, 70, Component.literal("Delete"), AbyssButton.Style.DANGER, b -> {})));
            AbyssButton locked = buttons.add(ctx.add(new AbyssButton(0, 0, 64, Component.literal("Locked"), b -> {})));
            locked.active = false;

            // Column: toggle + slider under the second title
            var col = AbyssLayout.column(ctx.x, ctx.y + TOGGLES_Y + 12, 4);
            col.add(ctx.add(new AbyssToggle(0, 0, Component.literal("Vanish"), vanish, v -> vanish = v)));
            col.add(ctx.add(new AbyssSlider(0, 0, 150, radius,
                    v -> Component.literal("Radius: " + (int) (v * 128)), v -> radius = v)));

            // Text field under the third title
            AbyssTextField field = ctx.add(new AbyssTextField(ctx.x, ctx.y + FIELD_Y + 12, 150, Component.literal("Region name...")));
            field.setValue(name);
            field.setResponder(text -> name = text);
        }

        @Override
        public void render(GuiGraphics g, TabContext ctx, int mouseX, int mouseY, float partialTick) {
            AbyssDraw.sectionTitle(g, ctx.font(), Component.literal("BUTTONS"), ctx.x, ctx.y + BUTTONS_Y);
            AbyssDraw.sectionTitle(g, ctx.font(), Component.literal("TOGGLE & SLIDER"), ctx.x, ctx.y + TOGGLES_Y);
            AbyssDraw.sectionTitle(g, ctx.font(), Component.literal("TEXT FIELD"), ctx.x, ctx.y + FIELD_Y);
        }
    }

    // ── Players: AbyssScrollList with a custom row renderer + selection ──
    private static class PlayersTab implements AbyssTab {
        private String selected;
        private AbyssLabel info;

        @Override public Component title() { return Component.literal("Players"); }

        @Override
        public void init(TabContext ctx) {
            var font = ctx.font();
            AbyssScrollList<String> list = ctx.add(new AbyssScrollList<String>(ctx.x, ctx.y, ctx.width, ctx.height - 16, 12,
                    (g, name, x, y, w, h, hovered, isSelected) ->
                            g.drawString(font, name, x, y + 2, isSelected ? AbyssTheme.TEXT_BRIGHT : AbyssTheme.TEXT, true)));
            list.setItems(onlinePlayers());
            list.setSelected(selected);
            list.onSelect(name -> {
                selected = name;
                info.setText(Component.literal("Selected: " + name));
            });

            info = ctx.add(new AbyssLabel(ctx.x, ctx.y + ctx.height - 9, ctx.width,
                    Component.literal(selected == null ? "Click a player" : "Selected: " + selected)).color(AbyssTheme.TEXT_DIM));
        }

        private static List<String> onlinePlayers() {
            var connection = Minecraft.getInstance().getConnection();
            if (connection == null) return List.of();
            return connection.getOnlinePlayers().stream()
                    .map(PlayerInfo::getProfile).map(p -> p.getName())
                    .sorted(Comparator.naturalOrder()).toList();
        }
    }

    // ── Dialogs: every kind ──
    private static class DialogsTab implements AbyssTab {
        private String lastResult = "-";

        @Override public Component title() { return Component.literal("Dialogs"); }

        @Override
        public void init(TabContext ctx) {
            var screen = ctx.screen();
            var col = ctx.column(4);
            col.add(ctx.add(new AbyssButton(0, 0, 120, Component.literal("Alert"), b -> ctx.open(
                    AbyssDialog.alert(screen, Component.literal("Heads up"), Component.literal("This is an alert dialog."))))));
            col.add(ctx.add(new AbyssButton(0, 0, 120, Component.literal("Confirm"), b -> ctx.open(
                    AbyssDialog.confirm(screen, Component.literal("Save changes?"),
                            Component.literal("The region settings will be saved."), () -> lastResult = "confirmed")))));
            col.add(ctx.add(new AbyssButton(0, 0, 120, Component.literal("Delete..."), AbyssButton.Style.DANGER, b -> ctx.open(
                    AbyssDialog.confirmDanger(screen, Component.literal("Delete region?"),
                            Component.literal("This cannot be undone. All protections in this region will be removed."),
                            () -> lastResult = "deleted")))));
            col.add(ctx.add(new AbyssButton(0, 0, 120, Component.literal("Rename..."), b -> ctx.open(
                    AbyssDialog.input(screen, Component.literal("Rename"), Component.literal("New name (3+ characters):"),
                            "spawn", s -> s.trim().length() >= 3, s -> lastResult = "renamed to " + s)))));
        }

        @Override
        public void render(GuiGraphics g, TabContext ctx, int mouseX, int mouseY, float partialTick) {
            g.drawString(ctx.font(), "Last result: " + lastResult, ctx.x, ctx.y + 100, AbyssTheme.TEXT_DIM, false);
        }
    }

    // ── Grid layout ──
    private static class GridTab {
        static void build(TabContext ctx) {
            var grid = AbyssLayout.grid(ctx.x, ctx.y, 3, 64, 20, 4, 4);
            for (int i = 1; i <= 9; i++) {
                grid.add(ctx.add(new AbyssButton(0, 0, 0, Component.literal("Slot " + i), b -> {})));
            }
        }
    }
}
