package com.gugas749.abysscore.api.gui.dialog;

import com.gugas749.abysscore.api.gui.layout.AbyssLayout;
import com.gugas749.abysscore.api.gui.render.AbyssDraw;
import com.gugas749.abysscore.api.gui.theme.AbyssTheme;
import com.gugas749.abysscore.api.gui.widget.AbyssButton;
import com.gugas749.abysscore.api.gui.widget.AbyssTextField;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Pop-up dialogs drawn over the screen that opened them.
 *
 *   ctx.open(AbyssDialog.alert(screen, title, message));
 *   ctx.open(AbyssDialog.confirm(screen, title, message, () -> save()));
 *   ctx.open(AbyssDialog.confirmDanger(screen, title, message, () -> deleteRegion()));   // red button
 *   ctx.open(AbyssDialog.input(screen, title, message, "default", name -> !name.isBlank(), name -> rename(name)));
 *
 * Closing (button, Esc) goes back to the parent screen; the callback runs AFTER that, so a
 * callback may open yet another screen/dialog. Enter = confirm.
 */
public class AbyssDialog extends Screen {

    private enum Kind { ALERT, CONFIRM, INPUT }

    private static final int WIDTH = 220;
    private static final int PAD = 10;
    private static final int LINE_H = 10;
    private static final float DIALOG_Z = 300;

    private final Screen parent;
    private final Kind kind;
    private final Component message;
    private final boolean danger;
    @Nullable private final Runnable onConfirm;
    @Nullable private final Consumer<String> onSubmit;
    private final Predicate<String> validator;
    private String inputValue;

    private int boxX, boxY, boxH;
    private List<FormattedCharSequence> lines = List.of();
    private AbyssButton confirmButton;
    private AbyssTextField field;

    private AbyssDialog(Screen parent, Kind kind, Component title, Component message, boolean danger,
                        @Nullable Runnable onConfirm, @Nullable Consumer<String> onSubmit,
                        Predicate<String> validator, String initialInput) {
        super(title);
        this.parent = parent;
        this.kind = kind;
        this.message = message;
        this.danger = danger;
        this.onConfirm = onConfirm;
        this.onSubmit = onSubmit;
        this.validator = validator;
        this.inputValue = initialInput;
    }

    // ── Factories ──────────────────────────────────────────────────────────────

    public static AbyssDialog alert(Screen parent, Component title, Component message) {
        return new AbyssDialog(parent, Kind.ALERT, title, message, false, null, null, s -> true, "");
    }

    public static AbyssDialog confirm(Screen parent, Component title, Component message, Runnable onConfirm) {
        return new AbyssDialog(parent, Kind.CONFIRM, title, message, false, onConfirm, null, s -> true, "");
    }

    /** Same as confirm, with a red DANGER confirm button — for delete/reset/ban. */
    public static AbyssDialog confirmDanger(Screen parent, Component title, Component message, Runnable onConfirm) {
        return new AbyssDialog(parent, Kind.CONFIRM, title, message, true, onConfirm, null, s -> true, "");
    }

    /** @param validator the confirm button is only enabled while this returns true */
    public static AbyssDialog input(Screen parent, Component title, Component message, String initial,
                                    Predicate<String> validator, Consumer<String> onSubmit) {
        return new AbyssDialog(parent, Kind.INPUT, title, message, false, null, onSubmit, validator, initial);
    }

    // ── Setup ──────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        // Keep the parent laid out for the current window size (it's drawn behind us)
        parent.init(this.minecraft, this.width, this.height);

        lines = this.font.split(message, WIDTH - 2 * PAD);
        int inputH = kind == Kind.INPUT ? AbyssTextField.HEIGHT + 6 : 0;
        boxH = 22 + 6 + lines.size() * LINE_H + 8 + inputH + AbyssButton.HEIGHT + PAD;
        boxX = (this.width - WIDTH) / 2;
        boxY = (this.height - boxH) / 2;

        int contentY = boxY + 22 + 6 + lines.size() * LINE_H + 8;

        if (kind == Kind.INPUT) {
            field = addRenderableWidget(new AbyssTextField(boxX + PAD, contentY, WIDTH - 2 * PAD, Component.empty()));
            field.setMaxLength(128);
            field.setValue(inputValue);
            field.setResponder(value -> {
                inputValue = value;
                if (confirmButton != null) confirmButton.active = validator.test(value);
            });
            setInitialFocus(field);
            contentY += inputH;
        }

        // Buttons bottom-right: [Cancel] [Confirm]   (alert: just [OK])
        int buttonW = 70;
        var buttons = AbyssLayout.row(boxX + WIDTH - PAD - (kind == Kind.ALERT ? buttonW : 2 * buttonW + 4), contentY, 4);
        if (kind != Kind.ALERT) {
            buttons.add(addRenderableWidget(new AbyssButton(0, 0, buttonW,
                    Component.translatable("gui.abysscore.dialog.cancel"), b -> onClose())));
        }
        Component confirmText = Component.translatable(kind == Kind.ALERT ? "gui.abysscore.dialog.ok" : "gui.abysscore.dialog.confirm");
        confirmButton = buttons.add(addRenderableWidget(new AbyssButton(0, 0, buttonW, confirmText,
                danger ? AbyssButton.Style.DANGER : AbyssButton.Style.NORMAL, b -> confirm())));
        confirmButton.active = kind != Kind.INPUT || validator.test(inputValue);
    }

    private void confirm() {
        if (!confirmButton.active) return;
        this.minecraft.setScreen(parent);                      // back first...
        if (onConfirm != null) onConfirm.run();                // ...then the action
        if (onSubmit != null) onSubmit.accept(inputValue);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            confirm();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    // ── Rendering ──────────────────────────────────────────────────────────────

    /** Dark overlay + the dialog box — everything UNDER the buttons, inside the raised layer. */
    protected void renderDialog(GuiGraphics g) {
        g.fill(0, 0, this.width, this.height, AbyssTheme.DIM_OVERLAY);

        AbyssDraw.nineSlice(g, AbyssTheme.PANEL, AbyssTheme.PANEL_BORDER, boxX, boxY, WIDTH, boxH);
        AbyssDraw.nineSlice(g, AbyssTheme.HEADER, AbyssTheme.HEADER_BORDER, boxX + 3, boxY + 3, WIDTH - 6, 16);
        g.drawString(this.font, AbyssDraw.trimmed(this.font, this.title, WIDTH - 20), boxX + 9, boxY + 7,
                danger ? AbyssTheme.DANGER_TEXT : AbyssTheme.TITLE, true);

        int y = boxY + 22 + 6;
        for (FormattedCharSequence line : lines) {
            g.drawString(this.font, line, boxX + PAD, y, AbyssTheme.TEXT, false);
            y += LINE_H;
        }
    }

    // Parent at Z = 0, then the whole dialog (box + widgets) raised in front of it.
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        parent.render(g, -1, -1, partialTick);         // -1,-1: nothing in the parent looks hovered

        g.pose().pushPose();
        g.pose().translate(0, 0, DIALOG_Z);
        renderDialog(g);                               // our overlay + box (see renderBackground)
        super.render(g, mouseX, mouseY, partialTick);  // the widgets
        g.pose().popPose();
    }

    /**
     * No @Override: on 1.21.1 it replaces vanilla's blurred background (super.render() calls
     * it) with nothing, because render() already drew our box. On 1.20.1 nothing calls it.
     */
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}