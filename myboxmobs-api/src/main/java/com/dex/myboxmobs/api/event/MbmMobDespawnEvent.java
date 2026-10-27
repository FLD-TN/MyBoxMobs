package com.dex.myboxmobs.api.event;

import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** A managed mob left the world without being killed. */
public final class MbmMobDespawnEvent extends Event {

    public enum Reason {
        /** Its lifetime ran out. */
        LIFETIME,
        /** {@code /mbm kill} or {@code /mbm killall}. */
        COMMAND,
        /** Unloaded with its chunk, removed by another plugin, or the plugin is disabling. */
        REMOVED
    }

    private static final HandlerList HANDLERS = new HandlerList();

    private final Entity entity;
    private final String mobId;
    private final Reason reason;

    public MbmMobDespawnEvent(Entity entity, String mobId, Reason reason) {
        this.entity = entity;
        this.mobId = mobId;
        this.reason = reason;
    }

    public Entity getEntity() {
        return entity;
    }

    public String getMobId() {
        return mobId;
    }

    public Reason getReason() {
        return reason;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
