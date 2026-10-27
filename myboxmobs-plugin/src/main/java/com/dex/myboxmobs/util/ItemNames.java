package com.dex.myboxmobs.util;

import java.util.Locale;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Item names as plain legacy text for holograms and lore (where translatable names cannot be shown). */
public final class ItemNames {

    private static final LegacyComponentSerializer AMPERSAND = LegacyComponentSerializer.builder()
            .character('&').hexColors().build();

    private ItemNames() {
    }

    /** "&#FFAA00Chìa Khoá Rồng" for named items, "&fDiamond Sword" otherwise ('&' format, for Text). */
    public static String of(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName() && meta.displayName() != null) {
            return AMPERSAND.serialize(meta.displayName());
        }
        StringBuilder sb = new StringBuilder("&f");
        for (String word : item.getType().name().toLowerCase(Locale.ROOT).split("_")) {
            if (word.isEmpty()) continue;
            if (sb.length() > 2) sb.append(' ');
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return sb.toString();
    }
}
