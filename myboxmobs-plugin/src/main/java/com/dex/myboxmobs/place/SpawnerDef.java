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
    public final List<String> holoLines = new ArrayList<>();
    public boolean enabled = true;

    // runtime, not saved
    public long nextWaveAt;
    public boolean active;

    public SpawnerDef(String id, Location at, List<String> defaultHolo) {
        this(id);
        setLocation(at);
        holoLines.addAll(defaultHolo);
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
        s.set("hologram.lines", holoLines);
        s.set("enabled", enabled);
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
        d.holoLines.addAll(s.getStringList("hologram.lines"));
        d.enabled = s.getBoolean("enabled", true);
        return d;
    }
}
