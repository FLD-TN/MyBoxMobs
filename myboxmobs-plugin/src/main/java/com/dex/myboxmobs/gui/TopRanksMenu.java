package com.dex.myboxmobs.gui;

import com.dex.myboxmobs.MyBoxMobsPlugin;
import com.dex.myboxmobs.mob.MobDefinition;
import com.dex.myboxmobs.mob.TopRank;
import com.dex.myboxmobs.text.Format;
import com.dex.myboxmobs.text.Lore;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

/** Top damage ranks 1..N (spec 5.2): receive chance and reward list per rank. */
public final class TopRanksMenu extends Menu {

    private static final int MAX = 45;

    private final MobDefinition def;
    private final Menu parent;

    public TopRanksMenu(MyBoxMobsPlugin plugin, Player player, MobDefinition def, Menu parent) {
        super(plugin, player, 6, "&8Quà top sát thương &8[" + def.id + "&8]");
        this.def = def;
        this.parent = parent;
    }

    @Override
    protected void draw() {
        for (int i = 0; i < Math.min(MAX, def.topRanks.size()); i++) {
            TopRank rank = def.topRanks.get(i);
            int number = i + 1;
            Material material = number == 1 ? Material.GOLD_BLOCK : number == 2 ? Material.IRON_BLOCK
                    : number == 3 ? Material.COPPER_BLOCK : Material.STONE;
            set(i, icon(material, Palette.REWARD + "Top " + number, Lore.of()
                    .section("Thông tin:").row("Tỉ lệ nhận quà", Format.decimal(rank.chance) + "%")
                    .row("Số mục quà", String.valueOf(rank.rewards.size()))
                    .action("Chuột trái để chỉnh quà").secondary("Chuột phải để đổi tỉ lệ nhận")
                    .danger("Shift + chuột phải để xoá hạng").build()), click -> {
                if (click == ClickType.SHIFT_RIGHT) {
                    def.topRanks.remove(rank);
                    plugin.registry().save(def);
                    refresh();
                } else if (click.isRightClick()) {
                    askNumber("input.rank-chance", 0.01, 100, v -> {
                        rank.chance = v;
                        plugin.registry().save(def);
                    });
                } else {
                    new RewardListMenu(plugin, player, def, rank.rewards,
                            "&8Quà top " + number + " &8[" + def.id + "&8]", this).open();
                }
            });
        }
        set(45, icon(Material.ARROW, "&#BFC7D5Quay lại", Lore.of().action("Chuột trái để quay lại").build()),
                click -> parent.open());
        set(49, icon(Material.LIME_DYE, Palette.REWARD + "Thêm hạng", Lore.of()
                .desc("Thêm hạng tiếp theo, mặc định tỉ lệ nhận 100%.")
                .section("Thông tin:").row("Số hạng", def.topRanks.size() + "/" + MAX)
                .action("Chuột trái để thêm").build()), click -> {
            if (def.topRanks.size() >= MAX) return;
            def.topRanks.add(new TopRank());
            plugin.registry().save(def);
            refresh();
        });
        fill(5);
    }
}
