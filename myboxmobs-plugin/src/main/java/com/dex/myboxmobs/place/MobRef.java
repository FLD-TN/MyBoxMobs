package com.dex.myboxmobs.place;

import org.jetbrains.annotations.Nullable;

/** A mob a spawner or altar spawns: {@code mbm:<definition>} or {@code mythic:<MythicMobs id>} (spec 7). */
public record MobRef(boolean mythic, String id) {

    public static @Nullable MobRef parse(@Nullable String raw) {
        if (raw == null || raw.isBlank()) return null;
        String s = raw.strip();
        if (s.regionMatches(true, 0, "mythic:", 0, 7)) return s.length() > 7 ? new MobRef(true, s.substring(7)) : null;
        if (s.regionMatches(true, 0, "mbm:", 0, 4)) s = s.substring(4);
        return s.isEmpty() ? null : new MobRef(false, s.toLowerCase());
    }

    @Override
    public String toString() {
        return (mythic ? "mythic:" : "mbm:") + id;
    }
}
