package com.gugas749.abysscore.client.ui;

import com.gugas749.abysscore.api.gui.dialog.AbyssDialog;
import com.gugas749.abysscore.api.permission.AbyssClientPermission;
import com.gugas749.abysscore.api.permission.AbyssPermissionLevel;
import com.gugas749.abysscore.api.gui.layout.AbyssLayout;
import com.gugas749.abysscore.api.gui.render.AbyssDraw;
import com.gugas749.abysscore.api.gui.screen.AbyssPanelScreen;
import com.gugas749.abysscore.api.gui.screen.AbyssTab;
import com.gugas749.abysscore.api.gui.screen.TabContext;
import com.gugas749.abysscore.api.gui.theme.AbyssTheme;
import com.gugas749.abysscore.api.gui.widget.*;
import com.gugas749.abysscore.client.ui.screens.BulkCommandScreen;
import com.gugas749.abysscore.client.ui.screens.DimenCreateScreen;
import com.gugas749.abysscore.client.ui.screens.RegionManagerScreen;
import com.gugas749.abysscore.features.vanish.VanishOption;
import com.gugas749.abysscore.network.AbyssNetwork;
import com.gugas749.abysscore.network.menu.packets.MenuActionPacket;
import com.gugas749.abysscore.network.menu.packets.OpenMainMenuPacket;
import com.gugas749.abysscore.network.menu.packets.RequestRegionScreenPacket;
import com.gugas749.abysscore.network.menu.packets.SaveTitlePacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.*;

/**
 * The AbyssCore staff menu (/abysscore). One tab per category.
 *
 * Every tab follows the same pattern: a list, you SELECT a row, and the buttons below the
 * list act on the selection. (The old screen drew fake buttons inside every row and matched
 * clicks against hand-written pixel boxes — now every button is a real widget.)
 *
 * The data comes from OpenMainMenuPacket. Tabs are inner classes so they can use the screen's
 * lists directly, and each keeps its own state (selection, edit mode, drafts) in FIELDS, so
 * switching tabs, resizing or closing a dialog never loses it.
 */
public class AbyssCoreMenuScreen extends AbyssPanelScreen {

    private static final int ROW_H = 14;
    private static final int BTN_GAP = 4;

    // ── Data from the server ──────────────────────────────────────────────────
    private final List<OpenMainMenuPacket.PlayerState> players;
    private final List<OpenMainMenuPacket.TitleEntry>  titles;
    private final List<OpenMainMenuPacket.DimEntry>    dims;
    private final List<OpenMainMenuPacket.HelpEntry>   helpRequests;
    private final List<OpenMainMenuPacket.BulkEntry>   bulkCommands;
    private final Map<Integer, String>                 bindSlots;
    private boolean teamVisibility;
    // The viewer's own vanish settings (OpenMainMenuPacket.VanishInfo)
    private int vanishOptions;
    private final Set<UUID> vanishShownTo;
    private final Set<UUID> vanishHiddenFrom;

    public AbyssCoreMenuScreen(OpenMainMenuPacket pkt) {
        super(Component.literal("ABYSS CORE"));
        this.players        = new ArrayList<>(pkt.players());
        this.titles         = new ArrayList<>(pkt.titles());
        this.dims           = new ArrayList<>(pkt.dims());
        this.helpRequests   = new ArrayList<>(pkt.helpRequests());
        this.bulkCommands   = new ArrayList<>(pkt.bulkCommands());
        this.bindSlots      = new LinkedHashMap<>(pkt.bindSlots());
        this.teamVisibility = pkt.teamVisibility();
        this.vanishOptions    = pkt.vanish().options();
        this.vanishShownTo    = new HashSet<>(pkt.vanish().shownTo());
        this.vanishHiddenFrom = new HashSet<>(pkt.vanish().hiddenFrom());
    }

    @Override protected int panelWidth()  { return 430; }
    @Override protected int panelHeight() { return 260; }

    @Override
    protected void addTabs(List<AbyssTab> tabs) {
        // The menu opens for MODERATOR+. Vanish, regions and dimensions are ADMIN-only on the
        // server (ABY-63), so a moderator doesn't get tabs whose every button would be refused.
        boolean admin = AbyssClientPermission.has(AbyssPermissionLevel.ADMIN);
        tabs.add(new PlayersTab());
        tabs.add(new TitlesTab());
        if (admin) tabs.add(new VanishTab());
        if (admin) tabs.add(new RegionsTab());
        if (admin) tabs.add(new DimensionsTab());
        tabs.add(new HelpTab());
        tabs.add(new BulkTab());
    }

