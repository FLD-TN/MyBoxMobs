package com.dex.myboxmobs.gui;

import com.dex.myboxmobs.MyBoxMobsPlugin;
import com.dex.myboxmobs.place.AltarDef;
import com.dex.myboxmobs.place.MobRef;
import com.dex.myboxmobs.text.Format;
import com.dex.myboxmobs.text.Lore;
import com.dex.myboxmobs.text.Text;
import com.dex.myboxmobs.util.ItemNames;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

/** Every field of an altar (spec 8). Click an item in your own inventory to set the requirement. */
public final class AltarEditorMenu extends Menu {

    private final AltarDef d;

    public AltarEditorMenu(MyBoxMobsPlugin plugin, Player player, AltarDef d) {
        super(plugin, player, 3, "&8Bàn triệu hồi &8[" + d.id + "&8]");
        this.d = d;
    }

    private void changed() {
        plugin.altars().save();
        refresh();
    }

    @Override
    protected void draw() {
        set(10, icon(Material.ZOMBIE_HEAD, Palette.ALTAR + "Boss triệu hồi", Lore.of()
                .section("Thông tin:").row("Hiện tại", d.mob == null ? "chưa chọn" : plugin.spawners().mobName(d.mob))
                .action("Chuột trái để chọn mob MyBoxMobs").secondary("Chuột phải để chọn mob MythicMobs gốc").build()),
                click -> {
                    if (click.isRightClick()) {
                        Gui.pickMythic(plugin, player, "&8Chọn mob MythicMobs", name -> {
                            d.mob = new MobRef(true, name);
                            plugin.altars().save();
                            open();
                        }, this::open);
                        return;
                    }
                    Gui.pickDef(plugin, player, "&8Chọn mob", def -> {
                        d.mob = new MobRef(false, def.id);
                        plugin.altars().save();
                        open();
                    }, this::open);
                });
        ItemStack shown;
        if (d.requirement == null) {
            shown = icon(Material.STRUCTURE_VOID, Palette.ALTAR + "Vật phẩm yêu cầu &8(chưa đặt)", Lore.of()
                    .desc("Bấm vào một vật phẩm trong túi của bạn để đặt làm vật phẩm yêu cầu. Số lượng lấy theo chồng đó, so khớp tuyệt đối.")
                    .build());
        } else {
            shown = withExtraLore(d.requirement, Lore.of().section("Vật phẩm yêu cầu:")
                    .row("Cần", "&fx" + d.requirement.getAmount() + " " + ItemNames.of(d.requirement))
                    .desc("Muốn thay vật phẩm khác: bấm vào vật phẩm đó trong túi của bạn.")
                    .action("Chuột trái để đổi số lượng").danger("Chuột phải để bỏ vật phẩm yêu cầu").build());
        }
        set(12, shown, click -> {
            if (d.requirement == null) return;
            if (click.isRightClick()) {
                d.requirement = null;
                changed();
                plugin.messages().send(player, "altar.requirement-cleared");
                return;
            }
            askNumber("input.amount-required", 1, 64, v -> {
                d.requirement.setAmount((int) v);
                plugin.altars().save();
            });
        });
        set(13, icon(Material.CLOCK, Palette.ALTAR + "Thời gian chờ", Lore.of()
                .desc("Sau khi boss chết hoặc biến mất, chờ bao lâu mới triệu hồi tiếp.")
                .section("Thông tin:").row("Hiện tại", d.cooldownSeconds <= 0 ? "không chờ" : Format.duration(d.cooldownSeconds))
                .action("Chuột trái để nhập số giây").danger("Chuột phải để xoá thời gian chờ đang chạy").build()), click -> {
            if (click.isRightClick()) {
                d.cooldownUntil = 0;
                changed();
                return;
            }
            askNumber("input.cooldown", 0, 604800, v -> {
                d.cooldownSeconds = (int) v;
                plugin.altars().save();
            });
        });
        if (plugin.clans().available() || d.clan) {
            Lore clanLore = Lore.of()
                    .desc("Chỉ Minh Chủ được triệu hồi. Boss thuộc về bang hội đó: chỉ thành viên mới đánh được, boss vẫn đánh mọi người. Thông báo chỉ gửi trong bang.")
                    .section("Thông tin:").row("Hiện tại", Palette.onOff(d.clan));
            if (!plugin.clans().available()) clanLore.note("&#FF6B6B", "✘ Chưa cài BoxClans, bàn đang không dùng được");
            if (d.clan && d.mob != null && !d.mob.mythic()) {
                var def = plugin.registry().get(d.mob.id());
                if (def != null && def.rewardMode != com.dex.myboxmobs.mob.MobDefinition.RewardMode.CLAN_SHARED) {
                    clanLore.note("&#FFD166", "(!) Mob đang ở chế độ " + def.rewardMode.label + ", nên đổi sang Quà bang hội");
                }
            }
            set(14, icon(d.clan ? Material.WHITE_BANNER : Material.GRAY_BANNER, Palette.CLAN + "Bàn bang hội",
                    clanLore.action("Chuột trái để bật/tắt").build()), click -> {
                d.clan = !d.clan;
                changed();
            });
        }
        set(15, icon(Material.NAME_TAG, Palette.ALTAR + "Hologram", Lore.of()
                .section("Thông tin:").row("Số dòng", String.valueOf(d.holoLines.size()))
                .row("DecentHolograms", plugin.holograms().available() ? "có" : "&#FF6B6Bchưa cài")
                .action("Chuột trái để sửa nội dung").build()), click -> new LinesMenu(plugin, player,
                "&8Holo bàn &8[" + d.id + "&8]", d.holoLines,
                List.of("%mob% tên boss", "%item% vật phẩm yêu cầu", "%amount% số lượng"),
                () -> plugin.altars().save(), () -> player.sendMessage(Text.parse(String.join("\n", d.holoLines))), this).open());
        set(16, icon(Material.ENDER_PEARL, Palette.ALTAR + "Vị trí", Lore.of()
                .section("Thông tin:").row("Hiện tại", d.world + " " + d.x + " " + d.y + " " + d.z)
                .row("Boss", d.boss != null ? "đang sống" : "không")
                .action("Chuột trái để dịch chuyển tới").build()), click -> {
            Location at = d.blockLocation();
            if (at != null) {
                player.closeInventory();
                player.teleport(at.clone().add(0.5, 1, 0.5));
            }
        });
        set(18, icon(Material.ARROW, "&#BFC7D5Quay lại", Lore.of().action("Chuột trái để quay lại").build()),
                click -> Gui.altars(plugin, player));
        set(26, icon(Material.BARRIER, "&#FF8A8AXoá bàn triệu hồi", Lore.of().danger("Chuột trái để xoá").build()),
                click -> new ConfirmMenu(plugin, player, "bàn " + d.id, () -> {
                    plugin.altars().delete(d);
                    plugin.messages().send(player, "altar.deleted", "id", d.id);
                    Gui.altars(plugin, player);
                }, this::open).open());
        fill(0);
    }

    @Override
    protected void onOwnItemClick(ItemStack item, ClickType click) {
        boolean replaced = d.requirement != null;
        d.requirement = item.clone();
        changed();
        plugin.messages().send(player, replaced ? "altar.requirement-replaced" : "altar.requirement-set",
                "item", ItemNames.of(item), "amount", String.valueOf(item.getAmount()));
    }
}
