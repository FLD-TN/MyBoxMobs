package com.dex.myboxmobs.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

/** One pastel colour per editor section (spec 2), and spawn egg lookup. */
public final class Palette {

    public static final String MOB = "&#E0BBE4";
    public static final String GENERAL = "&#A0E7E5";
    public static final String STATS = "&#FFAEBC";
    public static final String EQUIP = "&#B4F8C8";
    public static final String MODEL = "&#FBE7C6";
    public static final String REWARD = "&#C3B1E1";
    public static final String ANNOUNCE = "&#FFC8A2";
    public static final String SPAWNER = "&#9AD0F5";
    public static final String ALTAR = "&#F7A4C0";
    public static final String ALIVE = "&#F6E58D";
    public static final String SKILL = "&#FFB38A";
    public static final String MONEY = "&a";
    public static final String GEM = "&#8245F8";

    private Palette() {
    }

    public static Material egg(EntityType type) {
        if (type == null) return Material.EGG;
        try {
            Material m = Bukkit.getItemFactory().getSpawnEgg(type);
            return m == null ? Material.EGG : m;
        } catch (RuntimeException ex) {
            return Material.EGG;
        }
    }

    public static Material egg(String entityType) {
        if (entityType == null || entityType.isEmpty()) return Material.NETHER_STAR;
        try {
            return egg(EntityType.valueOf(entityType.toUpperCase()));
        } catch (IllegalArgumentException ex) {
            return Material.NETHER_STAR;
        }
    }

    public static String onOff(boolean on) {
        return on ? "&#7CFC9A✔ Bật" : "&#FF6B6B✘ Tắt";
    }
}
