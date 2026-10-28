package com.dex.myboxmobs.gui;

import com.dex.myboxmobs.MyBoxMobsPlugin;
import com.dex.myboxmobs.mob.MobDefinition;
import com.dex.myboxmobs.mob.MobDefinition.RewardMode;
import com.dex.myboxmobs.mob.MobDefinition.SharedRecipients;
import com.dex.myboxmobs.text.Format;
import com.dex.myboxmobs.text.Lore;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Reward mode and its settings (spec 5). Only the settings the current mode uses are shown; the others come back
 * when switching to a mode that uses them. Clan modes need BoxClans.
 */
public final class RewardMenu extends Menu {

    private static final int[] SLOTS = {12, 13, 14, 15, 16};

    private final MobDefinition def;
    private final Menu parent;

    public RewardMenu(MyBoxMobsPlugin plugin, Player player, MobDefinition def, Menu parent) {
        super(plugin, player, 3, "&8Phần thưởng &8[" + def.id + "&8]");
        this.def = def;
        this.parent = parent;
    }

    private void changed() {
        plugin.registry().save(def);
        refresh();
    }

    private record Button(ItemStack item, Handler handler) {
    }

    @Override
    protected void draw() {
        boolean clans = plugin.clans().available();
        List<RewardMode> modes = new ArrayList<>();
        for (RewardMode m : RewardMode.values()) if (!m.clan || clans || m == def.rewardMode) modes.add(m);
        Lore modeLore = Lore.of().desc(describe(def.rewardMode)).section("Chế độ:");
        for (RewardMode m : modes) modeLore.line(m == def.rewardMode ? "&#7CFC9A✔ " + m.label : "&7" + m.label);
        if (def.rewardMode.clan && !clans) modeLore.note("&#FF6B6B", "✘ Chưa cài BoxClans, chế độ này không trả thưởng");
        set(10, icon(icon(def.rewardMode), Palette.REWARD + "Chế độ phần thưởng", modeLore
                .action("Chuột trái để đổi chế độ tiếp theo").secondary("Chuột phải để quay lại chế độ trước").build()),
                click -> {
                    int i = modes.indexOf(def.rewardMode);
                    int next = click.isRightClick() ? (i - 1 + modes.size()) % modes.size() : (i + 1) % modes.size();
                    def.switchMode(modes.get(next));
                    changed();
                });

        List<Button> buttons = new ArrayList<>();
        switch (def.rewardMode) {
            case KILLER -> buttons.add(new Button(icon(Material.CHEST, Palette.REWARD + "Quà cho người hạ gục", Lore.of()
                    .section("Thông tin:").row("Số mục", String.valueOf(def.killerRewards.size()))
                    .action("Chuột trái để chỉnh").build()),
                    click -> new RewardListMenu(plugin, player, def, def.killerRewards,
                            "&8Quà người hạ gục &8[" + def.id + "&8]", this).open()));
            case TOP -> buttons.add(new Button(icon(Material.GOLD_BLOCK, Palette.REWARD + "Quà top sát thương", Lore.of()
                    .desc("Mỗi hạng có tỉ lệ nhận và quà riêng.")
                    .section("Thông tin:").row("Số hạng", String.valueOf(def.topRanks.size()))
                    .action("Chuột trái để chỉnh").build()), click -> new TopRanksMenu(plugin, player, def, this).open()));
            case TOP_CLAN -> {
                buttons.add(new Button(icon(Material.BEACON, Palette.CLAN + "Quà top bang hội", Lore.of()
                        .desc("Mỗi hạng: Danh Vọng, Minh Huy cho bang hội và quà cho từng thành viên đã đánh.")
                        .section("Thông tin:").row("Số hạng", String.valueOf(def.clanRanks.size()))
                        .action("Chuột trái để chỉnh").build()), click -> new ClanRanksMenu(plugin, player, def, this).open()));
                buttons.add(new Button(icon(Material.COMPARATOR, Palette.CLAN + "Đóng góp tối thiểu", Lore.of()
                        .desc("Thành viên phải gây ít nhất bấy nhiêu % sát thương của bang mình mới nhận quà thành viên. 0% là ai đánh cũng nhận.")
                        .section("Thông tin:").row("Hiện tại", Format.decimal(def.clanMinShare) + "%")
                        .action("Chuột trái để nhập số").build()), click -> askNumber("input.clan-min-share", 0, 100, v -> {
                            def.clanMinShare = v;
                            plugin.registry().save(def);
                        })));
            }
            case CLAN_SHARED -> {
                buttons.add(new Button(icon(Material.ENDER_CHEST, Palette.CLAN + "Quà bang hội", Lore.of()
                        .desc("Quay một lần, mọi người nhận quà như nhau.")
                        .section("Thông tin:").row("Số mục", String.valueOf(def.sharedRewards.size()))
                        .action("Chuột trái để chỉnh").build()),
                        click -> new RewardListMenu(plugin, player, def, def.sharedRewards,
                                "&8Quà bang hội &8[" + def.id + "&8]", this).open()));
                boolean participants = def.sharedRecipients == SharedRecipients.PARTICIPANTS;
                buttons.add(new Button(icon(participants ? Material.IRON_SWORD : Material.PLAYER_HEAD,
                        Palette.CLAN + "Người nhận quà", Lore.of()
                                .desc("Đã tham gia: thành viên có gây sát thương và đang online. Cả bang: mọi thành viên đang online.")
                                .section("Thông tin:").row("Hiện tại", participants ? "Đã tham gia" : "Cả bang")
                                .action("Chuột trái để đổi").build()), click -> {
                    def.sharedRecipients = participants ? SharedRecipients.ONLINE : SharedRecipients.PARTICIPANTS;
                    changed();
                }));
            }
        }
        if (def.rewardMode != RewardMode.CLAN_SHARED && clans) {
            buttons.add(new Button(icon(Material.GOLDEN_HELMET, Palette.CLAN + "Danh Vọng cho bang người hạ gục", Lore.of()
                    .desc("Cộng cho bang hội của người đánh đòn cuối. 0 là tắt.")
                    .section("Thông tin:").row("Hiện tại", def.killerClanPoints <= 0 ? "tắt"
                            : Palette.DANH_VONG + Format.number(def.killerClanPoints) + " Danh Vọng")
                    .action("Chuột trái để nhập số").build()), click -> askNumber("input.clan-points", 0, 1_000_000, v -> {
                        def.killerClanPoints = Math.round(v);
                        plugin.registry().save(def);
                    })));
        }
        buttons.add(new Button(icon(Material.EXPERIENCE_BOTTLE, Palette.REWARD + "Nhân thưởng theo rank", Lore.of()
                .desc("Dùng boost của BoxCore (quyền boxcore.boost." + plugin.settings().boostType
                        + ".<số>). Nhân vật phẩm, tiền, gem; không nhân lệnh, tỉ lệ, Danh Vọng và Minh Huy.")
                .section("Thông tin:").row("Hiện tại", Palette.onOff(def.multiplierEnabled))
                .action("Chuột trái để bật/tắt").build()), click -> {
            def.multiplierEnabled = !def.multiplierEnabled;
            changed();
        }));
        for (int i = 0; i < buttons.size() && i < SLOTS.length; i++) set(SLOTS[i], buttons.get(i).item(), buttons.get(i).handler());

        set(22, icon(Material.ARROW, "&#BFC7D5Quay lại", Lore.of().action("Chuột trái để quay lại").build()),
                click -> parent.open());
        fill(0);
    }

    private static String describe(RewardMode mode) {
        return switch (mode) {
            case KILLER -> "Chỉ người đánh đòn cuối nhận quà.";
            case TOP -> "Chia quà theo hạng sát thương của từng người, mỗi hạng có tỉ lệ nhận và quà riêng.";
            case TOP_CLAN -> "Cộng sát thương theo bang hội. Mỗi hạng bang nhận Danh Vọng, Minh Huy; thành viên đã đánh và đang online nhận quà.";
            case CLAN_SHARED -> "Cho boss của tế đàn chỉ Minh Chủ triệu hồi. Không có Danh Vọng hay Minh Huy, mọi người nhận quà như nhau.";
        };
    }

    private static Material icon(RewardMode mode) {
        return switch (mode) {
            case KILLER -> Material.WOODEN_SWORD;
            case TOP -> Material.GOLDEN_SWORD;
            case TOP_CLAN -> Material.BEACON;
            case CLAN_SHARED -> Material.ENDER_CHEST;
        };
    }
}
