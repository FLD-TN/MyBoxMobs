package com.dex.myboxmobs.mob;

import com.dex.myboxmobs.util.ItemCodec;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/** One reward (spec 5.1): an item, money, gem or a console command, with its own chance. */
public final class RewardEntry {

    public enum Kind { ITEM, MONEY, GEM, COMMAND }

    public Kind kind;
    /** ITEM only; amount of the stack is ignored (min/max is used). */
    public @Nullable ItemStack item;
    public long min = 1;
    public long max = 1;
    /** 0.01 to 100. */
    public double chance = 100.0;
    /** COMMAND only, {@code %player%} replaced. */
    public String command = "";

    public RewardEntry(Kind kind) {
        this.kind = kind;
    }

    public static RewardEntry item(ItemStack stack) {
        RewardEntry e = new RewardEntry(Kind.ITEM);
        ItemStack one = stack.clone();
        e.min = e.max = Math.max(1, one.getAmount());
        one.setAmount(1);
        e.item = one;
        return e;
    }

    public RewardEntry copy() {
        RewardEntry e = new RewardEntry(kind);
        e.item = item == null ? null : item.clone();
        e.min = min;
        e.max = max;
        e.chance = chance;
        e.command = command;
        return e;
    }

    public Map<String, Object> serialize() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("kind", kind.name());
        if (kind == Kind.ITEM && item != null) m.put("item", ItemCodec.encode(item));
        if (kind == Kind.COMMAND) {
            m.put("command", command);
        } else {
            m.put("min", min);
            m.put("max", max);
        }
        m.put("chance", chance);
        return m;
    }

    public static @Nullable RewardEntry deserialize(Map<?, ?> m) {
        Object k = m.get("kind");
        Kind kind;
        try {
            kind = Kind.valueOf(String.valueOf(k).toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
        RewardEntry e = new RewardEntry(kind);
        if (kind == Kind.ITEM) {
            e.item = ItemCodec.decode(String.valueOf(m.get("item")));
            if (e.item == null) return null;
        }
        if (kind == Kind.COMMAND) e.command = String.valueOf(m.containsKey("command") ? m.get("command") : "");
        e.min = Math.max(0, toLong(m.get("min"), 1));
        e.max = Math.max(e.min, toLong(m.get("max"), e.min));
        e.chance = clampChance(toDouble(m.get("chance"), 100.0));
        return e;
    }

    public static double clampChance(double c) {
        if (Double.isNaN(c)) return 100.0;
        return Math.max(0.01, Math.min(100.0, c));
    }

    static long toLong(Object o, long def) {
        if (o instanceof Number n) return n.longValue();
        try {
            return o == null ? def : Long.parseLong(String.valueOf(o));
        } catch (NumberFormatException ex) {
            return def;
        }
    }

    static double toDouble(Object o, double def) {
        if (o instanceof Number n) return n.doubleValue();
        try {
            return o == null ? def : Double.parseDouble(String.valueOf(o));
        } catch (NumberFormatException ex) {
            return def;
        }
    }
}
