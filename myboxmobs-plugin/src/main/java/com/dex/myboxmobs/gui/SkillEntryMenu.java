package com.dex.myboxmobs.gui;

import com.dex.myboxmobs.MyBoxMobsPlugin;
import com.dex.myboxmobs.mob.MobDefinition;
import com.dex.myboxmobs.mob.SkillEntry;
import com.dex.myboxmobs.text.Format;
import com.dex.myboxmobs.text.Lore;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** One skill of a mob: trigger, target, interval / cooldown, chance. */
public final class SkillEntryMenu extends Menu {

    private final MobDefinition def;
    private final SkillEntry e;
    private final Menu parent;

    public SkillEntryMenu(MyBoxMobsPlugin plugin, Player player, MobDefinition def, SkillEntry e, Menu parent) {
        super(plugin, player, 3, "&8Skill &8[" + e.skill + "&8]");
        this.def = def;
        this.e = e;
        this.parent = parent;
    }

    private void changed() {
        plugin.registry().save(def);
        refresh();
    }

    @Override
    protected void draw() {
        set(10, icon(Material.REPEATER, Palette.SKILL + "Kích hoạt", Lore.of()
                .desc("Khi nào mob dùng skill này.")
                .section("Thông tin:").row("Hiện tại", SkillsMenu.triggerName(e))
                .action("Chuột trái để đổi").build()), click -> {
            SkillEntry.Trigger[] all = SkillEntry.Trigger.values();
            e.trigger = all[(e.trigger.ordinal() + 1) % all.length];
            // event triggers default to a short cooldown so a fast fight does not spam the skill
            if ((e.trigger == SkillEntry.Trigger.DAMAGED || e.trigger == SkillEntry.Trigger.ATTACK) && e.seconds == 0) {
                e.seconds = 3;
            }
            changed();
        });
        boolean timer = e.trigger == SkillEntry.Trigger.TIMER;
        boolean once = e.trigger == SkillEntry.Trigger.SPAWN || e.trigger == SkillEntry.Trigger.DEATH;
        set(12, icon(Material.CLOCK, Palette.SKILL + (timer ? "Mỗi bao lâu" : "Thời gian hồi"), Lore.of()
                .desc(timer ? "Khoảng cách giữa hai lần dùng skill." : once ? "Không dùng cho kích hoạt này."
                        : "Thời gian chờ trước khi skill có thể kích hoạt lại, 0 = không chờ.")
                .section("Thông tin:").row("Hiện tại", e.seconds <= 0 ? "không" : Format.duration(e.seconds))
                .action("Chuột trái để nhập số giây").build()), click -> askNumber("input.skill-seconds",
                timer ? 1 : 0, 86400, v -> {
                    e.seconds = (int) v;
                    plugin.registry().save(def);
                }));
        set(14, icon(Material.TARGET, Palette.SKILL + "Mục tiêu", Lore.of()
                .desc("Mục tiêu cho các dòng skill không ghi targeter riêng.")
                .section("Thông tin:").row("Hiện tại", SkillsMenu.targetName(e.target))
                .action("Chuột trái để đổi").build()), click -> {
            SkillEntry.Target[] all = SkillEntry.Target.values();
            e.target = all[(e.target.ordinal() + 1) % all.length];
            changed();
        });
        set(16, icon(Material.RABBIT_FOOT, Palette.SKILL + "Tỉ lệ dùng", Lore.of()
                .desc("Mỗi lần đến lượt, skill có tỉ lệ này để thực sự được dùng.")
                .section("Thông tin:").row("Hiện tại", Format.decimal(e.chance) + "%")
                .action("Chuột trái để nhập (0,01 - 100)").build()), click -> askNumber("input.chance", 0.01, 100, v -> {
            e.chance = v;
            plugin.registry().save(def);
        }));
        set(18, icon(Material.ARROW, "&#BFC7D5Quay lại", Lore.of().action("Chuột trái để quay lại").build()),
                click -> parent.open());
        set(26, icon(Material.BARRIER, "&#FF8A8AXoá skill", Lore.of().danger("Chuột trái để xoá").build()), click -> {
            def.skills.remove(e);
            plugin.registry().save(def);
            parent.open();
        });
        fill(0);
    }
}
