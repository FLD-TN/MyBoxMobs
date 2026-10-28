package com.dex.myboxmobs.place;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.Nullable;

/** Finds a standing place on the ground: a solid block with free space above it. */
public final class Ground {

    private Ground() {
    }

    /**
     * Scans column {@code x z} from {@code topY} down to {@code bottomY} for a solid block with 3 passable blocks
     * above; returns the centre of the block on top of it, raised by {@code lift} blocks. Null when none, or when
     * the chunk is not loaded (never loads chunks).
     */
    public static @Nullable Location find(World world, int x, int z, int topY, int bottomY, double lift) {
        if (!world.isChunkLoaded(x >> 4, z >> 4)) return null;
        int top = Math.min(topY, world.getMaxHeight() - 4);
        int bottom = Math.max(bottomY, world.getMinHeight());
        for (int y = top; y >= bottom; y--) {
            Block floor = world.getBlockAt(x, y, z);
            if (floor.isPassable() || floor.isLiquid()) continue;
            if (floor.getRelative(0, 1, 0).isPassable() && floor.getRelative(0, 2, 0).isPassable()
                    && floor.getRelative(0, 3, 0).isPassable() && !floor.getRelative(0, 1, 0).isLiquid()) {
                return new Location(world, x + 0.5, y + 1 + lift, z + 0.5);
            }
        }
        return null;
    }
}
