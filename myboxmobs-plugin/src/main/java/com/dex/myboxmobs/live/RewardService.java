package com.dex.myboxmobs.live;

import com.dex.myboxmobs.config.Messages;
import com.dex.myboxmobs.config.Settings;
import com.dex.myboxmobs.hook.Economy;
import com.dex.myboxmobs.mob.MobDefinition;
import com.dex.myboxmobs.mob.RewardEntry;
import com.dex.myboxmobs.mob.TopRank;
import com.dex.myboxmobs.text.Format;
import java.time.Duration;
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

/** Pays the rewards of a killed mob (spec 5): killer mode or top damage mode, multipliers, straight to inventory. */
public final class RewardService {

    private final Supplier<Settings> settings;
    private final Messages messages;
    private final Economy economy;
    private final Function<MobDefinition, String> names;
    private final Logger log;

    public RewardService(Supplier<Settings> settings, Messages messages, Economy economy,
                         Function<MobDefinition, String> names, Logger log) {
        this.settings = settings;
        this.messages = messages;
        this.economy = economy;
        this.names = names;
        this.log = log;
    }

    public void pay(MobDefinition def, UUID killer, Map<UUID, Double> damage) {
        if (def.rewardMode == MobDefinition.RewardMode.KILLER) {
            Player p = killer == null ? null : Bukkit.getPlayer(killer);
            if (p != null) give(p, def, def.killerRewards);
            return;
        }
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
            give(p, def, r.rewards);
        }
    }

    private void give(Player p, MobDefinition def, List<RewardEntry> entries) {
        Settings s = settings.get();
        boolean overflow = false;
        for (RewardEntry e : entries) {
            if (!roll(e.chance)) continue;
            long amount = e.kind == RewardEntry.Kind.COMMAND ? 1 : random(e.min, e.max);
            if (amount <= 0) continue;
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
