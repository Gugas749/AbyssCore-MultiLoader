package com.gugas749.abysscore.api.gui.screen;

import com.gugas749.abysscore.api.gui.render.AbyssDraw;
import com.gugas749.abysscore.api.gui.theme.AbyssTheme;
import com.gugas749.abysscore.api.gui.widget.AbyssButton;
import com.gugas749.abysscore.api.gui.widget.AbyssIconButton;
import com.gugas749.abysscore.api.gui.widget.AbyssTabButton;
import com.gugas749.abysscore.api.permission.AbyssClientPermission;
import com.gugas749.abysscore.api.permission.AbyssPermissionLevel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class for Abyss staff screens: panel + header + sidebar tabs + content area.
 *
 *   public class RegionScreen extends AbyssPanelScreen {
 *       public RegionScreen() { super(Component.literal("Regions")); }
 *
 *       @Override protected void addTabs(List<AbyssTab> tabs) {
 *           tabs.add(new RegionListTab());
 *           tabs.add(AbyssTab.of(Component.literal("Settings"), ctx -> { ... }));
 *       }
 *   }
 *   // open it:  Minecraft.getInstance().setScreen(new RegionScreen());
 *
 *   ┌─ TITLE ─────────────────────── STAFF ✕ ┐
 *   │ [Tab 1] │                              │
 *   │  Tab 2  │   content area (TabContext)  │
 *   │  Tab 3  │                              │
 *   └─────────┴──────────────────────────────┘
 *
 * With only ONE tab there is no sidebar and the content uses the full width.
 *
 * PERMISSIONS: by default only MODERATOR and ADMIN can use these screens (override
 * requiredLevel()). This client-side check is for the user experience only — a modified
 * client can skip it — so the SERVER must still check the permission on every packet
 * the screen sends.
 */
public abstract class AbyssPanelScreen extends Screen {

    private static final int HEADER_H = 22;          // header bar incl. margins
    private static final int SIDEBAR_W = 84;
    private static final int TAB_SPACING = 20;       // 18 tall + 2 gap
    private static final int MARGIN = 8;

    /** Remembers the last tab per screen class while the game runs. */
    private static final Map<Class<?>, Integer> LAST_TAB = new HashMap<>();

    private final List<AbyssTab> tabs = new ArrayList<>();
    private final List<AbstractWidget> tabWidgets = new ArrayList<>();
    private int selectedTab;
    private boolean denied;
    private TabContext context;

    protected int panelX, panelY, panelW, panelH;

    /** Where Close / Esc goes back to. null = close the GUI completely. */
    @Nullable protected final Screen parent;

    protected AbyssPanelScreen(Component title) {
        this(title, null);
    }

    /** @param parent the screen to return to on Close/Esc (e.g. the main menu that opened this one) */
    protected AbyssPanelScreen(Component title, @Nullable Screen parent) {
        super(title);
        this.parent = parent;
    }

    // ── What subclasses define ─────────────────────────────────────────────────

    /** Add the tabs, in sidebar order. Called once, the first time the screen opens. */
    protected abstract void addTabs(List<AbyssTab> tabs);

    /** Preferred panel size; shrunk automatically on small windows. */
    protected int panelWidth()  { return 320; }
    protected int panelHeight() { return 200; }

    /** Who may use this screen. */
    protected AbyssPermissionLevel requiredLevel() { return AbyssPermissionLevel.MODERATOR; }

    // ── Setup ──────────────────────────────────────────────────────────────────

    /** Runs on open AND on every window resize — so all positions are computed here. */
    @Override
    protected void init() {
        panelW = Math.min(panelWidth(), this.width - 10);
        panelH = Math.min(panelHeight(), this.height - 10);
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;

        addRenderableWidget(new AbyssIconButton(panelX + panelW - 16, panelY + 6,
                AbyssTheme.ICON_CLOSE, AbyssTheme.ICON_CLOSE_HOVER,
                Component.translatable("gui.abysscore.close"), this::onClose));

        denied = !AbyssClientPermission.has(requiredLevel());
        if (denied) {
            addRenderableWidget(new AbyssButton(panelX + (panelW - 60) / 2, panelY + panelH / 2 + 8, 60,
                    Component.translatable("gui.abysscore.close"), b -> onClose()));
            return;
        }

        if (tabs.isEmpty()) {
            addTabs(tabs);
            selectedTab = Math.min(LAST_TAB.getOrDefault(getClass(), 0), Math.max(0, tabs.size() - 1));
        }

        if (hasSidebar()) {
            for (int i = 0; i < tabs.size(); i++) {
                int index = i;
                addRenderableWidget(new AbyssTabButton(panelX + 6, panelY + HEADER_H + 4 + i * TAB_SPACING,
                        SIDEBAR_W - 10, tabs.get(i).title(),
                        () -> selectedTab == index, () -> selectTab(index)));
            }
        }
        rebuildTab();
    }

