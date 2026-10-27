package com.dex.myboxmobs.live;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;
import java.util.UUID;
import org.bukkit.entity.Entity;
import org.junit.jupiter.api.Test;

class TrackerTest {

    private static LiveMob mob(UUID uuid, String id, String origin) {
        Entity fake = (Entity) Proxy.newProxyInstance(Entity.class.getClassLoader(), new Class<?>[]{Entity.class},
                (proxy, method, args) -> method.getName().equals("getUniqueId") ? uuid : null);
        return new LiveMob(fake, null, id, origin, null, 0);
    }

    @Test
    void countsFollowAddReplaceAndRemove() {
        Tracker t = new Tracker();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID();
        t.add(mob(a, "boss", "spawner:x"));
        t.add(mob(b, "boss", "spawner:x"));
        t.add(mob(c, "zombie", "command"));
        assertEquals(2, t.count("boss"));
        assertEquals(1, t.count("zombie"));

        t.add(mob(a, "boss", "spawner:x"));
        assertEquals(2, t.count("boss"), "the same entity added again is not counted twice");

        t.remove(a);
        t.remove(a);
        assertEquals(1, t.count("boss"));
        t.remove(b);
        assertEquals(0, t.count("boss"));

        assertEquals(1, t.byOrigin().get("command").size());
        assertEquals(null, t.byOrigin().get("spawner:x"));
    }
}
