package com.dex.myboxmobs.gui;

import com.dex.myboxmobs.MyBoxMobsPlugin;
import com.dex.myboxmobs.api.event.MbmMobDespawnEvent;
import com.dex.myboxmobs.live.LiveMob;
import com.dex.myboxmobs.mob.MobDefinition;
import com.dex.myboxmobs.place.AltarDef;
import com.dex.myboxmobs.place.SpawnerDef;
import com.dex.myboxmobs.text.Format;
import com.dex.myboxmobs.text.Lore;
import com.dex.myboxmobs.text.Text;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

/** Openers of the list menus and the shared pickers (spec 11). */
public final class Gui {

    private Gui() {
    }

    /** Closes the GUI and waits for one chat line; {@code reopen} runs on "huy" and on timeout. */
    public static void prompt(MyBoxMobsPlugin plugin, Player player, String promptKey, Consumer<String> onInput,
                              Runnable reopen, String... placeholders) {
        player.closeInventory();
        plugin.messages().send(player, promptKey, placeholders);
        plugin.messages().send(player, "input.hint", "seconds", String.valueOf(plugin.settings().inputTimeoutSeconds));
        plugin.input().ask(player, onInput, reopen);
    }

    // ------------------------------------------------------------------ mobs

    public static void mobs(MyBoxMobsPlugin plugin, Player p) {
        new PickMenu<MobDefinition>(plugin, p, "&8Danh sách mob", () -> new ArrayList<>(plugin.registry().all()),
                def -> mobIcon(plugin, def), def -> def.id + " " + Text.plain(plugin.mobs().displayName(def)),
                (def, click) -> {
                    if (click == ClickType.SHIFT_RIGHT) {
                        new ConfirmMenu(plugin, p, "mob " + def.id, () -> {
                            plugin.registry().delete(def.id);
                            plugin.messages().send(p, "mob.deleted", "id", def.id);
                            mobs(plugin, p);
                        }, () -> mobs(plugin, p)).open();
                    } else if (click.isRightClick()) {
                        p.closeInventory();
                        if (plugin.mobs().spawn(def, com.dex.myboxmobs.command.MbmCommand.inFront(p), "command", null, true) == null) {
                            plugin.messages().send(p, "mob.spawn-failed", "id", def.id);
                        } else {
                            plugin.messages().send(p, "mob.spawned", "id", def.id, "amount", "1");
                        }
                    } else {
                        new MobEditorMenu(plugin, p, def).open();
                    }
                }, () -> new MainMenu(plugin, p).open()) {
            @Override
            protected void extraButtons() {
                set(46, icon(Material.NETHER_STAR, Palette.MOB + "Tạo từ MythicMobs", Lore.of()
                        .desc("Chọn một mob MythicMobs có sẵn (gói mua về) để quản lý bằng MyBoxMobs.")
                        .action("Chuột trái để chọn mob").build()), click -> createMythic(plugin, p));
                set(47, icon(Material.ZOMBIE_SPAWN_EGG, Palette.MOB + "Tạo mob thường", Lore.of()
                        .desc("Chọn loại mob vanilla bằng trứng spawn.")
                        .action("Chuột trái để chọn loại mob").build()), click -> createCustom(plugin, p));
            }
        }.open();
    }

    private static ItemStack mobIcon(MyBoxMobsPlugin plugin, MobDefinition def) {
        boolean mythic = def.source == MobDefinition.Source.MYTHIC;
        Material material = mythic ? Palette.egg(plugin.mythic().entityType(def.mythicId)) : Palette.egg(def.entityType);
        Lore lore = Lore.of().section("Thông tin:").row("Mã", def.id)
                .row("Nguồn", mythic ? "MythicMobs &8[" + def.mythicId + "&8]" : "Mob thường &8[" + def.entityType.name() + "&8]")
                .row("Phần thưởng", def.rewardMode.label)
                .row("Đang sống", String.valueOf(plugin.mobs().tracker().count(def.id)));
        if (mythic && !plugin.mythic().exists(def.mythicId)) lore.line("&#FFD166(!) Không tìm thấy mob MythicMobs này");
        return Menu.icon(material, Palette.MOB + plugin.mobs().displayName(def) + " &8[" + def.id + "&8]",
                lore.action("Chuột trái để chỉnh sửa").secondary("Chuột phải để spawn tại chỗ bạn")
                        .danger("Shift + chuột phải để xoá").build());
    }

