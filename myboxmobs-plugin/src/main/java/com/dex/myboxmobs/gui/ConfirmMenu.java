package com.dex.myboxmobs.gui;

import com.dex.myboxmobs.MyBoxMobsPlugin;
import com.dex.myboxmobs.text.Lore;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** Yes / no before deleting something. */
public final class ConfirmMenu extends Menu {

    private final String what;
    private final Runnable yes;
    private final Runnable no;

    public ConfirmMenu(MyBoxMobsPlugin plugin, Player player, String what, Runnable yes, Runnable no) {
        super(plugin, player, 3, "&8Xác nhận xoá");
        this.what = what;
        this.yes = yes;
        this.no = no;
    }

    @Override
    protected void draw() {
        set(11, icon(Material.RED_CONCRETE, "&#FF8A8AXoá " + what, Lore.of()
                .desc("Không thể hoàn tác sau khi xoá.").danger("Chuột trái để xoá").build()), click -> yes.run());
        set(15, icon(Material.LIME_CONCRETE, "&#7CFC9AGiữ lại", Lore.of()
                .action("Chuột trái để quay lại").build()), click -> no.run());
        fill(0);
    }
}
