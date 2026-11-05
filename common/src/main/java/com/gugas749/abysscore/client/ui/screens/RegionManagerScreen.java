package com.gugas749.abysscore.client.ui.screens;

import com.gugas749.abysscore.api.gui.dialog.AbyssDialog;
import com.gugas749.abysscore.api.gui.render.AbyssDraw;
import com.gugas749.abysscore.api.gui.screen.AbyssPanelScreen;
import com.gugas749.abysscore.api.gui.screen.AbyssTab;
import com.gugas749.abysscore.api.gui.screen.TabContext;
import com.gugas749.abysscore.api.gui.theme.AbyssTheme;
import com.gugas749.abysscore.api.gui.widget.AbyssButton;
import com.gugas749.abysscore.api.gui.widget.AbyssScrollList;
import com.gugas749.abysscore.api.gui.widget.AbyssTextField;
import com.gugas749.abysscore.api.gui.widget.AbyssToggle;
import com.gugas749.abysscore.features.regions.ACBlockProtectionListener;
import com.gugas749.abysscore.network.AbyssNetwork;
import com.gugas749.abysscore.network.region.OpenRegionScreenPacket;
import com.gugas749.abysscore.network.region.SubmitRegionUpdatePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Region Manager: list on the left, the selected region's restrictions on the right.
 *
 *   ┌ Regions ──┬ spawn ──────────────────────────────┐
 *   │ spawn  [3]│ minecraft:overworld                 │
 *   │ arena     │ -20 60 -20  →  20 90 20             │
 *   │           │ RESTRICTIONS                        │
 *   │           │ (●) No Build       ( ) No Fly  ...  │
 *   │           │ [Delete]                    [Save]  │
 *   └───────────┴─────────────────────────────────────┘
 */
public class RegionManagerScreen extends AbyssPanelScreen {

    private static final int LIST_W = 130;
    private static final int ROW_H = 14;

    /** Every restriction tag, in display order, with a short label that fits two columns. */
    private static final LinkedHashMap<String, String> TAGS = new LinkedHashMap<>();
    static {
        TAGS.put(ACBlockProtectionListener.NO_BUILD_TAG, "No Build");
        TAGS.put(ACBlockProtectionListener.NO_INTERACT_TAG, "No Interact");
        TAGS.put(ACBlockProtectionListener.NO_FLY_TAG, "No Fly");
        TAGS.put(ACBlockProtectionListener.NO_FRIENDLYFIRE_TAG, "No Friendly Fire");
        TAGS.put(ACBlockProtectionListener.NO_HUNGER_TAG, "No Hunger");
        TAGS.put(ACBlockProtectionListener.NO_TP_TAG, "No Teleport");
        TAGS.put(ACBlockProtectionListener.NO_MOBSPAWNING_HOSTILE_TAG, "No Hostile Mobs");
        TAGS.put(ACBlockProtectionListener.NO_MOBSPAWNING_PACIFIC_TAG, "No Passive Mobs");
        TAGS.put(ACBlockProtectionListener.NO_ENTRY_TAG, "No Entry");
        TAGS.put(ACBlockProtectionListener.KEEP_INVENTORY_TAG, "Keep Inventory");
    }

    /** Set by whoever sends RequestRegionScreenPacket, so the server's answer knows where "back" is. */
    public static Screen pendingPreviousScreen = null;

    private final List<OpenRegionScreenPacket.RegionEntry> regions;

    // Editing state for the selected region (fields → survives rebuilds and the delete dialog)
    @Nullable private String selectedName;
    private final Set<String> activeTags = new LinkedHashSet<>();
    private String filterTag = "";

    public RegionManagerScreen(List<OpenRegionScreenPacket.RegionEntry> regions, @Nullable Screen parent) {
        super(Component.literal("\u25A6  REGION MANAGER"), parent);
        this.regions = new ArrayList<>(regions);
    }

    /** Used by the packet handler: comes back to the screen that asked for the region list. */
    public RegionManagerScreen(List<OpenRegionScreenPacket.RegionEntry> regions) {
        this(regions, pendingPreviousScreen);
        pendingPreviousScreen = null;
    }

    @Override protected int panelWidth()  { return 430; }
    @Override protected int panelHeight() { return 270; }

    @Override
    protected void addTabs(List<AbyssTab> tabs) {
        tabs.add(new ManagerTab());
    }

    @Nullable
    private OpenRegionScreenPacket.RegionEntry selected() {
        return regions.stream().filter(r -> r.name().equals(selectedName)).findFirst().orElse(null);
    }

    /** Load a region's saved state into the editing fields. */
    private void select(@Nullable OpenRegionScreenPacket.RegionEntry region) {
        selectedName = region == null ? null : region.name();
        activeTags.clear();
        if (region != null) activeTags.addAll(region.tags());
        filterTag = region == null || region.entryFilterTag() == null ? "" : region.entryFilterTag();
    }

    private class ManagerTab implements AbyssTab {

        @Override public Component title() { return Component.literal("Regions"); }

