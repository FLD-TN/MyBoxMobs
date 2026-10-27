package com.dex.myboxmobs.gui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class RangeTest {

    @Test
    void amountRanges() {
        assertArrayEquals(new long[]{5, 5}, RewardListMenu.parseRange("5"));
        assertArrayEquals(new long[]{5, 10}, RewardListMenu.parseRange("5-10"));
        assertArrayEquals(new long[]{5, 10}, RewardListMenu.parseRange("10 - 5"));
        assertArrayEquals(new long[]{1000, 2500}, RewardListMenu.parseRange("1.000-2.500"));
        assertNull(RewardListMenu.parseRange("0"));
        assertNull(RewardListMenu.parseRange("abc"));
    }
}
