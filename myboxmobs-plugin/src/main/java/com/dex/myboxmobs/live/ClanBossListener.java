package com.dex.myboxmobs.live;

import com.dex.myboxmobs.MyBoxMobsPlugin;
import com.dex.myboxmobs.hook.ClanSupport;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * A clan boss (clan altar) can only be damaged by members of its clan. The boss still attacks everyone, so other
 * players cannot block the clan without being hit (owner decision). Runs before the damage is recorded (MONITOR).
 */
public final class ClanBossListener implements Listener {

    private static final long MESSAGE_GAP_MS = 2000;

    private final MyBoxMobsPlugin plugin;
    private final Map<UUID, Long> lastMessage = new HashMap<>();

    public ClanBossListener(MyBoxMobsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        LiveMob mob = plugin.mobs().tracker().get(e.getEntity().getUniqueId());
        if (mob == null || mob.ownerClan == null) return;
        Player p = player(e.getDamager());
        if (p == null) return;
        ClanSupport clans = plugin.clans();
        // without BoxClans, or once the clan was disbanded, the boss is open to everyone
        ClanSupport.ClanInfo owner = clans.clan(mob.ownerClan);
        if (owner == null) return;
        ClanSupport.ClanInfo own = clans.clanOf(p.getUniqueId());
        if (own != null && own.uuid().equals(mob.ownerClan)) return;
        e.setCancelled(true);
        long now = System.currentTimeMillis();
        Long last = lastMessage.get(p.getUniqueId());
        if (last == null || now - last >= MESSAGE_GAP_MS) {
            lastMessage.put(p.getUniqueId(), now);
            plugin.messages().send(p, "clan.not-member", "clan", owner.name());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        lastMessage.remove(e.getPlayer().getUniqueId());
    }

    private static Player player(Entity damager) {
        if (damager instanceof Player p) return p;
        if (damager instanceof Projectile proj && proj.getShooter() instanceof Player p) return p;
        if (damager instanceof AreaEffectCloud cloud && cloud.getSource() instanceof Player p) return p;
        if (damager instanceof TNTPrimed tnt && tnt.getSource() instanceof Player p) return p;
        return null;
    }
}