    //-----------------------------------------------------------------------------------
    //                                 SHARED HELPERS
    //-----------------------------------------------------------------------------------

    private static void send(MenuActionPacket packet) {
        AbyssNetwork.sendToServer(packet);
    }

    /** A row of buttons along the bottom of the content area. */
    private static AbyssLayout.Flow bottomRow(TabContext ctx) {
        return AbyssLayout.row(ctx.x, ctx.y + ctx.height - AbyssButton.HEIGHT, BTN_GAP);
    }

    /** Height left for a list that sits above the bottom button row (and `extra` more pixels). */
    private static int listHeight(TabContext ctx, int extra) {
        return ctx.height - AbyssButton.HEIGHT - 6 - extra;
    }

    /** Right-aligned text inside a list row. */
    private static void rightText(GuiGraphics g, Font font, Component text, int rowX, int rowY, int rowW, int color) {
        g.drawString(font, text, rowX + rowW - 4 - font.width(text), rowY + 3, color, false);
    }

    //-----------------------------------------------------------------------------------
    //                                     PLAYERS
    //-----------------------------------------------------------------------------------

    /** Online players with their vanish / god / blind state. */
    private class PlayersTab implements AbyssTab {
        @Nullable private UUID selected;
        private AbyssScrollList<OpenMainMenuPacket.PlayerState> list;

        @Override public Component title() { return Component.literal("\u2605 Players"); }

        @Override
        public void init(TabContext ctx) {
            Font font = ctx.font();
            list = ctx.add(new AbyssScrollList<OpenMainMenuPacket.PlayerState>(
                    ctx.x, ctx.y, ctx.width, listHeight(ctx, 0), ROW_H,
                    (g, p, x, y, w, h, hover, sel) -> {
                        g.drawString(font, p.name(), x, y + 3, sel ? AbyssTheme.TEXT_BRIGHT : AbyssTheme.TEXT, false);
                        // state badges on the right: V G B, lit when ON
                        badge(g, font, "B", p.blinded(), x + w - 12, y);
                        badge(g, font, "G", p.godMode(), x + w - 24, y);
                        badge(g, font, "V", p.vanished(), x + w - 36, y);
                    }));
            list.emptyText(Component.literal("No players online."));
            list.setItems(players);
            list.setSelected(find(selected));
            list.onSelect(p -> { selected = p.uuid(); ctx.rebuild(); });

            // Toggles for the selected player (instead of three tiny ON/OFF boxes per row)
            OpenMainMenuPacket.PlayerState p = find(selected);
            var row = bottomRow(ctx);
            if (p == null) {
                ctx.add(new AbyssLabel(ctx.x, ctx.y + ctx.height - 14, ctx.width,
                        Component.literal("Select a player to change their state.")).color(AbyssTheme.TEXT_DIM));
                return;
            }
            if (AbyssClientPermission.has(AbyssPermissionLevel.ADMIN)) {   // vanish is ADMIN-only
                row.add(ctx.add(new AbyssToggle(0, 0, Component.literal("Vanish"), p.vanished(), on -> toggle(p.uuid(), 0))));
                row.space(8);
            }
            row.add(ctx.add(new AbyssToggle(0, 0, Component.literal("God"), p.godMode(), on -> toggle(p.uuid(), 1))));
            row.space(8);
            row.add(ctx.add(new AbyssToggle(0, 0, Component.literal("Blind"), p.blinded(), on -> toggle(p.uuid(), 2))));
        }

        private void badge(GuiGraphics g, Font font, String letter, boolean on, int x, int y) {
            g.drawString(font, letter, x, y + 3, on ? AbyssTheme.ACCENT : AbyssTheme.TEXT_OFF, false);
        }

        @Nullable
        private OpenMainMenuPacket.PlayerState find(@Nullable UUID id) {
            return id == null ? null : players.stream().filter(p -> p.uuid().equals(id)).findFirst().orElse(null);
        }

        /** col: 0 = vanish, 1 = god, 2 = blind. Sends it and updates our copy right away. */
        private void toggle(UUID id, int col) {
            MenuActionPacket.Action action = switch (col) {
                case 0 -> MenuActionPacket.Action.TOGGLE_VANISH;
                case 1 -> MenuActionPacket.Action.TOGGLE_GOD;
                default -> MenuActionPacket.Action.TOGGLE_BLIND;
            };
            send(MenuActionPacket.playerAction(action, id));

            for (int i = 0; i < players.size(); i++) {
                var p = players.get(i);
                if (!p.uuid().equals(id)) continue;
                var updated = new OpenMainMenuPacket.PlayerState(p.uuid(), p.name(),
                        col == 0 ? !p.vanished() : p.vanished(),
                        col == 1 ? !p.godMode() : p.godMode(),
                        col == 2 ? !p.blinded() : p.blinded());
                players.set(i, updated);
                list.setItems(players);
                list.setSelected(updated);
            }
        }
    }

