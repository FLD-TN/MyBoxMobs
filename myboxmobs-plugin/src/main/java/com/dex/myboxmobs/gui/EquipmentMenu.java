package com.dex.myboxmobs.gui;

import com.dex.myboxmobs.MyBoxMobsPlugin;
import com.dex.myboxmobs.mob.MobDefinition;
import com.dex.myboxmobs.text.Lore;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Six equipment slots: pick a slot, then click the item in your own inventory (drop chance is always 0). */
public final class EquipmentMenu extends Menu {

    private static final List<String> NAMES = List.of("Tay chính", "Tay phụ", "Mũ", "Áo giáp", "Quần", "Giày");
    private static final List<Material> PLACEHOLDERS = List.of(Material.WOODEN_SWORD, Material.SHIELD,
            Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS);

    private final MobDefinition def;
    private final Menu parent;
    private @Nullable EquipmentSlot selected;

    public EquipmentMenu(MyBoxMobsPlugin plugin, Player player, MobDefinition def, Menu parent) {
        super(plugin, player, 3, "&8Trang bị &8[" + def.id + "&8]");
        this.def = def;
        this.parent = parent;
    }

    @Override
    protected void draw() {
        for (int i = 0; i < MobDefinition.SLOTS.size(); i++) {
            EquipmentSlot slot = MobDefinition.SLOTS.get(i);
            ItemStack item = def.equipment.get(slot);
            boolean chosen = slot == selected;
            Lore lore = Lore.of().section("Thông tin:").row("Ô", NAMES.get(i))
                    .row("Đang chọn", chosen ? "&#7CFC9A✔ có" : "không");
            lore.action(chosen ? "Bấm vào vật phẩm trong túi để đặt" : "Chuột trái để chọn ô này")
                    .danger("Chuột phải để bỏ trống");
            ItemStack shown = item == null
                    ? icon(PLACEHOLDERS.get(i), Palette.EQUIP + NAMES.get(i) + " &8(trống)", lore.build())
                    : withExtraLore(item, lore.build());
            set(10 + i, shown, click -> {
                if (click.isRightClick()) {
                    def.equipment.remove(slot);
                    plugin.registry().save(def);
                } else {
                    selected = slot;
                }
                refresh();
            });
        }
        set(22, icon(Material.ARROW, "&#BFC7D5Quay lại", Lore.of().action("Chuột trái để quay lại").build()),
                click -> parent.open());
        fill(0);
    }

    @Override
    protected void onOwnItemClick(ItemStack item, ClickType click) {
        if (selected == null) {
            plugin.messages().send(player, "gui.select-slot-first");
            return;
        }
        ItemStack one = item.clone();
        one.setAmount(1);
        def.equipment.put(selected, one);
        plugin.registry().save(def);
        selected = null;
        refresh();
    }
}
