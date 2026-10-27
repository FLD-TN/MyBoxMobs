package com.dex.myboxmobs.hook;

import com.dex.myboxmobs.MyBoxMobsPlugin;
import java.util.function.BiFunction;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/** The only class touching PlaceholderAPI; loaded only when it is enabled (spec 9). */
public final class PapiHook {

    private PapiHook() {
    }

    public static BiFunction<Player, String, String> resolver() {
        return (player, s) -> PlaceholderAPI.setPlaceholders(player, s);
    }

    public static void register(MyBoxMobsPlugin plugin) {
        new Expansion(plugin).register();
    }

    /** %myboxmobs_kills_<id>%, %myboxmobs_kills_total%, %myboxmobs_alive_<id>%. */
    private static final class Expansion extends PlaceholderExpansion {

        private final MyBoxMobsPlugin plugin;

        Expansion(MyBoxMobsPlugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public @NotNull String getIdentifier() {
            return "myboxmobs";
        }

        @Override
        public @NotNull String getAuthor() {
            return "Dex";
        }

        @Override
        public @NotNull String getVersion() {
            return plugin.getPluginMeta().getVersion();
        }

        @Override
        public boolean persist() {
            return true;
        }

        @Override
        public String onRequest(OfflinePlayer player, @NotNull String params) {
            String p = params.toLowerCase();
            if (p.startsWith("alive_")) return String.valueOf(plugin.mobs().tracker().count(p.substring(6)));
            if (player == null) return "";
            if (p.equals("kills_total")) return String.valueOf(plugin.kills().total(player.getUniqueId()));
            if (p.startsWith("kills_")) return String.valueOf(plugin.kills().kills(player.getUniqueId(), p.substring(6)));
            return null;
        }
    }
}
