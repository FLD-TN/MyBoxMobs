package com.dex.myboxmobs.hook;

import java.util.UUID;
import org.bukkit.entity.Player;

/** Money (Vault), gems and boost multipliers (BoxCore) behind one interface; each part is optional. */
public interface Economy {

    boolean hasMoney();

    boolean hasGems();

    /** False when the deposit failed or the economy is missing. */
    boolean depositMoney(Player player, long amount);

    boolean depositGems(Player player, long amount, String reason);

    /** Vault balance rounded down, 0 without Vault. */
    long moneyBalance(Player player);

    /** False when the withdrawal failed (not enough, no Vault). */
    boolean withdrawMoney(Player player, long amount);

    long gemBalance(Player player);

    boolean withdrawGems(Player player, long amount, String reason);

    /** BoxCore boost multiplier of {@code type} in tenths (10 = x1). 10 when BoxCore is missing. */
    int boostTenths(UUID player, String type);

    /** Gem display name from BoxCore ("Gem"). */
    String gemName();
}
