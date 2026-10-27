package com.dex.myboxmobs.gui;

import com.dex.myboxmobs.MyBoxMobsPlugin;
import com.dex.myboxmobs.place.MobRef;
import com.dex.myboxmobs.place.SpawnerDef;
import com.dex.myboxmobs.text.Format;
import com.dex.myboxmobs.text.Lore;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

/** Every field of a spawner (spec 7). Every change saves spawners.yml. */
public final class SpawnerEditorMenu extends Menu {

    private final SpawnerDef d;

    public SpawnerEditorMenu(MyBoxMobsPlugin plugin, Player player, SpawnerDef d) {
        super(plugin, player, 6, "&8Spawner &8[" + d.id + "&8]");
        this.d = d;
    }

    private void save() {
        plugin.spawners().save();
    }

    private void changed() {
        save();
        refresh();
    }

    @Override
    protected void draw() {
        Lore mobs = Lore.of().desc("Mob của MyBoxMobs hoặc MythicMobs. Nhiều mob thì chọn ngẫu nhiên theo trọng số.")
                .section("Danh sách:");
        if (d.mobs.isEmpty()) mobs.line("&7• chưa có mob nào");
        for (SpawnerDef.Entry e : d.mobs) {
            mobs.line("&7• &f" + plugin.spawners().mobName(e.mob) + (d.mobs.size() > 1 ? " &8(trọng số " + e.weight + ")" : ""));
        }
        set(10, icon(Material.ZOMBIE_HEAD, Palette.SPAWNER + "Mob spawn ra", mobs.action("Chuột trái để chỉnh").build()),
                click -> new SpawnerMobsMenu(plugin, player, d, this).open());
        number(11, Material.CLOCK, "Thời gian giữa các đợt", Format.duration(d.intervalSeconds), "input.interval",
                1, 86400, v -> d.intervalSeconds = (int) v);
        number(12, Material.EGG, "Số mob mỗi đợt", String.valueOf(d.amount), "input.amount-wave", 1, 100,
                v -> d.amount = (int) v);
        number(13, Material.CHAIN, "Tối đa sống cùng lúc", String.valueOf(d.maxAlive), "input.max-alive", 1, 500,
                v -> d.maxAlive = (int) v);
        boolean nearby = d.mode == SpawnerDef.Mode.NEARBY;
        set(14, icon(nearby ? Material.PLAYER_HEAD : Material.REDSTONE_TORCH, Palette.SPAWNER + "Chế độ spawn", Lore.of()
                .desc("Có người gần: chỉ spawn khi có người chơi trong bán kính. Luôn spawn: không cần người chơi.")
                .section("Thông tin:").row("Hiện tại", nearby ? "Có người gần" : "Luôn spawn")
                .action("Chuột trái để đổi").build()), click -> {
            d.mode = nearby ? SpawnerDef.Mode.ALWAYS : SpawnerDef.Mode.NEARBY;
            changed();
        });
        number(15, Material.COMPASS, "Bán kính kích hoạt", d.activationRadius + " block", "input.radius", 1, 256,
                v -> d.activationRadius = (int) v);
        number(16, Material.SLIME_BALL, "Bán kính rải mob", Format.decimal(d.spread) + " block", "input.radius", 0, 64,
                v -> d.spread = v);
        number(19, Material.LEAD, "Giới hạn đi xa", d.leash <= 0 ? "tắt" : Format.decimal(d.leash) + " block",
                "input.leash", 0, 512, v -> d.leash = v);
        set(20, icon(Material.STRUCTURE_VOID, Palette.SPAWNER + "Giữ chunk luôn tải", Lore.of()
                .desc("Chỉ dùng với chế độ luôn spawn. Tốn tài nguyên server, chỉ bật khi thật cần.")
                .section("Thông tin:").row("Hiện tại", Palette.onOff(d.keepChunkLoaded))
                .action("Chuột trái để bật/tắt").build()), click -> {
            d.keepChunkLoaded = !d.keepChunkLoaded;
            changed();
        });
        set(21, icon(Material.NAME_TAG, Palette.SPAWNER + "Hologram", Lore.of()
                .section("Thông tin:").row("Hiện tại", Palette.onOff(d.hologram))
                .row("DecentHolograms", plugin.holograms().available() ? "có" : "&#FF6B6Bchưa cài")
                .action("Chuột trái để bật/tắt").secondary("Chuột phải để sửa nội dung").build()), click -> {
            if (click.isRightClick()) {
                new LinesMenu(plugin, player, "&8Holo spawner &8[" + d.id + "&8]", d.holoLines,
                        List.of("%mob% tên mob", "%health% máu", "%damage% sát thương", "%countdown% đếm ngược",
                                "%alive% đang sống", "%max% tối đa", "#ICON: SPAWNER (dòng icon xoay)"),
                        this::save, () -> player.sendMessage(com.dex.myboxmobs.text.Text.parse(String.join("\n", d.holoLines))),
                        this).open();
                return;
            }
            d.hologram = !d.hologram;
            changed();
        });
        set(22, icon(d.enabled ? Material.LIME_DYE : Material.GRAY_DYE, Palette.SPAWNER + "Hoạt động", Lore.of()
                .desc("Tạm dừng mà không xoá spawner.")
                .section("Thông tin:").row("Hiện tại", Palette.onOff(d.enabled))
                .row("Đang sống", plugin.spawners().alive(d) + "/" + d.maxAlive)
                .action("Chuột trái để bật/tắt").build()), click -> {
            d.enabled = !d.enabled;
            changed();
        });
        set(23, icon(Material.ENDER_PEARL, Palette.SPAWNER + "Vị trí", Lore.of()
                .section("Thông tin:").row("Hiện tại", d.world + " " + (int) Math.floor(d.x) + " " + (int) d.y + " " + (int) Math.floor(d.z))
                .action("Chuột trái để dịch chuyển tới").secondary("Chuột phải để dời spawner tới chỗ bạn").build()), click -> {
            if (click.isRightClick()) {
                d.setLocation(player.getLocation());
                changed();
                return;
            }
            Location at = d.location();
            if (at != null) {
                player.closeInventory();
                player.teleport(at);
            }
        });
        set(45, icon(Material.ARROW, "&#BFC7D5Quay lại", Lore.of().action("Chuột trái để quay lại").build()),
                click -> Gui.spawners(plugin, player));
        set(53, icon(Material.BARRIER, "&#FF8A8AXoá spawner", Lore.of().danger("Chuột trái để xoá").build()),
                click -> new ConfirmMenu(plugin, player, "spawner " + d.id, () -> {
                    plugin.spawners().delete(d);
                    plugin.messages().send(player, "spawner.deleted", "id", d.id);
                    Gui.spawners(plugin, player);
                }, this::open).open());
        fill(0);
    }

