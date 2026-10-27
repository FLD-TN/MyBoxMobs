package com.dex.myboxmobs.gui;

import com.dex.myboxmobs.MyBoxMobsPlugin;
import com.dex.myboxmobs.mob.MobDefinition;
import com.dex.myboxmobs.mob.SkillEntry;
import com.dex.myboxmobs.text.Format;
import com.dex.myboxmobs.text.Lore;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

/** MythicMobs skills a mob casts, reusing skills of other MythicMobs bosses (skill settings of a mob). */
public final class SkillsMenu extends Menu {

    private static final int MAX = 45;

    private final MobDefinition def;
    private final Menu parent;

    public SkillsMenu(MyBoxMobsPlugin plugin, Player player, MobDefinition def, Menu parent) {
        super(plugin, player, 6, "&8Skill &8[" + def.id + "&8]");
        this.def = def;
        this.parent = parent;
    }

    @Override
    protected void draw() {
        for (int i = 0; i < Math.min(MAX, def.skills.size()); i++) {
            SkillEntry e = def.skills.get(i);
            boolean exists = plugin.mythic().skillExists(e.skill);
            Lore lore = Lore.of().section("Cài đặt:")
                    .row("Kích hoạt", triggerName(e))
                    .row("Mục tiêu", targetName(e.target))
                    .row("Tỉ lệ", Format.decimal(e.chance) + "%");
            if (!exists) lore.line("&#FFD166(!) MythicMobs không có skill này");
            set(i, icon(exists ? Material.BLAZE_POWDER : Material.GUNPOWDER, Palette.SKILL + e.skill,
                    lore.action("Chuột trái để chỉnh").danger("Shift + chuột phải để xoá").build()), click -> {
                if (click == ClickType.SHIFT_RIGHT) {
                    def.skills.remove(e);
                    plugin.registry().save(def);
                    refresh();
                    return;
                }
                new SkillEntryMenu(plugin, player, def, e, this).open();
            });
        }
        set(45, icon(Material.ARROW, "&#BFC7D5Quay lại", Lore.of().action("Chuột trái để quay lại").build()),
                click -> parent.open());
        set(49, icon(Material.LIME_DYE, Palette.SKILL + "Thêm skill", Lore.of()
                .desc("Chọn một skill có sẵn của MythicMobs (ví dụ skill của boss khác) để mob này dùng lại.")
                .section("Thông tin:").row("Số skill", def.skills.size() + "/" + MAX)
                .action("Chuột trái để chọn skill").build()), click -> {
            if (def.skills.size() >= MAX) return;
            new PickMenu<>(plugin, player, "&8Chọn skill MythicMobs", plugin.mythic()::skillNames,
                    name -> icon(Material.BLAZE_POWDER, Palette.SKILL + name, Lore.of().action("Chuột trái để thêm").build()),
                    name -> name, (name, c) -> {
                SkillEntry e = new SkillEntry(name);
                def.skills.add(e);
                plugin.registry().save(def);
                new SkillEntryMenu(plugin, player, def, e, this).open();
            }, this::open).open();
        });
        set(51, icon(Material.BOOK, Palette.SKILL + "Hướng dẫn", Lore.of()
                .desc("Skill vẫn nằm trong file MythicMobs. Ở đây chỉ chọn khi nào mob dùng skill và nhắm vào ai.")
                .section("Lưu ý:")
                .line("&7• Dòng skill có targeter riêng")
                .line("&7  (@PIR, @target...) giữ nguyên")
                .line("&7• Skill gọi model của boss khác")
                .line("&7  sẽ không có hiệu ứng model")
                .build()), null);
        fill(5);
    }

    static String triggerName(SkillEntry e) {
        return switch (e.trigger) {
            case SPAWN -> "khi xuất hiện";
            case TIMER -> "mỗi " + Format.duration(Math.max(1, e.seconds));
            case DAMAGED -> "khi bị đánh" + cooldown(e);
            case ATTACK -> "khi đánh trúng" + cooldown(e);
            case DEATH -> "khi chết";
        };
    }

    private static String cooldown(SkillEntry e) {
        return e.seconds > 0 ? " &8(hồi " + Format.duration(e.seconds) + "&8)" : "";
    }

    static String targetName(SkillEntry.Target t) {
        return switch (t) {
            case TARGET -> "mục tiêu của mob";
            case TRIGGER -> "người kích hoạt";
            case NEAREST_PLAYER -> "người chơi gần nhất";
            case SELF -> "chính mob";
        };
    }
}
