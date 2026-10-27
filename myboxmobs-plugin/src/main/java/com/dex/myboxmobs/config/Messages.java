package com.dex.myboxmobs.config;

import com.dex.myboxmobs.text.Text;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Player-facing strings from messages.yml. Missing keys fall back to the copy inside the jar. Placeholders are
 * pairs {@code "mob", name} and replace {@code %mob%}. An empty string disables a chat message.
 */
public final class Messages {

    private final JavaPlugin plugin;
    private volatile YamlConfiguration yml = new YamlConfiguration();

    public Messages(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        ConfigFiles.ensure(plugin, "messages.yml");
        YamlConfiguration loaded = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "messages.yml"));
        try (InputStream in = plugin.getResource("messages.yml")) {
            if (in != null) {
                loaded.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8)));
            }
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not read default messages.yml: " + ex.getMessage());
        }
        yml = loaded;
    }

    public String raw(String path, String... placeholders) {
        String s = yml.getString(path);
        return s == null ? path : apply(s, placeholders);
    }

    public List<String> rawList(String path, String... placeholders) {
        List<String> out = new ArrayList<>();
        for (String s : yml.getStringList(path)) out.add(apply(s, placeholders));
        return out;
    }

    public void send(CommandSender to, String path, String... placeholders) {
        String s = raw(path, placeholders);
        if (s.isEmpty()) return;
        to.sendMessage(Text.parse(raw("prefix") + s, to instanceof Player p ? p : null));
    }

    /** Like {@link #send} but {@code %item%} is replaced by the item's hoverable name. */
    public void sendWithItem(CommandSender to, String path, Component item, String... placeholders) {
        String s = raw(path, placeholders);
        if (s.isEmpty()) return;
        Component c = Text.parse(raw("prefix") + s, to instanceof Player p ? p : null);
        to.sendMessage(c.replaceText(TextReplacementConfig.builder().matchLiteral("%item%").replacement(item).build()));
    }

    public Component title(String path, String... placeholders) {
        return Text.parse(raw(path, placeholders));
    }

    public static String apply(String s, String... placeholders) {
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            s = s.replace("%" + placeholders[i] + "%", placeholders[i + 1]);
        }
        return s;
    }
}
