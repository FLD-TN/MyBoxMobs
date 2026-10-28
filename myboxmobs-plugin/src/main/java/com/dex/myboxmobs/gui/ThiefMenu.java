package com.dex.myboxmobs.gui;

import com.dex.myboxmobs.MyBoxMobsPlugin;
import com.dex.myboxmobs.mob.MobDefinition;
import com.dex.myboxmobs.mob.ThiefSettings;
import com.dex.myboxmobs.text.Format;
import com.dex.myboxmobs.text.Lore;
import java.util.function.DoubleConsumer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/** The thief skill of one mob ({@link ThiefSettings}): one row per part, every value editable. */
public final class ThiefMenu extends Menu {

    private final MobDefinition def;
    private final Menu parent;

    public ThiefMenu(MyBoxMobsPlugin plugin, Player player, MobDefinition def, Menu parent) {
        super(plugin, player, 6, "&8Kỹ năng kẻ trộm &8[" + def.id + "&8]");
        this.def = def;
        this.parent = parent;
    }

    private void save() {
        plugin.registry().save(def);
    }

    @Override
    protected void draw() {
        ThiefSettings t = def.thief;
        String gem = plugin.economy().gemName();
        // row 1: stealing
        label(0, "Cướp", "Mỗi lần mob đánh trúng một người chơi.");
        toggle(1, Material.EMERALD, "Cướp tiền", t.stealMoney, "Cướp tiền Vault của người bị đánh.", () -> t.stealMoney = !t.stealMoney);
        percentRange(2, Material.PAPER, "Mức cướp tiền", t.moneyMinPercent, t.moneyMaxPercent, "% số dư mỗi lần, ngẫu nhiên trong khoảng.",
                (min, max) -> {
                    t.moneyMinPercent = min;
                    t.moneyMaxPercent = max;
                });
        number(3, Material.GOLD_INGOT, "Tối đa mỗi lần (tiền)", Palette.MONEY + Format.number(t.moneyCap) + "$",
                "Dù % ra bao nhiêu cũng không cướp quá số này. 0 là không giới hạn.", 0, 1_000_000_000, v -> t.moneyCap = Math.round(v));
        toggle(5, Material.AMETHYST_SHARD, "Cướp gem", t.stealGem, "Cướp gem BoxCore của người bị đánh.", () -> t.stealGem = !t.stealGem);
        percentRange(6, Material.PAPER, "Mức cướp gem", t.gemMinPercent, t.gemMaxPercent, "% số gem mỗi lần, ngẫu nhiên trong khoảng.",
                (min, max) -> {
                    t.gemMinPercent = min;
                    t.gemMaxPercent = max;
                });
        number(7, Material.AMETHYST_CLUSTER, "Tối đa mỗi lần (gem)", Palette.GEM + Format.number(t.gemCap) + " " + gem,
                "Dù % ra bao nhiêu cũng không cướp quá số này. 0 là không giới hạn.", 0, 1_000_000_000, v -> t.gemCap = Math.round(v));
        number(8, Material.CLOCK, "Hồi chiêu mỗi người", t.stealCooldown + " giây",
                "Một người chỉ bị cướp lại sau bấy nhiêu giây.", 0, 3600, v -> t.stealCooldown = (int) v);
        // row 2: bag
        label(9, "Túi", "Người hạ gục nhận cả túi và phần cướp còn lại.");
        number(10, Material.GOLD_BLOCK, "Túi tiền ban đầu", Palette.MONEY + Format.number(t.bagMoney) + "$",
                "Có sẵn khi mob xuất hiện.", 0, 1_000_000_000, v -> t.bagMoney = Math.round(v));
        number(11, Material.AMETHYST_BLOCK, "Túi gem ban đầu", Palette.GEM + Format.number(t.bagGem) + " " + gem,
                "Có sẵn khi mob xuất hiện.", 0, 1_000_000_000, v -> t.bagGem = Math.round(v));
        number(12, Material.GOLD_NUGGET, "Thêm tiền mỗi người", Palette.MONEY + Format.number(t.bonusMoney) + "$",
                "Cộng vào túi cho mỗi người chơi ở gần lúc mob xuất hiện.", 0, 1_000_000_000, v -> t.bonusMoney = Math.round(v));
        number(13, Material.PRISMARINE_CRYSTALS, "Thêm gem mỗi người", Palette.GEM + Format.number(t.bonusGem) + " " + gem,
                "Cộng vào túi cho mỗi người chơi ở gần lúc mob xuất hiện.", 0, 1_000_000_000, v -> t.bonusGem = Math.round(v));
        number(14, Material.COMPASS, "Phạm vi", t.radius + " block",
                "Đếm người chơi lúc xuất hiện và tìm người để cướp trong phạm vi này.", 1, 128, v -> t.radius = (int) v);
        // row 3: hunt
        label(18, "Đi săn", "Khoảng thời gian ngay sau khi xuất hiện.");
        number(19, Material.CLOCK, "Thời gian đi săn", Format.duration(t.huntSeconds),
                "Mob chạy nhanh và đuổi theo người chơi gần nhất chưa bị cướp.", 0, 3600, v -> t.huntSeconds = (int) v);
        number(20, Material.SUGAR, "Tốc độ khi đi săn", "x" + Format.decimal(t.huntSpeed),
                "Nhân với tốc độ gốc của mob.", 0.1, 5, v -> t.huntSpeed = v);
        number(21, Material.COBWEB, "Tốc độ sau đi săn", "x" + Format.decimal(t.slowSpeed),
                "Nhân với tốc độ gốc của mob, dưới 1 là đi chậm lại.", 0.1, 5, v -> t.slowSpeed = v);
        // row 4: teleport
        label(27, "Dịch chuyển", "Chỉ sau khi hết thời gian đi săn.");
        number(28, Material.ENDER_PEARL, "Dịch chuyển mỗi khi mất", Format.decimal(t.blinkPercent) + "% máu tối đa",
                "0 là tắt dịch chuyển.", 0, 100, v -> t.blinkPercent = v);
        number(29, Material.ENDER_EYE, "Khoảng cách dịch chuyển", t.blinkDistance + " block",
                "Mob từ spawner có khu vực thì luôn ở trong khu vực đó.", 1, 64, v -> t.blinkDistance = (int) v);
        // row 5: refund
        label(36, "Trả lại", "Mỗi đòn người chơi đánh trúng mob.");
        percentRange(37, Material.HOPPER, "Trả lại mỗi hit", t.refundMinPercent, t.refundMaxPercent,
                "% của tổng số đã cướp, ngẫu nhiên trong khoảng, tới khi hết phần đã cướp.", (min, max) -> {
                    t.refundMinPercent = min;
                    t.refundMaxPercent = max;
                });
        set(45, icon(Material.ARROW, "&#BFC7D5Quay lại", Lore.of().action("Chuột trái để quay lại").build()),
                click -> parent.open());
        fill(0);
    }

