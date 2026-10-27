package com.dex.myboxmobs.live;

import com.dex.myboxmobs.MyBoxMobsPlugin;
import com.dex.myboxmobs.api.event.MbmMobDeathEvent;
import com.dex.myboxmobs.api.event.MbmMobDespawnEvent;
import com.dex.myboxmobs.api.event.MbmMobSpawnEvent;
import com.dex.myboxmobs.mob.MobDefinition;
import com.dex.myboxmobs.mob.SkillEntry;
import com.dex.myboxmobs.text.Text;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

/** Spawning, overrides, lifetime and the end of every tracked mob (spec 3, 4). Main thread only. */
public final class MobService {

    /** Told when a tracked mob is gone (spawners refill, altars reopen). */
    public interface EndListener {
        void onEnd(LiveMob mob, boolean killed);
    }

    private final MyBoxMobsPlugin plugin;
    private final Tracker tracker = new Tracker();
    private final NamespacedKey defKey;
    private final NamespacedKey originKey;
    private final List<EndListener> endListeners = new ArrayList<>();

    public MobService(MyBoxMobsPlugin plugin) {
        this.plugin = plugin;
        this.defKey = new NamespacedKey(plugin, "def");
        this.originKey = new NamespacedKey(plugin, "origin");
    }

    public Tracker tracker() {
        return tracker;
    }

    public void addEndListener(EndListener listener) {
        endListeners.add(listener);
    }

    /** Name shown for a definition: its own name, else the MythicMobs name, else the id. */
    public String displayName(MobDefinition def) {
        if (!def.displayName.isEmpty()) return def.displayName;
        if (def.source == MobDefinition.Source.MYTHIC) {
            String mythic = plugin.mythic().displayName(def.mythicId);
            if (!mythic.isEmpty()) return mythic;
        }
        return def.id;
    }