    private boolean hasSidebar() {
        return tabs.size() > 1;
    }

    public void selectTab(int index) {
        selectedTab = index;
        LAST_TAB.put(getClass(), index);
        rebuildTab();
    }

    /** Throws away the current tab's widgets and lets the tab build them again. */
    void rebuildTab() {
        tabWidgets.forEach(this::removeWidget);
        tabWidgets.clear();
        if (tabs.isEmpty()) return;

        int contentX = panelX + (hasSidebar() ? SIDEBAR_W + MARGIN + 2 : MARGIN);
        int contentY = panelY + HEADER_H + 4;
        int contentW = panelX + panelW - MARGIN - contentX;
        int contentH = panelY + panelH - MARGIN - contentY;
        context = new TabContext(this, contentX, contentY, contentW, contentH);
        tabs.get(selectedTab).init(context);
    }

    <W extends AbstractWidget> W addTabWidget(W widget) {
        tabWidgets.add(widget);
        return addRenderableWidget(widget);
    }

    @Override
    public void tick() {
        super.tick();
        if (!denied && context != null) tabs.get(selectedTab).tick(context);
    }

    // ── Rendering ──────────────────────────────────────────────────────────────

    /** Everything UNDER the widgets: dim, panel, stars, header, sidebar, the tab's own drawing. */
    protected void renderPanel(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, 0x90000000);   // dim the world (no blur, on both versions)

        AbyssDraw.nineSlice(g, AbyssTheme.PANEL, AbyssTheme.PANEL_BORDER, panelX, panelY, panelW, panelH);
        AbyssDraw.stars(g, panelX + 6, panelY + HEADER_H, panelW - 12, panelH - HEADER_H - 6, (long) panelW * 31 + panelH);

        // header bar: title left, STAFF badge right (before the close icon)
        AbyssDraw.nineSlice(g, AbyssTheme.HEADER, AbyssTheme.HEADER_BORDER, panelX + 3, panelY + 3, panelW - 6, 16);
        g.drawString(this.font, AbyssDraw.trimmed(this.font, this.title, panelW - 90), panelX + 9, panelY + 7, AbyssTheme.TITLE, true);
        Component badge = Component.translatable("gui.abysscore.staff");
        g.drawString(this.font, badge, panelX + panelW - 22 - this.font.width(badge), panelY + 7, AbyssTheme.BADGE, true);

        if (denied) {
            g.drawCenteredString(this.font, Component.translatable("gui.abysscore.no_permission"),
                    panelX + panelW / 2, panelY + panelH / 2 - 10, AbyssTheme.DANGER_TEXT);
            return;
        }

        if (hasSidebar()) {   // vertical divider between sidebar and content
            g.fill(panelX + SIDEBAR_W + 3, panelY + HEADER_H, panelX + SIDEBAR_W + 4, panelY + panelH - 6, AbyssTheme.DIVIDER);
        }
        if (context != null) tabs.get(selectedTab).render(g, context, mouseX, mouseY, partialTick);
    }

    /*
     * Our panel IS the background. We draw it ourselves in render(), then super.render()
     * draws the widgets on top. Same result on both versions:
     *   - 1.20.1: Screen.render() only draws the widgets.
     *   - 1.21.1: Screen.render() first calls renderBackground(4 args), whose vanilla version
     *     BLURS the world (and 1.21 allows only one blur per frame — a dialog drawing this
     *     panel behind it would crash). Our empty renderBackground below replaces it.
     */
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderPanel(g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);
    }

    /** No @Override: overrides vanilla's on 1.21.1; on 1.20.1 nothing calls it. See render(). */
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    }

    /** Close / Esc: back to the parent screen if there is one, otherwise out of the GUI. */
    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
