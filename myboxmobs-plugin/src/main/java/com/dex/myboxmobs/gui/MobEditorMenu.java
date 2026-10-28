package com.dex.myboxmobs.gui;

import com.dex.myboxmobs.MyBoxMobsPlugin;
import com.dex.myboxmobs.mob.MobDefinition;
import com.dex.myboxmobs.mob.MobDefinition.Stat;
import com.dex.myboxmobs.text.Format;
import com.dex.myboxmobs.text.Lore;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** One mob's editor (spec 11): general, stats, then the sub-menus. Every change saves the mob file. */
public final class MobEditorMenu extends Menu {

    private final MobDefinition def;

    public MobEditorMenu(MyBoxMobsPlugin plugin, Player player, MobDefinition def) {
        super(plugin, player, 6, "&8Chỉnh sửa mob &8[" + def.id + "&8]");
        this.def = def;
    }

    private void save() {
        plugin.registry().save(def);
    }

    @Override
    protected void draw() {
        boolean mythic = def.source == MobDefinition.Source.MYTHIC;
        // row 1: general
        set(1, icon(Material.NAME_TAG, Palette.GENERAL + "Tên hiển thị", Lore.of()
                .desc("Tên trên đầu mob và trong thông báo. Hỗ trợ &&a, &&#hex, \\<gradient>.")
                .section("Thông tin:").row("Hiện tại", def.displayName.isEmpty() ? (mythic ? "giữ tên MythicMobs" : "không có") : def.displayName)
                .action("Chuột trái để đổi tên").danger("Chuột phải để bỏ tên riêng").build()), click -> {
            if (click.isRightClick()) {
                def.displayName = "";
                save();
                refresh();
                return;
            }
            ask("input.display-name", text -> {
                def.displayName = text;
                save();
            });
        });
        set(2, icon(mythic ? Material.NETHER_STAR : Palette.egg(def.entityType), Palette.GENERAL + "Nguồn", Lore.of()
                .section("Thông tin:")
                .row("Loại", mythic ? "MythicMobs" : "Mob thường")
                .row(mythic ? "Mob MythicMobs" : "Loại mob", mythic ? def.mythicId : def.entityType.name())
                .lines(packWarnings())
                .action(mythic ? "Chuột trái để đổi mob MythicMobs" : "Chuột trái để đổi loại mob").build()), click -> {
            if (mythic) {
                Gui.pickMythic(plugin, player, "&8Chọn mob MythicMobs", name -> {
                    def.mythicId = name;
                    save();
                    open();
                }, this::open);
            } else {
                new PickMenu<>(plugin, player, "&8Chọn loại mob", Gui::customTypes,
                        t -> icon(Palette.egg(t), Palette.MOB + t.name(), Lore.of().action("Chuột trái để chọn").build()),
                        Enum::name, (t, c) -> {
                    def.entityType = t;
                    save();
                    open();
                }, this::open).open();
            }
        });
        set(5, icon(Material.CLOCK, Palette.GENERAL + "Thời gian tồn tại", Lore.of()
                .desc("Hết thời gian mà chưa bị giết thì mob biến mất, không có thưởng.")
                .section("Thông tin:").row("Hiện tại", def.lifetimeSeconds <= 0 ? "mãi mãi" : Format.duration(def.lifetimeSeconds))
                .action("Chuột trái để nhập số giây (0 = mãi mãi)").build()),
                click -> askNumber("input.lifetime", 0, 604800, v -> {
                    def.lifetimeSeconds = (int) v;
                    save();
                }));
        set(6, icon(Material.EXPERIENCE_BOTTLE, Palette.GENERAL + "Kinh nghiệm rơi ra", Lore.of()
                .section("Thông tin:").row("Hiện tại", Format.number(def.xp))
                .action("Chuột trái để nhập số").build()),
                click -> askNumber("input.xp", 0, 1_000_000, v -> {
                    def.xp = (int) v;
                    save();
                }));
        String name = plugin.mobs().nameOf(def);
        Lore barLore = Lore.of()
                .desc("Bật: sau tên hiện thanh máu, đỏ là máu còn lại, xám là máu đã mất (giảm từ phải sang trái). Tắt: chỉ hiện tên.")
                .section("Thông tin:").row("Hiện tại", Palette.onOff(def.nameBar))
                .row("Độ cao trên model", Format.decimal(def.nametagOffset) + " block");
        if (name == null) barLore.note("&#FFD166", "(!) Mob chưa có tên nên chưa hiện gì");
        set(28, icon(Material.RED_DYE, Palette.GENERAL + "Thanh máu ở tên", barLore
                .action("Chuột trái để bật/tắt").secondary("Chuột phải để chỉnh độ cao (mob có model)").build()), click -> {
            if (click.isRightClick()) {
                askNumber("input.nametag-offset", -5, 10, v -> {
                    def.nametagOffset = v;
                    save();
                });
                return;
            }
            def.nameBar = !def.nameBar;
            save();
            refresh();
        });
        Lore bossLore = Lore.of()
                .desc("Hiện tên và máu của mob trên bossbar cho người chơi ở gần. Dùng cho boss không có model, hoặc model không có bossbar riêng.")
                .section("Thông tin:").row("Hiện tại", Palette.onOff(def.bossBar))
                .row("Tầm hiện", plugin.settings().bossBarRange + " block");
        if (name == null) bossLore.note("&#FFD166", "(!) Mob chưa có tên nên chưa hiện gì");
        set(29, icon(Material.DRAGON_HEAD, Palette.GENERAL + "Thanh máu bossbar", bossLore
                .action("Chuột trái để bật/tắt").build()), click -> {
            def.bossBar = !def.bossBar;
            save();
            refresh();
        });
        // row 2: stats
        stat(10, Stat.HEALTH, Material.RED_DYE, "Máu tối đa", "Giới hạn: " + Format.number(plugin.settings().maxHealth)
                + " (spigot.yml maxHealth)", 1, plugin.settings().maxHealth);
        stat(11, Stat.ARMOR, Material.IRON_CHESTPLATE, "Giáp", "Giáp vanilla, tối đa 30.", 0, 30);
        stat(12, Stat.TOUGHNESS, Material.DIAMOND_CHESTPLATE, "Độ cứng giáp", "Tối đa 20.", 0, 20);
        stat(13, Stat.DAMAGE, Material.IRON_SWORD, "Sát thương đánh thường",
                "Chỉ đòn cận chiến. Sát thương skill chỉnh trong file MythicMobs.", 0, 2048);
        stat(14, Stat.SPEED, Material.FEATHER, "Tốc độ di chuyển", "Zombie mặc định 0,23.", 0, 1);
        stat(15, Stat.FOLLOW_RANGE, Material.SPYGLASS, "Tầm truy đuổi", "Số block mob phát hiện người chơi.", 0, 2048);
        stat(16, Stat.KNOCKBACK, Material.SHIELD, "Kháng đẩy lùi", "0 = bị đẩy bình thường, 1 = không bị đẩy.", 0, 1);
        // rows 1, 4, 5: sub-menus
        set(4, icon(Material.DIAMOND_SWORD, Palette.EQUIP + "Trang bị", Lore.of()
                .desc("Vũ khí, vật phẩm cầm tay và giáp mob mặc.")
                .section("Thông tin:").row("Đã đặt", def.equipment.size() + "/6 ô")
                .action("Chuột trái để mở").build()), click -> new EquipmentMenu(plugin, player, def, this).open());
        set(3, icon(Material.ARMOR_STAND, Palette.MODEL + "Model", Lore.of()
                .desc("Gắn model ModelEngine cho mob.")
                .section("Thông tin:").row("Model", def.modelId.isEmpty() ? "không" : def.modelId)
                .action("Chuột trái để mở").build()), click -> new ModelMenu(plugin, player, def, this).open());
        set(37, icon(Material.CHEST, Palette.REWARD + "Phần thưởng", Lore.of()
                .desc("Thay hoàn toàn đồ rơi gốc của mob.")
                .section("Thông tin:")
                .row("Chế độ", def.rewardMode.label)
                .row("Nhân thưởng theo rank", Palette.onOff(def.multiplierEnabled))
                .action("Chuột trái để mở").build()), click -> new RewardMenu(plugin, player, def, this).open());
        set(38, icon(Material.BELL, Palette.ANNOUNCE + "Thông báo", Lore.of()
                .desc("Thông báo chat toàn server khi mob xuất hiện, chết, biến mất.")
                .section("Thông tin:").row("Xuất hiện", Palette.onOff(def.spawn.enabled))
                .row("Bị hạ gục", Palette.onOff(def.death.enabled)).row("Biến mất", Palette.onOff(def.despawn.enabled))
                .action("Chuột trái để mở").build()), click -> new AnnounceMenu(plugin, player, def, this).open());
        set(19, icon(Material.BLAZE_POWDER, Palette.SKILL + "Skill", Lore.of()
                .desc("Dùng lại skill có sẵn trong MythicMobs cho mob này: chọn skill, khi nào dùng và nhắm vào ai.")
                .section("Thông tin:").row("Số skill", String.valueOf(def.skills.size()))
                .action("Chuột trái để mở").build()), click -> new SkillsMenu(plugin, player, def, this).open());
        set(30, icon(Material.SPAWNER, Palette.SPAWNER + "Holo spawner", Lore.of()
                .desc("Mẫu holo của các spawner spawn ra mob này. Mỗi mob một mẫu riêng.")
                .section("Thông tin:").row("Số dòng", String.valueOf(def.spawnerHolo.size()))
                .action("Chuột trái để sửa").danger("Shift + chuột phải để về mẫu mặc định").build()), click -> {
            if (click == org.bukkit.event.inventory.ClickType.SHIFT_RIGHT) {
                def.spawnerHolo.clear();
                def.spawnerHolo.addAll(plugin.settings().spawnerHoloLines);
                save();
                refresh();
                return;
            }
            openSpawnerHolo(plugin, player, def, this);
        });
        // row 6: actions
        set(45, icon(Material.ARROW, "&#BFC7D5Quay lại", Lore.of().action("Chuột trái để quay lại").build()),
                click -> Gui.mobs(plugin, player));
        set(49, icon(Material.EGG, "&#7CFC9ASpawn thử", Lore.of()
                .desc("Spawn mob này tại chỗ bạn đứng để kiểm tra.").action("Chuột trái để spawn").build()), click -> {
            player.closeInventory();
            if (plugin.mobs().spawn(def, com.dex.myboxmobs.command.MbmCommand.inFront(player), "command", null, true) == null) {
                plugin.messages().send(player, "mob.spawn-failed", "id", def.id);
            } else {
                plugin.messages().send(player, "mob.spawned", "id", def.id, "amount", "1");
            }
        });
        set(53, icon(Material.BARRIER, "&#FF8A8AXoá mob", Lore.of()
                .danger("Chuột trái để xoá").build()), click -> new ConfirmMenu(plugin, player, "mob " + def.id, () -> {
            plugin.registry().delete(def.id);
            plugin.messages().send(player, "mob.deleted", "id", def.id);
            Gui.mobs(plugin, player);
        }, this::open).open());
        set(17, icon(Material.TARGET, Palette.STATS + "Máu theo số hit", Lore.of()
                .desc("Bật: mỗi đòn của người chơi chỉ trừ đúng 1 máu, dù sát thương bao nhiêu. Máu tối đa 100 = 100 hit. Lửa, rơi, mob khác không làm mất máu.")
                .section("Thông tin:").row("Hiện tại", Palette.onOff(def.hitHealth))
                .action("Chuột trái để bật/tắt").build()), click -> {
            def.hitHealth = !def.hitHealth;
            save();
            refresh();
        });
        set(20, icon(Material.GOLD_NUGGET, Palette.SKILL + "Kỹ năng kẻ trộm", Lore.of()
                .desc("Cướp tiền / gem của người chơi khi đánh trúng, đi săn người chơi sau khi xuất hiện, dịch chuyển khi mất máu. Người đánh lấy lại dần, người hạ gục nhận cả túi.")
                .section("Thông tin:").row("Cướp tiền", Palette.onOff(def.thief.stealMoney))
                .row("Cướp gem", Palette.onOff(def.thief.stealGem))
                .action("Chuột trái để mở").build()), click -> new ThiefMenu(plugin, player, def, this).open());
        label(0, Palette.GENERAL, "Chung", "Tên, nguồn, model, trang bị, thời gian tồn tại, kinh nghiệm.");
        label(9, Palette.STATS, "Chỉ số", "Máu, giáp, sát thương, tốc độ và cách tính máu.");
        label(18, Palette.SKILL, "Chiến đấu", "Skill MythicMobs và kỹ năng riêng của MyBoxMobs.");
        label(27, Palette.GENERAL, "Hiển thị", "Thanh máu ở tên, bossbar, holo spawner.");
        label(36, Palette.REWARD, "Thưởng & thông báo", "Phần thưởng khi hạ gục và thông báo chat.");
        fill(0);
    }

