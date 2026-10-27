package com.dex.myboxmobs.place;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class RespawnTimerTest {

    private static final long MIN = 60_000;

    private static SpawnerDef spawner(int maxAlive) {
        SpawnerDef d = SpawnerDef.load("boss", new YamlConfiguration());
        d.afterDeath = true;
        d.intervalSeconds = 30 * 60;
        d.maxAlive = maxAlive;
        return d;
    }

    @Test
    void firstSpawnWaitsOneInterval() {
        SpawnerDef d = spawner(1);
        assertTrue(d.syncRespawnTimers(0, 0));
        assertEquals(List.of(30 * MIN), d.respawnAt);
        assertEquals(0, d.takeDue(0, 29 * MIN));
        assertEquals(1, d.takeDue(0, 30 * MIN));
        assertTrue(d.respawnAt.isEmpty());
    }

    @Test
    void noTimerWhileTheBossLives() {
        SpawnerDef d = spawner(1);
        assertFalse(d.syncRespawnTimers(1, 0));
        assertEquals(0, d.nextRespawn());
        // killed at minute 50: back at minute 80
        d.syncRespawnTimers(0, 50 * MIN);
        assertEquals(80 * MIN, d.nextRespawn());
    }

    @Test
    void eachDeadMobHasItsOwnTimer() {
        SpawnerDef d = spawner(3);
        d.syncRespawnTimers(3, 0);
        d.syncRespawnTimers(2, 10 * MIN);
        d.syncRespawnTimers(1, 20 * MIN);
        assertEquals(List.of(40 * MIN, 50 * MIN), d.respawnAt);
        assertEquals(1, d.takeDue(1, 45 * MIN), "only the first one is due");
        assertEquals(List.of(50 * MIN), d.respawnAt);
    }

    @Test
    void mobsComingBackCancelTheLatestTimers() {
        SpawnerDef d = spawner(2);
        d.syncRespawnTimers(0, 0);
        assertEquals(2, d.respawnAt.size());
        assertTrue(d.syncRespawnTimers(2, 5 * MIN), "both mobs were only unloaded");
        assertTrue(d.respawnAt.isEmpty());
    }

    @Test
    void timersSurviveARestart() throws Exception {
        SpawnerDef d = spawner(2);
        d.syncRespawnTimers(0, 0);
        YamlConfiguration y = new YamlConfiguration();
        d.save(y);
        YamlConfiguration back = new YamlConfiguration();
        back.loadFromString(y.saveToString());
        SpawnerDef loaded = SpawnerDef.load("boss", back);
        assertTrue(loaded.afterDeath);
        assertEquals(d.respawnAt, loaded.respawnAt);
    }
}
