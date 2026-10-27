package com.dex.myboxmobs.mob;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dex.myboxmobs.place.MobRef;
import java.util.List;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;

class MobDefinitionTest {

    private static MobDefinition blank(String id, MobDefinition.Source source) {
        return new MobDefinition(id, source, List.of("spawn"), List.of("death"), List.of("despawn"));
    }

    @Test
    void roundTripKeepsEverythingButItems() throws InvalidConfigurationException {
        MobDefinition d = blank("frost_dragon", MobDefinition.Source.MYTHIC);
        d.mythicId = "FrostDragon";
        d.displayName = "&bRồng Băng";
        d.stats.put(MobDefinition.Stat.HEALTH, 500.0);
        d.lifetimeSeconds = 900;
        d.rewardMode = MobDefinition.RewardMode.TOP;
        d.multiplierEnabled = false;
        RewardEntry money = new RewardEntry(RewardEntry.Kind.MONEY);
        money.min = 100;
        money.max = 200;
        money.chance = 50;
        TopRank first = new TopRank();
        first.chance = 70;
        first.rewards.add(money);
        d.topRanks.add(first);
        d.death.enabled = true;
        d.death.lines.add("%top%");

        YamlConfiguration y = new YamlConfiguration();
        y.loadFromString(d.toYaml().saveToString());
        MobDefinition back = MobDefinition.fromYaml("frost_dragon", y, blank("defaults", MobDefinition.Source.CUSTOM));

        assertEquals(MobDefinition.Source.MYTHIC, back.source);
        assertEquals("FrostDragon", back.mythicId);
        assertEquals("&bRồng Băng", back.displayName);
        assertEquals(500.0, back.stats.get(MobDefinition.Stat.HEALTH));
        assertNull(back.stats.get(MobDefinition.Stat.ARMOR), "unset stats stay 'keep original'");
        assertEquals(900, back.lifetimeSeconds);
        assertEquals(MobDefinition.RewardMode.TOP, back.rewardMode);
        assertFalse(back.multiplierEnabled);
        assertEquals(1, back.topRanks.size());
        assertEquals(70.0, back.topRanks.get(0).chance);
        RewardEntry m = back.topRanks.get(0).rewards.get(0);
        assertEquals(RewardEntry.Kind.MONEY, m.kind);
        assertEquals(100, m.min);
        assertEquals(200, m.max);
        assertEquals(50.0, m.chance);
        assertTrue(back.death.enabled);
        assertEquals(List.of("death", "%top%"), back.death.lines);
    }

    @Test
    void badValuesFallBackToDefaults() throws InvalidConfigurationException {
        YamlConfiguration y = new YamlConfiguration();
        y.loadFromString("source: NOPE\nentity-type: NOT_A_MOB\nrewards:\n  mode: WEIRD\n  killer:\n    - kind: X\n");
        MobDefinition d = MobDefinition.fromYaml("x", y, blank("defaults", MobDefinition.Source.CUSTOM));
        assertEquals(MobDefinition.Source.CUSTOM, d.source);
        assertEquals(EntityType.ZOMBIE, d.entityType);
        assertEquals(MobDefinition.RewardMode.KILLER, d.rewardMode);
        assertTrue(d.killerRewards.isEmpty());
        assertEquals(List.of("spawn"), d.spawn.lines, "missing lines come from config defaults");
    }

    @Test
    void chanceIsClamped() {
        assertEquals(100.0, RewardEntry.clampChance(250));
        assertEquals(0.01, RewardEntry.clampChance(0));
    }

    @Test
    void mobRefs() {
        assertEquals(new MobRef(false, "boss"), MobRef.parse("mbm:Boss"));
        assertEquals(new MobRef(false, "boss"), MobRef.parse("boss"));
        assertEquals(new MobRef(true, "FrostDragon"), MobRef.parse("mythic:FrostDragon"));
        assertNull(MobRef.parse("mythic:"));
        assertNull(MobRef.parse(" "));
        assertEquals("mythic:FrostDragon", MobRef.parse("MYTHIC:FrostDragon").toString());
    }

    @Test
    void ids() {
        assertTrue(MobDefinition.ID.matcher("frost_dragon_2").matches());
        assertFalse(MobDefinition.ID.matcher("Frost").matches());
        assertFalse(MobDefinition.ID.matcher("a-b").matches());
    }
}