    //-----------------------------------------------------------------------------------
    //                                     TITLES
    //-----------------------------------------------------------------------------------

    /** Saved titles: list → edit / send views, all inside this one tab. */
    private class TitlesTab implements AbyssTab {
        private enum Mode { LIST, EDIT, SEND }

        private Mode mode = Mode.LIST;
        @Nullable private String selectedId;
        // Editor draft (kept in fields so it survives tab switches and resizes)
        @Nullable private String editingId;          // null = new title
        private String draftName = "", draftText = "", draftSub = "";
        private String draftIn = "10", draftStay = "70", draftOut = "20";
        // Send view
        private String sendTag = "", sendTeam = "";

        @Override public Component title() { return Component.literal("\u2736 Titles"); }

        @Override
        public void init(TabContext ctx) {
            switch (mode) {
                case LIST -> initList(ctx);
                case EDIT -> initEditor(ctx);
                case SEND -> initSend(ctx);
            }
        }

        // ── List ──
        private void initList(TabContext ctx) {
            Font font = ctx.font();
            var list = ctx.add(new AbyssScrollList<OpenMainMenuPacket.TitleEntry>(
                    ctx.x, ctx.y, ctx.width, listHeight(ctx, 0), ROW_H,
                    (g, t, x, y, w, h, hover, sel) -> {
                        g.drawString(font, t.name(), x, y + 3, sel ? AbyssTheme.TEXT_BRIGHT : AbyssTheme.TEXT, false);
                        g.drawString(font, AbyssDraw.trimmed(font, Component.literal(t.titleText()), w - 120),
                                x + 110, y + 3, AbyssTheme.TEXT_DIM, false);
                    }));
            list.emptyText(Component.literal("No titles yet. Click + New."));
            list.setItems(titles);
            list.setSelected(selected());
            list.onSelect(t -> { selectedId = t.id(); ctx.rebuild(); });

            var t = selected();
            var row = bottomRow(ctx);
            row.add(ctx.add(new AbyssButton(0, 0, 60, Component.literal("+ New"), b -> openEditor(ctx, null))));
            AbyssButton edit = row.add(ctx.add(new AbyssButton(0, 0, 60, Component.literal("Edit"), b -> openEditor(ctx, t))));
            AbyssButton sendBtn = row.add(ctx.add(new AbyssButton(0, 0, 60, Component.literal("Send..."), b -> { mode = Mode.SEND; ctx.rebuild(); })));
            AbyssButton delete = row.add(ctx.add(new AbyssButton(0, 0, 60, Component.literal("Delete"), AbyssButton.Style.DANGER,
                    b -> ctx.open(AbyssDialog.confirmDanger(ctx.screen(), Component.literal("Delete title?"),
                            Component.literal("\"" + t.name() + "\" will be deleted for everyone."), () -> {
                                send(MenuActionPacket.stringAction(MenuActionPacket.Action.DELETE_TITLE, t.id(), ""));
                                titles.remove(t);
                                selectedId = null;
                            })))));
            edit.active = sendBtn.active = delete.active = (t != null);
        }

        private void openEditor(TabContext ctx, @Nullable OpenMainMenuPacket.TitleEntry t) {
            editingId = t == null ? null : t.id();
            draftName = t == null ? "" : t.name();
            draftText = t == null ? "" : t.titleText();
            draftSub  = t == null ? "" : t.subtitleText();
            draftIn   = t == null ? "10" : String.valueOf(t.fadeIn());
            draftStay = t == null ? "70" : String.valueOf(t.stay());
            draftOut  = t == null ? "20" : String.valueOf(t.fadeOut());
            mode = Mode.EDIT;
            ctx.rebuild();
        }

        // ── Editor ──
        private AbyssButton saveButton;

