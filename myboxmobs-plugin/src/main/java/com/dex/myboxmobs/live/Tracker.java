package com.dex.myboxmobs.live;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/** The mobs MyBoxMobs spawned and that are still alive. Main thread only. */
public final class Tracker {

    private final Map<UUID, LiveMob> live = new LinkedHashMap<>();
    /** Living mobs per id, kept up to date so placeholders do not scan every mob. */
    private final Map<String, Integer> counts = new HashMap<>();

    public void add(LiveMob mob) {
        LiveMob old = live.put(mob.uuid(), mob);
        if (old != null) uncount(old);
        counts.merge(mob.id, 1, Integer::sum);
    }

    private void uncount(LiveMob mob) {
        counts.computeIfPresent(mob.id, (k, v) -> v <= 1 ? null : v - 1);
    }

    public @Nullable LiveMob get(UUID uuid) {
        return live.get(uuid);
    }

    public @Nullable LiveMob remove(UUID uuid) {
        LiveMob mob = live.remove(uuid);
        if (mob != null) uncount(mob);
        return mob;
    }

    public Collection<LiveMob> all() {
        return List.copyOf(live.values());
    }

    public int count(String id) {
        return counts.getOrDefault(id, 0);
    }

    /** Every living mob grouped by origin, built once per spawner tick. */
    public Map<String, List<LiveMob>> byOrigin() {
        Map<String, List<LiveMob>> out = new HashMap<>();
        for (LiveMob m : live.values()) out.computeIfAbsent(m.origin, k -> new ArrayList<>()).add(m);
        return out;
    }

    public List<LiveMob> byOrigin(String origin) {
        List<LiveMob> out = new ArrayList<>();
        for (LiveMob m : live.values()) if (m.origin.equals(origin)) out.add(m);
        return out;
    }

    public int size() {
        return live.size();
    }
}
