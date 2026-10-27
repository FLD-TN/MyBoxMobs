package com.dex.myboxmobs.gui;

import com.dex.myboxmobs.MyBoxMobsPlugin;
import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

/** One chat line typed by an admin for a GUI field (spec 2). The line never reaches the chat. */
public final class ChatInput implements Listener {

    private record Pending(Consumer<String> onInput, Runnable onCancel, BukkitTask timeout) {
    }

    private final MyBoxMobsPlugin plugin;
    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();

    public ChatInput(MyBoxMobsPlugin plugin) {
        this.plugin = plugin;
    }

    public void ask(Player player, Consumer<String> onInput, Runnable onCancel) {
        cancel(player.getUniqueId());
        BukkitTask timeout = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Pending p = pending.remove(player.getUniqueId());
            if (p != null && player.isOnline()) {
                plugin.messages().send(player, "input.timeout");
                p.onCancel.run();
            }
        }, plugin.settings().inputTimeoutSeconds * 20L);
        pending.put(player.getUniqueId(), new Pending(onInput, onCancel, timeout));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent e) {
        Pending p = pending.remove(e.getPlayer().getUniqueId());
        if (p == null) return;
        e.setCancelled(true);
        p.timeout.cancel();
        String text = PlainTextComponentSerializer.plainText().serialize(e.originalMessage()).strip();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!e.getPlayer().isOnline()) return;
            if (text.equalsIgnoreCase("huy") || text.equalsIgnoreCase("hủy")) {
                plugin.messages().send(e.getPlayer(), "input.cancelled");
                p.onCancel.run();
                return;
            }
            p.onInput.accept(text);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        cancel(e.getPlayer().getUniqueId());
    }

    private void cancel(UUID player) {
        Pending p = pending.remove(player);
        if (p != null) p.timeout.cancel();
    }
}