        private void initEditor(TabContext ctx) {
            int w = ctx.width;
            int third = (w - 8) / 3;
            // Labels are drawn in render(); every field sits 10px under its label
            field(ctx, ctx.x, ctx.y + 22, w, "Name", draftName, v -> draftName = v);
            field(ctx, ctx.x, ctx.y + 54, w, "Title text  (&a green, &c red, &e yellow...)", draftText, v -> draftText = v);
            field(ctx, ctx.x, ctx.y + 86, w, "Subtitle (optional)", draftSub, v -> draftSub = v);
            numberField(ctx, ctx.x, ctx.y + 118, third, draftIn, v -> draftIn = v);
            numberField(ctx, ctx.x + third + 4, ctx.y + 118, third, draftStay, v -> draftStay = v);
            numberField(ctx, ctx.x + 2 * (third + 4), ctx.y + 118, third, draftOut, v -> draftOut = v);

            var row = bottomRow(ctx);
            saveButton = row.add(ctx.add(new AbyssButton(0, 0, 70, Component.literal("Save"), b -> save())));
            row.add(ctx.add(new AbyssButton(0, 0, 70, Component.literal("Cancel"), b -> { mode = Mode.LIST; ctx.rebuild(); })));
            updateSave();
        }

        private void field(TabContext ctx, int x, int y, int w, String hint, String value, java.util.function.Consumer<String> onChange) {
            var f = ctx.add(new AbyssTextField(x, y, w, Component.literal(hint)));
            f.setMaxLength(256);
            f.setValue(value);
            f.setResponder(v -> { onChange.accept(v); updateSave(); });
        }

        private void numberField(TabContext ctx, int x, int y, int w, String value, java.util.function.Consumer<String> onChange) {
            var f = ctx.add(new AbyssTextField(x, y, w, Component.literal("ticks")));
            f.setMaxLength(5);
            f.setFilter(s -> s.chars().allMatch(Character::isDigit));   // digits only
            f.setValue(value);
            f.setResponder(onChange);
        }

        /** Save only makes sense with a name and a title text. */
        private void updateSave() {
            if (saveButton != null) saveButton.active = !draftName.isBlank() && !draftText.isBlank();
        }

        private void save() {
            AbyssNetwork.sendToServer(new SaveTitlePacket(
                    editingId == null ? "" : editingId, draftName.trim(), draftText.trim(), draftSub.trim(),
                    parse(draftIn, 10), parse(draftStay, 70), parse(draftOut, 20)));
            // The server assigns ids to new titles, so our list can't show the result yet:
            // close like before — reopen the menu to see the saved title.
            onClose();
        }

        // ── Send ──
        private void initSend(TabContext ctx) {
            var t = selected();
            if (t == null) { mode = Mode.LIST; initList(ctx); return; }

            var col = AbyssLayout.column(ctx.x, ctx.y + 14, 6);
            col.add(ctx.add(new AbyssButton(0, 0, ctx.width, Component.literal("Send to ALL players"), b -> {
                send(MenuActionPacket.stringAction(MenuActionPacket.Action.SEND_TITLE_ALL, t.id(), ""));
                back(ctx);
            })));

            int fieldW = ctx.width - 90 - 4;
            var tag = ctx.add(new AbyssTextField(ctx.x, col.y(), fieldW, Component.literal("Scoreboard tag...")));
            tag.setValue(sendTag);
            tag.setResponder(v -> sendTag = v);
            ctx.add(new AbyssButton(ctx.x + fieldW + 4, col.y(), 90, Component.literal("Send by tag"), b -> {
                send(MenuActionPacket.stringAction(MenuActionPacket.Action.SEND_TITLE_TAG, t.id(), sendTag.trim()));
                back(ctx);
            }));
            col.space(AbyssTextField.HEIGHT + 6);

            var team = ctx.add(new AbyssTextField(ctx.x, col.y(), fieldW, Component.literal("Team name...")));
            team.setValue(sendTeam);
            team.setResponder(v -> sendTeam = v);
            ctx.add(new AbyssButton(ctx.x + fieldW + 4, col.y(), 90, Component.literal("Send by team"), b -> {
                send(MenuActionPacket.stringAction(MenuActionPacket.Action.SEND_TITLE_TEAM, t.id(), sendTeam.trim()));
                back(ctx);
            }));

            bottomRow(ctx).add(ctx.add(new AbyssButton(0, 0, 70, Component.literal("Back"), b -> back(ctx))));
        }

        private void back(TabContext ctx) {
            mode = Mode.LIST;
            ctx.rebuild();
        }

