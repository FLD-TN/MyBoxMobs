package com.dex.myboxmobs.live;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/** The mobs MyBoxMobs spawned and that are still alive. Main thread only. */
public final class Tracker {

    private final Map<UUID, LiveMob> live = new LinkedHashMap<>();

    public void add(LiveMob mob) {
        live.put(mob.uuid(), mob);
    }

    public @Nullable LiveMob get(UUID uuid) {
        return live.get(uuid);
    }

    public @Nullable LiveMob remove(UUID uuid) {
        return live.remove(uuid);
    }

    public Collection<LiveMob> all() {
        return List.copyOf(live.values());
    }

    public int count(String id) {
        int n = 0;
        for (LiveMob m : live.values()) if (m.id.equals(id)) n++;
        return n;
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