    private void label(int slot, String name, String desc) {
        set(slot, icon(Material.ORANGE_STAINED_GLASS_PANE, Palette.SKILL + name, Lore.of().desc(desc).build()), null);
    }

    private void toggle(int slot, Material material, String name, boolean on, String desc, Runnable flip) {
        set(slot, icon(material, Palette.SKILL + name, Lore.of().desc(desc)
                .section("Thông tin:").row("Hiện tại", Palette.onOff(on))
                .action("Chuột trái để bật/tắt").build()), click -> {
            flip.run();
            save();
            refresh();
        });
    }

    private void number(int slot, Material material, String name, String current, String desc, double min, double max,
                        DoubleConsumer apply) {
        set(slot, icon(material, Palette.SKILL + name, Lore.of().desc(desc)
                .section("Thông tin:").row("Hiện tại", current)
                .action("Chuột trái để nhập số").build()), click -> askNumber("input.stat", min, max, v -> {
                    apply.accept(v);
                    save();
                }));
    }

    private interface RangeSetter {
        void set(double min, double max);
    }

    private void percentRange(int slot, Material material, String name, double min, double max, String desc,
                              RangeSetter apply) {
        set(slot, icon(material, Palette.SKILL + name, Lore.of().desc(desc)
                .section("Thông tin:").row("Hiện tại", Format.decimal(min) + "% - " + Format.decimal(max) + "%")
                .action("Chuột trái để nhập khoảng").build()), click -> ask("input.percent-range", text -> {
                    double[] r = percentRange(text);
                    if (r == null) {
                        plugin.messages().send(player, "input.invalid-percent-range");
                        return;
                    }
                    apply.set(r[0], r[1]);
                    save();
                }));
    }

    /** "1-3" / "0,5 - 2" / "2" -> {min, max} within 0-100; null when invalid. */
    static @Nullable double[] percentRange(String text) {
        String[] parts = text.split("-");
        if (parts.length < 1 || parts.length > 2) return null;
        double a = Format.parse(parts[0].strip());
        double b = parts.length == 2 ? Format.parse(parts[1].strip()) : a;
        if (Double.isNaN(a) || Double.isNaN(b) || a < 0 || b < 0 || a > 100 || b > 100) return null;
        return new double[]{Math.min(a, b), Math.max(a, b)};
    }
}
