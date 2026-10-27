package com.dex.myboxmobs.api;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

/**
 * Public entry point of MyBoxMobs, registered in Bukkit's {@code ServicesManager}. Main thread only.
 *
 * <pre>{@code
 * MyBoxMobsService mbm = Bukkit.getServicesManager().load(MyBoxMobsService.class);
 * }</pre>
 */
public interface MyBoxMobsService {

    /** Spawns the mob definition {@code mobId} at {@code location}; empty when the id is unknown or the spawn failed. */
    Optional<Entity> spawn(String mobId, Location location);

    /** True when the entity was spawned by MyBoxMobs (command, spawner or altar) and is still alive. */
    boolean isManaged(Entity entity);

    /** Definition id of a managed mob; empty for raw MythicMobs mobs of a spawner and for unmanaged entities. */
    Optional<String> definitionOf(Entity entity);

    /** Number of living managed mobs of this definition. */
    int alive(String mobId);

    /** Live damage per player (capped at the mob's remaining health), highest first. Empty when not managed. */
    Map<UUID, Double> damageOf(Entity entity);
}
