package com.gugas749.abysscore;

import com.gugas749.abysscore.api.effects.AbyssEffectHandler;
import com.gugas749.abysscore.api.permission.AbyssPermissionHandler;
import com.gugas749.abysscore.api.permission.AbyssPermissionSync;
import com.gugas749.abysscore.commands.subRegisters.ACGodCommands;
import com.gugas749.abysscore.features.blind.ACBlindManager;
import com.gugas749.abysscore.features.chat.ACChatLockListener;
import com.gugas749.abysscore.features.regions.ACBlockProtectionListener;
import com.gugas749.abysscore.features.regions.ACKeepInventoryListener;
import com.gugas749.abysscore.features.regions.ACNoEntryListener;
import com.gugas749.abysscore.features.vanish.ACVanishExtras;
import com.gugas749.abysscore.features.vanish.ACVanishStateListener;
import com.gugas749.abysscore.api.attachment.AbyssSyncedAttachment;
import com.gugas749.abysscore.api.overlay.AbyssScreenOverlay;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public class NeoForgeEventListeners {

    private final ACGodCommands godCommands              = new ACGodCommands();
    private final ACBlockProtectionListener blockProtect = new ACBlockProtectionListener();
    private final ACVanishStateListener vanishState      = new ACVanishStateListener();
    private final ACNoEntryListener noEntry              = new ACNoEntryListener();
    private final ACKeepInventoryListener keepInventory  = new ACKeepInventoryListener();
    private final ACChatLockListener chatLock            = new ACChatLockListener();
    private final AbyssEffectHandler effectHandler       = new AbyssEffectHandler();

    // ── Player tick ──────────────────────────────────────────────────────────
    // NeoForge 21.x splits tick into Pre/Post classes instead of a phase field.
    // PlayerTickEvent.Post fires once per tick after vanilla logic completes.

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        if (!(p instanceof ServerPlayer player)) return;

        godCommands.onPlayerTick(player);
        blockProtect.onPlayerTick(player);
        vanishState.onPlayerTick(player);
        noEntry.onPlayerTick(player);
    }

    // ── Server tick ──────────────────────────────────────────────────────────

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        AbyssPermissionSync.onServerTick(event.getServer());
    }

    // ── Entity tick ──────────────────────────────────────────────────────────

    @SubscribeEvent
    public void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof LivingEntity entity) {   // fires for items, arrows... too
            effectHandler.onEntityTick(entity);
        }
    }

    // Continuous effects of a mob that was killed / discarded are forgotten (AbyssEffectHandler)
    @SubscribeEvent
    public void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        effectHandler.onEntityLeaveLevel(event.getEntity());
    }

    // ── God mode — cancel incoming damage ────────────────────────────────────

    @SubscribeEvent
    public void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (godCommands.onIncomingDamage(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    // ── Block protection ─────────────────────────────────────────────────────

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (blockProtect.checkBlockChange(player, level, event.getPos())) {
            event.setCanceled(true);
        }
    }

    // no_build also blocks PLACING (before only breaking was blocked)
    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (blockProtect.checkBlockChange(player, level, event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (blockProtect.checkRightClickBlock(player, level, event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (blockProtect.checkEntityInteract(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getTarget() instanceof Player)) return;
        if (blockProtect.checkAttackPlayer(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onEntityTeleport(EntityTeleportEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (blockProtect.checkTeleport(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (blockProtect.checkRightClickItem(player, event.getItemStack())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onMobSpawn(FinalizeSpawnEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        BlockPos pos = event.getEntity().blockPosition();
        MobCategory category = event.getEntity().getType().getCategory();
        if (blockProtect.checkMobSpawn(level, pos, category)) {
            event.setSpawnCancelled(true);
        }
    }

    // ── Screen overlays bound to a damage type (AbyssScreenOverlay.bindDamageType) ──
    // Post = after armor / resistance, only fires if damage actually went through.
    @SubscribeEvent
    public void onDamage(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AbyssScreenOverlay.onDamage(player, event.getSource(), event.getNewDamage());
        }
    }

    // ── Keep inventory ───────────────────────────────────────────────────────

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerDeath(LivingDeathEvent event) {
        if (event.isCanceled()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        keepInventory.onDeath(player);
    }

    @SubscribeEvent
    public void onExperienceDrop(LivingExperienceDropEvent event) {
        if (keepInventory.onExperienceDrop(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer newPlayer)) return;
        if (!(event.getOriginal() instanceof ServerPlayer original)) return;
        keepInventory.onClone(newPlayer, original, event.isWasDeath());
        AbyssSyncedAttachment.onPlayerClone(original, newPlayer);   // corruption etc. survive death
    }

    // ── Chat lock ────────────────────────────────────────────────────────────

    @SubscribeEvent
    public void onServerChat(ServerChatEvent event) {
        if (chatLock.onChat(event.getPlayer()) || vanishState.onChat(event.getPlayer())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onCommand(CommandEvent event) {
        var source = event.getParseResults().getContext().getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) return;
        String command = event.getParseResults().getReader().getString();
        if (chatLock.onCommand(player, command)) {
            event.setCanceled(true);
        }
    }

    // ── Player login / logout ────────────────────────────────────────────────

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ACBlindManager.onPlayerJoin(player);
        AbyssPermissionHandler.onPlayerJoin(player);
        AbyssPermissionSync.onLogin(player);
        vanishState.onPlayerJoin(player);
        AbyssSyncedAttachment.onLogin(player);
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) AbyssSyncedAttachment.onRespawn(player);
    }

    @SubscribeEvent
    public void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) AbyssSyncedAttachment.onChangeDimension(player);
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        effectHandler.onPlayerLeave(player);
        AbyssPermissionSync.onLogout(player);
        ACVanishExtras.onPlayerLeave(player.getUUID());
        ACGodCommands.onPlayerLeave(player.getUUID());
        ACNoEntryListener.onPlayerLeave(player.getUUID());
        if (player instanceof ServerPlayer sp) {
            vanishState.onPlayerLeave(sp);
        }
    }
}
