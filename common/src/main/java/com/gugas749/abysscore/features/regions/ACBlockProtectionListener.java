package com.gugas749.abysscore.features.regions;

import com.gugas749.abysscore.Constants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Logic for all region protection tags (no_build, no_interact, no_fly, etc.).
 *
 * All methods here take plain vanilla types and return boolean for cancellable checks.
 * The NeoForge loader wires these to events in NeoForgeEventListeners.
 */
public class ACBlockProtectionListener {

    // ── Tag constants ─────────────────────────────────────────────────────────
    public static final String NO_BUILD_TAG = "no_build";
    public static final String NO_INTERACT_TAG = "no_interact";
    public static final String NO_FLY_TAG = "no_fly";
    public static final String NO_FRIENDLYFIRE_TAG = "no_friendlyfire";
    public static final String NO_HUNGER_TAG = "no_hunger";
    public static final String NO_TP_TAG = "no_tp";
    public static final String NO_MOBSPAWNING_HOSTILE_TAG  = "no_mobspawning_hostile";
    public static final String NO_MOBSPAWNING_PACIFIC_TAG  = "no_mobspawning_pacific";
    public static final String NO_ENTRY_TAG = "no_entry";
    /** Not a restriction but a perk: deaths here keep inventory + XP (see ACKeepInventoryListener). */
    public static final String KEEP_INVENTORY_TAG = "keep_inventory";

    // ── Waystones registry names ──────────────────────────────────────────────
    private static final java.util.Set<String> WAYSTONE_BLOCK_IDS = java.util.Set.of(
            "waystones:waystone",
            "waystones:sharestone",
            "waystones:portstone",
            "waystones:warp_plate"
    );

    private static final java.util.Set<String> WAYSTONE_ITEM_IDS = java.util.Set.of(
            "waystones:warp_stone",
            "waystones:warp_scroll",
            "waystones:return_scroll",
            "waystones:bound_scroll",
            "waystones:attuned_shard"
    );

    // ── no_build ──────────────────────────────────────────────────────────────

    /** Returns true if the block change should be cancelled. */
    public boolean checkBlockChange(ServerPlayer player, ServerLevel level, BlockPos pos) {
        if (isExempt(player)) return false;
        if (isRestrictedAt(level, pos, NO_BUILD_TAG)) {
            notify(player, "message.abysscore.no_build_blocked");
            Constants.LOG.debug("[AbyssCore] Blocked block change by {} at {}", player.getName().getString(), pos);
            return true;
        }
        return false;
    }

    // ── no_interact ───────────────────────────────────────────────────────────

    /** Returns true if the right-click block should be cancelled. */
    public boolean checkRightClickBlock(ServerPlayer player, ServerLevel level, BlockPos pos) {
        if (isExempt(player)) return false;
        if (!isRestrictedAt(level, pos, NO_INTERACT_TAG)) return false;

        String blockId = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString();
        if (WAYSTONE_BLOCK_IDS.contains(blockId)) return false;

        notify(player, "message.abysscore.no_interact_blocked");
        return true;
    }

    /** Returns true if the entity interaction should be cancelled. */
    public boolean checkEntityInteract(ServerPlayer player) {
        if (isExempt(player)) return false;
        if (!(player.level() instanceof ServerLevel level)) return false;
        BlockPos pos = player.blockPosition();
        if (isRestrictedAt(level, pos, NO_INTERACT_TAG)) {
            notify(player, "message.abysscore.no_interact_blocked");
            return true;
        }
        return false;
    }

    // ── no_fly + no_hunger ────────────────────────────────────────────────────

    /** Called every server-side player tick by NeoForgeEventListeners. */
    public void onPlayerTick(ServerPlayer player) {
        if (isExempt(player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        BlockPos pos = player.blockPosition();
        String dimension = level.dimension().location().toString();

        if (isRestrictedAt(level, pos, NO_FLY_TAG)) {
            if (player.getAbilities().flying) {
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
                notify(player, "message.abysscore.no_fly_blocked");
            }
            if (player.getAbilities().mayfly && !player.isCreative() && !player.isSpectator()) {
                player.getAbilities().mayfly = false;
                player.onUpdateAbilities();
            }
        }

        if (isRestrictedAt(level, pos, NO_HUNGER_TAG)) {
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(5.0f);
        }
    }

    // ── no_friendlyfire ───────────────────────────────────────────────────────

    /** Returns true if the player-vs-player attack should be cancelled. */
    public boolean checkAttackPlayer(ServerPlayer attacker) {
        if (isExempt(attacker)) return false;
        if (!(attacker.level() instanceof ServerLevel level)) return false;
        BlockPos pos = attacker.blockPosition();
        if (isRestrictedAt(level, pos, NO_FRIENDLYFIRE_TAG)) {
            notify(attacker, "message.abysscore.no_friendlyfire_blocked");
            return true;
        }
        return false;
    }

    // ── no_tp ─────────────────────────────────────────────────────────────────

    /** Returns true if the teleport should be cancelled (ender pearl, chorus fruit, others). */
    public boolean checkTeleport(ServerPlayer player) {
        if (isExempt(player)) return false;
        if (!(player.level() instanceof ServerLevel level)) return false;
        BlockPos pos = player.blockPosition();
        if (isRestrictedAt(level, pos, NO_TP_TAG)) {
            notify(player, "message.abysscore.no_tp_blocked");
            return true;
        }
        return false;
    }

    /** Returns true if the waystone item use should be cancelled. */
    public boolean checkRightClickItem(ServerPlayer player, ItemStack stack) {
        if (isExempt(player)) return false;
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if (!WAYSTONE_ITEM_IDS.contains(itemId)) return false;
        if (!(player.level() instanceof ServerLevel level)) return false;
        BlockPos pos = player.blockPosition();
        if (isRestrictedAt(level, pos, NO_TP_TAG)) {
            notify(player, "message.abysscore.no_tp_blocked");
            return true;
        }
        return false;
    }

    // ── no_mobspawning ────────────────────────────────────────────────────────

    /** Returns true if the mob spawn should be cancelled. */
    public boolean checkMobSpawn(ServerLevel level, BlockPos pos, MobCategory category) {
        String dimension = level.dimension().location().toString();
        if (category == MobCategory.MONSTER) {
            if (ACRegionSavedData.get(level).isRestrictedAt(dimension, pos, NO_MOBSPAWNING_HOSTILE_TAG)) {
                return true;
            }
        }
        if (category != MobCategory.MONSTER && category != MobCategory.MISC) {
            if (ACRegionSavedData.get(level).isRestrictedAt(dimension, pos, NO_MOBSPAWNING_PACIFIC_TAG)) {
                return true;
            }
        }
        return false;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isExempt(ServerPlayer player) {
        return player.hasPermissions(2);
    }

    private boolean isRestrictedAt(ServerLevel level, BlockPos pos, String tag) {
        String dimension = level.dimension().location().toString();
        return ACRegionSavedData.get(level).isRestrictedAt(dimension, pos, tag);
    }

    private void notify(Player player, String translationKey) {
        player.sendSystemMessage(Component.translatable(translationKey));
    }
}
