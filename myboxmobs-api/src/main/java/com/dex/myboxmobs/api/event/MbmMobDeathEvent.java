package com.dex.myboxmobs.api.event;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** A managed mob was killed. Fired before rewards are paid and the death is announced. */
public final class MbmMobDeathEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Entity entity;
    private final String mobId;
    private final UUID killer;
    private final LinkedHashMap<UUID, Double> damage;
    private boolean rewardsCancelled;

    public MbmMobDeathEvent(Entity entity, String mobId, @Nullable UUID killer, LinkedHashMap<UUID, Double> damage) {
        this.entity = entity;
        this.mobId = mobId;
        this.killer = killer;
        this.damage = damage;
    }

    public Entity getEntity() {
        return entity;
    }

    public String getMobId() {
        return mobId;
    }

    /** Player who dealt the last counted hit, {@code null} when no player did. */
    public @Nullable UUID getKiller() {
        return killer;
    }

    /** Damage per player, highest first. Read-only view. */
    public Map<UUID, Double> getDamage() {
        return java.util.Collections.unmodifiableMap(damage);
    }

    public boolean isRewardsCancelled() {
        return rewardsCancelled;
    }

    /** True: MyBoxMobs pays no reward for this death (the death is still announced and counted). */
    public void setRewardsCancelled(boolean rewardsCancelled) {
        this.rewardsCancelled = rewardsCancelled;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