        @Override
        public void render(GuiGraphics g, TabContext ctx, int mouseX, int mouseY, float partialTick) {
            Font font = ctx.font();
            if (mode == Mode.EDIT) {
                AbyssDraw.sectionTitle(g, font, Component.literal(editingId == null ? "NEW TITLE" : "EDIT TITLE"), ctx.x, ctx.y);
                label(g, font, "Name", ctx.x, ctx.y + 13);
                label(g, font, "Title text", ctx.x, ctx.y + 45);
                label(g, font, "Subtitle", ctx.x, ctx.y + 77);
                int third = (ctx.width - 8) / 3;
                label(g, font, "Fade in", ctx.x, ctx.y + 109);
                label(g, font, "Stay", ctx.x + third + 4, ctx.y + 109);
                label(g, font, "Fade out", ctx.x + 2 * (third + 4), ctx.y + 109);
            } else if (mode == Mode.SEND) {
                var t = selected();
                if (t != null) AbyssDraw.sectionTitle(g, font, Component.literal("SEND: " + t.name()), ctx.x, ctx.y);
            }
        }

        @Nullable
        private OpenMainMenuPacket.TitleEntry selected() {
            return selectedId == null ? null : titles.stream().filter(t -> t.id().equals(selectedId)).findFirst().orElse(null);
        }
    }

    //-----------------------------------------------------------------------------------
    //                                     VANISH
    //-----------------------------------------------------------------------------------

    /**
     * Two pages:
     *   OPTIONS    → your own vanish settings (one toggle per VanishOption, tooltip = description)
     *   VISIBILITY → per-player exceptions: force yourself visible / hidden for one player
     * The top row (your vanish + team visibility) is on both pages.
     */
    private class VanishTab implements AbyssTab {
        private enum Page { OPTIONS, VISIBILITY }

        private Page page = Page.OPTIONS;
        @Nullable private UUID selected;

        @Override public Component title() { return Component.literal("◎ Vanish"); }

        @Override
        public void init(TabContext ctx) {
            UUID me = myUuid();
            var self = players.stream().filter(p -> p.uuid().equals(me)).findFirst().orElse(null);

            // ── Top row ──
            var top = AbyssLayout.row(ctx.x, ctx.y, 8);
            top.add(ctx.add(new AbyssToggle(0, 0, Component.translatable("gui.abysscore.vanish.self"),
                    self != null && self.vanished(), on -> {
                        send(MenuActionPacket.playerAction(MenuActionPacket.Action.TOGGLE_VANISH, me));
                        setVanished(me, on);
                    })));
            top.add(ctx.add(new AbyssToggle(0, 0, Component.translatable("gui.abysscore.vanish.team_visibility"),
                    teamVisibility, on -> {
                        send(MenuActionPacket.of(MenuActionPacket.Action.TOGGLE_TEAM_VISIBILITY));
                        teamVisibility = on;
                    })));

            int pageBtnW = 64;
            AbyssButton optionsBtn = ctx.add(new AbyssButton(ctx.x + ctx.width - pageBtnW * 2 - 4, ctx.y, pageBtnW,
                    Component.translatable("gui.abysscore.vanish.page_options"), b -> { page = Page.OPTIONS; ctx.rebuild(); }));
            AbyssButton visibilityBtn = ctx.add(new AbyssButton(ctx.x + ctx.width - pageBtnW, ctx.y, pageBtnW,
                    Component.translatable("gui.abysscore.vanish.page_visibility"), b -> { page = Page.VISIBILITY; ctx.rebuild(); }));
            optionsBtn.active = page != Page.OPTIONS;          // the current page's button looks "pressed"
            visibilityBtn.active = page != Page.VISIBILITY;

            if (page == Page.OPTIONS) initOptions(ctx);
            else initVisibility(ctx, me);
        }

        // ── Page: options ──
        private void initOptions(TabContext ctx) {
            int colW = (ctx.width - 8) / 2;
            var grid = AbyssLayout.grid(ctx.x, ctx.y + 44, 2, colW, 20, 8, 2);
            for (VanishOption option : VanishOption.values()) {
                boolean on = (vanishOptions & option.bit()) != 0;
                var toggle = grid.add(ctx.add(new AbyssToggle(0, 0, Component.translatable(option.labelKey()), on, value -> {
                    send(new MenuActionPacket(MenuActionPacket.Action.VANISH_SET_OPTION, null, option.id(), "", value ? 1 : 0));
                    vanishOptions = value ? vanishOptions | option.bit() : vanishOptions & ~option.bit();
                })));
                toggle.setTooltip(Tooltip.create(Component.translatable(option.descriptionKey())));
            }
        }

