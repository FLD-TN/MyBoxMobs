package com.dex.myboxmobs.config;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

/** Values of config.yml, re-read on /mbm reload. */
public final class Settings {

    public final int clickCooldownTicks;
    public final int inputTimeoutSeconds;
    public final String boostType;
    public final String boostItems;
    public final String boostMoney;
    public final String boostGem;
    public final double spawnerHoloHeight;
    public final double altarHoloHeight;
    public final Material altarDefaultBlock;
    public final int altarInteractRange;
    public final List<String> spawnerHoloLines;
    public final List<String> altarHoloLines;
    public final List<String> altarHoloCooldownLines;
    public final String topLine;
    public final List<String> topHeader;
    public final List<String> rankColors;
    public final String rankColorOthers;
    public final List<String> topClanHeader;
    public final String topClanLine;
    public final int boardSize;
    public final String nametagFormat;
    public final int barLength;
    public final String barChar;
    public final String barFull;
    public final String barEmpty;
    public final String clanSourceTop;
    public final String clanSourceKill;
    public final String moneyItem;
    public final String gemItem;
    public final int maxHealth;
    public final java.time.ZoneId timezone;

    public Settings(FileConfiguration c) {
        clickCooldownTicks = Math.max(1, c.getInt("gui.click-cooldown-ticks", 4));
        inputTimeoutSeconds = Math.max(5, c.getInt("gui.input-timeout-seconds", 30));
        boostType = c.getString("multiplier.boost-type", "mob");
        boostItems = orDefault(c.getString("multiplier.types.items", ""), boostType);
        boostMoney = orDefault(c.getString("multiplier.types.money", ""), boostType);
        boostGem = orDefault(c.getString("multiplier.types.gem", ""), boostType);
        spawnerHoloHeight = c.getDouble("hologram.spawner-height", 2.5);
        altarHoloHeight = c.getDouble("hologram.altar-height", 2.3);
        Material block = Material.matchMaterial(c.getString("altar.default-block", "SCULK_SHRIEKER"));
        altarDefaultBlock = block == null || !block.isBlock() ? Material.SCULK_SHRIEKER : block;
        altarInteractRange = Math.max(2, c.getInt("altar.target-range", 6));
        spawnerHoloLines = c.getStringList("hologram.spawner-lines");
        altarHoloLines = c.getStringList("hologram.altar-lines");
        altarHoloCooldownLines = c.getStringList("hologram.altar-cooldown-lines");
        topLine = c.getString("announce.top-line",
                "<center>%color%%icon% %player% &#64748B— &#94A3B8gây ra %color%%damage% &#94A3B8sát thương");
        topHeader = c.getStringList("announce.top-header");
        List<String> colors = c.getStringList("announce.rank-colors");
        rankColors = colors.isEmpty() ? List.of("&#FCD34D", "&#E2E8F0", "&#D97706") : colors;
        rankColorOthers = c.getString("announce.rank-color-others", "&7");
        // 0.7.0 keys: defaults here so a config.yml from an older version keeps working untouched
        topClanHeader = c.isSet("announce.top-clan-header") ? c.getStringList("announce.top-clan-header")
                : List.of("", "<center>&#FFD166Bảng xếp hạng bang hội", "");
        topClanLine = c.getString("announce.top-clan-line",
                "<center>%color%%icon% %clan% &#64748B— &#94A3B8gây ra %color%%damage% &#94A3B8sát thương &8(%members% người)");
        boardSize = Math.max(1, c.getInt("announce.board-size", 5));
        nametagFormat = c.getString("nametag.format", "%name% &8| %bar%");
        barLength = Math.max(1, Math.min(100, c.getInt("nametag.bar-length", 20)));
        barChar = c.getString("nametag.bar-char", "|");
        barFull = c.getString("nametag.full-color", "&#FF4D4D");
        barEmpty = c.getString("nametag.empty-color", "&7");
        clanSourceTop = c.getString("clans.points-source-top", "mob-boss");
        clanSourceKill = c.getString("clans.points-source-kill", "mob-kill");
        moneyItem = c.getString("gui.money-item", "treasure:money_item");
        gemItem = c.getString("gui.gem-item", "treasure:gem_item");
        maxHealth = Math.max(1, c.getInt("max-health", 2048));
        java.time.ZoneId zone;
        try {
            zone = java.time.ZoneId.of(c.getString("timezone", "Asia/Ho_Chi_Minh"));
        } catch (java.time.DateTimeException ex) {
            zone = java.time.ZoneId.of("Asia/Ho_Chi_Minh");
        }
        timezone = zone;
    }

    private static String orDefault(String s, String def) {
        return s == null || s.isBlank() ? def : s;
    }
}
