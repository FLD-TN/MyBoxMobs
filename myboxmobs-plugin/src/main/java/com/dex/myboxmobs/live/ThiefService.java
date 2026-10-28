package com.dex.myboxmobs.live;

import com.dex.myboxmobs.MyBoxMobsPlugin;
import com.dex.myboxmobs.hook.Economy;
import com.dex.myboxmobs.mob.ThiefSettings;
import com.dex.myboxmobs.place.Ground;
import com.dex.myboxmobs.place.SpawnerDef;
import com.dex.myboxmobs.place.SpawnerService;
import com.dex.myboxmobs.text.Format;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Special skills of MyBoxMobs mobs (not MythicMobs): the thief ({@link ThiefSettings}) and hit-based health.
 * Main thread only.
 */
public final class ThiefService implements Listener {

    private final MyBoxMobsPlugin plugin;

    public ThiefService(MyBoxMobsPlugin plugin) {
        this.plugin = plugin;
    }

    private static boolean isThief(LiveMob mob) {
        return mob != null && mob.def != null && mob.def.thief.enabled() && mob.thief != null;
    }

    /** A new thief: its bag grows with the players around it, the hunt starts. */
    public void start(LiveMob mob) {
        if (mob.def == null || !mob.def.thief.enabled()) return;
        ThiefSettings t = mob.def.thief;
        ThiefState s = new ThiefState();
        int players = playersAround(mob.entity, t.radius).size();
        s.bagMoney = t.stealMoney ? t.bagMoney + players * t.bonusMoney : 0;
        s.bagGem = t.stealGem ? t.bagGem + players * t.bonusGem : 0;
        s.huntUntil = mob.spawnedAt + t.huntSeconds * 1000L;
        mob.thief = s;
        speed(mob, t.huntSpeed);
    }

    /** A thief tracked again after an unload / restart: its saved bag, or a new one for an older mob. */
    public void adopt(LiveMob mob, @Nullable String saved) {
        if (mob.def == null || !mob.def.thief.enabled()) return;
        ThiefState s = ThiefState.parse(saved);
        if (s == null) {
            start(mob);
            return;
        }
        mob.thief = s;
        speed(mob, System.currentTimeMillis() < s.huntUntil ? mob.def.thief.huntSpeed : mob.def.thief.slowSpeed);
    }

    /** Every second: hunt (chase the closest player not robbed lately), then slow down. */
    public void tick() {
        long now = System.currentTimeMillis();
        for (LiveMob mob : plugin.mobs().tracker().all()) {
            if (!isThief(mob) || !(mob.entity instanceof LivingEntity le) || !le.isValid()) continue;
            ThiefState s = mob.thief;
            ThiefSettings t = mob.def.thief;
            if (now < s.huntUntil) {
                if (le instanceof Mob m) {
                    Player prey = prey(mob, t, now);
                    if (prey != null && m.getTarget() != prey) m.setTarget(prey);
                }
            } else if (!s.slowed) {
                s.slowed = true;
                speed(mob, t.slowSpeed);
                s.nextBlinkHealth = le.getHealth() - maxHealth(le) * t.blinkPercent / 100.0;
            }
        }
    }

    private @Nullable Player prey(LiveMob mob, ThiefSettings t, long now) {
        Player best = null;
        boolean bestFresh = false;
        double bestDist = Double.MAX_VALUE;
        for (Player p : playersAround(mob.entity, t.radius)) {
            Long last = mob.thief.lastSteal.get(p.getUniqueId());
            boolean fresh = last == null || now - last >= t.stealCooldown * 1000L;
            double d = p.getLocation().distanceSquared(mob.entity.getLocation());
            // players it can rob right now come first, then the closest
            if (best == null || (fresh && !bestFresh) || (fresh == bestFresh && d < bestDist)) {
                best = p;
                bestFresh = fresh;
                bestDist = d;
            }
        }
        return best;
    }

    private static List<Player> playersAround(Entity e, int radius) {
        List<Player> out = new ArrayList<>();
        double max = (double) radius * radius;
        for (Player p : e.getWorld().getPlayers()) {
            if (p.isDead() || p.getGameMode() == GameMode.SPECTATOR || p.getGameMode() == GameMode.CREATIVE) continue;
            if (p.getLocation().distanceSquared(e.getLocation()) <= max) out.add(p);
        }
        return out;
    }

