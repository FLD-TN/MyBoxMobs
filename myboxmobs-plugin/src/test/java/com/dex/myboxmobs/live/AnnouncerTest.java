package com.dex.myboxmobs.live;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AnnouncerTest {

    @Test
    void rankIconsLikeCustomFishing() {
        assertEquals("①", Announcer.rankIcon(1));
        assertEquals("③", Announcer.rankIcon(3));
        assertEquals("⑳", Announcer.rankIcon(20));
        assertEquals("(21)", Announcer.rankIcon(21));
    }
}
