package com.dex.myboxmobs.live;

import com.dex.myboxmobs.config.Messages;
import com.dex.myboxmobs.config.Settings;
import com.dex.myboxmobs.hook.ClanSupport;
import com.dex.myboxmobs.hook.Economy;
import com.dex.myboxmobs.mob.ClanRank;
import com.dex.myboxmobs.mob.MobDefinition;
import com.dex.myboxmobs.mob.RewardEntry;
import com.dex.myboxmobs.mob.TopRank;
import com.dex.myboxmobs.text.Format;
import com.dex.myboxmobs.text.Text;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Pays the rewards of a killed mob (spec 5): killer, top damage, clan top or shared clan gifts, multipliers,
 * straight to inventory. Players offline at the death get nothing (owner decision).
 */
public final class RewardService {

    /** A rolled reward line: the same result can be given to several players (shared clan gifts). */
    private record Rolled(RewardEntry entry, long amount) {
    }

    private final Supplier<Settings> settings;
    private final Messages messages;
    private final Economy economy;
    private final Function<MobDefinition, String> names;
    private final Supplier<ClanSupport> clans;
    private final Logger log;

    public RewardService(Supplier<Settings> settings, Messages messages, Economy economy,
                         Function<MobDefinition, String> names, Supplier<ClanSupport> clans, Logger log) {
        this.settings = settings;
        this.messages = messages;
        this.economy = economy;
        this.names = names;
        this.clans = clans;
        this.log = log;
    }

    /** {@code clanBoard}: clans by damage in clan top mode, else null. */
    public void pay(MobDefinition def, @Nullable UUID killer, Map<UUID, Double> damage, @Nullable UUID ownerClan,
                    @Nullable List<ClanDamage.Entry> clanBoard) {
        if (def.rewardMode != MobDefinition.RewardMode.CLAN_SHARED) killerClanPoints(def, killer);
        switch (def.rewardMode) {
            case KILLER -> {
                Player p = killer == null ? null : Bukkit.getPlayer(killer);
                if (p != null) give(p, def, roll(def.killerRewards));
            }
            case TOP -> payTop(def, damage);
            case TOP_CLAN -> payClanTop(def, clanBoard == null ? List.of() : clanBoard);
            case CLAN_SHARED -> payShared(def, damage, ownerClan);
        }
    }

    private void payTop(MobDefinition def, Map<UUID, Double> damage) {
        int rank = 0;
        for (UUID uuid : damage.keySet()) {
            if (rank >= def.topRanks.size()) break;
            TopRank r = def.topRanks.get(rank++);
            // offline at death: this rank is simply not paid (owner decision), it does not pass down
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            if (!roll(r.chance)) {
                messages.send(p, "reward.top-missed", "rank", String.valueOf(rank), "mob", names.apply(def));
                continue;
            }
            messages.send(p, "reward.top-rank", "rank", String.valueOf(rank), "mob", names.apply(def));
            give(p, def, roll(r.rewards));
        }
    }

    /**
     * Clan top: each ranked clan gets its Danh Vọng and Minh Huy (BoxClans tells the clan about both), its online
     * members are told their rank, and members who dealt at least the minimum share get the member gifts.
     */
    private void payClanTop(MobDefinition def, List<ClanDamage.Entry> board) {
        ClanSupport c = clans.get();
        if (!c.available()) return;
        String mob = names.apply(def);
        int rank = 0;
        for (ClanDamage.Entry e : board) {
            if (rank >= def.clanRanks.size()) break;
            ClanRank r = def.clanRanks.get(rank++);
            String rankText = String.valueOf(rank);
            List<Player> online = c.onlineMembers(e.clan().uuid());
            if (!roll(r.chance)) {
                for (Player p : online) messages.send(p, "reward.clan-top-missed", "rank", rankText, "mob", mob);
                continue;
            }
            for (Player p : online) messages.send(p, "reward.clan-top-rank", "rank", rankText, "mob", mob);
            c.awardPoints(e.clan().uuid(), settings.get().clanSourceTop, r.danhVong);
            if (r.minhHuy > 0 && !c.giveCurrency(e.clan().uuid(), r.minhHuy, "Top " + rank + " " + Text.plain(mob))) {
                log.warning("Minh Huy reward of " + r.minhHuy + " for clan " + e.clan().uuid() + " failed");
            }
            for (UUID member : e.members().keySet()) {
                Player p = Bukkit.getPlayer(member);
                if (p == null || e.share(member) < def.clanMinShare) continue;
                give(p, def, roll(r.rewards));
            }
        }
    }

