package com.gugas749.abysscore.network;

import com.gugas749.abysscore.Constants;
import com.gugas749.abysscore.api.attachment.SyncAttachmentClientHandler;
import com.gugas749.abysscore.api.attachment.SyncAttachmentPacket;
import com.gugas749.abysscore.api.network.AbyssChannel;
import com.gugas749.abysscore.api.network.AbyssNetworking;
import com.gugas749.abysscore.api.network.AbyssPacketRegistrar;
import com.gugas749.abysscore.api.overlay.AbyssOverlayRenderer;
import com.gugas749.abysscore.api.overlay.ScreenOverlayPacket;
import com.gugas749.abysscore.api.permission.AbyssClientPermission;
import com.gugas749.abysscore.api.permission.PermissionSyncPacket;
import com.gugas749.abysscore.client.ACVanishHudHandler;
import com.gugas749.abysscore.client.ui.screens.BlindScreen;
import com.gugas749.abysscore.features.regions.ACNoEntryListener;
import com.gugas749.abysscore.network.binds.KeyPressHandler;
import com.gugas749.abysscore.network.binds.KeyPressPacket;
import com.gugas749.abysscore.network.blind.BlindSyncPacket;
import com.gugas749.abysscore.network.bulk.SubmitBulkCommandHandler;
import com.gugas749.abysscore.network.bulk.SubmitBulkCommandPacket;
import com.gugas749.abysscore.network.dimen.DimenPacketHandlers;
import com.gugas749.abysscore.network.dimen.OpenDimenCreateScreenPacket;
import com.gugas749.abysscore.network.dimen.SubmitDimenCreatePacket;
import com.gugas749.abysscore.network.menu.MenuPacketHandlers;
import com.gugas749.abysscore.network.menu.packets.MenuActionPacket;
import com.gugas749.abysscore.network.menu.packets.OpenMainMenuPacket;
import com.gugas749.abysscore.network.menu.packets.RequestRegionScreenPacket;
import com.gugas749.abysscore.network.menu.packets.SaveTitlePacket;
import com.gugas749.abysscore.network.region.*;
import com.gugas749.abysscore.network.vanish.VanishStateSyncPacket;

/**
 * Every AbyssCore packet, for BOTH loaders (moved here from the neoforge module).
 *
 * Call init() from each loader's mod constructor: it loads this class, which creates the
 * channel, which runs register() below.
 *
 * Version "5": the protocol changed with the multi-loader networking (bump on every change).
 * ORDER MATTERS on Forge (packets are numbered in this order): add new packets at the END.
 */
public class PacketHandler {

    /** Client and server must have the same version. Bump it whenever a packet changes. */
    public static final String PROTOCOL_VERSION = "6";   // 6: sync_attachment moved into this channel

    public static final AbyssChannel CHANNEL =
            AbyssNetworking.createChannel(Constants.MOD_ID, PROTOCOL_VERSION, PacketHandler::register);

    /** Does nothing by itself: calling it makes Java load the class → CHANNEL gets created. */
    public static void init() {}

    private static void register(AbyssPacketRegistrar r) {

        // ── S2C ──────────────────────────────────────────────────────────────
        r.s2c("vanish_state", VanishStateSyncPacket.class, VanishStateSyncPacket.CODEC,
                () -> ACVanishHudHandler::handleSync);
        r.s2c("no_entry", NoEntryPacket.class, NoEntryPacket.CODEC,
                () -> NoEntryHandler::handlePacket);
        r.s2c("blind_sync", BlindSyncPacket.class, BlindSyncPacket.CODEC,
                () -> BlindScreen::handleSync);
        r.s2c("open_dimen_create", OpenDimenCreateScreenPacket.class, OpenDimenCreateScreenPacket.CODEC,
                () -> ClientPacketHandler::handleOpenDimenCreate);
        r.s2c("open_region_screen", OpenRegionScreenPacket.class, OpenRegionScreenPacket.CODEC,
                () -> ClientPacketHandler::handleOpenRegionScreen);
        r.s2c("open_main_menu", OpenMainMenuPacket.class, OpenMainMenuPacket.CODEC,
                () -> ClientPacketHandler::handleOpenMainMenu);

        // ── C2S ──────────────────────────────────────────────────────────────
        r.c2s("key_press", KeyPressPacket.class, KeyPressPacket.CODEC, KeyPressHandler::handle);
        r.c2s("submit_bulk_command", SubmitBulkCommandPacket.class, SubmitBulkCommandPacket.CODEC,
                SubmitBulkCommandHandler::handle);
        r.c2s("submit_dimen_create", SubmitDimenCreatePacket.class, SubmitDimenCreatePacket.CODEC,
                DimenPacketHandlers::handleCreate);
        r.c2s("submit_region_update", SubmitRegionUpdatePacket.class, SubmitRegionUpdatePacket.CODEC,
                RegionScreenPacketHandlers::handleRegionUpdate);
        r.c2s("ready_to_tp", ReadyToTeleportPacket.class, ReadyToTeleportPacket.CODEC,
                ACNoEntryListener::handleReadyToTeleport);
        r.c2s("menu_action", MenuActionPacket.class, MenuActionPacket.CODEC, MenuPacketHandlers::handleAction);
        r.c2s("save_title", SaveTitlePacket.class, SaveTitlePacket.CODEC, MenuPacketHandlers::handleSaveTitle);
        r.c2s("request_region_screen", RequestRegionScreenPacket.class, RequestRegionScreenPacket.CODEC,
                RegionScreenPacketHandlers::handleRequest);

        // ── GUI API ──────────────────────────────────────────────────────────
        r.s2c("permission_sync", PermissionSyncPacket.class, PermissionSyncPacket.CODEC,
                () -> AbyssClientPermission::handle);

        // ── Screen overlays (ABY-15) ─────────────────────────────────────────
        r.s2c("screen_overlay", ScreenOverlayPacket.class, ScreenOverlayPacket.CODEC,
                () -> AbyssOverlayRenderer::handle);

        // ── Synced attachments (one API for both loaders now) ────────────────
        r.s2c("sync_attachment", SyncAttachmentPacket.class, SyncAttachmentPacket.CODEC,
                () -> SyncAttachmentClientHandler::handle);
    }
}
