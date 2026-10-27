package com.dex.myboxmobs.hook;

import com.dex.boxclans.api.Clan;
import com.dex.boxclans.api.ClanCurrency;
import com.dex.boxclans.api.ClanPoints;
import com.dex.boxclans.api.ClanRole;
import com.dex.boxclans.api.ClanService;
import com.dex.boxclans.api.CurrencyResult;
import com.dex.boxclans.api.PointResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/** BoxClans 1.4 implementation. Only loaded when BoxClans is enabled; services are looked up on use. */
public final class BoxClansSupport implements ClanSupport {

    private final Logger log;

    public BoxClansSupport(Logger log) {
        this.log = log;
    }

    private static @Nullable <T> T service(Class<T> type) {
        return Bukkit.getServicesManager().load(type);
    }

    @Override
    public boolean available() {
        return service(ClanService.class) != null;
    }

    @Override
    public @Nullable ClanInfo clanOf(UUID player) {
        ClanService s = service(ClanService.class);
        if (s == null) return null;
        Optional<Clan> c = s.clanOf(player);
        return c.map(clan -> new ClanInfo(clan.uuid(), clan.nameAmpersand())).orElse(null);
    }

    @Override
    public @Nullable ClanInfo clan(UUID clan) {
        ClanService s = service(ClanService.class);
        if (s == null) return null;
        return s.clan(clan).map(c -> new ClanInfo(c.uuid(), c.nameAmpersand())).orElse(null);
    }

    @Override
    public boolean isLeader(UUID player) {
        ClanService s = service(ClanService.class);
        return s != null && s.roleOf(player).orElse(null) == ClanRole.LEADER;
    }

    @Override
    public List<Player> onlineMembers(UUID clan) {
        ClanService s = service(ClanService.class);
        List<Player> out = new ArrayList<>();
        if (s == null) return out;
        s.clan(clan).ifPresent(c -> {
            for (UUID m : c.members()) {
                Player p = Bukkit.getPlayer(m);
                if (p != null) out.add(p);
            }
        });
        return out;
    }

    @Override
    public long awardPoints(UUID clan, String source, long amount) {
        ClanPoints points = service(ClanPoints.class);
        if (points == null || amount <= 0) return 0;
        PointResult r = points.awardClan(clan, source, amount);
        if (r.status() == PointResult.Status.UNKNOWN_SOURCE) {
            log.warning("BoxClans has no points source '" + source + "': add it to BoxClans points.yml (sources)");
        }
        return r.success() ? r.amount() : 0;
    }

    @Override
    public boolean giveCurrency(UUID clan, long amount, String reason) {
        ClanCurrency currency = service(ClanCurrency.class);
        if (currency == null || amount <= 0) return false;
        CurrencyResult r = currency.giveClan(clan, amount, reason);
        return r.success() || r.status() == CurrencyResult.Status.QUEUED;
    }
}
