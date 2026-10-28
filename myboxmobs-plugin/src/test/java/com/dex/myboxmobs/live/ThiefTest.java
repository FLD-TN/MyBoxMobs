package com.dex.myboxmobs.live;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dex.myboxmobs.mob.MobDefinition;
import com.dex.myboxmobs.place.SpawnerDef;
import java.util.List;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class ThiefTest {

    @Test
    void stealIsAPercentOfTheBalanceWithACap() {
        assertEquals(2_000, ThiefState.stealAmount(100_000, 2, 50_000));
        assertEquals(50_000, ThiefState.stealAmount(10_000_000, 3, 50_000), "capped");
        assertEquals(300_000, ThiefState.stealAmount(10_000_000, 3, 0), "0 = no cap");
        assertEquals(0, ThiefState.stealAmount(0, 3, 50_000));
    }

    @Test
    void refundIsAPercentOfEverythingStolenUntilNothingIsLeft() {
        ThiefState s = new ThiefState();
        s.stole(50_000, 0);
        long first = ThiefState.refundAmount(s.stolenMoney, s.leftMoney, 2);
        assertEquals(1_000, first);
        s.leftMoney = 500;
        assertEquals(500, ThiefState.refundAmount(s.stolenMoney, s.leftMoney, 2), "never more than what is left");
        assertEquals(0, ThiefState.refundAmount(0, 0, 2), "nothing stolen yet: nothing back");
    }

    @Test
    void stateSurvivesAnUnload() {
        ThiefState s = new ThiefState();
        s.bagMoney = 70_000;
        s.bagGem = 530;
        s.stole(12_000, 40);
        s.leftMoney = 9_000;
        s.huntUntil = 123;
        ThiefState back = ThiefState.parse(s.serialize());
        assertEquals(s.serialize(), back.serialize());
        assertNull(ThiefState.parse("broken"));
    }

    @Test
    void thiefSettingsAndHitHealthRoundTrip() throws Exception {
        MobDefinition d = new MobDefinition("goblin", MobDefinition.Source.CUSTOM, List.of(), List.of(), List.of());
        d.hitHealth = true;
        d.thief.stealMoney = true;
        d.thief.moneyMinPercent = 0.5;
        d.thief.moneyMaxPercent = 4;
        d.thief.bonusGem = 20;
        d.thief.huntSeconds = 90;
        YamlConfiguration y = new YamlConfiguration();
        y.loadFromString(d.toYaml().saveToString());
        MobDefinition back = MobDefinition.fromYaml("goblin", y, d);
        assertTrue(back.hitHealth);
        assertTrue(back.thief.stealMoney);
        assertFalse(back.thief.stealGem);
        assertEquals(0.5, back.thief.moneyMinPercent);
        assertEquals(4.0, back.thief.moneyMaxPercent);
        assertEquals(20, back.thief.bonusGem);
        assertEquals(90, back.thief.huntSeconds);

        MobDefinition old = MobDefinition.fromYaml("old", new YamlConfiguration(), d);
        assertFalse(old.hitHealth);
        assertFalse(old.thief.enabled(), "older mob files: no thief");
        assertEquals(50_000, old.thief.bagMoney);
    }

    @Test
    void percentRangeInput() {
        assertArrayEquals(new double[]{1, 3}, com.dex.myboxmobs.gui.ThiefMenuAccess.range("1-3"));
        assertArrayEquals(new double[]{1, 3}, com.dex.myboxmobs.gui.ThiefMenuAccess.range("3 - 1"));
        assertArrayEquals(new double[]{2, 2}, com.dex.myboxmobs.gui.ThiefMenuAccess.range("2"));
        assertNull(com.dex.myboxmobs.gui.ThiefMenuAccess.range("150"));
        assertNull(com.dex.myboxmobs.gui.ThiefMenuAccess.range("abc"));
    }

    @Test
    void spawnerAreaIsSavedAndKnowsItsBounds() throws Exception {
        SpawnerDef d = SpawnerDef.load("goblins", new YamlConfiguration());
        assertFalse(d.hasRegion());
        d.pos1 = new int[]{10, 64, -5};
        d.pos2 = new int[]{-2, 66, 7};
        YamlConfiguration y = new YamlConfiguration();
        d.save(y);
        YamlConfiguration back = new YamlConfiguration();
        back.loadFromString(y.saveToString());
        SpawnerDef loaded = SpawnerDef.load("goblins", back);
        assertTrue(loaded.hasRegion());
        assertEquals(-2, loaded.minX());
        assertEquals(10, loaded.maxX());
        assertEquals(64, loaded.minY());
        assertEquals(66, loaded.maxY());
        assertEquals(-5, loaded.minZ());
        assertEquals(7, loaded.maxZ());
    }
}
