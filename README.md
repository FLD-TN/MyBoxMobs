# MyBoxMobs

Custom mobs and bosses for the BoxPvP server on top of MythicMobs and ModelEngine.

## Install

1. Server: Paper 1.21.1. Required: **MythicMobs 5.13.0** (free release). Optional: ModelEngine R4.1.1,
   DecentHolograms, Vault, BoxCore, PlaceholderAPI, ItemsAdder.
2. Put `MyBoxMobs-<version>.jar` in `plugins/`, restart. An example mob `example_zombie` is created on the first start.
3. Donor multiplier: add a `mob` type to BoxCore `config.yml` (`decimal: true`), then give ranks
   `boxcore.boost.mob.<n>` (e.g. `boxcore.boost.mob.2`, `boxcore.boost.mob.1_5`).
4. `/mbm` opens the admin GUI (`myboxmobs.admin`).

## Build

```
./gradlew build
```

The jar is `myboxmobs-plugin/build/libs/MyBoxMobs-<version>.jar`. No jar is in git; put these in `libs/` before building:

- `DecentHolograms-2.10.1.jar`
- `ModelEngine-R4.1.1.jar`
- `MythicMobs-5.13.0.jar`
- `boxclans-api-1.4.4.jar`
- `boxcore-api-1.3.0.jar`

## Using a bought mob pack

Install each part into its own plugin (`.bbmodel` → `plugins/ModelEngine/blueprints/`, mob/skill `.yml` →
`plugins/MythicMobs/`, sounds and images → `plugins/ItemsAdder/contents/`), reload them, then in `/mbm` choose
**Tạo từ MythicMobs** and pick the mob. Skills stay in the MythicMobs files.
