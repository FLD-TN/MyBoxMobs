package com.dex.myboxmobs.mob;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One rank of the clan top mode: chance to receive anything, Danh Vọng and Minh Huy for the clan, and gifts for
 * each member of that clan who dealt damage and is online.
 */
public final class ClanRank {

    public double chance = 100.0;
    public long danhVong;
    public long minhHuy;
    public final List<RewardEntry> rewards = new ArrayList<>();

    public ClanRank() {
    }

    public ClanRank(long danhVong, long minhHuy) {
        this.danhVong = danhVong;
        this.minhHuy = minhHuy;
    }

    public Map<String, Object> serialize() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("chance", chance);
        m.put("danh-vong", danhVong);
        m.put("minh-huy", minhHuy);
        List<Map<String, Object>> list = new ArrayList<>();
        for (RewardEntry e : rewards) list.add(e.serialize());
        m.put("member-rewards", list);
        return m;
    }

    public static ClanRank deserialize(Map<?, ?> m) {
        ClanRank r = new ClanRank();
        r.chance = RewardEntry.clampChance(RewardEntry.toDouble(m.get("chance"), 100.0));
        r.danhVong = Math.max(0, Math.round(RewardEntry.toDouble(m.get("danh-vong"), 0)));
        r.minhHuy = Math.max(0, Math.round(RewardEntry.toDouble(m.get("minh-huy"), 0)));
        if (m.get("member-rewards") instanceof List<?> list) {
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
