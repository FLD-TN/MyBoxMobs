package com.dex.myboxmobs.place;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

/** A virtual spawner (spec 7). Edited by the GUI on the main thread, saved in spawners.yml. */
public final class SpawnerDef {

    public enum Mode { NEARBY, ALWAYS }

    public static final class Entry {
        public MobRef mob;
        /** Relative weight, used only when the spawner has several entries. */
        public int weight;

        public Entry(MobRef mob, int weight) {
            this.mob = mob;
            this.weight = Math.max(1, weight);
        }
    }

    public final String id;
    public String world;
    public double x;
    public double y;
    public double z;
    public final List<Entry> mobs = new ArrayList<>();
    public int intervalSeconds = 60;
    public int amount = 1;
    public int maxAlive = 3;
    public Mode mode = Mode.NEARBY;
    public int activationRadius = 32;
    public double spread = 3;
    public double leash = 24;
    public boolean keepChunkLoaded;
    public boolean hologram = true;
    public boolean enabled = true;
    /** Clock times "HH:mm" (server timezone of config.yml); not empty = spawn at these times instead of the interval. */
    public final List<String> times = new ArrayList<>();
    /** Epoch millis of the next wave, saved so a restart keeps the countdown; 0 = not planned yet. */
    public long nextWaveAt;
    /**
     * Respawn after death: every mob that is gone (killed, lifetime over, removed) comes back {@code intervalSeconds}
     * after it left, one timer per empty place; the first mobs wait one interval too. Replaces interval and schedule.
     */
    public boolean afterDeath;
    /** Epoch millis of the pending respawns (one per empty place), saved. */
    public final List<Long> respawnAt = new ArrayList<>();

    /** Spawn area (same world as the spawner): two corners on the ground, null = spawn around the spawner. */
    public @Nullable int[] pos1;
    public @Nullable int[] pos2;

    // runtime, not saved
    public boolean active;

    public SpawnerDef(String id, Location at) {
        this(id);
        setLocation(at);
    }

    private SpawnerDef(String id) {
        this.id = id;
    }

    public void setLocation(Location at) {
        this.world = at.getWorld().getName();
        this.x = at.getBlockX() + 0.5;
        this.y = at.getBlockY();
        this.z = at.getBlockZ() + 0.5;
    }

    public @Nullable Location location() {
        World w = Bukkit.getWorld(world);
        return w == null ? null : new Location(w, x, y, z);
    }

    public boolean hasRegion() {
        return pos1 != null && pos2 != null;
    }

    public int minX() {
        return Math.min(pos1[0], pos2[0]);
    }

    public int maxX() {
        return Math.max(pos1[0], pos2[0]);
    }

    public int minY() {
        return Math.min(pos1[1], pos2[1]);
    }

    public int maxY() {
        return Math.max(pos1[1], pos2[1]);
    }

    public int minZ() {
        return Math.min(pos1[2], pos2[2]);
    }

    public int maxZ() {
        return Math.max(pos1[2], pos2[2]);
    }

    /**
     * Where the spawner "is": the centre of its area (on top of the higher corner) when it has one, else its own
     * location. Activation radius, hologram and kept chunk use it.
     */
    public @Nullable Location center() {
        if (!hasRegion()) return location();
        World w = Bukkit.getWorld(world);
        if (w == null) return null;
        return new Location(w, (minX() + maxX() + 1) / 2.0, maxY() + 1, (minZ() + maxZ() + 1) / 2.0);
    }

    /** True when the block position is inside the area grown by {@code margin} blocks on every side. */
    public boolean inRegion(Location at, double margin) {
        if (!hasRegion() || !at.getWorld().getName().equals(world)) return false;
        return at.getX() >= minX() - margin && at.getX() <= maxX() + 1 + margin
                && at.getZ() >= minZ() - margin && at.getZ() <= maxZ() + 1 + margin
                && at.getY() >= minY() - 4 - margin && at.getY() <= maxY() + 6 + margin;
    }

