# MyBoxMobs changelog

## 0.7.3 (2026-10-27)

- `/mbm spawn` without coordinates, "Spawn thử" and the spawn button of the mob list put the mob 4 blocks in front
  of the admin, on the ground, facing them, instead of on the admin's own position. A mob spawned exactly on its
  target made MythicMobs `look` / `lunge` aim along a zero vector: console spam "Invalid entity rotation: NaN" and
  "x not finite" (e.g. the Kragan intro).

## 0.7.2 (2026-10-27)

- Health display is now opt-in per mob, both off by default (owner decision): "Thanh máu ở tên" (on: name + bar,
  off: the name alone) and the new "Thanh máu bossbar" (name and health in a boss bar for players within
  `bossbar.range` blocks; for bosses without a model or whose model has no boss bar). Mob files saved by 0.7.1
  (`nametag.health-bar`) are ignored: the keys are now `nametag.name-bar` and `nametag.boss-bar`.
- config.yml: `bossbar.title`, `color`, `style`, `range` (built-in defaults, no config-version bump).

## 0.7.1 (2026-10-27)

- Name + health bar above every MyBoxMobs mob that has a name: "Kragan &8| ||||||||||||||||||||", red for the
  health left, grey for the health lost, emptying from right to left; updated right after each hit or heal.
- Mobs with a ModelEngine model (whose base entity and its name are hidden, e.g. Kragan) get the same line as a
  floating text that follows the model at the height of its hitbox (+ a per-mob offset, default 0,3 block).
- Mob editor: "Thanh máu trên tên" (on/off, right click for the height). config.yml: `nametag.format`, `bar-length`,
  `bar-char`, `full-color`, `empty-color` (built-in defaults, no config-version bump).

## 0.7.0 (2026-10-27)

BoxClans integration (optional: everything clan-related is hidden or inactive without BoxClans 1.4+).

- New reward mode "Top bang hội": player damage summed per clan. Each clan rank has a chance, Danh Vọng (points
  source `mob-boss`) and Minh Huy for the clan, and member gifts for every member of that clan who dealt damage,
  is online and dealt at least the minimum share of the clan's damage (default 0%). Online members of a ranked clan
  are told their rank; BoxClans announces the Danh Vọng and Minh Huy itself.
- New reward mode "Quà bang hội" for clan altar bosses: no Danh Vọng or Minh Huy, one gift list rolled once and
  given equally to the members who dealt damage and are online (or every online member, per mob).
- Danh Vọng for the killer's clan on every kill, per mob (points source `mob-kill`, 0 = off; not in "Quà bang hội").
- Clan altar ("Bàn bang hội", altar editor): only a Minh Chủ can summon; the boss belongs to that clan, only its
  members can damage it (the boss still attacks everyone); its announcements and the summon message
  ("Minh Chủ X đã triệu hồi ... cho toàn bang hội") go to that clan only. One boss per altar, cooldown as before,
  a despawn gives nothing.
- `/mbm altar givekey <altar> <player> [amount]` gives the altar's summoning item (for the Minh Huy shop).
- Death announcement: `%top%` shows the player board in every mode but killer; in "Top bang hội" it is followed by
  the clan board, or the lines place it with `%top_clans%`. New config keys `announce.board-size`,
  `announce.top-clan-header`, `announce.top-clan-line`, `clans.points-source-top/kill` (defaults built in, old
  config.yml files keep working unchanged: no config-version bump).
- Reward GUI follows the mode: only the settings of the current mode are shown, switching to a mode with an empty
  setup fills its defaults (top: 3 ranks 100/70/50%; top clan: 3 ranks 50/30/15 Danh Vọng, 200/100/50 Minh Huy).
- API: `MyBoxMobsService.ownerClanOf(entity)`.
- BoxClans points.yml (default) gets the sources `mob-boss` and `mob-kill`; servers must add them to their own
  points.yml.

## 0.6.1 (2026-10-27)

- The damage table (and the last hitter) of a managed mob is saved in the mob itself when its chunk unloads or the
  server stops, and restored when it is tracked again: a boss left almost dead keeps its top damage list after a
  restart or when everybody walks away.

## 0.6.0 (2026-10-27)

- Mobs survive chunk unloads and restarts: managed mobs are saved with the world like vanilla mobs (spawn time,
  origin and summoner in their tags) and are tracked again when their chunk loads; lifetime keeps running from
  the original spawn. The damage table starts empty after a reload. (Before: removed on unload / restart.)
- Spawner countdown is saved (next-wave-at in spawners.yml) and keeps running without players; a due wave waits
  until the spawner is active. New schedule mode: spawn at clock times ("12:00 20:30", timezone in config.yml);
  a time missed while the server was off is skipped.
- Altars never give the summoning item back (owner decision). The altar's living boss is saved; the altar opens
  again when the boss dies, despawns, or is gone for 5 s while its last chunk is loaded.
- Damage leaderboard lines like the CustomFishing competition: "① Duy — gây ra 12.500 sát thương", colours
  #FCD34D / #E2E8F0 / #D97706, ranks after 3 in &7, placeholder %icon% (① to ⑳).
- config.yml config-version 7, messages.yml config-version 5.

## 0.5.5 (2026-10-27)

- Spawner hologram default: no blank line between "Đang tồn tại" and "Xuất hiện sau", a blank line before the
  spinning spawner icon. config.yml goes to config-version 6; mobs on the 0.5.3 / 0.5.4 default are migrated.

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
