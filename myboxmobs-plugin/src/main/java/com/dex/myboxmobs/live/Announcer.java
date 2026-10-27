package com.dex.myboxmobs.live;

import com.dex.myboxmobs.config.Messages;
import com.dex.myboxmobs.config.Settings;
import com.dex.myboxmobs.mob.MobDefinition;
import com.dex.myboxmobs.text.ChatCenter;
import com.dex.myboxmobs.text.Format;
import com.dex.myboxmobs.text.Text;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Whole-server chat announcements of spawns, deaths and despawns (spec 6). A line starting with {@code <center>}
 * is centred in the chat. A line containing only {@code %top%} becomes the damage leaderboard (top mode only):
 * the header of config.yml, then one line per rank coloured gold / silver / bronze / grey.
 */
public final class Announcer {

    private final Supplier<Settings> settings;
    private final Function<MobDefinition, String> names;

    public Announcer(Supplier<Settings> settings, Function<MobDefinition, String> names) {
        this.settings = settings;
        this.names = names;
    }

    public void spawn(MobDefinition def, Location at, @Nullable UUID summoner) {
        if (!def.spawn.enabled) return;
        send(Bukkit::broadcast, def.spawn.lines, null, "mob", names.apply(def), "world", at.getWorld().getName(),
                "x", String.valueOf(at.getBlockX()), "y", String.valueOf(at.getBlockY()), "z", String.valueOf(at.getBlockZ()),
                "summoner", summoner == null ? "" : playerName(summoner));
    }

    public void death(MobDefinition def, @Nullable UUID killer, Map<UUID, Double> damage) {
        if (!def.death.enabled) return;
        List<String> top = null;
        if (def.rewardMode == MobDefinition.RewardMode.TOP && !damage.isEmpty()) {
            Map<String, Double> named = new LinkedHashMap<>();
            for (Map.Entry<UUID, Double> e : damage.entrySet()) named.put(playerName(e.getKey()), e.getValue());
            top = topBlock(named, def.topRanks.size());
        }
        double killerDamage = killer == null ? 0 : damage.getOrDefault(killer, 0.0);
        send(Bukkit::broadcast, def.death.lines, top, "mob", names.apply(def),
                "killer", killer == null ? "?" : playerName(killer), "killer_damage", Format.number(Math.round(killerDamage)));
    }

    public void despawn(MobDefinition def) {
        if (!def.despawn.enabled) return;
        send(Bukkit::broadcast, def.despawn.lines, null, "mob", names.apply(def));
    }

    /** Sends the lines to one admin only (GUI preview), with sample values. */
    public void preview(CommandSender to, List<String> lines) {
        Map<String, Double> sample = new LinkedHashMap<>();
        sample.put(to.getName(), 12500.0);
        sample.put("NguoiChoi2", 8300.0);
        sample.put("NguoiChoi3", 4100.0);
        sample.put("NguoiChoi4", 950.0);
        send(to::sendMessage, lines, topBlock(sample, 4), "mob", "&#FFB000Boss", "killer", to.getName(),
                "killer_damage", "12.500", "world", "world", "x", "0", "y", "64", "z", "0", "summoner", to.getName());
    }

    /** Header lines + one line per rank, still containing %mob% (replaced with the other placeholders). */
    private List<String> topBlock(Map<String, Double> damage, int ranks) {
        Settings s = settings.get();
        List<String> out = new ArrayList<>(s.topHeader);
        int rank = 0;
        for (Map.Entry<String, Double> e : damage.entrySet()) {
            if (++rank > ranks) break;
            String color = rank <= s.rankColors.size() ? s.rankColors.get(rank - 1) : s.rankColorOthers;
            out.add(Messages.apply(s.topLine, "color", color, "icon", rankIcon(rank), "rank", String.valueOf(rank),
                    "player", e.getKey(), "damage", Format.number(Math.round(e.getValue()))));
        }
        return out;
    }

    /** ① ② ③ ... ⑳ like the CustomFishing competition, "(21)" after that. */
    static String rankIcon(int rank) {
        return rank >= 1 && rank <= 20 ? new String(Character.toChars(0x2460 + rank - 1)) : "(" + rank + ")";
    }

    private static String playerName(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) return online.getName();
        OfflinePlayer off = Bukkit.getOfflinePlayer(uuid);
        return off.getName() == null ? "?" : off.getName();
    }

    private static void send(Consumer<Component> out, List<String> lines, @Nullable List<String> top,
                             String... placeholders) {
        for (String line : lines) {
            if (line.trim().equals("%top%")) {
                if (top != null) for (String t : top) out.accept(render(t, placeholders));
                continue;
            }
            out.accept(render(line, placeholders));
        }
    }

    private static Component render(String raw, String... placeholders) {
        String line = Messages.apply(raw, placeholders);
        if (!line.startsWith(ChatCenter.TAG)) return Text.parse(line);
        Component body = Text.parse(line.substring(ChatCenter.TAG.length()));
        return Component.text(ChatCenter.padding(body)).append(body);
    }
}
