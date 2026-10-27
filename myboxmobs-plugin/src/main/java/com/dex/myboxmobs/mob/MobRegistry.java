package com.dex.myboxmobs.mob;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

/** Every mob definition, one file per mob in {@code mobs/}. Each GUI change saves only that mob's file. */
public final class MobRegistry {

    private final File folder;
    private final Logger log;
    private final Map<String, MobDefinition> mobs = new TreeMap<>();
    private List<String> defaultSpawn = List.of();
    private List<String> defaultDeath = List.of();
    private List<String> defaultDespawn = List.of();
    private List<String> defaultSpawnerHolo = List.of();

    private static final List<String> OLD_DEFAULT_DEATH = List.of("",
            "%mob% &#BFC7D5đã bị &#7CFC9A%killer% &#BFC7D5hạ gục", "%top%", "");

    private static final List<String> DEFAULT_SPAWNER_HOLO_053 = List.of("&8[%mob%&8]", "",
            "&#FF6B6B❤ &#FFC9C9%health%  &7|  &#FFB86B⚔ &#FFE3C2%damage%",
            "&#B4F8C8Đang tồn tại: &#7CFC9A%alive%&f/&#FFD166%max% &#B4F8C8quái", "",
            "&#9AD0F5Xuất hiện sau: &#E3F1FF%countdown%", "#ICON: SPAWNER");
    private static final List<String> DEFAULT_SPAWNER_HOLO_054 = List.of("&8[%mob%&8]", "",
            "&#FFC9C9%health% &#FF6B6B❤  &7|  &#FFE3C2%damage% &#FFB86B⚔",
            "&#B4F8C8Đang tồn tại: &#7CFC9A%alive%&f/&#FFD166%max% &#B4F8C8quái", "",
            "&#9AD0F5Xuất hiện sau: &#E3F1FF%countdown%", "#ICON: SPAWNER");

    public MobRegistry(File dataFolder, Logger log) {
        this.folder = new File(dataFolder, "mobs");
        this.log = log;
    }

    public void load(FileConfiguration config) {
        defaultSpawn = config.getStringList("announce.default-lines.spawn");
        defaultDeath = config.getStringList("announce.default-lines.death");
        defaultDespawn = config.getStringList("announce.default-lines.despawn");
        defaultSpawnerHolo = config.getStringList("hologram.spawner-lines");
        mobs.clear();
        if (!folder.isDirectory() && !folder.mkdirs()) {
            log.warning("Could not create " + folder);
            return;
        }
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;
        MobDefinition defaults = blank("defaults", MobDefinition.Source.CUSTOM);
        for (File f : files) {
            String id = f.getName().substring(0, f.getName().length() - 4).toLowerCase();
            if (!MobDefinition.ID.matcher(id).matches()) {
                log.warning("Skipped mobs/" + f.getName() + ": the id must match a-z 0-9 _ (max 32)");
                continue;
            }
            try {
                mobs.put(id, MobDefinition.fromYaml(id, YamlConfiguration.loadConfiguration(f), defaults));
            } catch (RuntimeException ex) {
                log.warning("Could not load mobs/" + f.getName() + ": " + ex.getMessage());
            }
        }
        // 0.5.0: mobs still on the old default death message get the new one (with the leaderboard)
        for (MobDefinition def : mobs.values()) {
            if (def.death.lines.equals(OLD_DEFAULT_DEATH) && !defaultDeath.isEmpty()) {
                def.death.lines.clear();
                def.death.lines.addAll(defaultDeath);
                save(def);
            }
        }
        // mobs that saved an older default spawner hologram (0.5.3 / 0.5.4) get the current default
        for (MobDefinition def : mobs.values()) {
            if ((def.spawnerHolo.equals(DEFAULT_SPAWNER_HOLO_053) || def.spawnerHolo.equals(DEFAULT_SPAWNER_HOLO_054))
                    && !defaultSpawnerHolo.isEmpty() && !def.spawnerHolo.equals(defaultSpawnerHolo)) {
                def.spawnerHolo.clear();
                def.spawnerHolo.addAll(defaultSpawnerHolo);
                save(def);
            }
        }
        log.info("Loaded " + mobs.size() + " mob definitions");
    }

    /** A new definition with the default announcement lines of config.yml (not saved yet). */
    public MobDefinition blank(String id, MobDefinition.Source source) {
        MobDefinition def = new MobDefinition(id, source, defaultSpawn, defaultDeath, defaultDespawn);
        def.spawnerHolo.addAll(defaultSpawnerHolo);
        return def;
    }

    public @Nullable MobDefinition get(String id) {
        return id == null ? null : mobs.get(id.toLowerCase());
    }

    public Collection<MobDefinition> all() {
        return mobs.values();
    }

    public List<String> ids() {
        return new ArrayList<>(mobs.keySet());
    }

    public void put(MobDefinition def) {
        mobs.put(def.id, def);
        save(def);
    }

    public void save(MobDefinition def) {
        try {
            def.toYaml().save(new File(folder, def.id + ".yml"));
        } catch (IOException ex) {
            log.severe("Could not save mobs/" + def.id + ".yml: " + ex.getMessage());
        }
    }

    public void delete(String id) {
        mobs.remove(id);
        File f = new File(folder, id + ".yml");
        if (f.exists() && !f.delete()) log.warning("Could not delete " + f);
    }
}
