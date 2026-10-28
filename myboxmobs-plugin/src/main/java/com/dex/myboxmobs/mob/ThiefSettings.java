package com.dex.myboxmobs.mob;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * "Kẻ trộm" special skill of a mob (e.g. a goblin): steals money (Vault) and / or gems (BoxCore) from the players it
 * hits, hunts players fast for a while after spawning, then slows down and teleports away each time it loses a part
 * of its health. Players hitting it get back a little of what it stole; the killer gets its bag (base + a bonus per
 * player nearby at spawn) and what is left of the stolen money. Everything off by default.
 */
public final class ThiefSettings {

    public boolean stealMoney;
    public boolean stealGem;
    /** Percent of the victim's balance taken per hit, random between min and max, capped. */
    public double moneyMinPercent = 1;
    public double moneyMaxPercent = 3;
    public long moneyCap = 50_000;
    public double gemMinPercent = 1;
    public double gemMaxPercent = 3;
    public long gemCap = 500;
    /** After the hunt: each hit robs every player within this many blocks of the mob (not only the one hit). */
    public int areaRadius = 5;
    /** Seconds before the same player can be robbed again. */
    public int stealCooldown = 5;

    /** Bag the killer gets: base + bonus per player within {@link #radius} when the mob spawns. */
    public long bagMoney = 50_000;
    public long bagGem = 500;
    public long bonusMoney = 10_000;
    public long bonusGem = 15;
    /** Blocks around the mob where it looks for players (hunt) and counts them (bag bonus). */
    public int radius = 24;

    /** Hunt phase after spawning: fast and going after players. */
    public int huntSeconds = 60;
    public double huntSpeed = 1.8;
    /** Speed multiplier after the hunt. */
    public double slowSpeed = 0.7;
    /** Teleports each time it loses this percent of its max health (from spawning on), this far away. */
    public double blinkPercent = 10;
    public int blinkDistance = 10;

    /** Each player hit gives back a random percent (of everything stolen so far) of the stolen money / gems. */
    public double refundMinPercent = 1;
    public double refundMaxPercent = 3;

    public boolean enabled() {
        return stealMoney || stealGem;
    }

    public void save(YamlConfiguration y, String path) {
        y.set(path + ".steal-money", stealMoney);
        y.set(path + ".steal-gem", stealGem);
        y.set(path + ".money.min-percent", moneyMinPercent);
        y.set(path + ".money.max-percent", moneyMaxPercent);
        y.set(path + ".money.cap", moneyCap);
        y.set(path + ".gem.min-percent", gemMinPercent);
        y.set(path + ".gem.max-percent", gemMaxPercent);
        y.set(path + ".gem.cap", gemCap);
        y.set(path + ".steal-cooldown", stealCooldown);
        y.set(path + ".area-radius", areaRadius);
        y.set(path + ".bag.money", bagMoney);
        y.set(path + ".bag.gem", bagGem);
        y.set(path + ".bag.bonus-money", bonusMoney);
        y.set(path + ".bag.bonus-gem", bonusGem);
        y.set(path + ".radius", radius);
        y.set(path + ".hunt.seconds", huntSeconds);
        y.set(path + ".hunt.speed", huntSpeed);
        y.set(path + ".slow-speed", slowSpeed);
        y.set(path + ".blink.percent", blinkPercent);
        y.set(path + ".blink.distance", blinkDistance);
        y.set(path + ".refund.min-percent", refundMinPercent);
        y.set(path + ".refund.max-percent", refundMaxPercent);
    }

    /** Missing values keep the defaults above (older mob files). */
    public static ThiefSettings load(ConfigurationSection s) {
        ThiefSettings t = new ThiefSettings();
        if (s == null) return t;
        t.stealMoney = s.getBoolean("steal-money", false);
        t.stealGem = s.getBoolean("steal-gem", false);
        t.moneyMinPercent = percent(s.getDouble("money.min-percent", t.moneyMinPercent));
        t.moneyMaxPercent = Math.max(t.moneyMinPercent, percent(s.getDouble("money.max-percent", t.moneyMaxPercent)));
        t.moneyCap = Math.max(0, s.getLong("money.cap", t.moneyCap));
        t.gemMinPercent = percent(s.getDouble("gem.min-percent", t.gemMinPercent));
        t.gemMaxPercent = Math.max(t.gemMinPercent, percent(s.getDouble("gem.max-percent", t.gemMaxPercent)));
        t.gemCap = Math.max(0, s.getLong("gem.cap", t.gemCap));
        t.stealCooldown = Math.max(0, s.getInt("steal-cooldown", t.stealCooldown));
        t.areaRadius = Math.max(1, Math.min(32, s.getInt("area-radius", t.areaRadius)));
        t.bagMoney = Math.max(0, s.getLong("bag.money", t.bagMoney));
        t.bagGem = Math.max(0, s.getLong("bag.gem", t.bagGem));
        t.bonusMoney = Math.max(0, s.getLong("bag.bonus-money", t.bonusMoney));
        t.bonusGem = Math.max(0, s.getLong("bag.bonus-gem", t.bonusGem));
        t.radius = Math.max(1, Math.min(128, s.getInt("radius", t.radius)));
        t.huntSeconds = Math.max(0, s.getInt("hunt.seconds", t.huntSeconds));
        t.huntSpeed = Math.max(0.1, Math.min(5, s.getDouble("hunt.speed", t.huntSpeed)));
        t.slowSpeed = Math.max(0.1, Math.min(5, s.getDouble("slow-speed", t.slowSpeed)));
        t.blinkPercent = Math.max(0, Math.min(100, s.getDouble("blink.percent", t.blinkPercent)));
        t.blinkDistance = Math.max(1, Math.min(64, s.getInt("blink.distance", t.blinkDistance)));
        t.refundMinPercent = percent(s.getDouble("refund.min-percent", t.refundMinPercent));
        t.refundMaxPercent = Math.max(t.refundMinPercent, percent(s.getDouble("refund.max-percent", t.refundMaxPercent)));
        return t;
    }

    private static double percent(double v) {
        return Math.max(0, Math.min(100, v));
    }
}
