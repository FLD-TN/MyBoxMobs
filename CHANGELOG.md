# MyBoxMobs changelog

## 0.5.4 (2026-10-27)

- Spawner hologram: the value comes before its icon ("2.500 ❤  |  8 ⚔"). config.yml goes to config-version 5;
  mobs that saved the 0.5.3 default template are migrated.

## 0.5.3 (2026-10-27)

- Spawner hologram per mob: every mob has its own template (mob editor > Holo spawner, Shift + right click
  resets it to the config default); a spawner shows the template of the mob it spawns (raw MythicMobs mobs use
  the config default). The per-spawner lines of spawners.yml are no longer used.
- New default layout: [mob name], blank, health | damage, "Đang tồn tại: x/x quái", blank, "Xuất hiện sau",
  spinning spawner icon at the bottom. config.yml goes to config-version 4.

## 0.5.2 (2026-10-27)

- Spawner hologram: damage is the icon and the number only, like health (no "sát thương" word). Health / damage
  come from the MyBoxMobs override, else the real values of the last mob of that kind that spawned, else the
  MythicMobs file; "?" until known. messages.yml goes to config-version 4.
- Fix: two ALWAYS spawners with keep-chunk-loaded in the same chunk removed each other's chunk ticket every
  second; tickets are now synced once per tick from the set of chunks that need one.

## 0.5.1 (2026-10-27)

- Spawner hologram: last line is "Đang tồn tại: x/x quái" with coloured numbers and a white "/" (the dark grey one
  was unreadable); a mob that only deals damage with skills shows "sát thương" instead of "theo skill".
- config.yml and messages.yml go to config-version 3. Spawners on the 0.5.0 default hologram are migrated.

## 0.5.0 (2026-10-27)

- Skills: each mob can reuse MythicMobs skills (GUI "Skill" in the mob editor, list of every loaded MythicMobs
  skill with search). Per skill: trigger (spawn, every X s, when damaged, when it hits, death), target (mob's
  target, trigger entity, nearest player, itself), interval / cooldown, chance. Works for vanilla and MythicMobs
  mobs; skill damage does not re-trigger skills in the same tick.
- Damage leaderboard in death announcements: header "Bảng xếp hạng sát thương", boss name centred under it, blank
  line, then (1) (2) (3)... with gold / silver / bronze / grey rank colours (config announce.top-header,
  top-line, rank-colors). Any announcement line starting with <center> is centred in the chat.
- Spawner hologram: spinning empty spawner icon (#ICON: SPAWNER), boss health (❤) and damage (⚔), pastel colours.
  New placeholders %health% %damage%; DecentHolograms line types (#ICON, #HEAD...) pass through untouched.
- Altar requirement: the item can be replaced (click another item in your inventory), right click clears it,
  chat confirmation each time.
- Quantities are written before the item, in white: "x1 Trial Key" (altar hologram, altar message, reward
  message, spawn message).
- config.yml and messages.yml go to config-version 2 (old files are moved to old-configs/). Existing altars,
  spawners and mobs still on the old default texts are migrated automatically; edited texts are kept.

## 0.4.1 (2026-10-27)

- Fix: killing a managed MythicMobs mob threw a NullPointerException in `MythicMobLootDropEvent` on MythicMobs
  5.13.1 (`setExp`/`setMoney` build a drop with a null config). Mythic exp and money are already removed by
  clearing the intangible loot table, so those calls were dropped.

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