    /** Coloured pane at the start of a row naming the group of that row. */
    private void label(int slot, String color, String name, String desc) {
        set(slot, icon(Material.LIGHT_BLUE_STAINED_GLASS_PANE, color + name, Lore.of().desc(desc).build()), null);
    }

    /** Editor of a mob's spawner hologram template (also opened from the spawner menu). */
    static void openSpawnerHolo(MyBoxMobsPlugin plugin, Player player, MobDefinition def, Menu parent) {
        new LinesMenu(plugin, player, "&8Holo spawner &8[" + def.id + "&8]", def.spawnerHolo,
                List.of("%mob% tên mob", "%health% máu", "%damage% sát thương", "%countdown% đếm ngược",
                        "%alive% đang tồn tại", "%max% tối đa", "#ICON: SPAWNER (lồng spawner xoay)"),
                () -> plugin.registry().save(def),
                () -> {
                    for (String line : def.spawnerHolo) {
                        if (!line.startsWith("#")) player.sendMessage(com.dex.myboxmobs.text.Text.parse(line));
                    }
                }, parent).open();
    }

    private void stat(int slot, Stat stat, Material material, String name, String desc, double min, double max) {
        Double value = def.stats.get(stat);
        set(slot, icon(material, Palette.STATS + name, Lore.of().desc(desc)
                .section("Thông tin:").row("Hiện tại", value == null ? "giữ nguyên gốc" : Format.decimal(value))
                .action("Chuột trái để nhập giá trị").danger("Chuột phải để giữ nguyên gốc").build()), click -> {
            if (click.isRightClick()) {
                def.stats.remove(stat);
                save();
                refresh();
                return;
            }
            askNumber("input.stat", min, max, v -> {
                def.stats.put(stat, v);
                save();
            });
        });
    }

    /** Spec 3.2 pack check: models the MythicMobs file uses that ModelEngine does not have. */
    private List<String> packWarnings() {
        List<String> out = new ArrayList<>();
        if (def.source != MobDefinition.Source.MYTHIC) return out;
        if (!plugin.mythic().exists(def.mythicId)) {
            out.add("&#FFD166(!) Không tìm thấy mob MythicMobs này");
            return out;
        }
        if (!plugin.models().available()) return out;
        Set<String> models = plugin.mythic().referencedModels(def.mythicId);
        for (String id : models) {
            if (!plugin.models().exists(id)) out.add("&#FFD166(!) Thiếu model " + id);
        }
        return out;
    }
}
