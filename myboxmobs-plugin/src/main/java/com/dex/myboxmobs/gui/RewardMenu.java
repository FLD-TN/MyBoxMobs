package com.dex.myboxmobs.gui;

import com.dex.myboxmobs.MyBoxMobsPlugin;
import com.dex.myboxmobs.mob.MobDefinition;
import com.dex.myboxmobs.mob.MobDefinition.RewardMode;
import com.dex.myboxmobs.text.Lore;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** Reward mode, rank multiplier and the two reward setups (spec 5.2, 5.4). */
public final class RewardMenu extends Menu {

    private final MobDefinition def;
    private final Menu parent;

    public RewardMenu(MyBoxMobsPlugin plugin, Player player, MobDefinition def, Menu parent) {
        super(plugin, player, 3, "&8Phần thưởng &8[" + def.id + "&8]");
        this.def = def;
        this.parent = parent;
    }

    @Override
    protected void draw() {
        boolean top = def.rewardMode == RewardMode.TOP;
        set(10, icon(top ? Material.GOLDEN_SWORD : Material.WOODEN_SWORD, Palette.REWARD + "Chế độ phần thưởng", Lore.of()
                .desc("Người hạ gục: chỉ người đánh đòn cuối nhận quà. Top sát thương: chia theo hạng, mỗi hạng có tỉ lệ nhận và quà riêng.")
                .section("Thông tin:").row("Hiện tại", top ? "Top sát thương" : "Người hạ gục")
                .action("Chuột trái để đổi chế độ").build()), click -> {
            def.rewardMode = top ? RewardMode.KILLER : RewardMode.TOP;
            plugin.registry().save(def);
            refresh();
        });
        set(12, icon(Material.EXPERIENCE_BOTTLE, Palette.REWARD + "Nhân thưởng theo rank", Lore.of()
                .desc("Dùng boost của BoxCore (quyền boxcore.boost." + plugin.settings().boostType
                        + ".<số>). Nhân vật phẩm, tiền, gem; không nhân lệnh và tỉ lệ.")
                .section("Thông tin:").row("Hiện tại", Palette.onOff(def.multiplierEnabled))
                .action("Chuột trái để bật/tắt").build()), click -> {
            def.multiplierEnabled = !def.multiplierEnabled;
            plugin.registry().save(def);
            refresh();
        });
        set(14, icon(Material.CHEST, Palette.REWARD + "Quà cho người hạ gục", Lore.of()
                .section("Thông tin:").row("Số mục", String.valueOf(def.killerRewards.size()))
                .row("Đang dùng", top ? "&#FF6B6B✘ không (đang ở chế độ top)" : "&#7CFC9A✔ có")
                .action("Chuột trái để chỉnh").build()),
                click -> new RewardListMenu(plugin, player, def, def.killerRewards,
                        "&8Quà người hạ gục &8[" + def.id + "&8]", this).open());
        set(16, icon(Material.GOLD_BLOCK, Palette.REWARD + "Quà top sát thương", Lore.of()
                .section("Thông tin:").row("Số hạng", String.valueOf(def.topRanks.size()))
                .row("Đang dùng", top ? "&#7CFC9A✔ có" : "&#FF6B6B✘ không (đang ở chế độ người hạ gục)")
                .action("Chuột trái để chỉnh").build()), click -> new TopRanksMenu(plugin, player, def, this).open());
        set(22, icon(Material.ARROW, "&#BFC7D5Quay lại", Lore.of().action("Chuột trái để quay lại").build()),
                click -> parent.open());
        fill(0);
    }
}