    private static void speed(LiveMob mob, double multiplier) {
        if (!(mob.entity instanceof LivingEntity le) || mob.thief == null) return;
        AttributeInstance speed = le.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (speed == null) return;
        if (Double.isNaN(mob.thief.baseSpeed)) mob.thief.baseSpeed = speed.getBaseValue();
        speed.setBaseValue(mob.thief.baseSpeed * multiplier);
    }

    private static double maxHealth(LivingEntity le) {
        AttributeInstance max = le.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        return max == null ? le.getHealth() : max.getValue();
    }

    // ------------------------------------------------------------------ hit-based health

    /** Hit-based health: a player hit takes exactly 1 health, any other damage nothing (/kill and void still work). */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    @SuppressWarnings("deprecation")
    public void onHitHealth(EntityDamageEvent e) {
        LiveMob mob = plugin.mobs().tracker().get(e.getEntity().getUniqueId());
        if (mob == null || mob.def == null || !mob.def.hitHealth) return;
        EntityDamageEvent.DamageCause cause = e.getCause();
        if (cause == EntityDamageEvent.DamageCause.KILL || cause == EntityDamageEvent.DamageCause.VOID) return;
        if (!(e instanceof EntityDamageByEntityEvent by) || player(by.getDamager()) == null) {
            e.setCancelled(true);
            return;
        }
        for (EntityDamageEvent.DamageModifier m : EntityDamageEvent.DamageModifier.values()) {
            if (m != EntityDamageEvent.DamageModifier.BASE && e.isApplicable(m)) e.setDamage(m, 0);
        }
        e.setDamage(EntityDamageEvent.DamageModifier.BASE, 1.0);
    }

    // ------------------------------------------------------------------ steal / refund

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        Entity source = e.getDamager() instanceof Projectile p && p.getShooter() instanceof Entity shooter ? shooter : e.getDamager();
        LiveMob attacker = plugin.mobs().tracker().get(source.getUniqueId());
        if (isThief(attacker) && e.getEntity() instanceof Player victim) steal(attacker, victim);

