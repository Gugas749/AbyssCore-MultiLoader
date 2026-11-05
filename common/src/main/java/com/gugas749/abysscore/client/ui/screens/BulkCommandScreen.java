package com.gugas749.abysscore.client.ui.screens;

import com.gugas749.abysscore.api.gui.layout.AbyssLayout;
import com.gugas749.abysscore.api.gui.screen.AbyssPanelScreen;
import com.gugas749.abysscore.api.gui.screen.AbyssTab;
import com.gugas749.abysscore.api.gui.screen.TabContext;
import com.gugas749.abysscore.api.gui.theme.AbyssTheme;
import com.gugas749.abysscore.api.gui.widget.AbyssButton;
import com.gugas749.abysscore.api.gui.widget.AbyssTextField;
import com.gugas749.abysscore.network.AbyssNetwork;
import com.gugas749.abysscore.network.bulk.SubmitBulkCommandPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Create a bulk command: a name, who may run it, and up to 10 commands.
 *
 * All state lives in fields (name, permission, the command texts, the scroll position),
 * so adding/removing/scrolling rows just rebuilds the widgets from that state. The old
 * version had to copy every EditBox value out and back in on each rebuild.
 */
public class BulkCommandScreen extends AbyssPanelScreen {

    private static final int MAX_COMMANDS = 10;
    private static final int VISIBLE_ROWS = 4;
    private static final int ROW = 24;
    private static final int LIST_Y = 72;   // relative to the content area

    private String name = "";
    private int permLevel = 0;              // 0 = everyone, 2 = OP only (what the server expects)
    private final List<String> commands = new ArrayList<>(List.of(""));
    private int scroll = 0;

    public BulkCommandScreen(@Nullable Screen parent) {
        super(Component.literal("\u2630  BULK COMMAND"), parent);
    }

    public BulkCommandScreen() { this(null); }

    @Override protected int panelWidth()  { return 320; }
    @Override protected int panelHeight() { return 250; }

    @Override
    protected void addTabs(List<AbyssTab> tabs) {
        tabs.add(new FormTab());
    }

    private class FormTab implements AbyssTab {
        private AbyssButton saveButton;

        @Override public Component title() { return Component.literal("Bulk"); }

        @Override
        public void init(TabContext ctx) {
            int w = ctx.width;

            var nameField = ctx.add(new AbyssTextField(ctx.x, ctx.y + 10, w, Component.literal("Bulk command name...")));
            nameField.setMaxLength(64);
            nameField.setValue(name);
            nameField.setResponder(v -> { name = v; updateSave(); });

            ctx.add(new AbyssButton(ctx.x, ctx.y + 38, w, Component.literal(permLevel == 0
                    ? "Permission: Everyone" : "Permission: OP only"), b -> {
                permLevel = permLevel == 0 ? 2 : 0;
                ctx.rebuild();
            }));

            // Command rows: [text field............][×]   + ▲▼ on the right
            scroll = Math.max(0, Math.min(scroll, commands.size() - VISIBLE_ROWS));
            int fieldW = w - 20 - 4 - 20 - 4;
            for (int row = 0; row < VISIBLE_ROWS && scroll + row < commands.size(); row++) {
                int index = scroll + row;
                int y = ctx.y + LIST_Y + row * ROW;
                var field = ctx.add(new AbyssTextField(ctx.x, y, fieldW, Component.literal("/command " + (index + 1))));
                field.setMaxLength(256);
                field.setValue(commands.get(index));
                field.setResponder(v -> { commands.set(index, v); updateSave(); });

                AbyssButton remove = ctx.add(new AbyssButton(ctx.x + fieldW + 4, y, 20, Component.literal("\u00d7"),
                        AbyssButton.Style.DANGER, b -> {
                            commands.remove(index);
                            ctx.rebuild();
                        }));
                remove.active = commands.size() > 1;   // always keep at least one row
            }

            if (commands.size() > VISIBLE_ROWS) {
                int arrowsX = ctx.x + w - 20;
                AbyssButton up = ctx.add(new AbyssButton(arrowsX, ctx.y + LIST_Y, 20, Component.literal("\u25B2"), b -> {
                    scroll--; ctx.rebuild();
                }));
                AbyssButton down = ctx.add(new AbyssButton(arrowsX, ctx.y + LIST_Y + (VISIBLE_ROWS - 1) * ROW, 20,
                        Component.literal("\u25BC"), b -> { scroll++; ctx.rebuild(); }));
                up.active = scroll > 0;
                down.active = scroll < commands.size() - VISIBLE_ROWS;
            }

            var bottom = AbyssLayout.row(ctx.x, ctx.y + ctx.height - AbyssButton.HEIGHT, 4);
            AbyssButton add = bottom.add(ctx.add(new AbyssButton(0, 0, 70, Component.literal("+ Add"), b -> {
                commands.add("");
                scroll = Math.max(0, commands.size() - VISIBLE_ROWS);   // jump to the new row
                ctx.rebuild();
            })));
            add.active = commands.size() < MAX_COMMANDS;
            saveButton = bottom.add(ctx.add(new AbyssButton(0, 0, 80, Component.literal("Save"), b -> save())));
            bottom.add(ctx.add(new AbyssButton(0, 0, 80, Component.literal("Cancel"), b -> onClose())));
            updateSave();
        }

        /** Needs a name and at least one non-empty command. */
        private void updateSave() {
            if (saveButton != null) {
                saveButton.active = !name.isBlank() && commands.stream().anyMatch(c -> !c.isBlank());
            }
        }

        @Override
        public void render(GuiGraphics g, TabContext ctx, int mouseX, int mouseY, float partialTick) {
            var font = ctx.font();
            g.drawString(font, "Name", ctx.x, ctx.y + 1, AbyssTheme.TEXT_DIM, false);
            g.drawString(font, "Commands", ctx.x, ctx.y + LIST_Y - 11, AbyssTheme.TEXT_DIM, false);
            String count = commands.size() + "/" + MAX_COMMANDS;
            g.drawString(font, count, ctx.x + ctx.width - font.width(count), ctx.y + LIST_Y - 11,
                    commands.size() >= MAX_COMMANDS ? AbyssTheme.DANGER_TEXT : AbyssTheme.TEXT_DIM, false);
        }
    }

    private void save() {
        List<String> cmds = commands.stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
        AbyssNetwork.sendToServer(new SubmitBulkCommandPacket(name.trim(), permLevel, new ArrayList<>(cmds)));
        onClose();
    }
}