    private void number(int slot, Material material, String name, String current, String prompt, double min, double max,
                        java.util.function.DoubleConsumer apply) {
        set(slot, icon(material, Palette.SPAWNER + name, Lore.of()
                .section("Thông tin:").row("Hiện tại", current)
                .action("Chuột trái để nhập số").build()), click -> askNumber(prompt, min, max, v -> {
            apply.accept(v);
            save();
        }));
    }

    /** Mobs of a spawner with weights. */
    static final class SpawnerMobsMenu extends Menu {

        private final SpawnerDef d;
        private final Menu parent;

        SpawnerMobsMenu(MyBoxMobsPlugin plugin, Player player, SpawnerDef d, Menu parent) {
            super(plugin, player, 6, "&8Mob của spawner &8[" + d.id + "&8]");
            this.d = d;
            this.parent = parent;
        }

        @Override
        protected void draw() {
            for (int i = 0; i < Math.min(45, d.mobs.size()); i++) {
                SpawnerDef.Entry e = d.mobs.get(i);
                set(i, icon(e.mob.mythic() ? Material.NETHER_STAR : Material.ZOMBIE_HEAD,
                        Palette.SPAWNER + plugin.spawners().mobName(e.mob), Lore.of().section("Thông tin:")
                                .row("Mã", e.mob.toString()).row("Trọng số", String.valueOf(e.weight))
                                .line(e.mob.mythic() ? "&8Mob MythicMobs gốc: không có thưởng MyBoxMobs" : "")
                                .action("Chuột trái để đổi trọng số").danger("Shift + chuột phải để xoá").build()), click -> {
                    if (click == ClickType.SHIFT_RIGHT) {
                        d.mobs.remove(e);
                        plugin.spawners().save();
                        refresh();
                        return;
                    }
                    askNumber("input.weight", 1, 10000, v -> {
                        e.weight = (int) v;
                        plugin.spawners().save();
                    });
                });
            }
            set(45, icon(Material.ARROW, "&#BFC7D5Quay lại", Lore.of().action("Chuột trái để quay lại").build()),
                    click -> parent.open());
            set(48, icon(Material.ZOMBIE_SPAWN_EGG, Palette.SPAWNER + "Thêm mob MyBoxMobs", Lore.of()
                    .desc("Có ghi đè chỉ số, phần thưởng và thông báo.").action("Chuột trái để chọn").build()),
                    click -> Gui.pickDef(plugin, player, "&8Chọn mob", def -> {
                        d.mobs.add(new SpawnerDef.Entry(new MobRef(false, def.id), 1));
                        plugin.spawners().save();
                        open();
                    }, this::open));
            set(50, icon(Material.NETHER_STAR, Palette.SPAWNER + "Thêm mob MythicMobs gốc", Lore.of()
                    .desc("Spawn nguyên bản, không có thưởng hay thông báo của MyBoxMobs.").action("Chuột trái để chọn").build()),
                    click -> Gui.pickMythic(plugin, player, "&8Chọn mob MythicMobs", name -> {
                        d.mobs.add(new SpawnerDef.Entry(new MobRef(true, name), 1));
                        plugin.spawners().save();
                        open();
                    }, this::open));
            fill(5);
        }
    }
}
