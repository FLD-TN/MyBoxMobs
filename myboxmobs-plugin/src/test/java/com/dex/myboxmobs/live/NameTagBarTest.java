package com.dex.myboxmobs.live;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dex.myboxmobs.mob.MobDefinition;
import java.util.List;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class NameTagBarTest {

    @Test
    void redForHealthLeftGreyForHealthLost() {
        assertEquals("R||||||||||G", NameTags.bar(100, 100, 10, "R", "G", "|"));
        assertEquals("R|||||||G|||", NameTags.bar(70, 100, 10, "R", "G", "|"));
        assertEquals("RG||||||||||", NameTags.bar(0, 100, 10, "R", "G", "|"));
    }

    @Test
    void anyHealthLeftShowsOneBar() {
        assertEquals("R|G|||||||||", NameTags.bar(0.5, 1000, 10, "R", "G", "|"));
    }

    @Test
    void nametagSettingsRoundTrip() throws InvalidConfigurationException {
        MobDefinition d = new MobDefinition("kragan", MobDefinition.Source.MYTHIC, List.of(), List.of(), List.of());
        d.healthBar = false;
        d.nametagOffset = 1.25;
        YamlConfiguration y = new YamlConfiguration();
        y.loadFromString(d.toYaml().saveToString());
        MobDefinition back = MobDefinition.fromYaml("kragan", y, d);
        assertEquals(false, back.healthBar);
        assertEquals(1.25, back.nametagOffset);

        MobDefinition old = MobDefinition.fromYaml("old", new YamlConfiguration(), d);
        assertEquals(true, old.healthBar, "older mob files get the bar");
        assertEquals(0.3, old.nametagOffset);
    }
}
