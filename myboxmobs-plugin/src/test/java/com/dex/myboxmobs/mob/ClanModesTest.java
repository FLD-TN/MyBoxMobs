package com.dex.myboxmobs.mob;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class ClanModesTest {

    private static MobDefinition blank(String id) {
        return new MobDefinition(id, MobDefinition.Source.CUSTOM, List.of(), List.of(), List.of());
    }

    @Test
    void clanSettingsRoundTrip() throws InvalidConfigurationException {
        MobDefinition d = blank("clan_boss");
        d.rewardMode = MobDefinition.RewardMode.CLAN_SHARED;
        d.clanMinShare = 12.5;
        d.killerClanPoints = 7;
        d.sharedRecipients = MobDefinition.SharedRecipients.ONLINE;
        RewardEntry money = new RewardEntry(RewardEntry.Kind.MONEY);
        money.min = 500;
        money.max = 500;
        d.sharedRewards.add(money);
        ClanRank rank = new ClanRank(40, 150);
        rank.chance = 80;
        rank.rewards.add(new RewardEntry(RewardEntry.Kind.GEM));
        d.clanRanks.add(rank);

        YamlConfiguration y = new YamlConfiguration();
        y.loadFromString(d.toYaml().saveToString());
        MobDefinition back = MobDefinition.fromYaml("clan_boss", y, blank("defaults"));

        assertEquals(MobDefinition.RewardMode.CLAN_SHARED, back.rewardMode);
        assertEquals(12.5, back.clanMinShare);
        assertEquals(7, back.killerClanPoints);
        assertEquals(MobDefinition.SharedRecipients.ONLINE, back.sharedRecipients);
        assertEquals(1, back.sharedRewards.size());
        assertEquals(500, back.sharedRewards.get(0).min);
        assertEquals(1, back.clanRanks.size());
        assertEquals(40, back.clanRanks.get(0).danhVong);
        assertEquals(150, back.clanRanks.get(0).minhHuy);
        assertEquals(80.0, back.clanRanks.get(0).chance);
        assertEquals(RewardEntry.Kind.GEM, back.clanRanks.get(0).rewards.get(0).kind);
    }

    @Test
    void olderFilesGetClanDefaults() throws InvalidConfigurationException {
        YamlConfiguration y = new YamlConfiguration();
        y.loadFromString("rewards:\n  mode: TOP\n");
        MobDefinition d = MobDefinition.fromYaml("old", y, blank("defaults"));
        assertTrue(d.clanRanks.isEmpty());
        assertEquals(0.0, d.clanMinShare);
        assertEquals(0, d.killerClanPoints);
        assertEquals(MobDefinition.SharedRecipients.PARTICIPANTS, d.sharedRecipients);
    }

    @Test
    void switchingModeSeedsAnEmptySetupOnly() {
        MobDefinition d = blank("seed");
        d.switchMode(MobDefinition.RewardMode.TOP);
        assertEquals(List.of(100.0, 70.0, 50.0), d.topRanks.stream().map(r -> r.chance).toList());

        d.switchMode(MobDefinition.RewardMode.TOP_CLAN);
        assertEquals(List.of(50L, 30L, 15L), d.clanRanks.stream().map(r -> r.danhVong).toList());
        assertEquals(List.of(200L, 100L, 50L), d.clanRanks.stream().map(r -> r.minhHuy).toList());

        d.topRanks.remove(2);
        d.switchMode(MobDefinition.RewardMode.TOP);
        assertEquals(2, d.topRanks.size(), "an edited setup is kept when coming back to its mode");
        assertEquals(3, d.clanRanks.size(), "the other mode's setup is kept too");
    }
}
