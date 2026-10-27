package com.dex.myboxmobs.mob;

import java.util.LinkedHashMap;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * A MythicMobs skill MyBoxMobs casts for a mob (vanilla or MythicMobs) on a trigger. The skill itself stays in the
 * MythicMobs files; only when and on whom it runs is set here.
 */
public final class SkillEntry {

    public enum Trigger {
        /** Once, right after the mob spawned. */
        SPAWN,
        /** Every {@link #seconds} seconds while the mob lives. */
        TIMER,
        /** When a player or mob damages it (the attacker is the trigger). */
        DAMAGED,
        /** When it damages something (the victim is the trigger). */
        ATTACK,
        /** When it dies. */
        DEATH
    }

    public enum Target {
        /** The mob's current target, else the trigger entity. Skill lines with their own targeter ignore it. */
        TARGET,
        /** The entity of the trigger: the attacker (DAMAGED) or the victim (ATTACK). */
        TRIGGER,
        /** The nearest player within 32 blocks. */
        NEAREST_PLAYER,
        /** The mob itself. */
        SELF
    }

    public String skill;
    public Trigger trigger = Trigger.TIMER;
    public Target target = Target.TARGET;
    /** TIMER: interval; other triggers: cooldown (0 = none). */
    public int seconds = 10;
    /** 0.01 to 100. */
    public double chance = 100.0;

    public SkillEntry(String skill) {
        this.skill = skill;
    }

    public Map<String, Object> serialize() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("skill", skill);
        m.put("trigger", trigger.name());
        m.put("target", target.name());
        m.put("seconds", seconds);
        m.put("chance", chance);
        return m;
    }

    public static @Nullable SkillEntry deserialize(Map<?, ?> m) {
        Object name = m.get("skill");
        if (name == null || String.valueOf(name).isBlank()) return null;
        SkillEntry e = new SkillEntry(String.valueOf(name));
        try {
            e.trigger = Trigger.valueOf(String.valueOf(m.get("trigger")).toUpperCase());
        } catch (IllegalArgumentException ex) {
            e.trigger = Trigger.TIMER;
        }
        try {
            e.target = Target.valueOf(String.valueOf(m.get("target")).toUpperCase());
        } catch (IllegalArgumentException ex) {
            e.target = Target.TARGET;
        }
        e.seconds = (int) Math.max(0, RewardEntry.toLong(m.get("seconds"), 10));
        e.chance = RewardEntry.clampChance(RewardEntry.toDouble(m.get("chance"), 100.0));
        return e;
    }
}