        @Override
        public void init(TabContext ctx) {
            var font = ctx.font();

            // ── Left: region list ──
            var list = ctx.add(new AbyssScrollList<OpenRegionScreenPacket.RegionEntry>(
                    ctx.x, ctx.y + 12, LIST_W, ctx.height - 12, ROW_H,
                    (g, r, x, y, w, h, hover, sel) -> {
                        String count = r.tags().isEmpty() ? "" : "[" + r.tags().size() + "]";
                        g.drawString(font, AbyssDraw.trimmed(font, Component.literal(r.name()), w - 24), x, y + 3,
                                sel ? AbyssTheme.TEXT_BRIGHT : AbyssTheme.TEXT, false);
                        g.drawString(font, count, x + w - 4 - font.width(count), y + 3, AbyssTheme.TEXT_DIM, false);
                    }));
            list.emptyText(Component.literal("No regions"));
            list.setItems(regions);
            list.setSelected(selected());
            list.onSelect(r -> { select(r); ctx.rebuild(); });

            var region = selected();
            if (region == null) return;   // right side shows "Select a region" (render)

            // ── Right: restriction toggles in two columns ──
            int dx = detailX(ctx), dw = detailW(ctx);
            int colW = dw / 2;
            int i = 0;
            for (var entry : TAGS.entrySet()) {
                String tag = entry.getKey();
                int x = dx + (i % 2) * colW;
                int y = ctx.y + 46 + (i / 2) * 22;
                ctx.add(new AbyssToggle(x, y, Component.literal(entry.getValue()), activeTags.contains(tag), on -> {
                    if (on) activeTags.add(tag); else activeTags.remove(tag);
                    if (tag.equals(ACBlockProtectionListener.NO_ENTRY_TAG)) ctx.rebuild();   // show/hide the filter field
                }));
                i++;
            }

            // ── "No Entry" filter tag, only when No Entry is on ──
            if (activeTags.contains(ACBlockProtectionListener.NO_ENTRY_TAG)) {
                var field = ctx.add(new AbyssTextField(dx, ctx.y + 168, dw,
                        Component.literal("Scoreboard tag allowed in (empty = nobody)")));
                field.setMaxLength(64);
                field.setValue(filterTag);
                field.setResponder(v -> filterTag = v);
            }

            // ── Buttons ──
            int by = ctx.y + ctx.height - AbyssButton.HEIGHT;
            ctx.add(new AbyssButton(dx, by, 80, Component.literal("Delete"), AbyssButton.Style.DANGER, b ->
                    ctx.open(AbyssDialog.confirmDanger(RegionManagerScreen.this,
                            Component.literal("Delete region?"),
                            Component.literal("\"" + region.name() + "\" and all its protections will be removed. This cannot be undone."),
                            () -> delete(region)))));
            ctx.add(new AbyssButton(dx + dw - 80, by, 80, Component.literal("Save"), b -> save(region)));
        }

        private int detailX(TabContext ctx) { return ctx.x + LIST_W + 12; }
        private int detailW(TabContext ctx) { return ctx.width - LIST_W - 12; }

        @Override
        public void render(GuiGraphics g, TabContext ctx, int mouseX, int mouseY, float partialTick) {
            var font = ctx.font();
            AbyssDraw.sectionTitle(g, font, Component.literal("REGIONS"), ctx.x, ctx.y);
            // divider between list and details
            g.fill(ctx.x + LIST_W + 5, ctx.y, ctx.x + LIST_W + 6, ctx.y + ctx.height, AbyssTheme.DIVIDER);

            int dx = detailX(ctx), dw = detailW(ctx);
            var r = selected();
            if (r == null) {
                g.drawCenteredString(font, Component.literal("\u2190  Select a region"),
                        dx + dw / 2, ctx.y + ctx.height / 2 - 4, AbyssTheme.TEXT_DIM);
                return;
            }
            AbyssDraw.sectionTitle(g, font, Component.literal(r.name()), dx, ctx.y);
            g.drawString(font, r.dimension(), dx, ctx.y + 12, AbyssTheme.TEXT_DIM, false);
            g.drawString(font, r.minX() + " " + r.minY() + " " + r.minZ() + "  \u2192  " + r.maxX() + " " + r.maxY() + " " + r.maxZ(),
                    dx, ctx.y + 22, AbyssTheme.TEXT, false);
            g.drawString(font, "RESTRICTIONS", dx, ctx.y + 36, AbyssTheme.TEXT_DIM, false);
            if (activeTags.contains(ACBlockProtectionListener.NO_ENTRY_TAG)) {
                g.drawString(font, "Entry filter tag", dx, ctx.y + 158, AbyssTheme.TEXT_DIM, false);
            }
        }
    }

    // ── Actions ─────────────────────────────────────────────────────────────────

    private void save(OpenRegionScreenPacket.RegionEntry old) {
        String filter = filterTag.trim();
        Set<String> tags = new LinkedHashSet<>(activeTags);
        AbyssNetwork.sendToServer(new SubmitRegionUpdatePacket(old.name(), false, tags, filter));
        // Update our copy so the list's [n] badge is right without reopening
        regions.set(regions.indexOf(old), new OpenRegionScreenPacket.RegionEntry(
                old.name(), old.dimension(), old.minX(), old.minY(), old.minZ(),
                old.maxX(), old.maxY(), old.maxZ(), tags, filter));
        rebuildCurrentTab();
    }

    private void delete(OpenRegionScreenPacket.RegionEntry region) {
        AbyssNetwork.sendToServer(new SubmitRegionUpdatePacket(region.name(), true, Set.of(), ""));
        regions.remove(region);
        select(null);
        // No rebuild needed: the dialog returns to this screen, and that re-runs init()
    }

    /** Rebuild via a fresh init (keeps selection & edits — they're fields). */
    private void rebuildCurrentTab() {
        this.init(this.minecraft, this.width, this.height);
    }
}
