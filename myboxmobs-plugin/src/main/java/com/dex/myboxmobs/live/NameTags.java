package com.dex.myboxmobs.live;

import com.dex.myboxmobs.MyBoxMobsPlugin;
import com.dex.myboxmobs.config.Messages;
import com.dex.myboxmobs.config.Settings;
import com.dex.myboxmobs.mob.MobDefinition;
import com.dex.myboxmobs.text.Text;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Display;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Health of MyBoxMobs mobs that have a name, both per mob and off by default:
 * <ul>
 * <li>name bar: "Kragan &8| ||||||||||" above the mob, red for the health left, grey for the health lost (empties
 * from right to left). A mob without a model shows it as its own name; a ModelEngine model hides its base entity
 * (and that name), so a text display follows the model at the height of its hitbox instead.</li>
 * <li>boss bar: the name and the health shown at the top of the screen of players nearby.</li>
 * </ul>
 * Main thread only.
 */
public final class NameTags implements Listener, MobService.EndListener {

    private final MyBoxMobsPlugin plugin;
    private final Map<UUID, TextDisplay> displays = new HashMap<>();
    /** Text last shown per mob, so the name is only sent again when it changes. */
    private final Map<UUID, String> shown = new HashMap<>();
    private final Set<UUID> dirty = new HashSet<>();
    private final Map<UUID, BossBar> bossBars = new HashMap<>();
    /** Players currently seeing each boss bar. */
    private final Map<UUID, Set<UUID>> viewers = new HashMap<>();
    private long ticks;

    public NameTags(MyBoxMobsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * {@code health} of {@code max} as {@code length} bar characters: full colour for what is left, then the empty
     * colour. Any health above 0 shows at least one full character.
     */
    public static String bar(double health, double max, int length, String full, String empty, String ch) {
        int filled = max <= 0 || health <= 0 ? 0 : (int) Math.ceil(Math.min(1.0, health / max) * length);
        return full + ch.repeat(filled) + empty + ch.repeat(length - filled);
    }

    /** Every tick: text displays follow their model; names of hit or healed mobs, and every second all of them. */
    public void tick() {
        boolean full = ++ticks % 20 == 0;
        List<UUID> ids = new ArrayList<>();
        if (full) {
            for (LiveMob mob : plugin.mobs().tracker().all()) ids.add(mob.uuid());
        } else {
            Set<UUID> some = new HashSet<>(dirty);
            some.addAll(displays.keySet());
            ids.addAll(some);
        }
        for (UUID id : ids) {
            LiveMob mob = plugin.mobs().tracker().get(id);
            if (mob == null) {
                remove(id);
                continue;
            }
            update(mob, full || dirty.contains(id), full);
        }
        dirty.clear();
    }

    private void update(LiveMob mob, boolean refresh, boolean full) {
        UUID id = mob.uuid();
        MobDefinition def = mob.def;
        if (def == null || !(mob.entity instanceof LivingEntity le) || !le.isValid()) {
            remove(id);
            return;
        }
        String name = plugin.mobs().nameOf(def);
        bossBar(mob, le, name, refresh, full);
        if (name == null || !def.nameBar) {
            // back to the plain name once the bar is turned off
            if (shown.containsKey(id) && name != null && !plugin.models().hasModel(le)) le.customName(Text.parse(name));
            removeDisplay(id);
            shown.remove(id);
            return;
        }
        String text = shown.get(id);
        if (refresh || text == null) text = text(name, le);
        boolean model = plugin.models().hasModel(le);
        if (!model) {
            removeDisplay(id);
            if (!text.equals(shown.get(id))) {
                le.customName(Text.parse(text));
                le.setCustomNameVisible(true);
                shown.put(id, text);
            }
            return;
        }
        double height = plugin.models().modelHeight(le);
        if (Double.isNaN(height)) height = le.getHeight();
        Location at = le.getLocation().add(0, height + def.nametagOffset, 0);
        at.setYaw(0);
        at.setPitch(0);
        TextDisplay display = displays.get(id);
        if (display == null || !display.isValid()) {
            final String first = text;
            display = at.getWorld().spawn(at, TextDisplay.class, d -> {
                d.setPersistent(false);
                d.setBillboard(Display.Billboard.CENTER);
                d.setTeleportDuration(2);
                d.text(Text.parse(first));
            });
            displays.put(id, display);
            shown.put(id, text);
            return;
        }
        if (!text.equals(shown.get(id))) {
            display.text(Text.parse(text));
            shown.put(id, text);
        }
        if (!display.getWorld().equals(at.getWorld()) || display.getLocation().distanceSquared(at) > 0.0001) {
            display.teleport(at);
        }
    }

    private String text(String name, LivingEntity le) {
        Settings s = plugin.settings();
        String bar = bar(le.getHealth(), maxHealth(le), s.barLength, s.barFull, s.barEmpty, s.barChar);
        return Messages.apply(s.nametagFormat, "name", name, "bar", bar);
    }

    private static double maxHealth(LivingEntity le) {
        AttributeInstance max = le.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        return max == null ? le.getHealth() : max.getValue();
    }

    /** Boss bar of one mob: progress on every refresh, viewers (players in range) once a second. */
    private void bossBar(LiveMob mob, LivingEntity le, @Nullable String name, boolean refresh, boolean full) {
        UUID id = mob.uuid();
        if (!mob.def.bossBar || name == null) {
            removeBossBar(id);
            return;
        }
        Settings s = plugin.settings();
        double max = maxHealth(le);
        float progress = max <= 0 ? 0f : (float) Math.max(0, Math.min(1, le.getHealth() / max));
        BossBar bar = bossBars.get(id);
        boolean created = bar == null;
        if (created) {
            bar = BossBar.bossBar(Text.parse(Messages.apply(s.bossBarTitle, "name", name)), progress,
                    s.bossBarColor, s.bossBarOverlay);
            bossBars.put(id, bar);
        } else if (refresh) {
            bar.progress(progress);
            bar.name(Text.parse(Messages.apply(s.bossBarTitle, "name", name)));
        }
        if (!created && !full) return;
        Set<UUID> seeing = viewers.computeIfAbsent(id, k -> new HashSet<>());
        double range = (double) s.bossBarRange * s.bossBarRange;
        Set<UUID> now = new HashSet<>();
        for (Player p : le.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(le.getLocation()) <= range) now.add(p.getUniqueId());
        }
        for (UUID v : seeing) {
            if (now.contains(v)) continue;
            Player p = Bukkit.getPlayer(v);
            if (p != null) p.hideBossBar(bar);
        }
        for (UUID v : now) {
            if (seeing.contains(v)) continue;
            Player p = Bukkit.getPlayer(v);
            if (p != null) p.showBossBar(bar);
        }
        seeing.clear();
        seeing.addAll(now);
    }

