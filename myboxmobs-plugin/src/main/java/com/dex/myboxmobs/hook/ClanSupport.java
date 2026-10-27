package com.dex.myboxmobs.hook;

import java.util.List;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/** BoxClans behind an interface (optional plugin); {@link #NONE} is used without it. */
public interface ClanSupport {

    /** A clan as MyBoxMobs needs it: id and coloured name ('&' format). */
    record ClanInfo(UUID uuid, String name) {
    }

    ClanSupport NONE = new ClanSupport() {
        @Override
        public boolean available() {
            return false;
        }

        @Override
        public @Nullable ClanInfo clanOf(UUID player) {
            return null;
        }

        @Override
        public @Nullable ClanInfo clan(UUID clan) {
            return null;
        }

        @Override
        public boolean isLeader(UUID player) {
            return false;
        }

        @Override
        public List<Player> onlineMembers(UUID clan) {
            return List.of();
        }

        @Override
        public long awardPoints(UUID clan, String source, long amount) {
            return 0;
        }

        @Override
        public boolean giveCurrency(UUID clan, long amount, String reason) {
            return false;
        }
    };

    boolean available();

    @Nullable ClanInfo clanOf(UUID player);

    @Nullable ClanInfo clan(UUID clan);

    /** True for the Minh Chủ (leader) of a clan. */
    boolean isLeader(UUID player);

    List<Player> onlineMembers(UUID clan);

    /** Danh Vọng through a BoxClans points source (its cooldown / daily caps apply); points actually added. */
    long awardPoints(UUID clan, String source, long amount);

    /** Minh Huy into the clan's fund. */
    boolean giveCurrency(UUID clan, long amount, String reason);
}
