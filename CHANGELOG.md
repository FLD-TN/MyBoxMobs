# MyBoxMobs changelog

## 0.4.0 (2026-10-27)

First build, milestones 0.1 to 0.4 of `MYBOXMOBS_SPEC.md` in one go (owner request). Not tested on a live server yet.

- 0.1 core: mob definitions from MythicMobs or vanilla (spawn egg list), stats override (max health, armor,
  toughness, melee damage, speed, follow range, knockback resistance) only for mobs MyBoxMobs spawns, name,
  6 equipment slots (drop chance 0), ModelEngine model (keep MythicMobs' model or replace, scale, hide base),
  pack check (`(!) Thiếu model ...`), lifetime, tracking with PDC tags, orphan cleanup, `/mbm spawn|kill|killall|list`,
  main GUI and mob editor.
- 0.2 rewards: own drops fully replace vanilla and MythicMobs drops (loot table, Mythic money and exp cleared);
  items with full NBT, Vault money, BoxCore gem, console commands, chance per entry, amount ranges; killer mode or
  top damage mode (ranks 1..N, receive chance per rank); straight to inventory, overflow dropped at the feet with a
  title; offline at death = no reward; BoxCore boost multiplier (`boxcore.boost.mob.<n>`) on items, money and gem;
  whole-server chat announcements (spawn, death with `%top%`, despawn); kill statistics in SQLite;
  PlaceholderAPI `%myboxmobs_kills_<id>%`, `%myboxmobs_kills_total%`, `%myboxmobs_alive_<id>%`.
- 0.3 spawners: virtual locations, one or several weighted mobs (`mbm:` or raw `mythic:`), interval, amount per wave,
  max alive, NEARBY / ALWAYS mode, spread, leash, optional chunk ticket, DecentHolograms countdown, pause.
- 0.4 altars: any block, exact item requirement (consumed), one boss at a time, hologram hidden while it lives,
  cooldown per altar (default 0), refund when the boss leaves unkilled, block protected from breaking, explosions
  and pistons.
- API module: `MyBoxMobsService`, `MbmMobSpawnEvent`, `MbmMobDeathEvent`, `MbmMobDespawnEvent`.

Deviations from the spec, to confirm with the owner:
- Items are put into reward lists, equipment and altars by **clicking the item in your own inventory** while the
  menu is open (no dragging into the GUI).
- Admin GUI labels are in the code (Vietnamese, BoxCore style); everything players see is in `messages.yml`.
- `keep-mythic-name-in-bossbar` was not built: the custom name is given to MythicMobs itself, so its bossbar shows
  the new name.
