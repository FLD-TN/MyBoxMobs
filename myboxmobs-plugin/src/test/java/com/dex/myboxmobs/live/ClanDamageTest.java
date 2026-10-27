package com.dex.myboxmobs.live;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dex.myboxmobs.hook.ClanSupport.ClanInfo;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClanDamageTest {

    @Test
    void sumsPerClanHighestFirstAndSkipsClanless() {
        ClanInfo red = new ClanInfo(UUID.randomUUID(), "&cDo");
        ClanInfo blue = new ClanInfo(UUID.randomUUID(), "&9Xanh");
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID(), loner = UUID.randomUUID();
        Map<UUID, ClanInfo> clanOf = Map.of(a, blue, b, red, c, red);
        LinkedHashMap<UUID, Double> damage = new LinkedHashMap<>();
        damage.put(a, 900.0);
        damage.put(loner, 800.0);
        damage.put(b, 600.0);
        damage.put(c, 400.0);

        List<ClanDamage.Entry> board = ClanDamage.of(damage, clanOf::get);

        assertEquals(2, board.size(), "players without a clan are left out");
        assertEquals(red.uuid(), board.get(0).clan().uuid());
        assertEquals(1000.0, board.get(0).total());
        assertEquals(List.of(b, c), List.copyOf(board.get(0).members().keySet()));
        assertEquals(60.0, board.get(0).share(b), 1e-9);
        assertEquals(40.0, board.get(0).share(c), 1e-9);
        assertEquals(blue.uuid(), board.get(1).clan().uuid());
        assertEquals(900.0, board.get(1).total());
    }
}
