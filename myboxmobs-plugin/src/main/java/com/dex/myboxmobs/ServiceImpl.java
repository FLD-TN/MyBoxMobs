package com.dex.myboxmobs;

import com.dex.myboxmobs.api.MyBoxMobsService;
import com.dex.myboxmobs.live.LiveMob;
import com.dex.myboxmobs.mob.MobDefinition;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

/** {@link MyBoxMobsService} for other plugins (spec 12). */
final class ServiceImpl implements MyBoxMobsService {

    private final MyBoxMobsPlugin plugin;

    ServiceImpl(MyBoxMobsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Optional<Entity> spawn(String mobId, Location location) {
        MobDefinition def = plugin.registry().get(mobId);
        if (def == null) return Optional.empty();
        LiveMob mob = plugin.mobs().spawn(def, location, "command", null, true);
        return mob == null ? Optional.empty() : Optional.of(mob.entity);
    }

    @Override
    public boolean isManaged(Entity entity) {
        return entity != null && plugin.mobs().tracker().get(entity.getUniqueId()) != null;
    }

    @Override
    public Optional<String> definitionOf(Entity entity) {
        LiveMob mob = entity == null ? null : plugin.mobs().tracker().get(entity.getUniqueId());
        return mob == null || mob.def == null ? Optional.empty() : Optional.of(mob.def.id);
    }

    @Override
    public int alive(String mobId) {
        return plugin.mobs().tracker().count(mobId);
    }

    @Override
    public Map<UUID, Double> damageOf(Entity entity) {
        LiveMob mob = entity == null ? null : plugin.mobs().tracker().get(entity.getUniqueId());
        return mob == null ? Map.of() : java.util.Collections.unmodifiableMap(mob.sortedDamage());
    }

    @Override
    public Optional<UUID> ownerClanOf(Entity entity) {
        LiveMob mob = entity == null ? null : plugin.mobs().tracker().get(entity.getUniqueId());
        return mob == null ? Optional.empty() : Optional.ofNullable(mob.ownerClan);
    }
}
