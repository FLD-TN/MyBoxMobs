package com.dex.myboxmobs.live;

import com.dex.myboxmobs.config.Messages;
import com.dex.myboxmobs.config.Settings;
import com.dex.myboxmobs.hook.ClanSupport;
import com.dex.myboxmobs.mob.MobDefinition;
import com.dex.myboxmobs.text.ChatCenter;
import com.dex.myboxmobs.text.Format;
import com.dex.myboxmobs.text.Text;
import java.util.ArrayList;
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
 * Chat announcements of spawns, deaths and despawns (spec 6), to the whole server or, for a clan boss, to the
 * online members of its clan only. A line starting with {@code <center>} is centred in the chat. A line containing
 * only {@code %top%} becomes the damage leaderboard (every mode but killer): the header of config.yml, then one line
 * per rank coloured gold / silver / bronze / grey. In clan top mode {@code %top%} is followed by the clan board,
 * unless the lines place it themselves with {@code %top_clans%}.
 */
public final class Announcer {

    private final Supplier<Settings> settings;
    private final Function<MobDefinition, String> names;
    private final Supplier<ClanSupport> clans;

    public Announcer(Supplier<Settings> settings, Function<MobDefinition, String> names, Supplier<ClanSupport> clans) {
        this.settings = settings;
        this.names = names;
        this.clans = clans;
    }

    public void spawn(MobDefinition def, Location at, @Nullable UUID summoner, @Nullable UUID ownerClan) {
        if (!def.spawn.enabled) return;
        send(audience(ownerClan), def.spawn.lines, null, null, "mob", names.apply(def), "world", at.getWorld().getName(),
                "x", String.valueOf(at.getBlockX()), "y", String.valueOf(at.getBlockY()), "z", String.valueOf(at.getBlockZ()),
                "summoner", summoner == null ? "" : playerName(summoner));
    }

    /** {@code clanBoard}: clans by damage in clan top mode, else null. */
    public void death(MobDefinition def, @Nullable UUID killer, Map<UUID, Double> damage, @Nullable UUID ownerClan,
                      @Nullable List<ClanDamage.Entry> clanBoard) {
        if (!def.death.enabled) return;
        Settings s = settings.get();
        List<String> top = null;
        if (def.rewardMode.playerBoard() && !damage.isEmpty()) {
            List<Row> rows = new ArrayList<>();
            for (Map.Entry<UUID, Double> e : damage.entrySet()) rows.add(new Row(playerName(e.getKey()), e.getValue(), 1));
            int ranks = def.rewardMode == MobDefinition.RewardMode.TOP ? def.topRanks.size() : s.boardSize;
            top = board(s.topHeader, s.topLine, rows, ranks);
        }
        List<String> clanTop = null;
        if (clanBoard != null && !clanBoard.isEmpty()) {
            List<Row> rows = new ArrayList<>();
            for (ClanDamage.Entry e : clanBoard) rows.add(new Row(e.clan().name(), e.total(), e.members().size()));
            clanTop = board(s.topClanHeader, s.topClanLine, rows, def.clanRanks.isEmpty() ? s.boardSize : def.clanRanks.size());
        }
        double killerDamage = killer == null ? 0 : damage.getOrDefault(killer, 0.0);
        send(audience(ownerClan), def.death.lines, top, clanTop, "mob", names.apply(def),
                "killer", killer == null ? "?" : playerName(killer), "killer_damage", Format.number(Math.round(killerDamage)));
    }

    public void despawn(MobDefinition def, @Nullable UUID ownerClan) {
        if (!def.despawn.enabled) return;
        send(audience(ownerClan), def.despawn.lines, null, null, "mob", names.apply(def));
    }

    /** Sends the lines to one admin only (GUI preview), with sample values and both boards. */
    public void preview(CommandSender to, List<String> lines) {
        Settings s = settings.get();
        List<Row> players = List.of(new Row(to.getName(), 12500, 1), new Row("NguoiChoi2", 8300, 1),
                new Row("NguoiChoi3", 4100, 1), new Row("NguoiChoi4", 950, 1));
        List<Row> clanRows = List.of(new Row("&#FF6B6BHuyetLong", 16800, 3), new Row("&#9AD0F5ThienBang", 9050, 2));
        send(to::sendMessage, lines, board(s.topHeader, s.topLine, players, 4),
                board(s.topClanHeader, s.topClanLine, clanRows, 3), "mob", "&#FFB000Boss", "killer", to.getName(),
                "killer_damage", "12.500", "world", "world", "x", "0", "y", "64", "z", "0", "summoner", to.getName());
    }

    /** Whole server, or the online members of the boss's clan. */
    private Consumer<Component> audience(@Nullable UUID ownerClan) {
        ClanSupport c = clans.get();
        if (ownerClan == null || c.clan(ownerClan) == null) return Bukkit::broadcast;
        return line -> {
            for (Player p : c.onlineMembers(ownerClan)) p.sendMessage(line);
            Bukkit.getConsoleSender().sendMessage(line);
        };
    }

    /** One board line: a player (members 1) or a clan. */
    private record Row(String name, double damage, int members) {
    }

    /** Header lines + one line per rank, still containing %mob% (replaced with the other placeholders). */
    private List<String> board(List<String> header, String template, List<Row> rows, int ranks) {
        Settings s = settings.get();
        List<String> out = new ArrayList<>(header);
        int rank = 0;
        for (Row r : rows) {
            if (++rank > ranks) break;
            String color = rank <= s.rankColors.size() ? s.rankColors.get(rank - 1) : s.rankColorOthers;
            out.add(Messages.apply(template, "color", color, "icon", rankIcon(rank), "rank", String.valueOf(rank),
                    "player", r.name(), "clan", r.name(), "members", String.valueOf(r.members()),
                    "damage", Format.number(Math.round(r.damage()))));
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
                             @Nullable List<String> clanTop, String... placeholders) {
        boolean clansPlaced = lines.stream().anyMatch(l -> l.trim().equals("%top_clans%"));
        for (String line : lines) {
            String t = line.trim();
            if (t.equals("%top%") || t.equals("%top_clans%")) {
                List<String> block = new ArrayList<>();
                if (t.equals("%top%") && top != null) block.addAll(top);
                if ((t.equals("%top_clans%") || !clansPlaced) && clanTop != null) block.addAll(clanTop);
                for (String b : block) out.accept(render(b, placeholders));
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
