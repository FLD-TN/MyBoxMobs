package com.dex.myboxmobs.command;

import com.dex.myboxmobs.MyBoxMobsPlugin;
import com.dex.myboxmobs.config.Messages;
import com.dex.myboxmobs.gui.MainMenu;
import com.dex.myboxmobs.live.LiveMob;
import com.dex.myboxmobs.mob.MobDefinition;
import com.dex.myboxmobs.place.AltarDef;
import com.dex.myboxmobs.place.MobRef;
import com.dex.myboxmobs.place.SpawnerDef;
import com.dex.myboxmobs.text.Format;
import com.dex.myboxmobs.text.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

/** {@code /myboxmobs} (alias {@code /mbm}), spec 10. Everything needs {@code myboxmobs.admin}. */
public final class MbmCommand implements TabExecutor {

    private static final List<String> SUBS = List.of("spawn", "kill", "killall", "list", "spawner", "altar", "reload", "help");

    /** Blocks in front of the admin where /mbm spawn puts the mob. */
    private static final double SPAWN_DISTANCE = 4;

    private final MyBoxMobsPlugin plugin;

    public MbmCommand(MyBoxMobsPlugin plugin) {
        this.plugin = plugin;
    }

    private Messages m() {
        return plugin.messages();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("myboxmobs.admin")) {
            m().send(sender, "command.no-permission");
            return true;
        }
        if (args.length == 0) {
            if (sender instanceof Player p) new MainMenu(plugin, p).open();
            else help(sender);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "spawn" -> spawn(sender, args);
            case "kill" -> kill(sender, args, false);
            case "killall" -> kill(sender, args, true);
            case "list" -> list(sender);
            case "tpmob" -> tpMob(sender, args);
            case "spawner" -> spawner(sender, args);
            case "altar" -> altar(sender, args);
            case "reload" -> {
                plugin.reloadAll();
                m().send(sender, "command.reloaded");
            }
            default -> help(sender);
        }
        return true;
    }

    private void help(CommandSender sender) {
        for (String line : m().rawList("command.help")) sender.sendMessage(Text.parse(line));
    }

    private void spawn(CommandSender sender, String[] args) {
        if (args.length < 2) {
            m().send(sender, "command.usage", "usage", "/mbm spawn <mob> [số lượng] [world x y z]");
            return;
        }
        MobDefinition def = plugin.registry().get(args[1]);
        if (def == null) {
            m().send(sender, "mob.unknown", "id", args[1]);
            return;
        }
        int amount = 1;
        if (args.length >= 3) {
            double v = Format.parse(args[2]);
            if (Double.isNaN(v) || v < 1 || v > 100) {
                m().send(sender, "command.usage", "usage", "/mbm spawn <mob> [1-100] [world x y z]");
                return;
            }
            amount = (int) v;
        }
        Location at;
        if (args.length >= 7) {
            World w = Bukkit.getWorld(args[3]);
            double x = Format.parse(args[4]), y = Format.parse(args[5]), z = Format.parse(args[6]);
            if (w == null || Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z)) {
                m().send(sender, "command.bad-location");
                return;
            }
            at = new Location(w, x, y, z);
        } else if (sender instanceof Player p) {
            at = inFront(p);
        } else {
            m().send(sender, "command.usage", "usage", "/mbm spawn <mob> <số lượng> <world> <x> <y> <z>");
            return;
        }
        int done = 0;
        for (int i = 0; i < amount; i++) {
            if (plugin.mobs().spawn(def, at, "command", null, i == 0) != null) done++;
        }
        if (done == 0) m().send(sender, "mob.spawn-failed", "id", def.id);
        else m().send(sender, "mob.spawned", "id", def.id, "amount", String.valueOf(done));
    }

    /**
     * A few blocks in front of the player, on the ground, facing them. Never on the player's own position: skills
     * aimed at a target standing exactly on the mob (look, lunge) get a zero direction and fail with NaN.
     */
    public static Location inFront(Player p) {
        Location base = p.getLocation();
        org.bukkit.util.Vector dir = base.getDirection().setY(0);
        if (dir.lengthSquared() < 1.0E-6) dir = new org.bukkit.util.Vector(0, 0, 1);
        dir.normalize();
        Location at = base.clone().add(dir.clone().multiply(SPAWN_DISTANCE));
        // out of a wall (up to 3 blocks), then down to the ground (up to 6 blocks)
        for (int i = 0; i < 3 && !at.getBlock().isPassable(); i++) at.add(0, 1, 0);
        for (int i = 0; i < 6 && at.clone().subtract(0, 1, 0).getBlock().isPassable(); i++) at.subtract(0, 1, 0);
        if (!at.getBlock().isPassable()) at = base.clone().add(dir.clone().multiply(1.5));
        at.setDirection(dir.multiply(-1));
        at.setPitch(0);
        return at;
    }

    private void kill(CommandSender sender, String[] args, boolean all) {
        String id = null;
        int worldArg = 1;
        if (!all) {
            if (args.length < 2) {
                m().send(sender, "command.usage", "usage", "/mbm kill <mob> [world]");
                return;
            }
            id = args[1];
            worldArg = 2;
        }
        World world = null;
        if (args.length > worldArg) {
            world = Bukkit.getWorld(args[worldArg]);
            if (world == null) {
                m().send(sender, "command.unknown-world", "world", args[worldArg]);
                return;
            }
        }
        int n = plugin.mobs().kill(id, world);
        m().send(sender, "mob.killed", "amount", String.valueOf(n));
    }

    private void list(CommandSender sender) {
        var all = plugin.mobs().tracker().all();
        m().send(sender, "list.header", "amount", String.valueOf(all.size()));
        for (LiveMob mob : all) {
            Location at = mob.entity.getLocation();
            String name = mob.def == null ? mob.id : plugin.mobs().displayName(mob.def);
            var line = Text.parse(Messages.apply(m().raw("list.line"), "mob", name, "id", mob.id,
                    "world", at.getWorld().getName(), "x", String.valueOf(at.getBlockX()),
                    "y", String.valueOf(at.getBlockY()), "z", String.valueOf(at.getBlockZ()), "origin", mob.origin));
            sender.sendMessage(line.clickEvent(ClickEvent.runCommand("/mbm tpmob " + mob.uuid()))
                    .hoverEvent(HoverEvent.showText(Text.parse(m().raw("list.hover")))));
        }
    }

    private void tpMob(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p) || args.length < 2) return;
        try {
            LiveMob mob = plugin.mobs().tracker().get(UUID.fromString(args[1]));
            if (mob != null && mob.entity.isValid()) p.teleport(mob.entity.getLocation());
        } catch (IllegalArgumentException ignored) {
            // not a uuid
        }
    }

    // ------------------------------------------------------------------ spawners

    private void spawner(CommandSender sender, String[] args) {
        String sub = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "list";
        if (sub.equals("list")) {
            m().send(sender, "spawner.list-header", "amount", String.valueOf(plugin.spawners().all().size()));
            for (SpawnerDef d : plugin.spawners().all()) {
                sender.sendMessage(Text.parse(Messages.apply(m().raw("spawner.list-line"), "id", d.id,
                        "alive", String.valueOf(plugin.spawners().alive(d)), "max", String.valueOf(d.maxAlive),
                        "state", d.enabled ? m().raw("state.on") : m().raw("state.off"))));
            }
            return;
        }
        if (args.length < 3) {
            m().send(sender, "command.usage", "usage", "/mbm spawner <create|delete|tp|toggle|holo|pos1|pos2> <mã>");
            return;
        }
        String id = args[2].toLowerCase(Locale.ROOT);
        if (sub.equals("create")) {
            if (!(sender instanceof Player p)) {
                m().send(sender, "command.player-only");
                return;
            }
            if (!MobDefinition.ID.matcher(id).matches()) {
                m().send(sender, "input.invalid-id");
            } else if (plugin.spawners().get(id) != null) {
                m().send(sender, "spawner.exists", "id", id);
            } else {
                plugin.spawners().create(id, p.getLocation());
                m().send(sender, "spawner.created", "id", id);
            }
            return;
        }
        SpawnerDef d = plugin.spawners().get(id);
        if (d == null) {
            m().send(sender, "spawner.unknown", "id", id);
            return;
        }
        switch (sub) {
            case "delete" -> {
                plugin.spawners().delete(d);
                m().send(sender, "spawner.deleted", "id", id);
            }
            case "tp" -> {
                Location at = d.location();
                if (sender instanceof Player p && at != null) p.teleport(at);
            }
            case "toggle" -> {
                d.enabled = !d.enabled;
                plugin.spawners().save();
                m().send(sender, "spawner.toggled", "id", id, "state", d.enabled ? m().raw("state.on") : m().raw("state.off"));
            }
            case "holo" -> {
                d.hologram = !d.hologram;
                plugin.spawners().save();
                m().send(sender, "spawner.holo-toggled", "id", id, "state", d.hologram ? m().raw("state.on") : m().raw("state.off"));
            }
            case "pos1", "pos2" -> {
                if (!(sender instanceof Player p)) {
                    m().send(sender, "command.player-only");
                    return;
                }
                com.dex.myboxmobs.gui.SpawnerEditorMenu.setCorner(plugin, p, d, sub.equals("pos1") ? 1 : 2);
            }
            default -> m().send(sender, "command.usage", "usage", "/mbm spawner <create|delete|tp|toggle|holo|pos1|pos2> <mã>");
        }
    }

    // ------------------------------------------------------------------ altars

    private void altar(CommandSender sender, String[] args) {
        String sub = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "list";
        if (sub.equals("list")) {
            m().send(sender, "altar.list-header", "amount", String.valueOf(plugin.altars().all().size()));
            for (AltarDef d : plugin.altars().all()) {
                sender.sendMessage(Text.parse(Messages.apply(m().raw("altar.list-line"), "id", d.id,
                        "mob", d.mob == null ? "?" : d.mob.toString(), "world", d.world,
                        "x", String.valueOf(d.x), "y", String.valueOf(d.y), "z", String.valueOf(d.z))));
            }
            return;
        }
        if (sub.equals("create")) {
            if (!(sender instanceof Player p)) {
                m().send(sender, "command.player-only");
                return;
            }
            if (args.length < 4) {
                m().send(sender, "command.usage", "usage", "/mbm altar create <mã> <mob | mythic:mob>");
                return;
            }
            String id = args[2].toLowerCase(Locale.ROOT);
            MobRef ref = MobRef.parse(args[3]);
            Block block = p.getTargetBlockExact(plugin.settings().altarInteractRange);
            if (!MobDefinition.ID.matcher(id).matches()) {
                m().send(sender, "input.invalid-id");
            } else if (plugin.altars().get(id) != null) {
                m().send(sender, "altar.exists", "id", id);
            } else if (ref == null || (!ref.mythic() && plugin.registry().get(ref.id()) == null)
                    || (ref.mythic() && !plugin.mythic().exists(ref.id()))) {
                m().send(sender, "mob.unknown", "id", args[3]);
            } else if (block == null || block.getType().isAir()) {
                m().send(sender, "altar.look-at-block", "block", plugin.settings().altarDefaultBlock.name());
            } else if (plugin.altars().at(block) != null) {
                m().send(sender, "altar.block-used");
            } else {
                plugin.altars().create(id, block, ref);
                m().send(sender, "altar.created", "id", id);
            }
            return;
        }
        if (args.length < 3) {
            m().send(sender, "command.usage", "usage", "/mbm altar <create|delete|list|givekey> <mã>");
            return;
        }
        AltarDef d = plugin.altars().get(args[2]);
        if (d == null) {
            m().send(sender, "altar.unknown", "id", args[2]);
            return;
        }
        if (sub.equals("delete")) {
            plugin.altars().delete(d);
            m().send(sender, "altar.deleted", "id", d.id);
        } else if (sub.equals("givekey")) {
            giveKey(sender, args, d);
        } else {
            m().send(sender, "command.usage", "usage", "/mbm altar <create|delete|list|givekey> <mã>");
        }
    }

    /** {@code /mbm altar givekey <altar> <player> [amount]}: works from the console (shops). */
    private void giveKey(CommandSender sender, String[] args, AltarDef d) {
        if (args.length < 4) {
            m().send(sender, "command.usage", "usage", "/mbm altar givekey <tế đàn> <người chơi> [số lượng]");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[3]);
        if (target == null) {
            m().send(sender, "command.player-offline", "player", args[3]);
            return;
        }
        if (d.requirement == null) {
            m().send(sender, "altar.no-key", "id", d.id);
            return;
        }
        int amount = d.requirement.getAmount();
        if (args.length >= 5) {
            double v = Format.parse(args[4]);
            if (Double.isNaN(v) || v < 1 || v > 2304) {
                m().send(sender, "command.usage", "usage", "/mbm altar givekey <tế đàn> <người chơi> [1-2304]");
                return;
            }
            amount = (int) v;
        }
        int given = plugin.altars().giveKey(d, target, amount);
        m().send(sender, "altar.key-given", "amount", String.valueOf(given), "item",
                com.dex.myboxmobs.util.ItemNames.of(d.requirement), "player", target.getName());
    }

    // ------------------------------------------------------------------ tab

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("myboxmobs.admin")) return List.of();
        if (args.length == 1) return filter(SUBS, args[0]);
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            return switch (sub) {
                case "spawn", "kill" -> filter(plugin.registry().ids(), args[1]);
                case "killall" -> filter(worlds(), args[1]);
                case "spawner" -> filter(List.of("create", "delete", "list", "tp", "toggle", "holo", "pos1", "pos2"), args[1]);
                case "altar" -> filter(List.of("create", "delete", "list", "givekey"), args[1]);
                default -> List.of();
            };
        }
        if (args.length == 3) {
            return switch (sub) {
                case "kill" -> filter(worlds(), args[2]);
                case "spawner" -> args[1].equalsIgnoreCase("create") ? List.of()
                        : filter(plugin.spawners().all().stream().map(d -> d.id).toList(), args[2]);
                case "altar" -> args[1].equalsIgnoreCase("delete") || args[1].equalsIgnoreCase("givekey")
                        ? filter(plugin.altars().all().stream().map(d -> d.id).toList(), args[2]) : List.of();
                default -> List.of();
            };
        }
        if (args.length == 4 && sub.equals("altar") && args[1].equalsIgnoreCase("create")) {
            List<String> mobs = new ArrayList<>(plugin.registry().ids());
            for (String name : plugin.mythic().mobNames()) mobs.add("mythic:" + name);
            return filter(mobs, args[3]);
        }
        if (args.length == 4 && sub.equals("altar") && args[1].equalsIgnoreCase("givekey")) {
            return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[3]);
        }
        if (args.length == 4 && sub.equals("spawn") && sender instanceof Player) return List.of();
        if (args.length == 4 && sub.equals("spawn")) return filter(worlds(), args[3]);
        return List.of();
    }

    private static List<String> worlds() {
        return Bukkit.getWorlds().stream().map(World::getName).toList();
    }

    private static List<String> filter(List<String> options, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        return options.stream().filter(o -> o.toLowerCase(Locale.ROOT).startsWith(p)).limit(50).toList();
    }
}
