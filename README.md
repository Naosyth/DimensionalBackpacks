# Dimensional Backpacks

Build a room inside your backpack, then wear it and use all of its storage from one screen.

Each backpack holds its own pocket-dimension interior. Place a backpack and sneak + right-click it to step inside, build whatever you like, then pick it up and wear it. Every chest, barrel or other item-storage block in the room is combined into one searchable list, so you can reach everything with a keypress, without going in.

Minecraft 1.21.1, NeoForge 21.1.x. Requires [Kotlin for Forge](https://modrinth.com/mod/kotlin-for-forge). [Curios](https://modrinth.com/mod/curios) is optional, and adds a back slot for the worn backpack.

## Features

- **Three tiers** of backpack, each with a bigger, differently shaped interior.
- **Placeable:** put a backpack down so others can walk in too.
- **Storage screen:** all item storage in the backpack shown as one list, with search, a sort button that cycles name/amount order, and a built-in crafting grid that refills from storage. Up to nine nameable storage groups (tabs along the top) each aggregate their own set of storages; use the gear to name a group, choose which storages belong to it and drag them into priority order. Buttons above the crafting grid deposit your inventory, or only items the group already holds.
- **Copying:** craft a linked backpack with a blank one of the same tier to get another backpack linked to the same interior (the original is kept).
- **Dyeable:** dye a backpack, and the color of the interior changes to match.

## Controls

| Key | Action |
| --- | --- |
| `B` | Open the storage screen of the backpack you're wearing (right-clicking a placed backpack also works) |
| `X` | Leave the backpack you're inside (the zipper item also works) |

Both keys can be rebound.

## Commands

Requires operator permissions.

- `/dbp backpack <plotId>` gives you a backpack linked to that plot, for recovering one that was lost.
- `/dbp new`, `/dbp enter <plotId>` and `/dbp loaded` are debug commands.

## Configuration

`config/dimensional_backpacks-common.toml`:

- `allowed_blocks.placement`: which blocks can be placed inside backpacks.
- `allow_backpack_copying`: whether backpacks can be copied by crafting (default `true`).

The list has a `mode` (`WHITELIST` or `BLACKLIST`) and a `blocks` list. Entries can be a block (`minecraft:chest`), a whole mod (`minecraft` or `minecraft:*`) or a tag (`#minecraft:logs`).

## Mod compatibility

Any block that exposes the standard item handler capability is picked up by the storage screen, so most storage mods work without special support.

## Resource packs

Backpack block models can be replaced at `assets/dimensional_backpacks/models/backpack_tier_1.json` through `backpack_tier_3.json`. The item models inherit those same models, so a replacement changes the placed, held and worn backpack appearance. Backpack model textures are stitched into Minecraft's block atlas: put them under `assets/dimensional_backpacks/textures/block/` and reference them as `dimensional_backpacks:block/<texture_name>` in the model.

The storage screen uses individual GUI sprites under `assets/dimensional_backpacks/textures/gui/sprites/storage/`. Replace these sprite files in a resource pack to theme the mod's panels, buttons, controls, and icons without changing vanilla inventory slots. Standard Minecraft item icons remain individually replaceable through their item models and textures.

The mod jar also includes the optional `DBSimple` resource pack, which provides a simple storage-screen theme. Enable it from the Resource Packs menu.

## Building

```
.\gradlew.bat build
```

The jar is written to `build\libs`. Use `.\gradlew.bat runClient` to test.

## License

[LGPL-3.0](LICENSE) (with the GPL text in [COPYING](COPYING)).