    public void save(ConfigurationSection s) {
        s.set("world", world);
        s.set("x", x);
        s.set("y", y);
        s.set("z", z);
        List<Map<String, Object>> list = new ArrayList<>();
        for (Entry e : mobs) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("mob", e.mob.toString());
            m.put("weight", e.weight);
            list.add(m);
        }
        s.set("mobs", list);
        s.set("interval-seconds", intervalSeconds);
        s.set("amount", amount);
        s.set("max-alive", maxAlive);
        s.set("mode", mode.name());
        s.set("activation-radius", activationRadius);
        s.set("spread", spread);
        s.set("leash", leash);
        s.set("keep-chunk-loaded", keepChunkLoaded);
        s.set("hologram.enabled", hologram);
        s.set("enabled", enabled);
        s.set("times", times);
        s.set("next-wave-at", nextWaveAt);
        s.set("after-death", afterDeath);
        s.set("respawn-at", respawnAt);
        s.set("region.pos1", pos1 == null ? null : List.of(pos1[0], pos1[1], pos1[2]));
        s.set("region.pos2", pos2 == null ? null : List.of(pos2[0], pos2[1], pos2[2]));
    }

    private static @Nullable int[] pos(List<Integer> raw) {
        return raw == null || raw.size() < 3 ? null : new int[]{raw.get(0), raw.get(1), raw.get(2)};
    }

    /**
     * After-death mode, every second: one timer per empty place. A place that just became empty gets a timer of
     * one interval from now; when mobs come back (chunk loaded again) the latest timers go. True when changed.
     */
    public boolean syncRespawnTimers(int alive, long now) {
        int missing = maxAlive - Math.max(0, alive) - respawnAt.size();
        if (missing == 0) return false;
        respawnAt.sort(null);
        for (int i = 0; i < missing; i++) respawnAt.add(now + intervalSeconds * 1000L);
        for (int i = 0; i < -missing && !respawnAt.isEmpty(); i++) respawnAt.remove(respawnAt.size() - 1);
        return true;
    }

    /** After-death mode: removes and counts the timers that are due (at most the free places). */
    public int takeDue(int alive, long now) {
        respawnAt.sort(null);
        int room = maxAlive - Math.max(0, alive);
        int n = 0;
        while (n < room && !respawnAt.isEmpty() && respawnAt.get(0) <= now) {
            respawnAt.remove(0);
            n++;
        }
        return n;
    }

    /** Earliest pending respawn, 0 when none. */
    public long nextRespawn() {
        long best = 0;
        for (long t : respawnAt) if (best == 0 || t < best) best = t;
        return best;
    }

    public static SpawnerDef load(String id, ConfigurationSection s) {
        SpawnerDef d = new SpawnerDef(id);
        d.world = s.getString("world", "world");
        d.x = s.getDouble("x");
        d.y = s.getDouble("y");
        d.z = s.getDouble("z");
        for (Map<?, ?> m : s.getMapList("mobs")) {
            MobRef ref = MobRef.parse(String.valueOf(m.get("mob")));
            Object w = m.get("weight");
            if (ref != null) d.mobs.add(new Entry(ref, w instanceof Number n ? n.intValue() : 1));
        }
        d.intervalSeconds = Math.max(1, s.getInt("interval-seconds", 60));
        d.amount = Math.max(1, s.getInt("amount", 1));
        d.maxAlive = Math.max(1, s.getInt("max-alive", 3));
        try {
            d.mode = Mode.valueOf(s.getString("mode", "NEARBY").toUpperCase());
        } catch (IllegalArgumentException ex) {
            d.mode = Mode.NEARBY;
        }
        d.activationRadius = Math.max(1, s.getInt("activation-radius", 32));
        d.spread = Math.max(0, s.getDouble("spread", 3));
        d.leash = Math.max(0, s.getDouble("leash", 24));
        d.keepChunkLoaded = s.getBoolean("keep-chunk-loaded", false);
        d.hologram = s.getBoolean("hologram.enabled", true);
        d.enabled = s.getBoolean("enabled", true);
        d.times.addAll(s.getStringList("times"));
        d.nextWaveAt = s.getLong("next-wave-at", 0);
        d.afterDeath = s.getBoolean("after-death", false);
        d.respawnAt.addAll(s.getLongList("respawn-at"));
        d.pos1 = pos(s.getIntegerList("region.pos1"));
        d.pos2 = pos(s.getIntegerList("region.pos2"));
        return d;
    }
}