        // ── Page: visibility exceptions ──
        private void initVisibility(TabContext ctx, UUID me) {
            Font font = ctx.font();
            var others = players.stream().filter(p -> !p.uuid().equals(me)).toList();
            int listY = ctx.y + 44;
            var list = ctx.add(new AbyssScrollList<OpenMainMenuPacket.PlayerState>(
                    ctx.x, listY, ctx.width, ctx.y + listHeight(ctx, 0) - listY, ROW_H,
                    (g, p, x, y, w, h, hover, sel) -> {
                        g.drawString(font, p.name(), x, y + 3, sel ? AbyssTheme.TEXT_BRIGHT : AbyssTheme.TEXT, false);
                        if (vanishShownTo.contains(p.uuid())) {
                            rightText(g, font, Component.translatable("gui.abysscore.vanish.forced_visible"), x, y, w, AbyssTheme.ACCENT);
                        } else if (vanishHiddenFrom.contains(p.uuid())) {
                            rightText(g, font, Component.translatable("gui.abysscore.vanish.forced_hidden").withStyle(ChatFormatting.RED), x, y, w, AbyssTheme.TEXT);
                        } else {
                            rightText(g, font, Component.translatable("gui.abysscore.vanish.default_rule"), x, y, w, AbyssTheme.TEXT_OFF);
                        }
                    }));
            list.emptyText(Component.translatable("gui.abysscore.vanish.no_other_players"));
            list.setItems(others);
            var current = others.stream().filter(p -> p.uuid().equals(selected)).findFirst().orElse(null);
            list.setSelected(current);
            list.onSelect(p -> { selected = p.uuid(); ctx.rebuild(); });

            // targetUUID = the viewer (selected player), stringArg1 = the vanished player (me)
            var row = bottomRow(ctx);
            AbyssButton show = row.add(ctx.add(new AbyssButton(0, 0, 80, Component.translatable("gui.abysscore.vanish.show_me"), b -> {
                send(new MenuActionPacket(MenuActionPacket.Action.VANISH_SHOW_TO, current.uuid(), me.toString(), "", 0));
                vanishHiddenFrom.remove(current.uuid());
                vanishShownTo.add(current.uuid());
            })));
            AbyssButton hide = row.add(ctx.add(new AbyssButton(0, 0, 80, Component.translatable("gui.abysscore.vanish.hide_me"), b -> {
                send(new MenuActionPacket(MenuActionPacket.Action.VANISH_HIDE_FROM, current.uuid(), me.toString(), "", 0));
                vanishShownTo.remove(current.uuid());
                vanishHiddenFrom.add(current.uuid());
            })));
            row.add(ctx.add(new AbyssButton(0, 0, 80, Component.translatable("gui.abysscore.vanish.clear_all"), AbyssButton.Style.DANGER, b -> {
                send(new MenuActionPacket(MenuActionPacket.Action.VANISH_CLEAR, null, me.toString(), "", 0));
                vanishShownTo.clear();
                vanishHiddenFrom.clear();
            })));
            show.active = hide.active = (current != null);
        }

        @Override
        public void render(GuiGraphics g, TabContext ctx, int mouseX, int mouseY, float partialTick) {
            g.fill(ctx.x, ctx.y + 24, ctx.x + ctx.width, ctx.y + 25, AbyssTheme.DIVIDER);
            AbyssDraw.sectionTitle(g, ctx.font(), Component.translatable(page == Page.OPTIONS
                    ? "gui.abysscore.vanish.section_options" : "gui.abysscore.vanish.section_visibility"), ctx.x, ctx.y + 30);
        }
    }

    /** The local player's UUID (the one using the menu). */
    private static UUID myUuid() {
        var player = Minecraft.getInstance().player;
        return player != null ? player.getUUID() : new UUID(0, 0);
    }

    /** Updates our copy of a player's vanish flag right away, like PlayersTab does for its toggles. */
    private void setVanished(UUID id, boolean vanished) {
        for (int i = 0; i < players.size(); i++) {
            var p = players.get(i);
            if (p.uuid().equals(id)) {
                players.set(i, new OpenMainMenuPacket.PlayerState(p.uuid(), p.name(), vanished, p.godMode(), p.blinded()));
            }
        }
    }

    //-----------------------------------------------------------------------------------
    //                                     REGIONS
    //-----------------------------------------------------------------------------------

    private class RegionsTab implements AbyssTab {
        @Override public Component title() { return Component.literal("\u25A6 Regions"); }

