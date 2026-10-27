package com.dex.myboxmobs.live;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import com.dex.myboxmobs.api.event.MbmMobDespawnEvent;
import com.dex.myboxmobs.mob.SkillEntry;
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
import org.bukkit.event.world.EntitiesUnloadEvent;
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
        Entity source = e.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Entity shooter
                ? shooter : e.getDamager();
        LiveMob victim = mobs.tracker().get(e.getEntity().getUniqueId());
        if (victim != null && victim.def != null) {
            Player player = attacker(e.getDamager());
            if (player != null) mobs.recordDamage(victim, player.getUniqueId(), e.getFinalDamage());
            if (!mobs.isCasting()) mobs.castSkills(victim, SkillEntry.Trigger.DAMAGED, source);
        }
        LiveMob attacker = mobs.tracker().get(source.getUniqueId());
        if (attacker != null && attacker.def != null && !mobs.isCasting()) {
            mobs.castSkills(attacker, SkillEntry.Trigger.ATTACK, e.getEntity());
        }
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
        if (mob == null || mob.def == null) return;
        // spec 5: MyBoxMobs rewards fully replace the mob's own drops
        e.getDrops().clear();
        e.setDroppedExp(mob.def.xp);
    }

    /** Rewards and announcements only when no plugin cancelled the death. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeathDone(EntityDeathEvent e) {
        LiveMob mob = mobs.tracker().get(e.getEntity().getUniqueId());
        if (mob != null) mobs.handleDeath(mob);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMythicLoot(MythicMobLootDropEvent e) {
        if (!mobs.hasDefinitionTag(e.getEntity())) return;
        // Mythic exp and money are intangible drops: clearing that table removes them. setExp/setMoney must not
        // be called: in 5.13.1 they build a new drop with a null config and throw.
        e.getDrops().setLootTable(new ArrayList<>());
        e.getDrops().setLootTableIntangible(new HashMap<>());
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
        mobs.adopt(e.getEntities());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntitiesUnload(EntitiesUnloadEvent e) {
        mobs.unloaded(e.getEntities());
    }
}
