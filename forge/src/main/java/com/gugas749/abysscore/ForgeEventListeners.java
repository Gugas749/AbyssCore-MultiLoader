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
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Bridges Forge 1.20.1 events to the common listener classes.
 *
 * Why this class exists:
 * Common listener classes (ACGodCommands, ACBlockProtectionListener, etc.) contain
 * plain Java methods with no loader-specific annotations. They cannot be registered
 * directly with MinecraftForge.EVENT_BUS because Forge only calls @SubscribeEvent
 * methods. This class holds instances of every common listener, subscribes to Forge
 * events via @SubscribeEvent, and delegates to those plain methods.
 *
 * Registered in Abysscore.java with:
 *   MinecraftForge.EVENT_BUS.register(new ForgeEventListeners());
 */
public class ForgeEventListeners {

    // One instance of each common listener — these hold their own state
    private final ACGodCommands godCommands              = new ACGodCommands();
    private final ACBlockProtectionListener blockProtect = new ACBlockProtectionListener();
    private final ACVanishStateListener vanishState      = new ACVanishStateListener();
    private final ACNoEntryListener noEntry              = new ACNoEntryListener();
    private final ACKeepInventoryListener keepInventory  = new ACKeepInventoryListener();
    private final ACChatLockListener chatLock            = new ACChatLockListener();
    private final AbyssEffectHandler effectHandler       = new AbyssEffectHandler();

    // ── Player tick ──────────────────────────────────────────────────────────
    //
    // Forge fires PlayerTickEvent twice per tick: once at Phase.START and once at
    // Phase.END. We guard against the START phase so logic runs once per tick.
    // FakePlayer guard prevents other mods' fake players from triggering our logic.

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player instanceof FakePlayer) return;

        godCommands.onPlayerTick(player);
        blockProtect.onPlayerTick(player);
        vanishState.onPlayerTick(player);
        noEntry.onPlayerTick(player);
    }

    // ── Server tick ──────────────────────────────────────────────────────────
    //
    // event.server is the MinecraftServer — AbyssPermissionSync uses it to iterate
    // online players and re-check their permission level every 5 seconds.

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        AbyssPermissionSync.onServerTick(event.getServer());
    }

    // ── Entity tick ──────────────────────────────────────────────────────────
    //
    // Fires once per tick for every living entity. AbyssEffectHandler refreshes
    // any registered continuous potion effects before they expire.
    // NOTE: In Forge 1.20.1 this event may also be named LivingUpdateEvent —
    // if this import fails, try: net.minecraftforge.event.entity.living.LivingUpdateEvent

    @SubscribeEvent
    public void onLivingTick(LivingEvent.LivingTickEvent event) {
        effectHandler.onEntityTick(event.getEntity());
    }

    // Continuous effects of a mob that was killed / discarded are forgotten (AbyssEffectHandler)
    @SubscribeEvent
    public void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        effectHandler.onEntityLeaveLevel(event.getEntity());
    }

    // ── God mode ─────────────────────────────────────────────────────────────
    //
    // LivingAttackEvent fires before damage is applied. We cancel it entirely
    // for players in god mode so they take zero damage from any source.

    @SubscribeEvent
    public void onLivingAttack(LivingAttackEvent event) {
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

    // Only cancel PvP — the no_friendlyfire tag only applies to player-vs-player combat.
    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getTarget() instanceof Player)) return;
        if (blockProtect.checkAttackPlayer(player)) {
            event.setCanceled(true);
        }
    }

    // Catches ender pearls, chorus fruit, and other teleport sources.
    @SubscribeEvent
    public void onEntityTeleport(EntityTeleportEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (blockProtect.checkTeleport(player)) {
            event.setCanceled(true);
        }
    }

    // Blocks waystone item uses (warp_stone, warp_scroll, etc.) inside no_tp regions.
    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (blockProtect.checkRightClickItem(player, event.getItemStack())) {
            event.setCanceled(true);
        }
    }

    // Blocks hostile and passive mob spawning inside no_mobspawning_* regions.
    // LivingSpawnEvent.CheckSpawn no longer exists in 1.20.1: MobSpawnEvent.FinalizeSpawn
    // replaced it (same event NeoForge calls FinalizeSpawnEvent), cancelled with setSpawnCancelled.
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onMobSpawn(MobSpawnEvent.FinalizeSpawn event) {
        // getLevel() is a ServerLevelAccessor: during world generation it's not a ServerLevel
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        BlockPos pos = BlockPos.containing(event.getX(), event.getY(), event.getZ());
        MobCategory category = event.getEntity().getType().getCategory();
        if (blockProtect.checkMobSpawn(level, pos, category)) {
            event.setSpawnCancelled(true);
        }
    }

    // ── Screen overlays bound to a damage type (AbyssScreenOverlay.bindDamageType) ──
    // Forge 1.20.1's LivingDamageEvent fires after armor / resistance (it has no Post version).
    // LOWEST priority so other mods have changed / cancelled it already; canceled events
    // don't reach this listener at all.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDamage(LivingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AbyssScreenOverlay.onDamage(player, event.getSource(), event.getAmount());
        }
    }

    // ── Keep inventory ───────────────────────────────────────────────────────
    //
    // LOWEST priority so that mods like PlayerRevive that cancel death run first.
    // If another mod already cancelled death, we skip — the player didn't die.
    // onDeath saves the inventory to a pending map and clears it so nothing drops.
    // onExperienceDrop cancels XP orb spawning for players in that pending map.
    // onPlayerClone restores inventory and XP to the new player object after respawn.

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

    // getParseResults().getReader().getString() gives the raw command string without '/'.
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
    //
    // Login order matters: AbyssPermissionHandler.onPlayerJoin must run BEFORE
    // AbyssPermissionSync.onLogin so the permission level is already set when
    // the sync packet is sent to the client.

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