    private static void createMythic(MyBoxMobsPlugin plugin, Player p) {
        pickMythic(plugin, p, "&8Chọn mob MythicMobs", name -> askNewId(plugin, p, suggestId(name), id -> {
            MobDefinition def = plugin.registry().blank(id, MobDefinition.Source.MYTHIC);
            def.mythicId = name;
            plugin.registry().put(def);
            plugin.messages().send(p, "mob.created", "id", id);
            new MobEditorMenu(plugin, p, def).open();
        }, () -> createMythic(plugin, p)), () -> mobs(plugin, p));
    }

    /** Living, spawnable entity types that have a spawn egg (spec 11), sorted by name. */
    public static List<EntityType> customTypes() {
        List<EntityType> types = new ArrayList<>();
        for (EntityType t : EntityType.values()) {
            if (t.isAlive() && t.isSpawnable() && t != EntityType.PLAYER && Palette.egg(t) != Material.EGG) types.add(t);
        }
        types.sort(Comparator.comparing(Enum::name));
        return types;
    }

    private static void createCustom(MyBoxMobsPlugin plugin, Player p) {
        new PickMenu<>(plugin, p, "&8Chọn loại mob", Gui::customTypes,
                t -> Menu.icon(Palette.egg(t), Palette.MOB + t.name(), Lore.of().action("Chuột trái để chọn").build()),
                Enum::name,
                (t, click) -> askNewId(plugin, p, suggestId(t.name()), id -> {
                    MobDefinition def = plugin.registry().blank(id, MobDefinition.Source.CUSTOM);
                    def.entityType = t;
                    plugin.registry().put(def);
                    plugin.messages().send(p, "mob.created", "id", id);
                    new MobEditorMenu(plugin, p, def).open();
                }, () -> createCustom(plugin, p)), () -> mobs(plugin, p)).open();
    }

    private static String suggestId(String name) {
        String s = name.toLowerCase().replaceAll("[^a-z0-9_]", "_");
        return s.length() > 32 ? s.substring(0, 32) : s;
    }

    /** Asks for the id of a new mob; "ok" takes the suggestion. */
    private static void askNewId(MyBoxMobsPlugin plugin, Player p, String suggestion, Consumer<String> onId,
                                 Runnable back) {
        prompt(plugin, p, "input.mob-id", text -> {
            String id = text.equalsIgnoreCase("ok") ? suggestion : text.toLowerCase();
            if (!MobDefinition.ID.matcher(id).matches()) {
                plugin.messages().send(p, "input.invalid-id");
                back.run();
                return;
            }
            if (plugin.registry().get(id) != null) {
                plugin.messages().send(p, "mob.exists", "id", id);
                back.run();
                return;
            }
            onId.accept(id);
        }, back, "suggestion", suggestion);
    }

    // ------------------------------------------------------------------ pickers

    public static void pickMythic(MyBoxMobsPlugin plugin, Player p, String title, Consumer<String> onPick, Runnable back) {
        new PickMenu<>(plugin, p, title, plugin.mythic()::mobNames,
                name -> Menu.icon(Palette.egg(plugin.mythic().entityType(name)), Palette.MOB + name, Lore.of()
                        .section("Thông tin:").row("Loại", plugin.mythic().entityType(name))
                        .row("Tên", plugin.mythic().displayName(name).isEmpty() ? "không có" : plugin.mythic().displayName(name))
                        .action("Chuột trái để chọn").build()),
                name -> name, (name, click) -> onPick.accept(name), back).open();
    }

