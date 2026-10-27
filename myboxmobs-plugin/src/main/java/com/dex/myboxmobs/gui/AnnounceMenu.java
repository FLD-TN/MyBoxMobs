package com.dex.myboxmobs.gui;

import com.dex.myboxmobs.MyBoxMobsPlugin;
import com.dex.myboxmobs.mob.MobDefinition;
import com.dex.myboxmobs.text.Lore;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** Spawn / death / despawn announcements of a mob (spec 6). */
public final class AnnounceMenu extends Menu {

    private final MobDefinition def;
    private final Menu parent;

    public AnnounceMenu(MyBoxMobsPlugin plugin, Player player, MobDefinition def, Menu parent) {
        super(plugin, player, 3, "&8Thông báo &8[" + def.id + "&8]");
        this.def = def;
        this.parent = parent;
    }

    @Override
    protected void draw() {
        entry(11, Material.LIME_BANNER, "Khi xuất hiện", def.spawn,
                List.of("%mob% tên mob", "%world% %x% %y% %z% vị trí", "%summoner% người triệu hồi (bàn triệu hồi)"));
        entry(13, Material.RED_BANNER, "Khi bị hạ gục", def.death,
                List.of("%mob% tên mob", "%killer% người hạ gục", "%killer_damage% sát thương của họ",
                        "%top% (một dòng riêng) bảng top, mọi chế độ trừ người hạ gục",
                        "%top_clans% (một dòng riêng) bảng bang hội, chế độ top bang hội; không có thì nằm ngay sau %top%"));
        entry(15, Material.GRAY_BANNER, "Khi biến mất", def.despawn, List.of("%mob% tên mob"));
        set(22, icon(Material.ARROW, "&#BFC7D5Quay lại", Lore.of().action("Chuột trái để quay lại").build()),
                click -> parent.open());
        fill(0);
    }

    private void entry(int slot, Material material, String name, MobDefinition.Announcement a, List<String> placeholders) {
        set(slot, icon(material, Palette.ANNOUNCE + name, Lore.of()
                .desc("Gửi chat cho toàn server.")
                .section("Thông tin:").row("Trạng thái", Palette.onOff(a.enabled)).row("Số dòng", String.valueOf(a.lines.size()))
                .action("Chuột trái để bật/tắt").secondary("Chuột phải để sửa nội dung").build()), click -> {
            if (click.isRightClick()) {
                new LinesMenu(plugin, player, "&8" + name + " &8[" + def.id + "&8]", a.lines, placeholders,
                        () -> plugin.registry().save(def), () -> plugin.announcer().preview(player, a.lines), this).open();
                return;
            }
            a.enabled = !a.enabled;
            plugin.registry().save(def);
            refresh();
        });
    }
}