    /**
     * Shared clan gifts: rolled once, every recipient gets the same lines. Recipients are the online players who
     * dealt damage, or every online member of the boss's clan; a boss without a clan always uses the first.
     */
    private void payShared(MobDefinition def, Map<UUID, Double> damage, @Nullable UUID ownerClan) {
        List<Player> to = new ArrayList<>();
        ClanSupport c = clans.get();
        if (ownerClan != null && def.sharedRecipients == MobDefinition.SharedRecipients.ONLINE && c.clan(ownerClan) != null) {
            to.addAll(c.onlineMembers(ownerClan));
        } else {
            for (UUID uuid : damage.keySet()) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) to.add(p);
            }
        }
        List<Rolled> rolled = roll(def.sharedRewards);
        String mob = names.apply(def);
        for (Player p : to) {
            messages.send(p, "reward.clan-shared", "mob", mob);
            give(p, def, rolled);
        }
    }

    /** Danh Vọng for the killer's clan (BoxClans tells the clan). */
    private void killerClanPoints(MobDefinition def, @Nullable UUID killer) {
        if (def.killerClanPoints <= 0 || killer == null) return;
        ClanSupport c = clans.get();
        ClanSupport.ClanInfo clan = c.clanOf(killer);
        if (clan != null) c.awardPoints(clan.uuid(), settings.get().clanSourceKill, def.killerClanPoints);
    }

    private static List<Rolled> roll(List<RewardEntry> entries) {
        List<Rolled> out = new ArrayList<>();
        for (RewardEntry e : entries) {
            if (!roll(e.chance)) continue;
            long amount = e.kind == RewardEntry.Kind.COMMAND ? 1 : random(e.min, e.max);
            if (amount > 0) out.add(new Rolled(e, amount));
        }
        return out;
    }

    private void give(Player p, MobDefinition def, List<Rolled> rolled) {
        Settings s = settings.get();
        boolean overflow = false;
        for (Rolled r : rolled) {
            RewardEntry e = r.entry();
            long amount = r.amount();
            switch (e.kind) {
                case ITEM -> {
                    if (e.item == null) continue;
                    amount = multiply(amount, def, p, s.boostItems);
                    overflow |= giveItem(p, e.item, amount);
                    messages.sendWithItem(p, "reward.item", e.item.displayName(), "amount", Format.number(amount));
                }
                case MONEY -> {
                    amount = multiply(amount, def, p, s.boostMoney);
                    if (economy.depositMoney(p, amount)) {
                        messages.send(p, "reward.money", "amount", Format.number(amount));
                    } else {
                        log.warning("Money reward of " + amount + " for " + p.getName() + " failed (Vault missing?)");
                        messages.send(p, "reward.failed");
                    }
                }
                case GEM -> {
                    amount = multiply(amount, def, p, s.boostGem);
                    if (economy.depositGems(p, amount, "MyBoxMobs " + def.id)) {
                        messages.send(p, "reward.gem", "amount", Format.number(amount), "gem", economy.gemName());
                    } else {
                        log.warning("Gem reward of " + amount + " for " + p.getName() + " failed (BoxCore missing or not loaded)");
                        messages.send(p, "reward.failed");
                    }
                }
                case COMMAND -> {
                    if (!e.command.isBlank()) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), e.command.replace("%player%", p.getName()));
                    }
                }
            }
        }
        if (overflow) {
            p.showTitle(Title.title(messages.title("reward.full-title"), messages.title("reward.full-subtitle"),
                    Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(2), Duration.ofMillis(400))));
        }
    }

    /** Spec 5.4: BoxCore boost of the type, rounded down, never below the rolled amount. */
    private long multiply(long amount, MobDefinition def, Player p, String type) {
        if (!def.multiplierEnabled) return amount;
        int tenths = economy.boostTenths(p.getUniqueId(), type);
        if (tenths <= 10) return amount;
        long product = amount > Long.MAX_VALUE / tenths ? Long.MAX_VALUE : amount * tenths;
        return Math.max(amount, product / 10);
    }

    /** True when something did not fit and was dropped at the player's feet. */
    private static boolean giveItem(Player p, ItemStack template, long amount) {
        boolean dropped = false;
        int max = Math.max(1, template.getMaxStackSize());
        while (amount > 0) {
            ItemStack stack = template.clone();
            int n = (int) Math.min(amount, max);
            stack.setAmount(n);
            amount -= n;
            for (ItemStack left : p.getInventory().addItem(stack).values()) {
                p.getWorld().dropItemNaturally(p.getLocation(), left);
                dropped = true;
            }
        }
        return dropped;
    }

    private static boolean roll(double chance) {
        return chance >= 100.0 || ThreadLocalRandom.current().nextDouble(100.0) < chance;
    }

    private static long random(long min, long max) {
        if (max <= min) return min;
        return ThreadLocalRandom.current().nextLong(min, max + 1);
    }
}