    public static void pickDef(MyBoxMobsPlugin plugin, Player p, String title, Consumer<MobDefinition> onPick,
                               Runnable back) {
        new PickMenu<MobDefinition>(plugin, p, title, () -> new ArrayList<>(plugin.registry().all()),
                def -> Menu.icon(def.source == MobDefinition.Source.MYTHIC
                                ? Palette.egg(plugin.mythic().entityType(def.mythicId)) : Palette.egg(def.entityType),
                        Palette.MOB + plugin.mobs().displayName(def) + " &8[" + def.id + "&8]",
                        Lore.of().action("Chuột trái để chọn").build()),
                def -> def.id, (def, click) -> onPick.accept(def), back).open();
    }

    public static void pickModel(MyBoxMobsPlugin plugin, Player p, Consumer<String> onPick, Runnable back) {
        new PickMenu<>(plugin, p, "&8Chọn model ModelEngine", plugin.models()::models,
                id -> Menu.icon(Material.ARMOR_STAND, Palette.MODEL + id, Lore.of().action("Chuột trái để chọn").build()),
                id -> id, (id, click) -> onPick.accept(id), back).open();
    }

    // ------------------------------------------------------------------ alive mobs

    public static void alive(MyBoxMobsPlugin plugin, Player p) {
        new PickMenu<LiveMob>(plugin, p, "&8Mob đang sống", () -> new ArrayList<>(plugin.mobs().tracker().all()),
                mob -> {
                    Location at = mob.entity.getLocation();
                    String hp = "?";
                    if (mob.entity instanceof LivingEntity le && le.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
                        hp = Format.number(Math.round(le.getHealth())) + "/"
                                + Format.number(Math.round(le.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()));
                    }
                    long age = (System.currentTimeMillis() - mob.spawnedAt) / 1000;
                    String name = mob.def == null ? mob.id : plugin.mobs().displayName(mob.def);
                    return Menu.icon(Material.ZOMBIE_HEAD, Palette.ALIVE + name, Lore.of().section("Thông tin:")
                            .row("Mã", mob.id).row("Nguồn", mob.origin)
                            .row("Vị trí", at.getWorld().getName() + " " + at.getBlockX() + " " + at.getBlockY() + " " + at.getBlockZ())
                            .row("Máu", hp).row("Đã sống", Format.duration(age))
                            .action("Chuột trái để dịch chuyển tới").danger("Shift + chuột phải để xoá mob").build());
                },
                mob -> mob.id,
                (mob, click) -> {
                    if (click == ClickType.SHIFT_RIGHT) {
                        plugin.mobs().end(mob, MbmMobDespawnEvent.Reason.COMMAND);
                        alive(plugin, p);
                    } else if (click.isLeftClick() && mob.entity.isValid()) {
                        p.closeInventory();
                        p.teleport(mob.entity.getLocation());
                    }
                }, () -> new MainMenu(plugin, p).open()).open();
    }

    // ------------------------------------------------------------------ spawners

