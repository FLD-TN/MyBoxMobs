package com.dex.myboxmobs.live;

import com.dex.myboxmobs.mob.MobDefinition;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

/** A mob spawned by MyBoxMobs that is still in the world (spec 3.3). Main thread only. */
public final class LiveMob {

    public final Entity entity;
    /** {@code null} for a raw MythicMobs mob of a spawner (no overrides, rewards or announcements). */
    public final @Nullable MobDefinition def;
    /** Display id: the definition id, or {@code mythic:<id>} for a raw spawner mob. */
    public final String id;
    /** {@code command}, {@code spawner:<id>} or {@code altar:<id>}. */
    public final String origin;
    public final @Nullable UUID summoner;
    public final long spawnedAt;
    /** Epoch millis, 0 = never. */
    public final long expiresAt;
    private final Map<UUID, Double> damage = new HashMap<>();
    public @Nullable UUID lastHitter;
    /** Set once the mob died or was removed, so the other listener does not handle it again. */
    public boolean ended;

    public LiveMob(Entity entity, @Nullable MobDefinition def, String id, String origin, @Nullable UUID summoner,
                   long now) {
        this.entity = entity;
        this.def = def;
        this.id = id;
        this.origin = origin;
        this.summoner = summoner;
        this.spawnedAt = now;
        this.expiresAt = def == null || def.lifetimeSeconds <= 0 ? 0 : now + def.lifetimeSeconds * 1000L;
    }

    public UUID uuid() {
        return entity.getUniqueId();
    }

    public void addDamage(UUID player, double amount) {
        if (amount <= 0) return;
        damage.merge(player, amount, Double::sum);
        lastHitter = player;
    }

    /** Damage per player, highest first. */
    public LinkedHashMap<UUID, Double> sortedDamage() {
        LinkedHashMap<UUID, Double> out = new LinkedHashMap<>();
        damage.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .forEachOrdered(e -> out.put(e.getKey(), e.getValue()));
        return out;
    }

    public boolean fromSpawner(String spawnerId) {
        return origin.equals("spawner:" + spawnerId);
    }

    public boolean fromAltar(String altarId) {
        return origin.equals("altar:" + altarId);
    }
}