        @Override
        public void init(TabContext ctx) {
            int w = 180;
            ctx.add(new AbyssButton(AbyssLayout.center(ctx.x, ctx.width, w), ctx.y + ctx.height / 2 - 10, w,
                    Component.literal("\u25A6  Open Region Manager"), b -> {
                        // The server answers with the region list; remember where to come back to
                        RegionManagerScreen.pendingPreviousScreen = AbyssCoreMenuScreen.this;
                        AbyssNetwork.sendToServer(new RequestRegionScreenPacket());
                    }));
        }

        @Override
        public void render(GuiGraphics g, TabContext ctx, int mouseX, int mouseY, float partialTick) {
            g.drawCenteredString(ctx.font(), Component.literal("Protected areas: build, entry, PvP and spawning rules."),
                    ctx.x + ctx.width / 2, ctx.y + ctx.height / 2 - 26, AbyssTheme.TEXT_DIM);
        }
    }

    //-----------------------------------------------------------------------------------
    //                                   DIMENSIONS
    //-----------------------------------------------------------------------------------

    private class DimensionsTab implements AbyssTab {
        @Nullable private String selected;

        @Override public Component title() { return Component.literal("\u2318 Dimensions"); }

        @Override
        public void init(TabContext ctx) {
            Font font = ctx.font();
            var list = ctx.add(new AbyssScrollList<OpenMainMenuPacket.DimEntry>(
                    ctx.x, ctx.y, ctx.width, listHeight(ctx, 0), ROW_H,
                    (g, d, x, y, w, h, hover, sel) -> {
                        g.drawString(font, d.name(), x, y + 3, sel ? AbyssTheme.TEXT_BRIGHT : AbyssTheme.TEXT, false);
                        ChatFormatting color = switch (d.state()) {
                            case "LOADED" -> ChatFormatting.GREEN;
                            case "UNLOADED" -> ChatFormatting.RED;
                            default -> ChatFormatting.YELLOW;
                        };
                        rightText(g, font, Component.literal(d.state().toLowerCase()).withStyle(color), x, y, w, AbyssTheme.TEXT);
                    }));
            list.emptyText(Component.literal("No dimensions registered."));
            list.setItems(dims);
            var current = dims.stream().filter(d -> d.name().equals(selected)).findFirst().orElse(null);
            list.setSelected(current);
            list.onSelect(d -> { selected = d.name(); ctx.rebuild(); });

            var row = bottomRow(ctx);
            row.add(ctx.add(new AbyssButton(0, 0, 80, Component.literal("+ Create"), b ->
                    ctx.open(new DimenCreateScreen(AbyssCoreMenuScreen.this)))));
            AbyssButton tp = row.add(ctx.add(new AbyssButton(0, 0, 80, Component.literal("Teleport"), b ->
                    send(MenuActionPacket.stringAction(MenuActionPacket.Action.DIMEN_TP, current.name(), "")))));
            tp.active = current != null;
        }
    }

    //-----------------------------------------------------------------------------------
    //                                  HELP REQUESTS
    //-----------------------------------------------------------------------------------

    private class HelpTab implements AbyssTab {
        @Nullable private UUID selected;

        @Override public Component title() { return Component.literal("\u2709 Help"); }

        @Override
        public void init(TabContext ctx) {
            Font font = ctx.font();
            var list = ctx.add(new AbyssScrollList<OpenMainMenuPacket.HelpEntry>(
                    ctx.x, ctx.y, ctx.width, listHeight(ctx, 0), ROW_H,
                    (g, h, x, y, w, rh, hover, sel) -> {
                        int nameW = font.width(h.playerName() + ": ");
                        g.drawString(font, h.playerName() + ":", x, y + 3, AbyssTheme.ACCENT, false);
                        g.drawString(font, AbyssDraw.trimmed(font, Component.literal(h.reason()), w - nameW - 4),
                                x + nameW, y + 3, sel ? AbyssTheme.TEXT_BRIGHT : AbyssTheme.TEXT, false);
                    }));
            list.emptyText(Component.literal("No active help requests."));
            list.setItems(helpRequests);
            var current = helpRequests.stream().filter(h -> h.playerUUID().equals(selected)).findFirst().orElse(null);
            list.setSelected(current);
            list.onSelect(h -> { selected = h.playerUUID(); ctx.rebuild(); });

            AbyssButton accept = bottomRow(ctx).add(ctx.add(new AbyssButton(0, 0, 120,
                    Component.literal("Accept & teleport"), b -> {
                        send(MenuActionPacket.playerAction(MenuActionPacket.Action.HELP_ACCEPT, current.playerUUID()));
                        helpRequests.remove(current);
                        selected = null;
                        ctx.rebuild();
                    })));
            accept.active = current != null;
        }
    }