    public static void spawners(MyBoxMobsPlugin plugin, Player p) {
        new PickMenu<SpawnerDef>(plugin, p, "&8Spawner", () -> new ArrayList<>(plugin.spawners().all()),
                d -> Menu.icon(Material.SPAWNER, Palette.SPAWNER + d.id, Lore.of().section("Thông tin:")
                        .row("Vị trí", d.world + " " + (int) Math.floor(d.x) + " " + (int) d.y + " " + (int) Math.floor(d.z))
                        .row("Mob", d.mobs.isEmpty() ? "chưa chọn" : plugin.spawners().mobName(d.mobs.get(0).mob)
                                + (d.mobs.size() > 1 ? " &8(+" + (d.mobs.size() - 1) + ")" : ""))
                        .row("Đang sống", plugin.spawners().alive(d) + "/" + d.maxAlive)
                        .row("Trạng thái", Palette.onOff(d.enabled))
                        .action("Chuột trái để chỉnh sửa").secondary("Chuột phải để dịch chuyển tới")
                        .danger("Shift + chuột phải để xoá").build()),
                d -> d.id,
                (d, click) -> {
                    if (click == ClickType.SHIFT_RIGHT) {
                        new ConfirmMenu(plugin, p, "spawner " + d.id, () -> {
                            plugin.spawners().delete(d);
                            plugin.messages().send(p, "spawner.deleted", "id", d.id);
                            spawners(plugin, p);
                        }, () -> spawners(plugin, p)).open();
                    } else if (click.isRightClick()) {
                        Location at = d.location();
                        if (at != null) {
                            p.closeInventory();
                            p.teleport(at);
                        }
                    } else {
                        new SpawnerEditorMenu(plugin, p, d).open();
                    }
                }, () -> new MainMenu(plugin, p).open()) {
            @Override
            protected void extraButtons() {
                set(47, icon(Material.LIME_DYE, Palette.SPAWNER + "Tạo spawner tại đây", Lore.of()
                        .desc("Tạo spawner mới ở vị trí bạn đang đứng.")
                        .action("Chuột trái để nhập mã spawner").build()), click -> ask("input.spawner-id", text -> {
                    String id = text.toLowerCase();
                    if (!MobDefinition.ID.matcher(id).matches()) {
                        plugin.messages().send(p, "input.invalid-id");
                    } else if (plugin.spawners().get(id) != null) {
                        plugin.messages().send(p, "spawner.exists", "id", id);
                    } else {
                        plugin.spawners().create(id, p.getLocation());
                        plugin.messages().send(p, "spawner.created", "id", id);
                    }
                }));
            }
        }.open();
    }

    // ------------------------------------------------------------------ altars

    public static void altars(MyBoxMobsPlugin plugin, Player p) {
        new PickMenu<AltarDef>(plugin, p, "&8Bàn triệu hồi", () -> new ArrayList<>(plugin.altars().all()),
                d -> Menu.icon(Material.SCULK_SHRIEKER, Palette.ALTAR + d.id, Lore.of().section("Thông tin:")
                        .row("Vị trí", d.world + " " + d.x + " " + d.y + " " + d.z)
                        .row("Mob", d.mob == null ? "chưa chọn" : plugin.spawners().mobName(d.mob))
                        .row("Trạng thái", d.boss != null ? "boss đang sống" : "sẵn sàng")
                        .action("Chuột trái để chỉnh sửa").secondary("Chuột phải để dịch chuyển tới")
                        .danger("Shift + chuột phải để xoá").build()),
                d -> d.id,
                (d, click) -> {
                    if (click == ClickType.SHIFT_RIGHT) {
                        new ConfirmMenu(plugin, p, "bàn " + d.id, () -> {
                            plugin.altars().delete(d);
                            plugin.messages().send(p, "altar.deleted", "id", d.id);
                            altars(plugin, p);
                        }, () -> altars(plugin, p)).open();
                    } else if (click.isRightClick()) {
                        Location at = d.blockLocation();
                        if (at != null) {
                            p.closeInventory();
                            p.teleport(at.clone().add(0.5, 1, 0.5));
                        }
                    } else {
                        new AltarEditorMenu(plugin, p, d).open();
                    }
                }, () -> new MainMenu(plugin, p).open()) {
            @Override
            protected void extraButtons() {
                set(47, icon(Material.BOOK, Palette.ALTAR + "Tạo bàn triệu hồi", Lore.of()
                        .desc("Đặt block, nhìn vào nó rồi dùng lệnh:")
                        .line("&f/mbm altar create <mã> <mob>")
                        .build()), null);
            }
        }.open();
    }
}
