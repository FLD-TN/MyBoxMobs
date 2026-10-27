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

    public MobRegistry(File dataFolder, Logger log) {
        this.folder = new File(dataFolder, "mobs");
        this.log = log;
    }

    public void load(FileConfiguration config) {
        defaultSpawn = config.getStringList("announce.default-lines.spawn");
        defaultDeath = config.getStringList("announce.default-lines.death");
        defaultDespawn = config.getStringList("announce.default-lines.despawn");
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
        log.info("Loaded " + mobs.size() + " mob definitions");
    }

    /** A new definition with the default announcement lines of config.yml (not saved yet). */
    public MobDefinition blank(String id, MobDefinition.Source source) {
        return new MobDefinition(id, source, defaultSpawn, defaultDeath, defaultDespawn);
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