    //-----------------------------------------------------------------------------------
    //                                  BULK COMMANDS
    //-----------------------------------------------------------------------------------

    private class BulkTab implements AbyssTab {
        private static final int SLOT_W = 20;
        @Nullable private String selected;

        @Override public Component title() { return Component.literal("\u2630 Bulk"); }

        @Override
        public void init(TabContext ctx) {
            Font font = ctx.font();
            // Two button rows at the bottom: slots, then actions → the list gets 26px less
            var list = ctx.add(new AbyssScrollList<OpenMainMenuPacket.BulkEntry>(
                    ctx.x, ctx.y, ctx.width, listHeight(ctx, AbyssButton.HEIGHT + 16), ROW_H,
                    (g, b, x, y, w, h, hover, sel) -> {
                        g.drawString(font, b.name(), x, y + 3, sel ? AbyssTheme.TEXT_BRIGHT : AbyssTheme.TEXT, false);
                        Integer slot = slotOf(b.name());
                        rightText(g, font, Component.literal(slot == null ? "-" : "slot " + slot), x, y, w,
                                slot == null ? AbyssTheme.TEXT_OFF : AbyssTheme.ACCENT);
                    }));
            list.emptyText(Component.literal("No bulk commands. Click + Create."));
            list.setItems(bulkCommands);
            var current = bulkCommands.stream().filter(b -> b.name().equals(selected)).findFirst().orElse(null);
            list.setSelected(current);
            list.onSelect(b -> { selected = b.name(); ctx.rebuild(); });

            // Quick-bind slots 1–9 for the selected command
            int slotsY = ctx.y + ctx.height - AbyssButton.HEIGHT * 2 - 6;
            var slots = AbyssLayout.row(ctx.x + 60, slotsY, 2);
            for (int slot = 1; slot <= 9; slot++) {
                int s = slot;
                String boundTo = bindSlots.get(s);
                // aqua = bound to THIS command, grey = bound to another one, white = free
                ChatFormatting color = current != null && current.name().equals(boundTo) ? ChatFormatting.AQUA
                        : boundTo != null ? ChatFormatting.DARK_GRAY : ChatFormatting.WHITE;
                AbyssButton btn = slots.add(ctx.add(new AbyssButton(0, 0, SLOT_W,
                        Component.literal(String.valueOf(s)).withStyle(color), b -> toggleBind(ctx, current.name(), s))));
                btn.active = current != null;
                if (boundTo != null) btn.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Bound: " + boundTo)));
            }

            var row = bottomRow(ctx);
            row.add(ctx.add(new AbyssButton(0, 0, 80, Component.literal("+ Create"), b ->
                    ctx.open(new BulkCommandScreen(AbyssCoreMenuScreen.this)))));
            AbyssButton run = row.add(ctx.add(new AbyssButton(0, 0, 80, Component.literal("Run"), b ->
                    send(MenuActionPacket.stringAction(MenuActionPacket.Action.BULK_RUN, current.name(), "")))));
            run.active = current != null;
        }

        /** Clicking the command's own slot unbinds it; any other slot binds it there. */
        private void toggleBind(TabContext ctx, String name, int slot) {
            if (name.equals(bindSlots.get(slot))) {
                send(MenuActionPacket.intAction(MenuActionPacket.Action.BULK_UNBIND, "", slot));
                bindSlots.remove(slot);
            } else {
                send(MenuActionPacket.intAction(MenuActionPacket.Action.BULK_BIND, name, slot));
                bindSlots.put(slot, name);
            }
            ctx.rebuild();   // recolor the slot buttons
        }

        @Nullable
        private Integer slotOf(String name) {
            return bindSlots.entrySet().stream().filter(e -> e.getValue().equals(name))
                    .map(Map.Entry::getKey).findFirst().orElse(null);
        }

        @Override
        public void render(GuiGraphics g, TabContext ctx, int mouseX, int mouseY, float partialTick) {
            int slotsY = ctx.y + ctx.height - AbyssButton.HEIGHT * 2 - 6;
            g.drawString(ctx.font(), "Quick bind", ctx.x, slotsY + 6, AbyssTheme.TEXT_DIM, false);
        }
    }

    //-----------------------------------------------------------------------------------

    private static void label(GuiGraphics g, Font font, String text, int x, int y) {
        g.drawString(font, text, x, y, AbyssTheme.TEXT_DIM, false);
    }

    private static int parse(String value, int fallback) {
        try { return Integer.parseInt(value.trim()); } catch (NumberFormatException e) { return fallback; }
    }
}
