package com.dex.myboxmobs.hook;

import java.util.List;
import org.bukkit.Location;

/** DecentHolograms behind an interface (optional). Lines are already section-coloured. */
public interface Holograms {

    Holograms NONE = new Holograms() {
        @Override
        public boolean available() {
            return false;
        }

        @Override
        public void show(String name, Location location, List<String> lines) {
        }

        @Override
        public void remove(String name) {
        }
    };

    boolean available();

    /** Creates or updates the hologram {@code name} (never saved to DecentHolograms' files). */
    void show(String name, Location location, List<String> lines);

    void remove(String name);
}
