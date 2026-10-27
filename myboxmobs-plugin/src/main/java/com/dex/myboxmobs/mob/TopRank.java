package com.dex.myboxmobs.mob;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** One rank of the top damage mode (spec 5.2): chance to receive anything, then its own reward list. */
public final class TopRank {

    public double chance = 100.0;
    public final List<RewardEntry> rewards = new ArrayList<>();

    public Map<String, Object> serialize() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("chance", chance);
        List<Map<String, Object>> list = new ArrayList<>();
        for (RewardEntry e : rewards) list.add(e.serialize());
        m.put("rewards", list);
        return m;
    }

    public static TopRank deserialize(Map<?, ?> m) {
        TopRank r = new TopRank();
        r.chance = RewardEntry.clampChance(RewardEntry.toDouble(m.get("chance"), 100.0));
        if (m.get("rewards") instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> map) {
                    RewardEntry e = RewardEntry.deserialize(map);
                    if (e != null) r.rewards.add(e);
                }
            }
        }
        return r;
    }
}
