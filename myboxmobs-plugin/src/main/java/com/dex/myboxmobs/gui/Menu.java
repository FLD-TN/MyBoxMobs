package com.dex.myboxmobs.gui;

import com.dex.myboxmobs.MyBoxMobsPlugin;
import com.dex.myboxmobs.text.Text;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Every MyBoxMobs admin GUI. Identified by its holder, never by its title. {@link GuiListener} cancels every click,
 * forwards top clicks to the buttons and clicks in the player's own inventory to {@link #onOwnItemClick}.
 */
public abstract class Menu implements InventoryHolder {

    public interface Handler {
        void click(ClickType click);
    }

    protected final MyBoxMobsPlugin plugin;
    protected final Player player;
    private final Inventory inventory;
    private final Map<Integer, Handler> handlers = new HashMap<>();

    protected Menu(MyBoxMobsPlugin plugin, Player player, int rows, String title) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, rows * 9, Text.parse(title));
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    /** Fills the inventory through {@link #set}. Called on open and on every {@link #refresh}. */
    protected abstract void draw();

    public void open() {
        refresh();
        player.openInventory(inventory);
    }

    public void refresh() {
        handlers.clear();
        inventory.clear();
        draw();
    }

    protected void set(int slot, ItemStack item, @Nullable Handler handler) {
        inventory.setItem(slot, item);
        if (handler == null) handlers.remove(slot);
        else handlers.put(slot, handler);
    }

    protected void fill(int fromRow) {
        ItemStack pane = icon(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = fromRow * 9; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) inventory.setItem(i, pane);
        }
    }

    void click(int slot, ClickType click) {
        Handler h = handlers.get(slot);
        if (h != null) h.click(click);
    }

    /** Clicked an item in the player's own inventory while this menu is open (used to put items in). */
    protected void onOwnItemClick(ItemStack item, ClickType click) {
    }

    void ownItemClick(ItemStack item, ClickType click) {
        onOwnItemClick(item, click);
    }

    /** Closes the GUI, asks for one chat line, then reopens this menu (spec 2: "huy" cancels). */
    public void ask(String promptKey, Consumer<String> onInput, String... placeholders) {
        Gui.prompt(plugin, player, promptKey, text -> {
            onInput.accept(text);
            open();
        }, this::open, placeholders);
    }

    /** Number input; invalid numbers send an error and reopen the menu unchanged. */
    public void askNumber(String promptKey, double min, double max, java.util.function.DoubleConsumer onValue) {
        ask(promptKey, text -> {
            double v = com.dex.myboxmobs.text.Format.parse(text);
            if (Double.isNaN(v) || v < min || v > max) {
                plugin.messages().send(player, "input.invalid-number", "min", com.dex.myboxmobs.text.Format.decimal(min),
                        "max", com.dex.myboxmobs.text.Format.decimal(max));
                return;
            }
            onValue.accept(v);
        });
    }

    public static ItemStack icon(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        return decorate(item, name, lore);
    }

    /** Sets name (not bold, no italics) and lore, hides vanilla tooltips. */
    public static ItemStack decorate(ItemStack item, String name, List<String> lore) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.displayName(Text.item(name));
        meta.lore(lore.stream().map(Text::item).toList());
        meta.addItemFlags(ItemFlag.values());
        item.setItemMeta(meta);
        return item;
    }

    /** A copy of a real item (reward, equipment) with extra lore lines appended after its own. */
    public static ItemStack withExtraLore(ItemStack item, List<String> extra) {
        ItemStack copy = item.clone();
        copy.setAmount(1);
        ItemMeta meta = copy.getItemMeta();
        if (meta == null) return copy;
        List<net.kyori.adventure.text.Component> lore = meta.lore() == null ? new java.util.ArrayList<>()
                : new java.util.ArrayList<>(meta.lore());
        if (!lore.isEmpty()) lore.add(net.kyori.adventure.text.Component.empty());
        for (String s : extra) lore.add(Text.item(s));
        meta.lore(lore);
        copy.setItemMeta(meta);
        return copy;
    }
}
