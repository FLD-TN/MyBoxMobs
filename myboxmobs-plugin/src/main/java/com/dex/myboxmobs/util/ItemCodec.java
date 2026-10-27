package com.dex.myboxmobs.util;

import java.util.Base64;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Items saved in YAML as Base64 of Paper's byte format: the full item (NBT, components) comes back exactly. */
public final class ItemCodec {

    private ItemCodec() {
    }

    public static String encode(ItemStack item) {
        return Base64.getEncoder().encodeToString(item.serializeAsBytes());
    }

    public static @Nullable ItemStack decode(@Nullable String data) {
        if (data == null || data.isEmpty()) return null;
        try {
            return ItemStack.deserializeBytes(Base64.getDecoder().decode(data));
        } catch (RuntimeException ex) {
            return null;
        }
    }

    public static boolean isEmpty(@Nullable ItemStack item) {
        return item == null || item.getType().isAir() || item.getAmount() <= 0;
    }
}
