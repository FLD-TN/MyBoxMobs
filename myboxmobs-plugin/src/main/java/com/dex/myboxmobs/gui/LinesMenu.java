package com.dex.myboxmobs.gui;

import com.dex.myboxmobs.MyBoxMobsPlugin;
import com.dex.myboxmobs.text.Lore;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

/** Edits a list of text lines (announcements, hologram lines): add, edit, move, delete, preview. */
public final class LinesMenu extends Menu {

    private static final int MAX = 45;

    private final List<String> lines;
    private final List<String> placeholders;
    private final Runnable onChange;
    private final Runnable preview;
    private final Menu parent;

    public LinesMenu(MyBoxMobsPlugin plugin, Player player, String title, List<String> lines, List<String> placeholders,
                     Runnable onChange, Runnable preview, Menu parent) {
        super(plugin, player, 6, title);
        this.lines = lines;
        this.placeholders = placeholders;
        this.onChange = onChange;
        this.preview = preview;
        this.parent = parent;
    }

    @Override
    protected void draw() {
        for (int i = 0; i < Math.min(MAX, lines.size()); i++) {
            int index = i;
            String line = lines.get(i);
            set(i, icon(Material.PAPER, "&#BFC7D5Dòng " + (i + 1), Lore.of()
                    .line(line.isEmpty() ? "&8(dòng trống)" : line)
                    .action("Chuột trái để sửa").secondary("Chuột phải để đưa lên trên")
                    .secondary("Shift + chuột trái để đưa xuống dưới").danger("Shift + chuột phải để xoá").build()),
                    click -> edit(index, click));
        }
        set(45, icon(Material.ARROW, "&#BFC7D5Quay lại", Lore.of().action("Chuột trái để quay lại").build()),
                click -> parent.open());
        set(48, icon(Material.LIME_DYE, Palette.ANNOUNCE + "Thêm dòng", Lore.of()
                .desc("Gõ \"trong\" để thêm một dòng trống.").action("Chuột trái để nhập dòng mới").build()), click -> {
            if (lines.size() >= MAX) return;
            ask("input.line", text -> {
                lines.add(text.equalsIgnoreCase("trong") ? "" : text);
                onChange.run();
            });
        });
        Lore help = Lore.of().desc("Mã màu: &&a, &&#RRGGBB, \\<gradient:#..:#..>, \\<rainbow>. Placeholder:");
        for (String p : placeholders) help.line("&7• &f" + p);
        set(49, icon(Material.BOOK, Palette.ANNOUNCE + "Hướng dẫn", help.build()), null);
        set(50, icon(Material.SPYGLASS, Palette.ANNOUNCE + "Xem thử", Lore.of()
                .desc("Gửi các dòng này cho riêng bạn trong chat.").action("Chuột trái để xem thử").build()), click -> {
            player.closeInventory();
            preview.run();
        });
        fill(5);
    }

    private void edit(int index, ClickType click) {
        if (index >= lines.size()) return;
        switch (click) {
            case SHIFT_RIGHT -> {
                lines.remove(index);
                onChange.run();
                refresh();
            }
            case RIGHT -> {
                if (index > 0) swap(index, index - 1);
            }
            case SHIFT_LEFT -> {
                if (index < lines.size() - 1) swap(index, index + 1);
            }
            default -> ask("input.line", text -> {
                if (index < lines.size()) lines.set(index, text.equalsIgnoreCase("trong") ? "" : text);
                onChange.run();
            });
        }
    }

    private void swap(int a, int b) {
        List<String> copy = new ArrayList<>(lines);
        lines.set(a, copy.get(b));
        lines.set(b, copy.get(a));
        onChange.run();
        refresh();
    }
}
