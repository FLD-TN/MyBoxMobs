package com.dex.myboxmobs.place;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class ScheduleTest {

    @Test
    void clockTimes() {
        assertEquals(LocalTime.of(8, 0), SpawnerService.parseTime("8:00"));
        assertEquals(LocalTime.of(20, 30), SpawnerService.parseTime(" 20:30 "));
        assertNull(SpawnerService.parseTime("24:00"));
        assertNull(SpawnerService.parseTime("20:61"));
        assertNull(SpawnerService.parseTime("8h"));
    }
}
