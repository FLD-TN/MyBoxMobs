package com.dex.myboxmobs.gui;

import com.dex.myboxmobs.MyBoxMobsPlugin;
import com.dex.myboxmobs.text.Lore;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** {@code /mbm}: entry to every admin menu (spec 11). */
public final class MainMenu extends Menu {

    public MainMenu(MyBoxMobsPlugin plugin, Player player) {
        super(plugin, player, 3, "&8MyBoxMobs");
    }

    @Override
    protected void draw() {
        set(10, icon(Material.ZOMBIE_HEAD, Palette.MOB + "Danh sách mob", Lore.of()
                .desc("Tạo và chỉnh sửa mob, phần thưởng, thông báo.")
                .section("Thông tin:").row("Số mob", String.valueOf(plugin.registry().all().size()))
                .action("Chuột trái để mở").build()), click -> Gui.mobs(plugin, player));
        set(12, icon(Material.SPAWNER, Palette.SPAWNER + "Spawner", Lore.of()
                .desc("Điểm spawn mob theo đợt, có holo đếm ngược.")
                .section("Thông tin:").row("Số spawner", String.valueOf(plugin.spawners().all().size()))
                .action("Chuột trái để mở").build()), click -> Gui.spawners(plugin, player));
        set(14, icon(Material.SCULK_SHRIEKER, Palette.ALTAR + "Tế đàn", Lore.of()
                .desc("Người chơi cầm vật phẩm yêu cầu, chuột phải vào block để triệu hồi boss.")
                .section("Thông tin:").row("Số tế đàn", String.valueOf(plugin.altars().all().size()))
                .action("Chuột trái để mở").build()), click -> Gui.altars(plugin, player));
        set(16, icon(Material.CLOCK, Palette.ALIVE + "Mob đang sống", Lore.of()
                .desc("Các mob do MyBoxMobs tạo ra còn sống.")
                .section("Thông tin:").row("Đang sống", String.valueOf(plugin.mobs().tracker().size()))
                .action("Chuột trái để mở").build()), click -> Gui.alive(plugin, player));
        set(22, icon(Material.REPEATER, "&#BFC7D5Tải lại", Lore.of()
                .desc("Đọc lại config, messages, mob, spawner và tế đàn từ file.")
                .action("Chuột trái để tải lại").build()), click -> {
            player.closeInventory();
            plugin.reloadAll();
            plugin.messages().send(player, "command.reloaded");
        });
        fill(0);
    }
}
