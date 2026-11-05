package com.gugas749.abysscore.client;

import com.gugas749.abysscore.Constants;
import com.gugas749.abysscore.api.corruption.AbyssCorruption;
import com.gugas749.abysscore.api.corruption.CorruptionData;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * CLIENT. ABY-16 — corruption bar, left of the hotbar. Hidden while the player has no
 * corruption at all. /corruptionhud toggles it (a CLIENT command: works on any server).
 *
 * Moved to common (was NeoForge-only): everything here exists in 1.20.1 and 1.21.1.
 * The loaders call render() from their RenderGuiEvent.Post listener and registerCommand()
 * through ClientCommands.
 */
public final class CorruptionHudOverlay {

    private static boolean showBar = true;

    private static final ResourceLocation TEXTURE = Constants.asResource("textures/gui/corruption_bar.png");

    // The texture is 1176x212: the empty bar on top (106 px), the full bar below it
    private static final int BAR_WIDTH  = 1176;
    private static final int BAR_HEIGHT = 106;
    private static final int TEX_WIDTH  = 1176;
    private static final int TEX_HEIGHT = 212;
    private static final float SCALE    = 0.15f;

    private CorruptionHudOverlay() {}

    public static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("corruptionhud").executes(ctx -> {
            showBar = !showBar;
            ctx.getSource().sendSuccess(() -> Component.translatable(
                    "command.abysscore.corruption.hud", showBar ? "visible" : "hidden"), false);
            return 1;
        }));
    }

    public static void render(GuiGraphics gfx) {
        if (!showBar) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        CorruptionData data = AbyssCorruption.data(mc.player);   // client copy, synced by the server
        if (data.level() == 0 && data.progress() == 0) return;
        int corruption = data.level();

        int scaledWidth  = (int) (BAR_WIDTH  * SCALE);
        int scaledHeight = (int) (BAR_HEIGHT * SCALE);
        int hotbarLeft = (gfx.guiWidth() - 182) / 2;   // the hotbar is 182 px wide, centred
        int barX = hotbarLeft - scaledWidth - 3;
        int barY = gfx.guiHeight() - 17;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        gfx.pose().pushPose();
        gfx.pose().translate(barX, barY, 0);
        gfx.pose().scale(SCALE, SCALE, 1.0f);
        gfx.blit(TEXTURE, 0, 0, 0, 0, BAR_WIDTH, BAR_HEIGHT, TEX_WIDTH, TEX_HEIGHT);           // empty bar
        int fillWidth = (int) Math.ceil((corruption / (double) AbyssCorruption.MAX) * BAR_WIDTH);
        if (fillWidth > 0) {
            gfx.blit(TEXTURE, 0, 0, 0, BAR_HEIGHT, fillWidth, BAR_HEIGHT, TEX_WIDTH, TEX_HEIGHT); // filled part
        }
        gfx.pose().popPose();

        String text = corruption + "%";
        int textX = barX + scaledWidth / 2 - mc.font.width(text) / 2;
        int textY = barY + scaledHeight / 2 - 4;
        gfx.drawString(mc.font, text, textX + 1, textY + 1, 0x44000000, false);   // shadow
        gfx.drawString(mc.font, text, textX, textY, textColor(corruption), false);

        RenderSystem.disableBlend();
    }

    /** white -> light purple -> purple -> dark purple -> dark red at 100%. */
    private static int textColor(int corruption) {
        if (corruption >= 100) return 0xFF4B0000;
        if (corruption >= 75)  return 0xFF6A006A;
        if (corruption >= 50)  return 0xFFAA00AA;
        if (corruption >= 25)  return 0xFFCC88FF;
        return 0xFFFFFFFF;
    }
}
