package com.dex.myboxmobs.gui;

import com.dex.myboxmobs.MyBoxMobsPlugin;
import com.dex.myboxmobs.mob.ClanRank;
import com.dex.myboxmobs.mob.MobDefinition;
import com.dex.myboxmobs.text.Format;
import com.dex.myboxmobs.text.Lore;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

/** Clan top ranks 1..N: chance, Danh Vọng, Minh Huy and member gifts per rank. */
public final class ClanRanksMenu extends Menu {

    private static final int MAX = 45;

    private final MobDefinition def;
    private final Menu parent;

    public ClanRanksMenu(MyBoxMobsPlugin plugin, Player player, MobDefinition def, Menu parent) {
        super(plugin, player, 6, "&8Quà top bang hội &8[" + def.id + "&8]");
        this.def = def;
        this.parent = parent;
    }

    @Override
    protected void draw() {
        for (int i = 0; i < Math.min(MAX, def.clanRanks.size()); i++) {
            ClanRank rank = def.clanRanks.get(i);
            int number = i + 1;
            Material material = number == 1 ? Material.GOLD_BLOCK : number == 2 ? Material.IRON_BLOCK
                    : number == 3 ? Material.COPPER_BLOCK : Material.STONE;
            set(i, icon(material, Palette.CLAN + "Top " + number + " bang hội", Lore.of()
                    .section("Thông tin:").row("Tỉ lệ nhận quà", Format.decimal(rank.chance) + "%")
                    .row("Danh Vọng", Palette.DANH_VONG + Format.number(rank.danhVong))
                    .row("Minh Huy", Palette.MINH_HUY + Format.number(rank.minhHuy))
                    .row("Quà thành viên", rank.rewards.size() + " mục")
                    .action("Chuột trái để chỉnh").danger("Shift + chuột phải để xoá hạng").build()), click -> {
                if (click == ClickType.SHIFT_RIGHT) {
                    def.clanRanks.remove(rank);
                    plugin.registry().save(def);
                    refresh();
                    return;
                }
                new ClanRankMenu(plugin, player, def, rank, number, this).open();
            });
        }
        set(45, icon(Material.ARROW, "&#BFC7D5Quay lại", Lore.of().action("Chuột trái để quay lại").build()),
                click -> parent.open());
        set(49, icon(Material.LIME_DYE, Palette.CLAN + "Thêm hạng", Lore.of()
                .desc("Thêm hạng tiếp theo, mặc định tỉ lệ nhận 100%, chưa có thưởng.")
                .section("Thông tin:").row("Số hạng", def.clanRanks.size() + "/" + MAX)
                .action("Chuột trái để thêm").build()), click -> {
            if (def.clanRanks.size() >= MAX) return;
            def.clanRanks.add(new ClanRank());
            plugin.registry().save(def);
            refresh();
        });
        fill(5);
    }

    /** One clan rank. */
    static final class ClanRankMenu extends Menu {

        private final MobDefinition def;
        private final ClanRank rank;
        private final int number;
        private final Menu parent;

        ClanRankMenu(MyBoxMobsPlugin plugin, Player player, MobDefinition def, ClanRank rank, int number, Menu parent) {
            super(plugin, player, 3, "&8Top " + number + " bang hội &8[" + def.id + "&8]");
            this.def = def;
            this.rank = rank;
            this.number = number;
            this.parent = parent;
        }

        private void save() {
            plugin.registry().save(def);
        }

        @Override
        protected void draw() {
            set(10, icon(Material.RABBIT_FOOT, Palette.CLAN + "Tỉ lệ nhận quà", Lore.of()
                    .desc("Trượt thì bang hội không nhận gì ở hạng này.")
                    .section("Thông tin:").row("Hiện tại", Format.decimal(rank.chance) + "%")
                    .action("Chuột trái để nhập số").build()), click -> askNumber("input.rank-chance", 0.01, 100, v -> {
                        rank.chance = v;
                        save();
                    }));
            set(12, icon(Material.GOLDEN_HELMET, Palette.DANH_VONG + "Danh Vọng", Lore.of()
                    .desc("Cộng vào điểm bang hội qua nguồn " + plugin.settings().clanSourceTop + " của BoxClans. 0 là không có.")
                    .section("Thông tin:").row("Hiện tại", Palette.DANH_VONG + Format.number(rank.danhVong))
                    .action("Chuột trái để nhập số").build()), click -> askNumber("input.clan-points", 0, 1_000_000, v -> {
                        rank.danhVong = Math.round(v);
                        save();
                    }));
            set(13, icon(Material.AMETHYST_SHARD, Palette.MINH_HUY + "Minh Huy", Lore.of()
                    .desc("Cộng vào quỹ bang hội. 0 là không có.")
                    .section("Thông tin:").row("Hiện tại", Palette.MINH_HUY + Format.number(rank.minhHuy))
                    .action("Chuột trái để nhập số").build()), click -> askNumber("input.clan-currency", 0, 1_000_000_000, v -> {
                        rank.minhHuy = Math.round(v);
                        save();
                    }));
            set(15, icon(Material.CHEST, Palette.CLAN + "Quà thành viên", Lore.of()
                    .desc("Mỗi thành viên của bang đã đánh, đang online và đủ đóng góp tối thiểu nhận một phần.")
                    .section("Thông tin:").row("Số mục", String.valueOf(rank.rewards.size()))
                    .action("Chuột trái để chỉnh").build()),
                    click -> new RewardListMenu(plugin, player, def, rank.rewards,
                            "&8Quà thành viên top " + number + " &8[" + def.id + "&8]", this).open());
            set(18, icon(Material.ARROW, "&#BFC7D5Quay lại", Lore.of().action("Chuột trái để quay lại").build()),
                    click -> parent.open());
            fill(0);
        }
    }
}
