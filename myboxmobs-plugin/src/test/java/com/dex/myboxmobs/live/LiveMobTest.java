package com.dex.myboxmobs.live;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;
import java.util.UUID;
import org.bukkit.entity.Entity;
import org.junit.jupiter.api.Test;

class LiveMobTest {

    private static LiveMob mob() {
        Entity fake = (Entity) Proxy.newProxyInstance(Entity.class.getClassLoader(), new Class<?>[]{Entity.class},
                (proxy, method, args) -> null);
        return new LiveMob(fake, null, "boss", "command", null, 0);
    }

    @Test
    void damageTableSurvivesSaveAndLoad() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        LiveMob before = mob();
        before.addDamage(a, 1200.5);
        before.addDamage(b, 300);
        before.addDamage(a, 99.5);
        LiveMob after = mob();
        after.restoreDamage(before.serializeDamage(), before.lastHitter);
        assertEquals(before.sortedDamage(), after.sortedDamage());
        assertEquals(a, after.lastHitter);
        assertEquals(1300.0, after.sortedDamage().get(a));
    }

    @Test
    void brokenDataIsIgnored() {
        LiveMob m = mob();
        m.restoreDamage("not-a-uuid=5;;" + UUID.randomUUID() + "=abc", null);
        assertEquals(0, m.sortedDamage().size());
    }
}