    /**
     * Spawns a definition. {@code announce} false for spawner waves after the first mob (spec 6: once per wave).
     * {@code null} when the spawn failed or a listener cancelled it.
     */
    public @Nullable LiveMob spawn(MobDefinition def, Location at, String origin, @Nullable UUID summoner,
                                   boolean announce) {
        if (at.getWorld() == null) return null;
        Entity entity;
        if (def.source == MobDefinition.Source.MYTHIC) {
            if (!plugin.mythic().exists(def.mythicId)) {
                plugin.getLogger().warning("Mob " + def.id + ": MythicMobs mob '" + def.mythicId + "' does not exist");
                return null;
            }
            entity = plugin.mythic().spawn(def.mythicId, at);
            if (entity != null) tag(entity, def.id, origin);
        } else {
            EntityType type = def.entityType;
            if (!type.isSpawnable() || !type.isAlive()) return null;
            entity = at.getWorld().spawnEntity(at, type, CreatureSpawnEvent.SpawnReason.CUSTOM,
                    e -> tag(e, def.id, origin));
        }
        if (entity == null) return null;
        apply(def, entity);
        LiveMob live = new LiveMob(entity, def, def.id, origin, summoner, System.currentTimeMillis());
        tracker.add(live);
        MbmMobSpawnEvent event = new MbmMobSpawnEvent(entity, def.id, origin, summoner);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            live.ended = true;
            tracker.remove(entity.getUniqueId());
            entity.remove();
            return null;
        }
        attachModel(def, entity);
        if (announce) plugin.announcer().spawn(def, entity.getLocation(), summoner);
        long now = System.currentTimeMillis();
        for (SkillEntry e : def.skills) if (e.trigger == SkillEntry.Trigger.TIMER) live.lastCast.put(e, now);
        // after MythicMobs' own spawn skills and the model
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!live.ended && entity.isValid()) castSkills(live, SkillEntry.Trigger.SPAWN, null);
        }, 5L);
        return live;
    }

    /**
     * Casts the definition's MythicMobs skills of this trigger (spec: skill settings). {@code triggerEntity} is the
     * attacker (DAMAGED) or the victim (ATTACK), null otherwise.
     */
    public void castSkills(LiveMob mob, SkillEntry.Trigger trigger, @Nullable Entity triggerEntity) {
        if (mob.def == null || mob.def.skills.isEmpty()) return;
        if (casting) return;
        casting = true;
        try {
            castSkillsNow(mob, trigger, triggerEntity);
        } finally {
            casting = false;
        }
    }

    /** True while MyBoxMobs is casting: damage dealt by that skill does not trigger more skills right away. */
    public boolean isCasting() {
        return casting;
    }

    private boolean casting;

    private void castSkillsNow(LiveMob mob, SkillEntry.Trigger trigger, @Nullable Entity triggerEntity) {
        long now = System.currentTimeMillis();
        for (SkillEntry e : mob.def.skills) {
            if (e.trigger != trigger) continue;
            Long last = mob.lastCast.get(e);
            if (trigger == SkillEntry.Trigger.TIMER) {
                if (last != null && now - last < Math.max(1, e.seconds) * 1000L) continue;
            } else if (e.seconds > 0 && last != null && now - last < e.seconds * 1000L) {
                continue;
            }
            mob.lastCast.put(e, now);
            if (e.chance < 100.0 && java.util.concurrent.ThreadLocalRandom.current().nextDouble(100.0) >= e.chance) continue;
            Entity target = switch (e.target) {
                case SELF -> mob.entity;
                case TRIGGER -> triggerEntity;
                case NEAREST_PLAYER -> nearestPlayer(mob.entity, 32);
                case TARGET -> mob.entity instanceof org.bukkit.entity.Mob m && m.getTarget() != null
                        ? m.getTarget() : triggerEntity;
            };
            try {
                if (!plugin.mythic().castSkill(mob.entity, e.skill, triggerEntity, target)) {
                    warnSkill(mob, e, "MythicMobs did not cast it (unknown skill?)");
                }
            } catch (RuntimeException ex) {
                warnSkill(mob, e, ex.toString());
            }
        }
    }

    private final java.util.Set<String> skillWarnings = new java.util.HashSet<>();

    private void warnSkill(LiveMob mob, SkillEntry e, String why) {
        if (skillWarnings.add(mob.id + "/" + e.skill)) {
            plugin.getLogger().warning("Mob " + mob.id + ": skill '" + e.skill + "' failed: " + why + " (reported once)");
        }
    }

    private static @Nullable Entity nearestPlayer(Entity from, double radius) {
        Entity best = null;
        double bestDist = radius * radius;
        for (org.bukkit.entity.Player p : from.getWorld().getPlayers()) {
            if (p.isDead() || p.getGameMode() == org.bukkit.GameMode.SPECTATOR) continue;
            double d = p.getLocation().distanceSquared(from.getLocation());
            if (d <= bestDist) {
                bestDist = d;
                best = p;
            }
        }
        return best;
    }

    /** A raw MythicMobs mob of a spawner: tracked for max-alive and leash only (spec 7). */
    public @Nullable LiveMob spawnRawMythic(String mythicId, Location at, String origin) {
        if (!plugin.mythic().exists(mythicId)) return null;
        Entity entity = plugin.mythic().spawn(mythicId, at);
        if (entity == null) return null;
        entity.getPersistentDataContainer().set(originKey, PersistentDataType.STRING, origin);
        entity.setPersistent(false);
        LiveMob live = new LiveMob(entity, null, "mythic:" + mythicId, origin, null, System.currentTimeMillis());
        tracker.add(live);
        return live;
    }

    private void tag(Entity e, String defId, String origin) {
        PersistentDataContainer pdc = e.getPersistentDataContainer();
        pdc.set(defKey, PersistentDataType.STRING, defId);
        pdc.set(originKey, PersistentDataType.STRING, origin);
        e.setPersistent(false);
        if (e instanceof LivingEntity le) le.setRemoveWhenFarAway(false);
    }

    /** True for entities tagged by MyBoxMobs, alive or not (also leftovers of a crash). */
    public boolean isTagged(Entity e) {
        return e.getPersistentDataContainer().has(originKey, PersistentDataType.STRING);
    }

    /** True for managed mobs (not raw spawner mobs): their own drops are replaced. */
    public boolean hasDefinitionTag(Entity e) {
        return e.getPersistentDataContainer().has(defKey, PersistentDataType.STRING);
    }

    private void apply(MobDefinition def, Entity entity) {
        if (!def.displayName.isEmpty()) {
            entity.customName(Text.parse(def.displayName));
            entity.setCustomNameVisible(true);
            if (def.source == MobDefinition.Source.MYTHIC) plugin.mythic().setDisplayName(entity, Text.legacy(def.displayName));
        }
        if (!(entity instanceof LivingEntity le)) return;
        for (var e : def.stats.entrySet()) {
            Attribute attribute = switch (e.getKey()) {
                case HEALTH -> Attribute.GENERIC_MAX_HEALTH;
                case ARMOR -> Attribute.GENERIC_ARMOR;
                case TOUGHNESS -> Attribute.GENERIC_ARMOR_TOUGHNESS;
                case DAMAGE -> Attribute.GENERIC_ATTACK_DAMAGE;
                case SPEED -> Attribute.GENERIC_MOVEMENT_SPEED;
                case FOLLOW_RANGE -> Attribute.GENERIC_FOLLOW_RANGE;
                case KNOCKBACK -> Attribute.GENERIC_KNOCKBACK_RESISTANCE;
            };
            AttributeInstance instance = le.getAttribute(attribute);
            if (instance == null) continue;
            double value = e.getValue();
            if (e.getKey() == MobDefinition.Stat.HEALTH) value = Math.min(value, plugin.settings().maxHealth);
            instance.setBaseValue(value);
            if (e.getKey() == MobDefinition.Stat.HEALTH) le.setHealth(Math.max(1, instance.getValue()));
        }
        EntityEquipment eq = le.getEquipment();
        if (eq != null) {
            for (EquipmentSlot slot : MobDefinition.SLOTS) {
                ItemStack item = def.equipment.get(slot);
                if (item != null) eq.setItem(slot, item.clone());
                eq.setDropChance(slot, 0f);
            }
        }
    }

    private void attachModel(MobDefinition def, Entity entity) {
        if (def.modelId.isEmpty() || !plugin.models().available()) return;
        if (def.source == MobDefinition.Source.CUSTOM) {
            plugin.models().apply(entity, def.modelId, def.modelScale, def.hideBase, true);
            return;
        }
        // MythicMobs attaches its own model in its spawn skills: look two ticks later (spec 3.2)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!entity.isValid()) return;
            boolean has = plugin.models().hasModel(entity);
            if (has && def.modelMode == MobDefinition.ModelMode.ONLY_IF_NONE) return;
            plugin.models().apply(entity, def.modelId, def.modelScale, def.hideBase,
                    def.modelMode == MobDefinition.ModelMode.REPLACE);
        }, 2L);
    }

    /** Spec 4: a counted hit on a tracked mob. */
    public void recordDamage(LiveMob mob, UUID player, double finalDamage) {
        if (!(mob.entity instanceof LivingEntity le) || le.isInvulnerable()) return;
        mob.addDamage(player, Math.min(finalDamage, le.getHealth()));
    }

    /** Called from EntityDeathEvent for a tracked mob. Drops and XP are already handled by the listener. */
    public void handleDeath(LiveMob mob) {
        if (mob.ended) return;
        mob.ended = true;
        tracker.remove(mob.uuid());
        MobDefinition def = mob.def;
        if (def != null) {
            castSkills(mob, SkillEntry.Trigger.DEATH, null);
            LinkedHashMap<UUID, Double> damage = mob.sortedDamage();
            UUID killer = mob.lastHitter;
            MbmMobDeathEvent event = new MbmMobDeathEvent(mob.entity, def.id, killer, damage);
            Bukkit.getPluginManager().callEvent(event);
            if (killer != null) plugin.kills().addKill(killer, def.id);
            if (!event.isRewardsCancelled()) plugin.rewards().pay(def, killer, damage);
            plugin.announcer().death(def, killer, damage);
        }
        for (EndListener l : endListeners) l.onEnd(mob, true);
    }

    /** The mob leaves without being killed (spec 3.4). */
    public void end(LiveMob mob, MbmMobDespawnEvent.Reason reason) {
        if (mob.ended) return;
        mob.ended = true;
        tracker.remove(mob.uuid());
        Bukkit.getPluginManager().callEvent(new MbmMobDespawnEvent(mob.entity, mob.id, reason));
        if (reason == MbmMobDespawnEvent.Reason.LIFETIME && mob.def != null) plugin.announcer().despawn(mob.def);
        if (mob.entity.isValid()) mob.entity.remove();
        for (EndListener l : endListeners) l.onEnd(mob, false);
    }

    /** Every second: lifetimes and mobs that vanished without an event. */
    public void tick() {
        long now = System.currentTimeMillis();
        for (LiveMob mob : tracker.all()) {
            if (!mob.entity.isValid() && !mob.entity.isDead()) {
                end(mob, MbmMobDespawnEvent.Reason.REMOVED);
            } else if (mob.expiresAt > 0 && now >= mob.expiresAt) {
                end(mob, MbmMobDespawnEvent.Reason.LIFETIME);
            } else {
                castSkills(mob, SkillEntry.Trigger.TIMER, null);
            }
        }
    }

    /** {@code /mbm kill}: tracked mobs whose id matches (or all when id is null), optionally in one world. */
    public int kill(@Nullable String id, @Nullable World world) {
        int n = 0;
        for (LiveMob mob : tracker.all()) {
            if (id != null && !mob.id.equalsIgnoreCase(id)) continue;
            if (world != null && !mob.entity.getWorld().equals(world)) continue;
            end(mob, MbmMobDespawnEvent.Reason.COMMAND);
            n++;
        }
        return n;
    }

    /** Leftovers from a crash in loaded chunks (spec 3.3). */
    public int removeOrphans(Iterable<Entity> entities) {
        int n = 0;
        for (Entity e : entities) {
            if (isTagged(e) && tracker.get(e.getUniqueId()) == null) {
                e.remove();
                n++;
            }
        }
        return n;
    }

    /** Plugin disable: every tracked mob is removed (they are not persistent anyway). */
    public void shutdown() {
        for (LiveMob mob : tracker.all()) end(mob, MbmMobDespawnEvent.Reason.REMOVED);
    }
}
