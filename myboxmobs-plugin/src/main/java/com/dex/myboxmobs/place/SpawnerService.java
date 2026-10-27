package com.dex.myboxmobs.place;

import com.dex.myboxmobs.MyBoxMobsPlugin;
import com.dex.myboxmobs.api.event.MbmMobDespawnEvent;
import com.dex.myboxmobs.live.LiveMob;
import com.dex.myboxmobs.mob.MobDefinition;
import com.dex.myboxmobs.text.Format;
import com.dex.myboxmobs.text.Text;
import com.dex.myboxmobs.config.Messages;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/** Timed waves at virtual locations (spec 7). Main thread only. */
public final class SpawnerService {

    private static final String HOLO = "mbm_spawner_";

    private final MyBoxMobsPlugin plugin;
    private final File file;
    private final Map<String, SpawnerDef> spawners = new TreeMap<>();
    private final Set<Chunk> tickets = new HashSet<>();

    public SpawnerService(MyBoxMobsPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "spawners.yml");
    }

    public void load() {
        clearRuntime();
        spawners.clear();
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = y.getConfigurationSection("spawners");
        if (root != null) {
            for (String id : root.getKeys(false)) {
                ConfigurationSection s = root.getConfigurationSection(id);
                if (s != null) spawners.put(id, SpawnerDef.load(id, s));
            }
        }
        // a scheduled time missed while the server was off is skipped (the next one is planned)
        long now = System.currentTimeMillis();
        for (SpawnerDef d : spawners.values()) {
            if (!d.times.isEmpty() && d.nextWaveAt > 0 && d.nextWaveAt < now - 60_000) d.nextWaveAt = 0;
        }
    }

    public void save() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("config-version", 1);
        for (SpawnerDef d : spawners.values()) d.save(y.createSection("spawners." + d.id));
        try {
            y.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Could not save spawners.yml: " + ex.getMessage());
        }
    }

    public Collection<SpawnerDef> all() {
        return spawners.values();
    }

    public @Nullable SpawnerDef get(String id) {
        return id == null ? null : spawners.get(id.toLowerCase());
    }

    public SpawnerDef create(String id, Location at) {
        SpawnerDef d = new SpawnerDef(id.toLowerCase(), at);
        spawners.put(d.id, d);
        save();
        return d;
    }

    public void delete(SpawnerDef d) {
        spawners.remove(d.id);
        plugin.holograms().remove(HOLO + d.id);
        for (LiveMob mob : plugin.mobs().tracker().byOrigin("spawner:" + d.id)) {
            plugin.mobs().end(mob, MbmMobDespawnEvent.Reason.COMMAND);
        }
        save();
    }

    public int alive(SpawnerDef d) {
        return plugin.mobs().tracker().byOrigin("spawner:" + d.id).size();
    }

    /** Every second. */
    public void tick() {
        long now = System.currentTimeMillis();
        boolean planned = false;
        Set<Chunk> wanted = new HashSet<>();
        for (SpawnerDef d : spawners.values()) {
            Location loc = d.location();
            if (loc == null) continue;
            if (d.enabled && d.keepChunkLoaded && d.mode == SpawnerDef.Mode.ALWAYS) wanted.add(loc.getChunk());
            boolean loaded = loc.isChunkLoaded();
            List<LiveMob> alive = plugin.mobs().tracker().byOrigin("spawner:" + d.id);
            d.active = d.enabled && loaded && !d.mobs.isEmpty()
                    && (d.mode == SpawnerDef.Mode.ALWAYS || playerNear(loc, d.activationRadius));
            // the countdown always runs (also without players); a due wave waits until the spawner is active
            if (d.nextWaveAt == 0) {
                d.nextWaveAt = nextWave(d, now);
                planned = true;
            }
            if (d.active && now >= d.nextWaveAt) {
                wave(d, loc, alive.size());
                d.nextWaveAt = nextWave(d, now + 1000);
                planned = true;
                alive = plugin.mobs().tracker().byOrigin("spawner:" + d.id);
            }
            if (d.leash > 0) leash(alive, loc, d.leash);
            if (loaded) hologram(d, loc, alive.size(), now);
        }
        syncTickets(wanted);
        if (planned) save();
    }

    /** Next wave time: the next clock time of the schedule, else now + interval. */
    public long nextWave(SpawnerDef d, long from) {
        if (d.times.isEmpty()) return from + d.intervalSeconds * 1000L;
        java.time.ZoneId zone = plugin.settings().timezone;
        java.time.ZonedDateTime base = java.time.Instant.ofEpochMilli(from).atZone(zone);
        long best = Long.MAX_VALUE;
        for (String t : d.times) {
            java.time.LocalTime time = parseTime(t);
            if (time == null) continue;
            java.time.ZonedDateTime at = base.with(time).withSecond(0).withNano(0);
            if (!at.isAfter(base)) at = at.plusDays(1);
            best = Math.min(best, at.toInstant().toEpochMilli());
        }
        return best == Long.MAX_VALUE ? from + d.intervalSeconds * 1000L : best;
    }

    /** "8:00" / "20:30" -> time, null when invalid. */
    public static @Nullable java.time.LocalTime parseTime(String raw) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("^(\\d{1,2}):(\\d{2})$").matcher(raw.strip());
        if (!m.matches()) return null;
        int h = Integer.parseInt(m.group(1));
        int min = Integer.parseInt(m.group(2));
        return h > 23 || min > 59 ? null : java.time.LocalTime.of(h, min);
    }

    private void wave(SpawnerDef d, Location center, int aliveCount) {
        int n = Math.min(d.amount, d.maxAlive - aliveCount);
        boolean announced = false;
        for (int i = 0; i < n; i++) {
            SpawnerDef.Entry entry = pick(d.mobs);
            if (entry == null) return;
            Location at = spread(center, d.spread);
            String origin = "spawner:" + d.id;
            if (entry.mob.mythic()) {
                plugin.mobs().spawnRawMythic(entry.mob.id(), at, origin);
                continue;
            }
            MobDefinition def = plugin.registry().get(entry.mob.id());
            if (def == null) {
                plugin.getLogger().warning("Spawner " + d.id + ": mob '" + entry.mob.id() + "' does not exist");
                continue;
            }
            if (plugin.mobs().spawn(def, at, origin, null, !announced) != null) announced = true;
        }
    }

    private static @Nullable SpawnerDef.Entry pick(List<SpawnerDef.Entry> entries) {
        if (entries.isEmpty()) return null;
        if (entries.size() == 1) return entries.get(0);
        int total = 0;
        for (SpawnerDef.Entry e : entries) total += e.weight;
        int roll = ThreadLocalRandom.current().nextInt(total);
        for (SpawnerDef.Entry e : entries) {
            roll -= e.weight;
            if (roll < 0) return e;
        }
        return entries.get(entries.size() - 1);
    }

    private static Location spread(Location center, double radius) {
        if (radius <= 0) return center.clone();
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int attempt = 0; attempt < 6; attempt++) {
            double angle = r.nextDouble(Math.PI * 2);
            double dist = r.nextDouble(radius);
            Location at = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
            Block feet = at.getBlock();
            if (feet.isPassable() && feet.getRelative(0, 1, 0).isPassable()) return at;
        }
        return center.clone();
    }

    private static boolean playerNear(Location loc, int radius) {
        double max = (double) radius * radius;
        for (Player p : loc.getWorld().getPlayers()) {
            if (!p.isDead() && p.getLocation().distanceSquared(loc) <= max) return true;
        }
        return false;
    }

    private static void leash(List<LiveMob> alive, Location home, double leash) {
        double max = leash * leash;
        for (LiveMob mob : alive) {
            if (!mob.entity.isValid()) continue;
            Location at = mob.entity.getLocation();
            if (!at.getWorld().equals(home.getWorld()) || at.distanceSquared(home) > max) mob.entity.teleport(home);
        }
    }

    private void hologram(SpawnerDef d, Location loc, int alive, long now) {
        if (!d.hologram || !plugin.holograms().available()) {
            plugin.holograms().remove(HOLO + d.id);
            return;
        }
        Messages m = plugin.messages();
        String countdown;
        if (!d.enabled) {
            countdown = m.raw("spawner.paused");
        } else if (alive >= d.maxAlive) {
            countdown = m.raw("spawner.full");
        } else if (!d.active && now >= d.nextWaveAt) {
            countdown = m.raw("spawner.waiting");
        } else {
            countdown = Format.duration(Math.max(0, (d.nextWaveAt - now + 999) / 1000));
        }
        String mob = d.mobs.isEmpty() ? "?" : mobName(d.mobs.get(0).mob);
        String[] stats = d.mobs.isEmpty() ? new String[]{"?", "?"} : mobStats(d.mobs.get(0).mob);
        List<String> lines = new ArrayList<>();
        for (String line : holoTemplate(d)) {
            String filled = Messages.apply(line, "mob", mob, "countdown", countdown, "alive", String.valueOf(alive),
                    "max", String.valueOf(d.maxAlive), "id", d.id, "health", stats[0], "damage", stats[1]);
            // DecentHolograms line types (#ICON: SPAWNER, #HEAD: ..., #ENTITY: ...) are passed through untouched
            lines.add(filled.startsWith("#") ? filled : Text.legacy(filled));
        }
        plugin.holograms().show(HOLO + d.id, loc.clone().add(0, plugin.settings().spawnerHoloHeight, 0), lines);
    }

    /**
     * Max health and melee damage shown in the hologram: the MyBoxMobs override, else the values of the last mob
     * of this kind that spawned, else the MythicMobs file.
     */
    public String[] mobStats(MobRef ref) {
        double health = Double.NaN;
        double damage = Double.NaN;
        String mythicId = null;
        if (ref.mythic()) {
            mythicId = ref.id();
        } else {
            MobDefinition def = plugin.registry().get(ref.id());
            if (def != null) {
                Double h = def.stats.get(MobDefinition.Stat.HEALTH);
                Double dmg = def.stats.get(MobDefinition.Stat.DAMAGE);
                if (h != null) health = h;
                if (dmg != null) damage = dmg;
                if (def.source == MobDefinition.Source.MYTHIC) mythicId = def.mythicId;
            }
        }
        double[] seen = plugin.mobs().observedStats(ref.mythic() ? "mythic:" + ref.id() : ref.id());
        if (seen != null) {
            if (Double.isNaN(health)) health = seen[0];
            if (Double.isNaN(damage)) damage = seen[1];
        }
        if (mythicId != null) {
            if (Double.isNaN(health)) health = plugin.mythic().baseHealth(mythicId);
            if (Double.isNaN(damage)) damage = plugin.mythic().baseDamage(mythicId);
        }
        String unknown = plugin.messages().raw("spawner.stat-default");
        String hp = Double.isNaN(health) ? unknown : Format.number(Math.round(health));
        String dmg = Double.isNaN(damage) ? unknown : Format.decimal(Math.max(0, damage));
        return new String[]{hp, dmg};
    }

    /** The hologram template of the spawner's first MyBoxMobs mob; raw MythicMobs mobs use the config default. */
    public List<String> holoTemplate(SpawnerDef d) {
        if (!d.mobs.isEmpty() && !d.mobs.get(0).mob.mythic()) {
            MobDefinition def = plugin.registry().get(d.mobs.get(0).mob.id());
            if (def != null && !def.spawnerHolo.isEmpty()) return def.spawnerHolo;
        }
        return plugin.settings().spawnerHoloLines;
    }

    public String mobName(MobRef ref) {
        if (ref.mythic()) {
            String name = plugin.mythic().displayName(ref.id());
            return name.isEmpty() ? ref.id() : name;
        }
        MobDefinition def = plugin.registry().get(ref.id());
        return def == null ? ref.id() : plugin.mobs().displayName(def);
    }

    /**
     * Keeps exactly one plugin chunk ticket per chunk that at least one enabled ALWAYS spawner with
     * keep-chunk-loaded needs (two spawners in one chunk no longer remove each other's ticket).
     */
    private void syncTickets(Set<Chunk> wanted) {
        for (Chunk c : List.copyOf(tickets)) {
            if (!wanted.contains(c)) {
                c.removePluginChunkTicket(plugin);
                tickets.remove(c);
            }
        }
        for (Chunk c : wanted) {
            if (tickets.add(c)) c.addPluginChunkTicket(plugin);
        }
    }

    /** Reload / disable: holograms and chunk tickets go away (spawned mobs are handled by MobService). */
    public void clearRuntime() {
        for (SpawnerDef d : spawners.values()) plugin.holograms().remove(HOLO + d.id);
        for (Chunk c : tickets) c.removePluginChunkTicket(plugin);
        tickets.clear();
    }
}
