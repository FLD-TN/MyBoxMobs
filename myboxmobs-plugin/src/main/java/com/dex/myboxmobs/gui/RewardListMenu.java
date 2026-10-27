package com.dex.myboxmobs.gui;

import com.dex.myboxmobs.MyBoxMobsPlugin;
import com.dex.myboxmobs.mob.MobDefinition;
import com.dex.myboxmobs.mob.RewardEntry;
import com.dex.myboxmobs.text.Format;
import com.dex.myboxmobs.text.Lore;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

/**
 * A reward list (spec 5.1): click an item in your own inventory to add it (full NBT), money / gem / command
 * buttons, each entry with its own chance and amount.
 */
public final class RewardListMenu extends Menu {

    private static final int MAX = 45;

    private final MobDefinition def;
    private final List<RewardEntry> list;
    private final Menu parent;

    public RewardListMenu(MyBoxMobsPlugin plugin, Player player, MobDefinition def, List<RewardEntry> list,
                          String title, Menu parent) {
        super(plugin, player, 6, title);
        this.def = def;
        this.list = list;
        this.parent = parent;
    }

    private void save() {
        plugin.registry().save(def);
    }

    @Override
    protected void draw() {
        for (int i = 0; i < Math.min(MAX, list.size()); i++) {
            RewardEntry e = list.get(i);
            set(i, entryIcon(e), click -> edit(e, click));
        }
        set(45, icon(Material.ARROW, "&#BFC7D5Quay lại", Lore.of().action("Chuột trái để quay lại").build()),
                click -> parent.open());
        set(47, currencyIcon(true, Palette.MONEY + "Thêm tiền", Lore.of().desc("Tiền Vault, cộng thẳng vào tài khoản.")
                .action("Chuột trái để thêm").build()), click -> add(new RewardEntry(RewardEntry.Kind.MONEY)));
        set(48, currencyIcon(false, Palette.GEM + "Thêm " + plugin.economy().gemName(), Lore.of()
                .desc("Gem của BoxCore.").action("Chuột trái để thêm").build()), click -> add(new RewardEntry(RewardEntry.Kind.GEM)));
        set(49, icon(Material.HOPPER, Palette.REWARD + "Thêm vật phẩm", Lore.of()
                .desc("Bấm vào vật phẩm trong túi của bạn để thêm. Giữ nguyên mọi dữ liệu (MMOItems, ItemsAdder, CE).")
                .section("Thông tin:").row("Số mục", list.size() + "/" + MAX).build()), null);
        set(50, icon(Material.COMMAND_BLOCK, Palette.REWARD + "Thêm lệnh", Lore.of()
                .desc("Lệnh console, %player% là tên người nhận. Không nhân theo rank.")
                .action("Chuột trái để nhập lệnh").build()), click -> ask("input.command", text -> {
            RewardEntry e = new RewardEntry(RewardEntry.Kind.COMMAND);
            e.command = text.startsWith("/") ? text.substring(1) : text;
            if (list.size() < MAX) list.add(e);
            save();
        }));
        fill(5);
    }

    private void add(RewardEntry e) {
        if (list.size() >= MAX) {
            plugin.messages().send(player, "gui.list-full");
            return;
        }
        list.add(e);
        save();
        refresh();
    }

    @Override
    protected void onOwnItemClick(ItemStack item, ClickType click) {
        add(RewardEntry.item(item));
    }

    private void edit(RewardEntry e, ClickType click) {
        if (click == ClickType.SHIFT_RIGHT) {
            list.remove(e);
            save();
            refresh();
        } else if (click.isRightClick()) {
            if (e.kind == RewardEntry.Kind.COMMAND) {
                ask("input.command", text -> {
                    e.command = text.startsWith("/") ? text.substring(1) : text;
                    save();
                });
                return;
            }
            ask("input.amount", text -> {
                long[] range = parseRange(text);
                if (range == null) {
                    plugin.messages().send(player, "input.invalid-range");
                    return;
                }
                e.min = range[0];
                e.max = range[1];
                save();
            });
        } else {
            askNumber("input.chance", 0.01, 100, v -> {
                e.chance = v;
                save();
            });
        }
    }

    /** "5" or "5-10" (dot thousands allowed). */
    static long[] parseRange(String text) {
        String[] parts = text.replace(" ", "").split("-", 2);
        double a = Format.parse(parts[0]);
        double b = parts.length > 1 ? Format.parse(parts[1]) : a;
        if (Double.isNaN(a) || Double.isNaN(b) || a < 1 || b < 1 || a > 1e12 || b > 1e12) return null;
        long min = (long) Math.min(a, b);
        long max = (long) Math.max(a, b);
        return new long[]{min, max};
    }

    private ItemStack entryIcon(RewardEntry e) {
        String amount = e.min == e.max ? Format.number(e.min) : Format.number(e.min) + " - " + Format.number(e.max);
        Lore lore = Lore.of().section("Phần thưởng:");
        switch (e.kind) {
            case ITEM -> lore.row("Số lượng", amount);
            case MONEY -> lore.row("Số tiền", Palette.MONEY + amount + "$");
            case GEM -> lore.row("Số " + plugin.economy().gemName(), Palette.GEM + amount);
            case COMMAND -> lore.row("Lệnh", "/" + e.command);
        }
        lore.row("Tỉ lệ rơi", Format.decimal(e.chance) + "%");
        lore.action("Chuột trái để đổi tỉ lệ")
                .secondary(e.kind == RewardEntry.Kind.COMMAND ? "Chuột phải để sửa lệnh" : "Chuột phải để đổi số lượng")
                .danger("Shift + chuột phải để xoá");
        return switch (e.kind) {
            case ITEM -> e.item == null ? icon(Material.BARRIER, "&#FF8A8AVật phẩm lỗi", lore.build())
                    : withExtraLore(e.item, lore.build());
            case MONEY -> currencyIcon(true, Palette.MONEY + "Tiền", lore.build());
            case GEM -> currencyIcon(false, Palette.GEM + plugin.economy().gemName(), lore.build());
            case COMMAND -> icon(Material.COMMAND_BLOCK, Palette.REWARD + "Lệnh", lore.build());
        };
    }

    /** {@code treasure:money_item} / {@code treasure:gem_item} when ItemsAdder has them (spec 2). */
    private ItemStack currencyIcon(boolean money, String name, List<String> lore) {
        String id = money ? plugin.settings().moneyItem : plugin.settings().gemItem;
        ItemStack base = plugin.itemsAdder() == null ? null : plugin.itemsAdder().item(id);
        if (base == null) base = new ItemStack(money ? Material.GOLD_INGOT : Material.AMETHYST_SHARD);
        return decorate(base, name, lore);
    }
}
