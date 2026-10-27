package com.dex.myboxmobs.hook;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Location;

/** DecentHolograms implementation. Only loaded when the plugin is enabled. */
public final class DecentHolograms implements Holograms {

    private final Logger log;
    /** Last lines sent per hologram: unchanged lines are not resent every second. */
    private final Map<String, List<String>> shown = new HashMap<>();
    private boolean warned;

    public DecentHolograms(Logger log) {
        this.log = log;
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public void show(String name, Location location, List<String> lines) {
        try {
            Hologram holo = DHAPI.getHologram(name);
            if (holo == null) {
                DHAPI.createHologram(name, location, false, lines);
                shown.put(name, List.copyOf(lines));
                return;
            }
            if (!holo.getLocation().equals(location)) DHAPI.moveHologram(holo, location);
            if (!lines.equals(shown.get(name))) {
                DHAPI.setHologramLines(holo, lines);
                shown.put(name, List.copyOf(lines));
            }
        } catch (RuntimeException ex) {
            warn(ex);
        }
    }

    @Override
    public void remove(String name) {
        shown.remove(name);
        try {
            DHAPI.removeHologram(name);
        } catch (RuntimeException ex) {
            warn(ex);
        }
    }

    private void warn(RuntimeException ex) {
        if (warned) return;
        warned = true;
        log.log(Level.WARNING, "DecentHolograms call failed (reported once)", ex);
    }
}
