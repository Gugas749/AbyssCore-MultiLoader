package com.gugas749.abysscore.client.ui.screens;

import com.gugas749.abysscore.api.gui.layout.AbyssLayout;
import com.gugas749.abysscore.api.gui.screen.AbyssPanelScreen;
import com.gugas749.abysscore.api.gui.screen.AbyssTab;
import com.gugas749.abysscore.api.gui.screen.TabContext;
import com.gugas749.abysscore.api.gui.theme.AbyssTheme;
import com.gugas749.abysscore.api.gui.widget.AbyssButton;
import com.gugas749.abysscore.api.gui.widget.AbyssTextField;
import com.gugas749.abysscore.network.AbyssNetwork;
import com.gugas749.abysscore.network.dimen.SubmitDimenCreatePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

/**
 * Create a new dimension. Opened from the menu's Dimensions tab (or by the server).
 * One tab → no sidebar, the form uses the whole panel.
 */
public class DimenCreateScreen extends AbyssPanelScreen {

    private static final String[] STYLES = {"NORMAL", "SUPERFLAT", "VOID"};

    // Form state — fields, so resizing the window keeps what was typed
    private String name = "", displayName = "", seed = "0";
    private int style = 0;

    public DimenCreateScreen(@Nullable Screen parent) {
        super(Component.literal("\u2318  CREATE DIMENSION"), parent);
    }

    /** Used when the server opens this screen directly. */
    public DimenCreateScreen() { this(null); }

    @Override protected int panelWidth()  { return 270; }
    @Override protected int panelHeight() { return 210; }

    @Override
    protected void addTabs(List<AbyssTab> tabs) {
        tabs.add(new FormTab());
    }

    private class FormTab implements AbyssTab {
        private AbyssButton createButton;

        @Override public Component title() { return Component.literal("Create"); }

        @Override
        public void init(TabContext ctx) {
            int w = ctx.width;

            var nameField = ctx.add(new AbyssTextField(ctx.x, ctx.y + 10, w, Component.literal("e.g. my_world")));
            nameField.setMaxLength(64);
            nameField.setValue(name);
            nameField.setResponder(v -> { name = v; updateCreate(); });

            var displayField = ctx.add(new AbyssTextField(ctx.x, ctx.y + 42, w, Component.literal("e.g. My World")));
            displayField.setMaxLength(64);
            displayField.setValue(displayName);
            displayField.setResponder(v -> displayName = v);

            // Style: one button that cycles NORMAL → SUPERFLAT → VOID
            ctx.add(new AbyssButton(ctx.x, ctx.y + 74, w, styleLabel(), b -> {
                style = (style + 1) % STYLES.length;
                ctx.rebuild();   // the seed row only exists for NORMAL
            }));

            if (STYLES[style].equals("NORMAL")) {
                int half = (w - 4) / 2;
                var seedField = ctx.add(new AbyssTextField(ctx.x, ctx.y + 110, half, Component.literal("0 = random")));
                seedField.setMaxLength(20);
                seedField.setFilter(s -> s.matches("-?\\d*"));   // a (possibly negative) whole number
                seedField.setValue(seed);
                seedField.setResponder(v -> seed = v);
                ctx.add(new AbyssButton(ctx.x + half + 4, ctx.y + 110, half, Component.literal("\u21BA Random"), b -> {
                    seed = String.valueOf(new Random().nextLong());
                    ctx.rebuild();
                }));
            }

            var row = AbyssLayout.row(ctx.x, ctx.y + ctx.height - AbyssButton.HEIGHT, 4);
            createButton = row.add(ctx.add(new AbyssButton(0, 0, 90, Component.literal("Create"), b -> create())));
            row.add(ctx.add(new AbyssButton(0, 0, 90, Component.literal("Cancel"), b -> onClose())));
            updateCreate();
        }

        private Component styleLabel() {
            String s = STYLES[style];
            return Component.literal("Style: " + s.charAt(0) + s.substring(1).toLowerCase());
        }

        private void updateCreate() {
            if (createButton != null) createButton.active = !sanitized().isEmpty();
        }

        @Override
        public void render(GuiGraphics g, TabContext ctx, int mouseX, int mouseY, float partialTick) {
            var font = ctx.font();
            g.drawString(font, "Internal name", ctx.x, ctx.y + 1, AbyssTheme.TEXT_DIM, false);
            // Show what the id will really be (lowercase, a–z 0–9 _)
            String id = sanitized();
            if (!id.isEmpty() && !id.equals(name)) {
                String preview = "\u2192 " + id;
                g.drawString(font, preview, ctx.x + ctx.width - font.width(preview), ctx.y + 1, AbyssTheme.ACCENT, false);
            }
            g.drawString(font, "Display name", ctx.x, ctx.y + 33, AbyssTheme.TEXT_DIM, false);
            if (STYLES[style].equals("NORMAL")) {
                g.drawString(font, "Seed", ctx.x, ctx.y + 101, AbyssTheme.TEXT_DIM, false);
            }
        }
    }

    /** Dimension ids may only contain a–z, 0–9 and _ . */
    private String sanitized() {
        return name.trim().toLowerCase().replaceAll("[^a-z0-9_]", "_");
    }

    private void create() {
        long seedValue = 0;
        try { seedValue = Long.parseLong(seed.trim()); } catch (NumberFormatException ignored) {}
        AbyssNetwork.sendToServer(new SubmitDimenCreatePacket(sanitized(), displayName.trim(), STYLES[style], seedValue));
        onClose();
    }
}
