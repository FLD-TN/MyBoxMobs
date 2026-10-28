package com.dex.myboxmobs.live;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/**
 * Money and gems a thief mob carries: its bag (for the killer) and what it stole (given back a little per hit, the
 * rest to the killer). Saved with the entity ({@link #serialize}) like the damage table.
 */
public final class ThiefState {

    public long bagMoney;
    public long bagGem;
    /** Everything stolen so far (the refund percent is taken of this). */
    public long stolenMoney;
    public long stolenGem;
    /** Stolen and not given back yet. */
    public long leftMoney;
    public long leftGem;
    /** Epoch millis the hunt phase ends. */
    public long huntUntil;

    // runtime
    public final Map<UUID, Long> lastSteal = new HashMap<>();
    public boolean slowed;
    public double baseSpeed = Double.NaN;
    /** Health marks (every blink percent of the max health) already crossed; -1 = not known yet. */
    public int marks = -1;

    /** Amount stolen from {@code balance}: {@code percent} of it, rounded down, at most {@code cap}. */
    public static long stealAmount(long balance, double percent, long cap) {
        if (balance <= 0 || percent <= 0) return 0;
        long amount = (long) Math.floor(balance * percent / 100.0);
        return cap > 0 ? Math.min(cap, amount) : amount;
    }

    /** Amount given back for one hit: {@code percent} of everything stolen, never more than what is left. */
    public static long refundAmount(long stolen, long left, double percent) {
        if (stolen <= 0 || left <= 0 || percent <= 0) return 0;
        return Math.min(left, Math.max(1, Math.round(stolen * percent / 100.0)));
    }

    /**
     * How many marks the health has crossed: one mark every {@code percent} of the max health, counted from the max
     * (max 50, 10 %: 45, 40, 35 ... so 43 = 1 mark, 35 = 3 marks).
     */
    public static int marksCrossed(double maxHealth, double health, double percent) {
        if (percent <= 0 || maxHealth <= 0) return 0;
        double step = maxHealth * percent / 100.0;
        return (int) Math.floor((maxHealth - Math.max(0, health)) / step + 1.0E-9);
    }

    public void stole(long money, long gem) {
        stolenMoney += money;
        leftMoney += money;
        stolenGem += gem;
        leftGem += gem;
    }

    public String serialize() {
        return bagMoney + ";" + bagGem + ";" + stolenMoney + ";" + stolenGem + ";" + leftMoney + ";" + leftGem + ";" + huntUntil;
    }

    public static @Nullable ThiefState parse(@Nullable String raw) {
        if (raw == null || raw.isEmpty()) return null;
        String[] p = raw.split(";");
        if (p.length < 7) return null;
        try {
            ThiefState s = new ThiefState();
            s.bagMoney = Long.parseLong(p[0]);
            s.bagGem = Long.parseLong(p[1]);
            s.stolenMoney = Long.parseLong(p[2]);
            s.stolenGem = Long.parseLong(p[3]);
            s.leftMoney = Long.parseLong(p[4]);
            s.leftGem = Long.parseLong(p[5]);
            s.huntUntil = Long.parseLong(p[6]);
            return s;
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
