package com.dex.myboxmobs.live;

import com.dex.myboxmobs.config.Messages;
import com.dex.myboxmobs.mob.MobDefinition;
import com.dex.myboxmobs.text.Format;
import com.dex.myboxmobs.text.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/** Whole-server chat announcements of spawns, deaths and despawns (spec 6). */
public final class Announcer {

    private final Supplier<String> topLine;
    private final Function<MobDefinition, String> names;

    public Announcer(Supplier<String> topLine, Function<MobDefinition, String> names) {
        this.topLine = topLine;
        this.names = names;
    }

    public void spawn(MobDefinition def, Location at, @Nullable UUID summoner) {
        if (!def.spawn.enabled) return;
        broadcast(def.spawn.lines, null, "mob", names.apply(def), "world", at.getWorld().getName(),
                "x", String.valueOf(at.getBlockX()), "y", String.valueOf(at.getBlockY()), "z", String.valueOf(at.getBlockZ()),
                "summoner", summoner == null ? "" : playerName(summoner));
    }

    public void death(MobDefinition def, @Nullable UUID killer, Map<UUID, Double> damage) {
        if (!def.death.enabled) return;
        List<String> top = new ArrayList<>();
        if (def.rewardMode == MobDefinition.RewardMode.TOP) {
            int rank = 0;
            for (Map.Entry<UUID, Double> e : damage.entrySet()) {
                if (++rank > def.topRanks.size()) break;
                top.add(Messages.apply(topLine.get(), "rank", String.valueOf(rank), "player", playerName(e.getKey()),
                        "damage", Format.number(Math.round(e.getValue()))));
            }
        }
        double killerDamage = killer == null ? 0 : damage.getOrDefault(killer, 0.0);
        broadcast(def.death.lines, top, "mob", names.apply(def), "killer", killer == null ? "?" : playerName(killer),
                "killer_damage", Format.number(Math.round(killerDamage)));
    }

    public void despawn(MobDefinition def) {
        if (!def.despawn.enabled) return;
        broadcast(def.despawn.lines, null, "mob", names.apply(def));
    }

    /** Sends the lines to one admin only (GUI preview), with sample values. */
    public void preview(CommandSender to, List<String> lines) {
        for (String line : lines) {
            if (line.trim().equals("%top%")) {
                to.sendMessage(Text.parse(Messages.apply(topLine.get(), "rank", "1", "player", to.getName(), "damage", "12.500")));
                continue;
            }
            to.sendMessage(Text.parse(Messages.apply(line, "mob", "Boss", "killer", to.getName(), "killer_damage", "12.500",
                    "world", "world", "x", "0", "y", "64", "z", "0", "summoner", to.getName())));
        }
    }

    private static String playerName(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) return online.getName();
        OfflinePlayer off = Bukkit.getOfflinePlayer(uuid);
        return off.getName() == null ? "?" : off.getName();
    }

    private static void broadcast(List<String> lines, @Nullable List<String> top, String... placeholders) {
        for (String line : lines) {
            if (line.trim().equals("%top%")) {
                if (top != null) for (String t : top) Bukkit.broadcast(Text.parse(t));
                continue;
            }
            Bukkit.broadcast(Text.parse(Messages.apply(line, placeholders)));
        }
    }
}
