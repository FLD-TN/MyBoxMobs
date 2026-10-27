package com.dex.myboxmobs.place;

import com.dex.myboxmobs.util.ItemCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/** A summoning altar on a block (spec 8). Saved in altars.yml. */
public final class AltarDef {

    public final String id;
    public String world;
    public int x;
    public int y;
    public int z;
    public @Nullable MobRef mob;
    /** Exact item (full NBT) and amount the player must hold; {@code null} = not set, the altar cannot be used. */
    public @Nullable ItemStack requirement;
    public int cooldownSeconds;
    public boolean refundOnDespawn = true;
    public final List<String> holoLines = new ArrayList<>();
    /** Epoch millis; saved so a restart keeps the cooldown. */
    public long cooldownUntil;

    // runtime
    public @Nullable UUID boss;
    public @Nullable UUID summoner;

    public AltarDef(String id, Block block, @Nullable MobRef mob, List<String> defaultHolo) {
        this.id = id;
        this.world = block.getWorld().getName();
        this.x = block.getX();
        this.y = block.getY();
        this.z = block.getZ();
        this.mob = mob;
        holoLines.addAll(defaultHolo);
    }

    private AltarDef(String id) {
        this.id = id;
    }

    public @Nullable Location blockLocation() {
        World w = Bukkit.getWorld(world);
        return w == null ? null : new Location(w, x, y, z);
    }

    public boolean is(Block block) {
        return block.getX() == x && block.getY() == y && block.getZ() == z && block.getWorld().getName().equals(world);
    }

    public void save(ConfigurationSection s) {
        s.set("world", world);
        s.set("x", x);
        s.set("y", y);
        s.set("z", z);
        s.set("mob", mob == null ? "" : mob.toString());
        s.set("requirement", requirement == null ? "" : ItemCodec.encode(requirement));
        s.set("cooldown-seconds", cooldownSeconds);
        s.set("refund-on-despawn", refundOnDespawn);
        s.set("hologram-lines", holoLines);
        s.set("cooldown-until", cooldownUntil);
    }

    public static AltarDef load(String id, ConfigurationSection s) {
        AltarDef d = new AltarDef(id);
        d.world = s.getString("world", "world");
        d.x = s.getInt("x");
        d.y = s.getInt("y");
        d.z = s.getInt("z");
        d.mob = MobRef.parse(s.getString("mob", ""));
        d.requirement = ItemCodec.decode(s.getString("requirement", ""));
        d.cooldownSeconds = Math.max(0, s.getInt("cooldown-seconds", 0));
        d.refundOnDespawn = s.getBoolean("refund-on-despawn", true);
        d.holoLines.addAll(s.getStringList("hologram-lines"));
        d.cooldownUntil = s.getLong("cooldown-until", 0);
        return d;
    }
}
