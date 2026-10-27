package com.dex.myboxmobs.live;

import com.dex.myboxmobs.hook.ClanSupport.ClanInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/** Player damage summed per clan (clan top mode). Players without a clan are left out. */
public final class ClanDamage {

    /** One clan: total damage and its members' damage, highest first. */
    public record Entry(ClanInfo clan, double total, LinkedHashMap<UUID, Double> members) {

        /** Share (0-100) of the clan's damage dealt by this member. */
        public double share(UUID member) {
            return total <= 0 ? 0 : members.getOrDefault(member, 0.0) * 100.0 / total;
        }
    }

    private ClanDamage() {
    }

    /** {@code damage} highest first (as {@link LiveMob#sortedDamage()}); clans highest total first. */
    public static List<Entry> of(Map<UUID, Double> damage, Function<UUID, ClanInfo> clanOf) {
        Map<UUID, ClanInfo> clans = new HashMap<>();
        Map<UUID, LinkedHashMap<UUID, Double>> members = new LinkedHashMap<>();
        Map<UUID, Double> totals = new HashMap<>();
        for (Map.Entry<UUID, Double> e : damage.entrySet()) {
            ClanInfo clan = clanOf.apply(e.getKey());
            if (clan == null) continue;
            clans.putIfAbsent(clan.uuid(), clan);
            members.computeIfAbsent(clan.uuid(), k -> new LinkedHashMap<>()).put(e.getKey(), e.getValue());
            totals.merge(clan.uuid(), e.getValue(), Double::sum);
        }
        List<Entry> out = new ArrayList<>();
        for (Map.Entry<UUID, LinkedHashMap<UUID, Double>> e : members.entrySet()) {
            out.add(new Entry(clans.get(e.getKey()), totals.get(e.getKey()), e.getValue()));
        }
        out.sort((a, b) -> Double.compare(b.total(), a.total()));
        return out;
    }
}