        LiveMob victim = plugin.mobs().tracker().get(e.getEntity().getUniqueId());
        if (!isThief(victim)) return;
        Player hitter = player(e.getDamager());
        if (hitter != null) refund(victim, hitter);
        // the health has not dropped yet: look one tick later
        Bukkit.getScheduler().runTask(plugin, () -> blinkCheck(victim));
    }

    private static @Nullable Player player(Entity damager) {
        if (damager instanceof Player p) return p;
        if (damager instanceof Projectile proj && proj.getShooter() instanceof Player p) return p;
        return null;
    }

    private void steal(LiveMob mob, Player victim) {
        ThiefSettings t = mob.def.thief;
        ThiefState s = mob.thief;
        long now = System.currentTimeMillis();
        Long last = s.lastSteal.get(victim.getUniqueId());
        if (last != null && now - last < t.stealCooldown * 1000L) return;
        s.lastSteal.put(victim.getUniqueId(), now);
        Economy eco = plugin.economy();
        String name = plugin.mobs().displayName(mob.def);
        ThreadLocalRandom r = ThreadLocalRandom.current();
        if (t.stealMoney) {
            long amount = ThiefState.stealAmount(eco.moneyBalance(victim), roll(r, t.moneyMinPercent, t.moneyMaxPercent), t.moneyCap);
            if (amount > 0 && eco.withdrawMoney(victim, amount)) {
                s.stole(amount, 0);
                plugin.messages().send(victim, "thief.robbed-money", "mob", name, "amount", Format.number(amount));
            }
        }
        if (t.stealGem) {
            long amount = ThiefState.stealAmount(eco.gemBalance(victim), roll(r, t.gemMinPercent, t.gemMaxPercent), t.gemCap);
            if (amount > 0 && eco.withdrawGems(victim, amount, "MyBoxMobs " + mob.def.id + " (bị cướp)")) {
                s.stole(0, amount);
                plugin.messages().send(victim, "thief.robbed-gem", "mob", name, "amount", Format.number(amount),
                        "gem", eco.gemName());
            }
        }
        if (s.lastSteal.size() > 200) s.lastSteal.values().removeIf(v -> now - v > t.stealCooldown * 1000L);
    }

    private void refund(LiveMob mob, Player hitter) {
        ThiefSettings t = mob.def.thief;
        ThiefState s = mob.thief;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        long money = ThiefState.refundAmount(s.stolenMoney, s.leftMoney, roll(r, t.refundMinPercent, t.refundMaxPercent));
        long gem = ThiefState.refundAmount(s.stolenGem, s.leftGem, roll(r, t.refundMinPercent, t.refundMaxPercent));
        Economy eco = plugin.economy();
        if (money > 0 && eco.depositMoney(hitter, money)) s.leftMoney -= money;
        else money = 0;
        if (gem > 0 && eco.depositGems(hitter, gem, "MyBoxMobs " + mob.def.id + " (lấy lại)")) s.leftGem -= gem;
        else gem = 0;
        String items = items(money, gem);
        if (!items.isEmpty()) {
            plugin.messages().send(hitter, "thief.refund", "items", items, "mob", plugin.mobs().displayName(mob.def));
        }
    }

    /** "&a1.200$ &7và &#8245F812 Gem" (only the non-zero parts). */
    private String items(long money, long gem) {
        List<String> parts = new ArrayList<>();
        if (money > 0) parts.add(plugin.messages().raw("thief.money", "amount", Format.number(money)));
        if (gem > 0) parts.add(plugin.messages().raw("thief.gem", "amount", Format.number(gem), "gem", plugin.economy().gemName()));
        return String.join(plugin.messages().raw("thief.and"), parts);
    }

    /** The killer gets the bag and what is left of the stolen money and gems. */
    public void payKiller(LiveMob mob, @Nullable Player killer) {
        if (!isThief(mob) || killer == null) return;
        ThiefState s = mob.thief;
        long money = s.bagMoney + s.leftMoney;
        long gem = s.bagGem + s.leftGem;
        Economy eco = plugin.economy();
        if (money > 0 && !eco.depositMoney(killer, money)) money = 0;
        if (gem > 0 && !eco.depositGems(killer, gem, "MyBoxMobs " + mob.def.id + " (hạ gục)")) gem = 0;
        s.leftMoney = 0;
        s.leftGem = 0;
        String items = items(money, gem);
        if (!items.isEmpty()) {
            plugin.messages().send(killer, "thief.killer", "items", items, "mob", plugin.mobs().displayName(mob.def));
        }
    }

    // ------------------------------------------------------------------ teleport

    /** After the hunt: each lost part of the max health makes it jump about {@code blinkDistance} blocks away. */
    private void blinkCheck(LiveMob mob) {
        if (!isThief(mob) || mob.ended || !mob.thief.slowed || !(mob.entity instanceof LivingEntity le) || !le.isValid()) return;
        ThiefSettings t = mob.def.thief;
        ThiefState s = mob.thief;
        if (t.blinkPercent <= 0 || Double.isNaN(s.nextBlinkHealth) || le.getHealth() > s.nextBlinkHealth || le.isDead()) return;
        double step = maxHealth(le) * t.blinkPercent / 100.0;
        while (s.nextBlinkHealth >= le.getHealth()) s.nextBlinkHealth -= step;
        Location to = blinkTarget(mob, t.blinkDistance);
        if (to == null) return;
        Location from = le.getLocation();
        from.getWorld().spawnParticle(Particle.PORTAL, from.clone().add(0, 1, 0), 40, 0.4, 0.8, 0.4, 0.2);
        to.setYaw(from.getYaw());
        le.teleport(to);
        to.getWorld().spawnParticle(Particle.PORTAL, to.clone().add(0, 1, 0), 40, 0.4, 0.8, 0.4, 0.2);
        to.getWorld().playSound(to, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.2f);
    }

    private @Nullable Location blinkTarget(LiveMob mob, int distance) {
        Location from = mob.entity.getLocation();
        SpawnerDef area = mob.origin.startsWith("spawner:") ? plugin.spawners().get(mob.origin.substring(8)) : null;
        if (area != null && !area.hasRegion()) area = null;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = r.nextDouble(Math.PI * 2);
            double dist = distance * (0.6 + 0.4 * r.nextDouble());
            int x = (int) Math.floor(from.getX() + Math.cos(angle) * dist);
            int z = (int) Math.floor(from.getZ() + Math.sin(angle) * dist);
            if (area != null) {
                // stays in its spawner's area
                x = Math.max(area.minX(), Math.min(area.maxX(), x));
                z = Math.max(area.minZ(), Math.min(area.maxZ(), z));
            }
            Location at = Ground.find(from.getWorld(), x, z, from.getBlockY() + 5, from.getBlockY() - 6, 0);
            if (at != null && at.distanceSquared(from) >= 9) return at;
        }
        return area == null ? null : SpawnerService.regionPoint(area, from);
    }

    private static double roll(ThreadLocalRandom r, double min, double max) {
        return max <= min ? min : r.nextDouble(min, max);
    }
}
