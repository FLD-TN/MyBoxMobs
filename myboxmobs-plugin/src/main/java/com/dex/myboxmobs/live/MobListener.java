package com.dex.myboxmobs.live;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import com.dex.myboxmobs.api.event.MbmMobDespawnEvent;
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import io.lumine.mythic.bukkit.events.MythicMobLootDropEvent;
import java.util.ArrayList;
import java.util.HashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.plugin.Plugin;

/** Damage tracking, deaths, MythicMobs drops and removals of tracked mobs. */
public final class MobListener implements Listener {

    private final Plugin plugin;
    private final MobService mobs;

    public MobListener(Plugin plugin, MobService mobs) {
        this.plugin = plugin;
        this.mobs = mobs;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        LiveMob mob = mobs.tracker().get(e.getEntity().getUniqueId());
        if (mob == null || mob.def == null) return;
        Player player = attacker(e.getDamager());
        if (player != null) mobs.recordDamage(mob, player.getUniqueId(), e.getFinalDamage());
    }

    /** Direct hits and projectiles only; tamed pets do not count (owner decision). */
    private static Player attacker(Entity damager) {
        if (damager instanceof Player p) return p;
        if (damager instanceof Projectile proj && proj.getShooter() instanceof Player p) return p;
        return null;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(EntityDeathEvent e) {
        LiveMob mob = mobs.tracker().get(e.getEntity().getUniqueId());
        if (mob == null) return;
        if (mob.def != null) {
            // spec 5: MyBoxMobs rewards fully replace the mob's own drops
            e.getDrops().clear();
            e.setDroppedExp(mob.def.xp);
        }
        mobs.handleDeath(mob);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMythicLoot(MythicMobLootDropEvent e) {
        if (!mobs.hasDefinitionTag(e.getEntity())) return;
        e.getDrops().setLootTable(new ArrayList<>());
        e.getDrops().setLootTableIntangible(new HashMap<>());
        e.setExp(0);
        e.setMoney(0);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMythicDeath(MythicMobDeathEvent e) {
        if (mobs.hasDefinitionTag(e.getEntity())) e.setDrops(new ArrayList<>());
    }

    /** Unloaded with its chunk or removed by another plugin: handled one tick later, after a possible death. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onRemove(EntityRemoveFromWorldEvent e) {
        LiveMob mob = mobs.tracker().get(e.getEntity().getUniqueId());
        if (mob == null || mob.ended) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!mob.ended && !mob.entity.isValid()) mobs.end(mob, MbmMobDespawnEvent.Reason.REMOVED);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntitiesLoad(EntitiesLoadEvent e) {
        mobs.removeOrphans(e.getEntities());
    }
}
