package com.dex.myboxmobs.gui;

import com.dex.myboxmobs.MyBoxMobsPlugin;
import com.dex.myboxmobs.mob.MobDefinition;
import com.dex.myboxmobs.text.Format;
import com.dex.myboxmobs.text.Lore;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** ModelEngine model of a mob (spec 3.2). */
public final class ModelMenu extends Menu {

    private final MobDefinition def;
    private final Menu parent;

    public ModelMenu(MyBoxMobsPlugin plugin, Player player, MobDefinition def, Menu parent) {
        super(plugin, player, 3, "&8Model &8[" + def.id + "&8]");
        this.def = def;
        this.parent = parent;
    }

    private void save() {
        plugin.registry().save(def);
        refresh();
    }

    @Override
    protected void draw() {
        if (!plugin.models().available()) {
            set(13, icon(Material.BARRIER, "&#FF8A8AChưa cài ModelEngine", Lore.of()
                    .desc("Cài ModelEngine để gắn model cho mob.").build()), null);
        } else {
            boolean missing = !def.modelId.isEmpty() && !plugin.models().exists(def.modelId);
            Lore lore = Lore.of().section("Thông tin:").row("Model", def.modelId.isEmpty() ? "không" : def.modelId);
            if (missing) lore.line("&#FFD166(!) ModelEngine không có model này");
            set(10, icon(Material.ARMOR_STAND, Palette.MODEL + "Chọn model", lore
                    .action("Chuột trái để chọn model").danger("Chuột phải để bỏ model").build()), click -> {
                if (click.isRightClick()) {
                    def.modelId = "";
                    save();
                    return;
                }
                Gui.pickModel(plugin, player, id -> {
                    def.modelId = id;
                    plugin.registry().save(def);
                    open();
                }, this::open);
            });
            boolean replace = def.modelMode == MobDefinition.ModelMode.REPLACE;
            set(12, icon(replace ? Material.LAVA_BUCKET : Material.WATER_BUCKET, Palette.MODEL + "Khi mob MythicMobs đã có model",
                    Lore.of().desc("Chỉ áp dụng cho mob MythicMobs. Không bao giờ gắn hai model cùng lúc.")
                            .section("Thông tin:").row("Hiện tại", replace ? "thay bằng model này" : "giữ model của MythicMobs")
                            .action("Chuột trái để đổi").build()), click -> {
                def.modelMode = replace ? MobDefinition.ModelMode.ONLY_IF_NONE : MobDefinition.ModelMode.REPLACE;
                save();
            });
            set(14, icon(Material.SLIME_BALL, Palette.MODEL + "Kích thước", Lore.of()
                    .section("Thông tin:").row("Hiện tại", "x" + Format.decimal(def.modelScale))
                    .action("Chuột trái để nhập (0,1 - 10)").build()), click -> askNumber("input.model-scale", 0.1, 10, v -> {
                def.modelScale = v;
                plugin.registry().save(def);
            }));
            set(16, icon(Material.GLASS, Palette.MODEL + "Ẩn mob gốc", Lore.of()
                    .desc("Ẩn thân mob vanilla, chỉ thấy model.")
                    .section("Thông tin:").row("Hiện tại", Palette.onOff(def.hideBase))
                    .action("Chuột trái để bật/tắt").build()), click -> {
                def.hideBase = !def.hideBase;
                save();
            });
        }
        set(22, icon(Material.ARROW, "&#BFC7D5Quay lại", Lore.of().action("Chuột trái để quay lại").build()),
                click -> parent.open());
        fill(0);
    }
}
