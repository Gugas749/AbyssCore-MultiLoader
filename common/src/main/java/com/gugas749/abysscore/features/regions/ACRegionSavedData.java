package com.gugas749.abysscore.features.regions;

import com.gugas749.abysscore.Constants;
import com.gugas749.abysscore.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class ACRegionSavedData extends SavedData {

    private static final String DATA_NAME = Constants.MOD_ID + "_regions";

    private final Map<String, ACRegion> regions = new LinkedHashMap<>();

    public static ACRegionSavedData get(ServerLevel level) {
        // "Get or create" is different on 1.20.1 and 1.21.1 → the loader does it (IPlatformHelper)
        return Services.PLATFORM.getSavedData(level, DATA_NAME, ACRegionSavedData::new, ACRegionSavedData::load);
    }

    private static ACRegionSavedData load(CompoundTag tag) {
        ACRegionSavedData data = new ACRegionSavedData();
        ListTag regionTags = tag.getList("regions", Tag.TAG_COMPOUND);

        for (int i = 0; i < regionTags.size(); i++) {
            ACRegion region = ACRegion.load(regionTags.getCompound(i));
            data.regions.put(region.name(), region);
        }

        return data;
    }

    /*
     * SavedData's abstract save() is save(CompoundTag) on 1.20.1 and
     * save(CompoundTag, HolderLookup.Provider) on 1.21.1. We write BOTH, without @Override:
     * on each version one of them implements the real method and the other is just an extra
     * method nobody calls. That's how this class compiles for both versions.
     */

    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        return save(tag);
    }

    public CompoundTag save(CompoundTag tag) {
        ListTag regionTags = new ListTag();

        for (ACRegion region : regions.values()) {
            regionTags.add(region.save());
        }

        tag.put("regions", regionTags);
        return tag;
    }

    // ── Region CRUD ───────────────────────────────────────────────────────────

    public boolean addRegion(String name, String dimension, BlockPos first, BlockPos second) {
        if (regions.containsKey(name)) return false;
        regions.put(name, ACRegion.fromCorners(name, dimension, first, second));
        setDirty();
        return true;
    }

    public boolean removeRegion(String name) {
        if (regions.remove(name) == null) return false;
        setDirty();
        return true;
    }

    public Optional<ACRegion> getRegion(String name) {
        return Optional.ofNullable(regions.get(name));
    }

    // ── Tag management ────────────────────────────────────────────────────────

    public boolean addTagToRegion(String regionName, String tag) {
        ACRegion region = regions.get(regionName);
        if (region == null) return false;
        if (region.hasTag(tag)) return false;

        // Records are immutable — replace with a new instance that has the tag
        regions.put(regionName, region.withTag(tag));
        setDirty();
        return true;
    }

    public boolean removeTagFromRegion(String regionName, String tag) {
        ACRegion region = regions.get(regionName);
        if (region == null) return false;
        if (!region.hasTag(tag)) return false;

        regions.put(regionName, region.withoutTag(tag));
        setDirty();
        return true;
    }

    // ── Protection queries ────────────────────────────────────────────────────

    public boolean isRestrictedAt(String dimension, BlockPos pos, String tag) {
        for (ACRegion region : regions.values()) {
            if (region.contains(dimension, pos) && region.hasTag(tag)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Legacy method — kept for backward compatibility.
     * Returns true if the position is inside ANY region (regardless of tags).
     */
    public boolean isProtected(String dimension, BlockPos pos) {
        for (ACRegion region : regions.values()) {
            if (region.contains(dimension, pos)) return true;
        }
        return false;
    }

    public Collection<ACRegion> regions() {
        return regions.values();
    }

    public boolean setEntryFilterTag(String regionName, String filterTag) {
        ACRegion region = regions.get(regionName);
        if (region == null) return false;
        regions.put(regionName, region.withEntryFilterTag(filterTag != null ? filterTag : ""));
        setDirty();
        return true;
    }
}
