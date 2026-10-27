package com.dex.myboxmobs.gui;

import com.dex.myboxmobs.MyBoxMobsPlugin;
import com.dex.myboxmobs.text.Lore;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * A paged list of anything (mobs, models, spawners...): 45 entries per page, back / previous / search / next on the
 * last row, optional extra buttons on slots 46-47 and 51-53.
 */
public class PickMenu<T> extends Menu {

    private static final int PER_PAGE = 45;

    private final Supplier<List<T>> items;
    private final Function<T, ItemStack> icon;
    private final Function<T, String> searchKey;
    private final BiConsumer<T, ClickType> onPick;
    private final @Nullable Runnable back;
    private int page;
    private String search = "";

    public PickMenu(MyBoxMobsPlugin plugin, Player player, String title, Supplier<List<T>> items,
                    Function<T, ItemStack> icon, Function<T, String> searchKey, BiConsumer<T, ClickType> onPick,
                    @Nullable Runnable back) {
        super(plugin, player, 6, title);
        this.items = items;
        this.icon = icon;
        this.searchKey = searchKey;
        this.onPick = onPick;
        this.back = back;
    }

    @Override
    protected void draw() {
        List<T> list = filtered();
        int pages = Math.max(1, (list.size() + PER_PAGE - 1) / PER_PAGE);
        page = Math.min(page, pages - 1);
        for (int i = 0; i < PER_PAGE; i++) {
            int index = page * PER_PAGE + i;
            if (index >= list.size()) break;
            T t = list.get(index);
            set(i, icon.apply(t), click -> onPick.accept(t, click));
        }
        if (back != null) {
            set(45, icon(Material.ARROW, "&#BFC7D5Quay lại", Lore.of().action("Chuột trái để quay lại").build()),
                    click -> back.run());
        }
        if (page > 0) {
            set(48, icon(Material.SPECTRAL_ARROW, "&#7FDBFFTrang trước", Lore.of()
                    .row("Trang", (page + 1) + "/" + pages).action("Chuột trái để lùi trang").build()), click -> {
                page--;
                refresh();
            });
        }
        set(49, icon(Material.OAK_SIGN, "&#FFD166Tìm kiếm", Lore.of()
                .desc("Lọc danh sách theo tên.")
                .section("Thông tin:").row("Đang lọc", search.isEmpty() ? "không" : search).row("Kết quả", String.valueOf(list.size()))
                .action("Chuột trái để nhập từ khoá").danger("Chuột phải để bỏ lọc").build()), click -> {
            if (click.isRightClick()) {
                search = "";
                page = 0;
                refresh();
                return;
            }
            ask("input.search", text -> {
                search = text.toLowerCase(Locale.ROOT);
                page = 0;
            });
        });
        if (page < pages - 1) {
            set(50, icon(Material.SPECTRAL_ARROW, "&#7FDBFFTrang sau", Lore.of()
                    .row("Trang", (page + 1) + "/" + pages).action("Chuột trái để sang trang").build()), click -> {
                page++;
                refresh();
            });
        }
        extraButtons();
        fill(5);
    }

    /** Override to add buttons on 46, 47, 51, 52, 53. */
    protected void extraButtons() {
    }

    private List<T> filtered() {
        List<T> all = items.get();
        if (search.isEmpty()) return all;
        List<T> out = new ArrayList<>();
        for (T t : all) if (searchKey.apply(t).toLowerCase(Locale.ROOT).contains(search)) out.add(t);
        return out;
    }
}
