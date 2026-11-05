package com.gugas749.abysscore.api.permission;

public enum AbyssPermissionLevel {
    PLAYER, MODERATOR, ADMIN;

    // Inheritance check — ADMIN passes a MODERATOR check too
    public boolean isAtLeast(AbyssPermissionLevel required) {
        return this.ordinal() >= required.ordinal();
    }
}
