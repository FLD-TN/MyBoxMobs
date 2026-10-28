package com.dex.myboxmobs.hook;

import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Builds the {@link Economy}: Vault and BoxCore classes are only touched inside the nested holders, which are only
 * loaded when their plugin is enabled. Services are looked up lazily because the economy plugin may enable later.
 */
public final class EconomyHooks implements Economy {

    private final Supplier<VaultPart> vault;
    private final Supplier<BoxCorePart> boxCore;
    private VaultPart vaultPart;
    private BoxCorePart boxCorePart;

    public EconomyHooks() {
        this.vault = () -> Bukkit.getPluginManager().isPluginEnabled("Vault") ? VaultPart.create() : null;
        this.boxCore = () -> Bukkit.getPluginManager().isPluginEnabled("BoxCore") ? BoxCorePart.create() : null;
    }

    private VaultPart vault() {
        if (vaultPart == null) vaultPart = vault.get();
        return vaultPart;
    }

    private BoxCorePart boxCore() {
        if (boxCorePart == null) boxCorePart = boxCore.get();
        return boxCorePart;
    }

    @Override
    public boolean hasMoney() {
        return vault() != null;
    }

    @Override
    public boolean hasGems() {
        return boxCore() != null && boxCore().gems != null;
    }

    @Override
    public boolean depositMoney(Player player, long amount) {
        VaultPart v = vault();
        return v != null && v.deposit(player, amount);
    }

    @Override
    public boolean depositGems(Player player, long amount, String reason) {
        BoxCorePart b = boxCore();
        return b != null && b.depositGems(player.getUniqueId(), amount, reason);
    }

    @Override
    public long moneyBalance(Player player) {
        VaultPart v = vault();
        return v == null ? 0 : v.balance(player);
    }

    @Override
    public boolean withdrawMoney(Player player, long amount) {
        VaultPart v = vault();
        return v != null && v.withdraw(player, amount);
    }

    @Override
    public long gemBalance(Player player) {
        BoxCorePart b = boxCore();
        return b == null ? 0 : b.gemBalance(player.getUniqueId());
    }

    @Override
    public boolean withdrawGems(Player player, long amount, String reason) {
        BoxCorePart b = boxCore();
        return b != null && b.withdrawGems(player.getUniqueId(), amount, reason);
    }

    @Override
    public int boostTenths(UUID player, String type) {
        BoxCorePart b = boxCore();
        return b == null ? 10 : b.tenths(player, type);
    }

    @Override
    public String gemName() {
        BoxCorePart b = boxCore();
        return b == null ? "Gem" : b.gemName();
    }

    private static final class VaultPart {
        private final net.milkbowl.vault.economy.Economy economy;

        private VaultPart(net.milkbowl.vault.economy.Economy economy) {
            this.economy = economy;
        }

        static VaultPart create() {
            var rsp = Bukkit.getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
            return rsp == null ? null : new VaultPart(rsp.getProvider());
        }

        boolean deposit(Player player, long amount) {
            return amount <= 0 || economy.depositPlayer(player, amount).transactionSuccess();
        }

        long balance(Player player) {
            return (long) Math.floor(Math.max(0, economy.getBalance(player)));
        }

        boolean withdraw(Player player, long amount) {
            return amount <= 0 || economy.withdrawPlayer(player, amount).transactionSuccess();
        }
    }

    private static final class BoxCorePart {
        private final com.dex.boxcore.api.GemService gems;
        private final com.dex.boxcore.api.BoostService boosts;

        private BoxCorePart(com.dex.boxcore.api.GemService gems, com.dex.boxcore.api.BoostService boosts) {
            this.gems = gems;
            this.boosts = boosts;
        }

        static BoxCorePart create() {
            var gems = Bukkit.getServicesManager().load(com.dex.boxcore.api.GemService.class);
            var boosts = Bukkit.getServicesManager().load(com.dex.boxcore.api.BoostService.class);
            return gems == null && boosts == null ? null : new BoxCorePart(gems, boosts);
        }

        boolean depositGems(UUID player, long amount, String reason) {
            if (amount <= 0) return true;
            return gems != null && gems.deposit(player, amount, reason) == com.dex.boxcore.api.GemResult.SUCCESS;
        }

        long gemBalance(UUID player) {
            return gems == null ? 0 : Math.max(0, gems.balance(player));
        }

        boolean withdrawGems(UUID player, long amount, String reason) {
            if (amount <= 0) return true;
            return gems != null && gems.withdraw(player, amount, reason) == com.dex.boxcore.api.GemResult.SUCCESS;
        }

        int tenths(UUID player, String type) {
            if (boosts == null || type == null || type.isEmpty() || !boosts.isType(type)) return 10;
            return Math.max(10, boosts.getMultiplierTenths(player, type));
        }

        String gemName() {
            return gems == null ? "Gem" : gems.displayName();
        }
    }
}
