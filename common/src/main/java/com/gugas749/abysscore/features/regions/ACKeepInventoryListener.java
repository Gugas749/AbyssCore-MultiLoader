package com.gugas749.abysscore.features.regions;

import com.gugas749.abysscore.Constants;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameRules;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * "keep_inventory" region tag: players who die inside one of these regions keep
 * their inventory and XP, regardless of the world's keepInventory gamerule.
 *
 * How it works (three events, in the order Minecraft fires them):
 *  1. onDeath — save inventory to NBT and clear it (nothing left to drop).
 *  2. onExperienceDrop — return true to cancel XP orb drops.
 *  3. onClone — restore saved inventory and XP to the newly-created player object.
 *
 * Event wiring (NeoForge, LOWEST priority) lives in NeoForgeEventListeners.
 */
public class ACKeepInventoryListener {

    private static final Map<UUID, ListTag> PENDING = new HashMap<>();

    /**
     * Called when a player dies. Should run at LOWEST priority so other mods
     * that cancel death (e.g. PlayerRevive) run first.
     */
    public void onDeath(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return;

        if (level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) return;

        String dimension = level.dimension().location().toString();
        if (!ACRegionSavedData.get(level).isRestrictedAt(
                dimension, player.blockPosition(), ACBlockProtectionListener.KEEP_INVENTORY_TAG)) return;

        PENDING.put(player.getUUID(), player.getInventory().save(new ListTag()));
        player.getInventory().clearContent();

        Constants.LOG.debug("[AbyssCore] {} died in a keep_inventory region — inventory kept.",
                player.getName().getString());
    }

    /** Returns true if XP drop should be cancelled. */
    public boolean onExperienceDrop(LivingEntity entity) {
        return entity instanceof ServerPlayer player && PENDING.containsKey(player.getUUID());
    }

    /** Called on player clone (respawn). wasDeath must be true, else skip. */
    public void onClone(ServerPlayer newPlayer, ServerPlayer original, boolean wasDeath) {
        if (!wasDeath) return;

        ListTag saved = PENDING.remove(newPlayer.getUUID());
        if (saved == null) return;

        newPlayer.getInventory().load(saved);

        newPlayer.setExperienceLevels(original.experienceLevel);
        newPlayer.experienceProgress = original.experienceProgress;
        newPlayer.totalExperience    = original.totalExperience;
    }
}
