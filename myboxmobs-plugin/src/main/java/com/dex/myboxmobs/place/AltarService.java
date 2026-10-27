package com.dex.myboxmobs.place;

import com.dex.myboxmobs.MyBoxMobsPlugin;
import com.dex.myboxmobs.config.Messages;
import com.dex.myboxmobs.hook.ClanSupport;
import com.dex.myboxmobs.live.LiveMob;
import com.dex.myboxmobs.live.MobService;
import com.dex.myboxmobs.mob.MobDefinition;
import com.dex.myboxmobs.text.Format;
import com.dex.myboxmobs.text.Text;
import com.dex.myboxmobs.util.ItemNames;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.Nullable;

/** Summoning altars (spec 8). Main thread only. */
public final class AltarService implements MobService.EndListener {

    private static final String HOLO = "mbm_altar_";

    private final MyBoxMobsPlugin plugin;
    private final File file;
    private final Map<String, AltarDef> altars = new TreeMap<>();

    public AltarService(MyBoxMobsPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "altars.yml");
    }

    public void load() {
        clearHolograms();
        altars.clear();
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = y.getConfigurationSection("altars");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s != null) altars.put(id, AltarDef.load(id, s));
        }
        // 0.5.0: amounts go before the item ("x1 Trial Key") in the old default requirement line
        boolean migrated = false;
        for (AltarDef d : altars.values()) {
            for (int i = 0; i < d.holoLines.size(); i++) {
                String line = d.holoLines.get(i);
                if (line.contains("%item% &8x&f%amount%")) {
                    d.holoLines.set(i, line.replace("%item% &8x&f%amount%", "&fx%amount% %item%"));
                    migrated = true;
                }
            }
        }
        if (migrated) save();
    }

    public void save() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("config-version", 1);
        for (AltarDef d : altars.values()) d.save(y.createSection("altars." + d.id));
        try {
            y.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Could not save altars.yml: " + ex.getMessage());
        }
    }

    public Collection<AltarDef> all() {
        return altars.values();
    }

    public @Nullable AltarDef get(String id) {
        return id == null ? null : altars.get(id.toLowerCase());
    }

    public @Nullable AltarDef at(Block block) {
        for (AltarDef d : altars.values()) if (d.is(block)) return d;
        return null;
    }

    public AltarDef create(String id, Block block, MobRef mob) {
        AltarDef d = new AltarDef(id.toLowerCase(), block, mob, plugin.settings().altarHoloLines);
        altars.put(d.id, d);
        save();
        return d;
    }

    public void delete(AltarDef d) {
        altars.remove(d.id);
        plugin.holograms().remove(HOLO + d.id);
        save();
    }

    /** Right-click on an altar block (spec 8 steps 4-5). */
    public void use(Player p, AltarDef d) {
        Messages m = plugin.messages();
        if (!p.hasPermission("myboxmobs.altar.use")) {
            m.send(p, "altar.no-permission");
            return;
        }
        if (d.mob == null || d.requirement == null) {
            m.send(p, "altar.not-ready");
            return;
        }
        UUID ownerClan = null;
        if (d.clan) {
            ClanSupport clans = plugin.clans();
            ClanSupport.ClanInfo own = clans.clanOf(p.getUniqueId());
            if (!clans.available()) {
                m.send(p, "altar.clan-unavailable");
                return;
            }
            if (own == null || !clans.isLeader(p.getUniqueId())) {
                m.send(p, "altar.clan-leader-only");
                return;
            }
            ownerClan = own.uuid();
        }
        if (d.boss != null) {
            m.send(p, "altar.busy", "mob", plugin.spawners().mobName(d.mob));
            return;
        }
        long now = System.currentTimeMillis();
        if (d.cooldownUntil > now) {
            m.send(p, "altar.cooldown", "time", Format.duration((d.cooldownUntil - now + 999) / 1000));
            return;
        }
        PlayerInventory inv = p.getInventory();
        ItemStack hand = inv.getItemInMainHand();
        int need = Math.max(1, d.requirement.getAmount());
        if (!hand.isSimilar(d.requirement) || hand.getAmount() < need) {
            m.send(p, "altar.wrong-item", "item", ItemNames.of(d.requirement), "amount", String.valueOf(need));
            return;
        }
        Location loc = d.blockLocation();
        if (loc == null) return;
        Location spawnAt = loc.clone().add(0.5, 1, 0.5);
        LiveMob boss = spawn(d, spawnAt, p, ownerClan);
        if (boss == null) {
            m.send(p, "altar.failed");
            return;
        }
        hand.setAmount(hand.getAmount() - need);
        inv.setItemInMainHand(hand.getAmount() <= 0 ? null : hand);
        d.boss = boss.uuid();
        d.summoner = p.getUniqueId();
        d.bossWorld = spawnAt.getWorld().getName();
        d.bossX = spawnAt.getBlockX();
        d.bossZ = spawnAt.getBlockZ();
        save();
        plugin.holograms().remove(HOLO + d.id);
        String mob = plugin.spawners().mobName(d.mob);
        if (ownerClan == null) {
            m.send(p, "altar.summoned", "mob", mob);
            return;
        }
        for (Player member : plugin.clans().onlineMembers(ownerClan)) {
            m.send(member, "altar.clan-summoned", "player", p.getName(), "mob", mob, "world", spawnAt.getWorld().getName(),
                    "x", String.valueOf(spawnAt.getBlockX()), "y", String.valueOf(spawnAt.getBlockY()),
                    "z", String.valueOf(spawnAt.getBlockZ()));
        }
    }

    private @Nullable LiveMob spawn(AltarDef d, Location at, Player summoner, @Nullable UUID ownerClan) {
        String origin = "altar:" + d.id;
        if (d.mob.mythic()) return plugin.mobs().spawnRawMythic(d.mob.id(), at, origin, ownerClan);
        MobDefinition def = plugin.registry().get(d.mob.id());
        if (def == null) return null;
        return plugin.mobs().spawn(def, at, origin, summoner.getUniqueId(), ownerClan, true);
    }

    /**
     * {@code /mbm altar givekey}: the altar's summoning item (e.g. bought in the Minh Huy shop). {@code amount} items,
     * the overflow drops at the player's feet. Returns the number given, 0 when the altar has no requirement.
     */
    public int giveKey(AltarDef d, Player to, int amount) {
        if (d.requirement == null || amount <= 0) return 0;
        int max = Math.max(1, d.requirement.getMaxStackSize());
        int left = amount;
        while (left > 0) {
            ItemStack stack = d.requirement.clone();
            stack.setAmount(Math.min(left, max));
            left -= stack.getAmount();
            for (ItemStack rest : to.getInventory().addItem(stack).values()) {
                to.getWorld().dropItemNaturally(to.getLocation(), rest);
            }
        }
        return amount;
    }

    /**
     * The boss left (killed, lifetime over, removed by an admin): the altar opens again after its cooldown.
     * The summoning item is never given back (owner decision).
     */
    @Override
    public void onEnd(LiveMob mob, boolean killed) {
        if (!mob.origin.startsWith("altar:")) return;
        AltarDef d = get(mob.origin.substring(6));
        if (d == null || !mob.uuid().equals(d.boss)) return;
        free(d);
    }

    private void free(AltarDef d) {
        d.boss = null;
        d.summoner = null;
        d.bossWorld = null;
        d.bossMissing = 0;
        if (d.cooldownSeconds > 0) d.cooldownUntil = System.currentTimeMillis() + d.cooldownSeconds * 1000L;
        save();
    }

    /**
     * Follows each altar's boss: remembers where it is, and frees the altar when the boss is gone for good
     * (its last chunk is loaded but the boss is not there for 5 seconds). An unloaded boss keeps the altar busy.
     */
    private void watchBosses() {
        for (AltarDef d : altars.values()) {
            if (d.boss == null) continue;
            LiveMob mob = plugin.mobs().tracker().get(d.boss);
            if (mob != null && mob.entity.isValid()) {
                Location at = mob.entity.getLocation();
                d.bossWorld = at.getWorld().getName();
                d.bossX = at.getBlockX();
                d.bossZ = at.getBlockZ();
                d.bossMissing = 0;
                continue;
            }
            org.bukkit.World w = d.bossWorld == null ? null : Bukkit.getWorld(d.bossWorld);
            if (w == null || !w.isChunkLoaded(d.bossX >> 4, d.bossZ >> 4)) continue;
            if (++d.bossMissing >= 5) {
                plugin.getLogger().info("Altar " + d.id + ": its boss is gone, the altar is open again");
                free(d);
            }
        }
    }

    /** Every second: holograms (hidden while the boss lives, countdown during the cooldown). */
    public void tick() {
        watchBosses();
        if (!plugin.holograms().available()) return;
        long now = System.currentTimeMillis();
        for (AltarDef d : altars.values()) {
            Location loc = d.blockLocation();
            if (loc == null || !loc.isChunkLoaded() || d.boss != null || d.holoLines.isEmpty()) {
                plugin.holograms().remove(HOLO + d.id);
                continue;
            }
            boolean cooling = d.cooldownUntil > now;
            List<String> source = cooling ? plugin.settings().altarHoloCooldownLines : d.holoLines;
            String item = d.requirement == null ? "?" : ItemNames.of(d.requirement);
            String amount = d.requirement == null ? "0" : String.valueOf(d.requirement.getAmount());
            String mob = d.mob == null ? "?" : plugin.spawners().mobName(d.mob);
            String countdown = cooling ? Format.duration((d.cooldownUntil - now + 999) / 1000) : "";
            List<String> lines = new ArrayList<>();
            for (String line : source) {
                String filled = Messages.apply(line, "mob", mob, "item", item, "amount", amount,
                        "countdown", countdown, "id", d.id);
                lines.add(filled.startsWith("#") ? filled : Text.legacy(filled));
            }
            plugin.holograms().show(HOLO + d.id, loc.clone().add(0.5, plugin.settings().altarHoloHeight, 0.5), lines);
        }
    }

    public void clearHolograms() {
        for (AltarDef d : altars.values()) plugin.holograms().remove(HOLO + d.id);
    }
}
