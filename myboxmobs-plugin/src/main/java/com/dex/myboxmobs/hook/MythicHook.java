package com.dex.myboxmobs.hook;

import io.lumine.mythic.api.config.MythicConfig;
import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

/** The only class calling the MythicMobs mob API (MythicMobs 5.13). Main thread only. */
public final class MythicHook {

    /** {@code model{mid=dragon}} / {@code model{m=dragon;...}} / {@code modelid=}. */
    private static final Pattern MODEL = Pattern.compile("(?i)model\\{[^}]*?\\b(?:mid|m|modelid)=([A-Za-z0-9_\\-]+)");

    public List<String> mobNames() {
        List<String> names = new ArrayList<>(MythicBukkit.inst().getMobManager().getMobNames());
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public boolean exists(String mythicId) {
        return mythicId != null && !mythicId.isEmpty()
                && MythicBukkit.inst().getMobManager().getMythicMob(mythicId).isPresent();
    }

    /** The spawned Bukkit entity, {@code null} when MythicMobs refused (unknown id, cancelled spawn). */
    public @Nullable Entity spawn(String mythicId, Location location) {
        ActiveMob mob = MythicBukkit.inst().getMobManager().spawnMob(mythicId, location);
        if (mob == null || mob.getEntity() == null) return null;
        return mob.getEntity().getBukkitEntity();
    }

    /** Changes the name MythicMobs shows (its name tag and bossbar use it). */
    public void setDisplayName(Entity entity, String legacyName) {
        ActiveMob mob = MythicBukkit.inst().getMobManager().getMythicMobInstance(entity);
        if (mob != null) mob.setDisplayName(legacyName);
    }

    /** Raw display name from the MythicMobs file, "" when unknown. */
    public String displayName(String mythicId) {
        Optional<MythicMob> mob = MythicBukkit.inst().getMobManager().getMythicMob(mythicId);
        if (mob.isEmpty()) return "";
        MythicConfig config = mob.get().getConfig();
        String name = config == null ? null : config.getString("Display");
        return name == null ? "" : name;
    }

    public String entityType(String mythicId) {
        return MythicBukkit.inst().getMobManager().getMythicMob(mythicId).map(MythicMob::getEntityTypeString).orElse("");
    }

    /** Every loaded MythicMobs skill (metaskill) name, sorted. */
    public List<String> skillNames() {
        List<String> names = new ArrayList<>(MythicBukkit.inst().getSkillManager().getSkillNames());
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public boolean skillExists(String skill) {
        return skill != null && MythicBukkit.inst().getSkillManager().getSkill(skill).isPresent();
    }

    /**
     * Casts a MythicMobs skill with {@code caster} (any entity, also vanilla mobs). {@code target} becomes the
     * inherited target of the skill lines without their own targeter, {@code trigger} is the trigger entity.
     */
    public boolean castSkill(Entity caster, String skill, @Nullable Entity trigger, @Nullable Entity target) {
        java.util.Collection<Entity> targets = target == null ? List.of() : List.of(target);
        return MythicBukkit.inst().getAPIHelper().castSkill(caster, skill, trigger, caster.getLocation(),
                targets, List.of(), 1f);
    }

    /** Health / Damage written in the MythicMobs file, NaN when missing or not a plain number. */
    public double baseHealth(String mythicId) {
        return configNumber(mythicId, "Health");
    }

    public double baseDamage(String mythicId) {
        return configNumber(mythicId, "Damage");
    }

    private double configNumber(String mythicId, String key) {
        Optional<MythicMob> mob = MythicBukkit.inst().getMobManager().getMythicMob(mythicId);
        if (mob.isEmpty() || mob.get().getConfig() == null) return Double.NaN;
        String raw = mob.get().getConfig().getString(key);
        if (raw == null) return Double.NaN;
        try {
            return Double.parseDouble(raw.strip());
        } catch (NumberFormatException ex) {
            return Double.NaN;
        }
    }

    /** Model ids the MythicMobs file references in its skills, for the pack check (spec 3.2). */
    public Set<String> referencedModels(String mythicId) {
        Set<String> out = new LinkedHashSet<>();
        Optional<MythicMob> mob = MythicBukkit.inst().getMobManager().getMythicMob(mythicId);
        if (mob.isEmpty() || mob.get().getConfig() == null) return out;
        MythicConfig config = mob.get().getConfig();
        List<String> lines = new ArrayList<>(config.getStringList("Skills"));
        // 5.13 mob option "Model: { Id: ... }" (older files: "Model: id")
        String option = config.isSet("Model.Id") ? config.getString("Model.Id") : null;
        if (option == null && config.isSet("Model") && !config.isSet("Model.Id")) option = config.getString("Model");
        if (option != null && !option.contains(":") && !option.contains("{")) lines.add("model{mid=" + option + "}");
        for (String line : lines) {
            Matcher m = MODEL.matcher(line);
            while (m.find()) out.add(m.group(1));
        }
        return out;
    }
}