    private void removeBossBar(UUID mob) {
        BossBar bar = bossBars.remove(mob);
        Set<UUID> seeing = viewers.remove(mob);
        if (bar == null || seeing == null) return;
        for (UUID v : seeing) {
            Player p = Bukkit.getPlayer(v);
            if (p != null) p.hideBossBar(bar);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (plugin.mobs().tracker().get(e.getEntity().getUniqueId()) != null) dirty.add(e.getEntity().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHeal(EntityRegainHealthEvent e) {
        if (plugin.mobs().tracker().get(e.getEntity().getUniqueId()) != null) dirty.add(e.getEntity().getUniqueId());
    }

    /** A new or adopted mob: shown on the next tick. */
    public void added(UUID mob) {
        dirty.add(mob);
    }

    @Override
    public void onEnd(LiveMob mob, boolean killed) {
        remove(mob.uuid());
    }

    public void remove(UUID mob) {
        removeDisplay(mob);
        removeBossBar(mob);
        shown.remove(mob);
        dirty.remove(mob);
    }

    private void removeDisplay(UUID mob) {
        TextDisplay d = displays.remove(mob);
        if (d != null && d.isValid()) d.remove();
    }

    /** Plugin disable: every text display goes (they are never saved with the world anyway). */
    public void clear() {
        for (TextDisplay d : displays.values()) if (d.isValid()) d.remove();
        displays.clear();
        for (UUID mob : List.copyOf(bossBars.keySet())) removeBossBar(mob);
        shown.clear();
        dirty.clear();
    }
}
