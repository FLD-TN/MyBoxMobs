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

    /** Reward modes; the clan modes need BoxClans. */
    public enum RewardMode {
        KILLER("Người hạ gục", false), TOP("Top sát thương", false),
        TOP_CLAN("Top bang hội", true), CLAN_SHARED("Quà bang hội", true);

        public final String label;
        public final boolean clan;

        RewardMode(String label, boolean clan) {
            this.label = label;
            this.clan = clan;
        }

        /** Shows the player damage board ({@code %top%}). */
        public boolean playerBoard() {
            return this != KILLER;
        }
    }

    /** Who receives the gifts of {@link RewardMode#CLAN_SHARED}. */
    public enum SharedRecipients { PARTICIPANTS, ONLINE }

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
    /** Health bar after the name above the mob (only when the mob has a name); off = the name alone. */
    public boolean nameBar;
    /** Health shown in a boss bar to nearby players (bosses without a model, or whose model has no boss bar). */
    public boolean bossBar;
    /** Every player hit takes exactly 1 health (max health = hits needed); other damage does nothing. */
    public boolean hitHealth;
    public ThiefSettings thief = new ThiefSettings();
    /** Extra height (blocks) of the name above a ModelEngine model. */
    public double nametagOffset = 0.3;
    public int lifetimeSeconds;
    public RewardMode rewardMode = RewardMode.KILLER;
    public boolean multiplierEnabled = true;
    public final List<RewardEntry> killerRewards = new ArrayList<>();
    public final List<TopRank> topRanks = new ArrayList<>();
    public final List<ClanRank> clanRanks = new ArrayList<>();
    /** Clan top: smallest share (%) of the clan's damage a member needs for the member gifts. */
    public double clanMinShare;
    /** Danh Vọng for the killer's clan on every kill (0 = off; not used by the shared clan mode). */
    public long killerClanPoints;
    public final List<RewardEntry> sharedRewards = new ArrayList<>();
    public SharedRecipients sharedRecipients = SharedRecipients.PARTICIPANTS;
    public final List<SkillEntry> skills = new ArrayList<>();
    /** Hologram of the spawners that spawn this mob (one template per mob, default from config.yml). */
    public final List<String> spawnerHolo = new ArrayList<>();
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
        y.set("nametag.name-bar", nameBar);
        y.set("nametag.boss-bar", bossBar);
        y.set("nametag.height-offset", nametagOffset);
        y.set("hit-health", hitHealth);
        thief.save(y, "thief");
        y.set("lifetime-seconds", lifetimeSeconds);
        y.set("rewards.mode", rewardMode.name());
        y.set("rewards.multiplier-enabled", multiplierEnabled);
        List<Map<String, Object>> killer = new ArrayList<>();
        for (RewardEntry e : killerRewards) killer.add(e.serialize());
        y.set("rewards.killer", killer);
        List<Map<String, Object>> top = new ArrayList<>();
        for (TopRank r : topRanks) top.add(r.serialize());
        y.set("rewards.top", top);
        List<Map<String, Object>> clanTop = new ArrayList<>();
        for (ClanRank r : clanRanks) clanTop.add(r.serialize());
        y.set("rewards.clan-top", clanTop);
        y.set("rewards.clan-min-share", clanMinShare);
        y.set("rewards.killer-clan-points", killerClanPoints);
        List<Map<String, Object>> shared = new ArrayList<>();
        for (RewardEntry e : sharedRewards) shared.add(e.serialize());
        y.set("rewards.clan-shared", shared);
        y.set("rewards.clan-shared-recipients", sharedRecipients.name());
        List<Map<String, Object>> skillList = new ArrayList<>();
        for (SkillEntry e : skills) skillList.add(e.serialize());
        y.set("skills", skillList);
        y.set("spawner-hologram", spawnerHolo);
        writeAnnouncement(y, "spawn", spawn);
        writeAnnouncement(y, "death", death);
        writeAnnouncement(y, "despawn", despawn);
        return y;
    }

    /**
     * Switches the reward mode. A mode whose setup is still empty gets a starting setup (owner request: every mode
     * comes with its own defaults); what was set up before is kept.
     */
    public void switchMode(RewardMode mode) {
        rewardMode = mode;
        switch (mode) {
            case TOP -> {
                if (topRanks.isEmpty()) {
                    for (double chance : new double[]{100, 70, 50}) {
                        TopRank r = new TopRank();
                        r.chance = chance;
                        topRanks.add(r);
                    }
                }
            }
            case TOP_CLAN -> {
                if (clanRanks.isEmpty()) {
                    clanRanks.add(new ClanRank(50, 200));
                    clanRanks.add(new ClanRank(30, 100));
                    clanRanks.add(new ClanRank(15, 50));
                }
            }
            default -> {
                // killer and shared clan gifts start as an empty list
            }
        }
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
        d.nameBar = y.getBoolean("nametag.name-bar", false);
        d.bossBar = y.getBoolean("nametag.boss-bar", false);
        d.nametagOffset = Math.max(-5, Math.min(10, y.getDouble("nametag.height-offset", 0.3)));
        d.hitHealth = y.getBoolean("hit-health", false);
        d.thief = ThiefSettings.load(y.getConfigurationSection("thief"));
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
        for (Map<?, ?> m : y.getMapList("rewards.clan-top")) d.clanRanks.add(ClanRank.deserialize(m));
        d.clanMinShare = Math.max(0, Math.min(100, y.getDouble("rewards.clan-min-share", 0)));
        d.killerClanPoints = Math.max(0, y.getLong("rewards.killer-clan-points", 0));
        for (Map<?, ?> m : y.getMapList("rewards.clan-shared")) {
            RewardEntry e = RewardEntry.deserialize(m);
            if (e != null) d.sharedRewards.add(e);
        }
        try {
            d.sharedRecipients = SharedRecipients.valueOf(
                    y.getString("rewards.clan-shared-recipients", "PARTICIPANTS").toUpperCase());
        } catch (IllegalArgumentException ex) {
            d.sharedRecipients = SharedRecipients.PARTICIPANTS;
        }
        for (Map<?, ?> m : y.getMapList("skills")) {
            SkillEntry e = SkillEntry.deserialize(m);
            if (e != null) d.skills.add(e);
        }
        d.spawnerHolo.addAll(y.isSet("spawner-hologram") ? y.getStringList("spawner-hologram") : defaults.spawnerHolo);
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
