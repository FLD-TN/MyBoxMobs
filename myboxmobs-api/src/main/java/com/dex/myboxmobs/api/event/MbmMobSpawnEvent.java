package com.dex.myboxmobs.api.event;

import java.util.UUID;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A mob definition was just spawned (overrides applied). Cancelling removes the mob again before anything is
 * announced.
 */
public final class MbmMobSpawnEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Entity entity;
    private final String mobId;
    private final String origin;
    private final UUID summoner;
    private boolean cancelled;

    public MbmMobSpawnEvent(Entity entity, String mobId, String origin, @Nullable UUID summoner) {
        this.entity = entity;
        this.mobId = mobId;
        this.origin = origin;
        this.summoner = summoner;
    }

    public Entity getEntity() {
        return entity;
    }

    public String getMobId() {
        return mobId;
    }

    /** {@code command}, {@code spawner:<id>} or {@code altar:<id>}. */
    public String getOrigin() {
        return origin;
    }

    /** Player who used the altar, {@code null} otherwise. */
    public @Nullable UUID getSummoner() {
        return summoner;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
