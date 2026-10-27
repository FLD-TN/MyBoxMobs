package com.dex.myboxmobs.mob;

import com.dex.myboxmobs.util.ItemCodec;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/** A mob MyBoxMobs can spawn (spec 3): file {@code mobs/<id>.yml}. Edited by the GUI on the main thread. */
public final class MobDefinition {

    public static final int CONFIG_VERSION = 1;
    public static final java.util.regex.Pattern ID = java.util.regex.Pattern.compile("^[a-z0-9_]{1,32}$");
    public static final List<EquipmentSlot> SLOTS = List.of(EquipmentSlot.HAND, EquipmentSlot.OFF_HAND,
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

    public enum Source { MYTHIC, CUSTOM }

    public enum ModelMode { ONLY_IF_NONE, REPLACE }

    public enum RewardMode { KILLER, TOP }

    /** Stats overrides; {@code null} keeps the original value. */
    public enum Stat {
        HEALTH("health"), ARMOR("armor"), TOUGHNESS("armor-toughness"), DAMAGE("damage"),
        SPEED("speed"), FOLLOW_RANGE("follow-range"), KNOCKBACK("knockback-resistance");

        public final String key;

        Stat(String key) {
            this.key = key;
        }
    }

    public static final class Announcement {
        public boolean enabled;
        public final List<String> lines = new ArrayList<>();

        Announcement(boolean enabled, List<String> lines) {
            this.enabled = enabled;
            this.lines.addAll(lines);
        }
    }

    public final String id;
    public Source source;
    public String mythicId = "";
    public EntityType entityType = EntityType.ZOMBIE;
    public String displayName = "";
    public final EnumMap<Stat, Double> stats = new EnumMap<>(Stat.class);
    public final EnumMap<EquipmentSlot, ItemStack> equipment = new EnumMap<>(EquipmentSlot.class);
    public String modelId = "";
    public ModelMode modelMode = ModelMode.ONLY_IF_NONE;
    public double modelScale = 1.0;
    public boolean hideBase = true;
    public int xp;
    public int lifetimeSeconds;
    public RewardMode rewardMode = RewardMode.KILLER;
    public boolean multiplierEnabled = true;
    public final List<RewardEntry> killerRewards = new ArrayList<>();
    public final List<TopRank> topRanks = new ArrayList<>();
    public final Announcement spawn;
    public final Announcement death;
    public final Announcement despawn;

    public MobDefinition(String id, Source source, List<String> spawnLines, List<String> deathLines,
                         List<String> despawnLines) {
        this.id = id;
        this.source = source;
        this.spawn = new Announcement(false, spawnLines);
        this.death = new Announcement(false, deathLines);
        this.despawn = new Announcement(false, despawnLines);
    }

    public YamlConfiguration toYaml() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("config-version", CONFIG_VERSION);
        y.set("source", source.name());
        y.set("mythic-id", mythicId);
        y.set("entity-type", entityType.name());
        y.set("display-name", displayName);
        for (Stat s : Stat.values()) y.set("stats." + s.key, stats.getOrDefault(s, -1.0));
        for (EquipmentSlot slot : SLOTS) {
            ItemStack item = equipment.get(slot);
            y.set("equipment." + slot.name(), ItemCodec.isEmpty(item) ? "" : ItemCodec.encode(item));
        }
        y.set("model.id", modelId);
        y.set("model.mode", modelMode.name());
        y.set("model.scale", modelScale);
        y.set("model.hide-base", hideBase);
        y.set("xp", xp);
        y.set("lifetime-seconds", lifetimeSeconds);
        y.set("rewards.mode", rewardMode.name());
        y.set("rewards.multiplier-enabled", multiplierEnabled);
        List<Map<String, Object>> killer = new ArrayList<>();
        for (RewardEntry e : killerRewards) killer.add(e.serialize());
        y.set("rewards.killer", killer);
        List<Map<String, Object>> top = new ArrayList<>();
        for (TopRank r : topRanks) top.add(r.serialize());
        y.set("rewards.top", top);
        writeAnnouncement(y, "spawn", spawn);
        writeAnnouncement(y, "death", death);
        writeAnnouncement(y, "despawn", despawn);
        return y;
    }

    private static void writeAnnouncement(YamlConfiguration y, String key, Announcement a) {
        y.set("announce." + key + ".enabled", a.enabled);
        y.set("announce." + key + ".lines", a.lines);
    }

    /** Reads a mob file; unknown or missing values fall back to defaults. */
    public static MobDefinition fromYaml(String id, YamlConfiguration y, MobDefinition defaults) {
        Source source;
        try {
            source = Source.valueOf(y.getString("source", "CUSTOM").toUpperCase());
        } catch (IllegalArgumentException ex) {
            source = Source.CUSTOM;
        }
        MobDefinition d = new MobDefinition(id, source, lines(y, "spawn", defaults.spawn),
                lines(y, "death", defaults.death), lines(y, "despawn", defaults.despawn));
        d.mythicId = y.getString("mythic-id", "");
        try {
            d.entityType = EntityType.valueOf(y.getString("entity-type", "ZOMBIE").toUpperCase());
        } catch (IllegalArgumentException ex) {
            d.entityType = EntityType.ZOMBIE;
        }
        d.displayName = y.getString("display-name", "");
        for (Stat s : Stat.values()) {
            double v = y.getDouble("stats." + s.key, -1);
            if (v >= 0) d.stats.put(s, v);
        }
        for (EquipmentSlot slot : SLOTS) {
            ItemStack item = ItemCodec.decode(y.getString("equipment." + slot.name(), ""));
            if (!ItemCodec.isEmpty(item)) d.equipment.put(slot, item);
        }
        d.modelId = y.getString("model.id", "");
        try {
            d.modelMode = ModelMode.valueOf(y.getString("model.mode", "ONLY_IF_NONE").toUpperCase());
        } catch (IllegalArgumentException ex) {
            d.modelMode = ModelMode.ONLY_IF_NONE;
        }
        d.modelScale = Math.max(0.1, y.getDouble("model.scale", 1.0));
        d.hideBase = y.getBoolean("model.hide-base", true);
        d.xp = Math.max(0, y.getInt("xp", 0));
        d.lifetimeSeconds = Math.max(0, y.getInt("lifetime-seconds", 0));
        try {
            d.rewardMode = RewardMode.valueOf(y.getString("rewards.mode", "KILLER").toUpperCase());
        } catch (IllegalArgumentException ex) {
            d.rewardMode = RewardMode.KILLER;
        }
        d.multiplierEnabled = y.getBoolean("rewards.multiplier-enabled", true);
        for (Map<?, ?> m : y.getMapList("rewards.killer")) {
            RewardEntry e = RewardEntry.deserialize(m);
            if (e != null) d.killerRewards.add(e);
        }
        for (Map<?, ?> m : y.getMapList("rewards.top")) d.topRanks.add(TopRank.deserialize(m));
        d.spawn.enabled = y.getBoolean("announce.spawn.enabled", false);
        d.death.enabled = y.getBoolean("announce.death.enabled", false);
        d.despawn.enabled = y.getBoolean("announce.despawn.enabled", false);
        return d;
    }

    private static List<String> lines(YamlConfiguration y, String key, Announcement fallback) {
        String path = "announce." + key + ".lines";
        return y.isSet(path) ? y.getStringList(path) : fallback.lines;
    }
}
