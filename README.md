# Dimensional Backpacks

Build a room inside your backpack, then wear it and use the blocks in it without going in.

Each backpack holds its own pocket-dimension interior. Place a backpack and right-click it to step inside, build whatever you like, then pick it up and wear it. Hotspots on the interior walls let you right-click the block touching them from anywhere, so a chest, furnace or crafting table in your backpack is a keypress away.

Minecraft 1.21.1, NeoForge 21.1.x. Requires [Kotlin for Forge](https://modrinth.com/mod/kotlin-for-forge). [Curios](https://modrinth.com/mod/curios) is optional, and adds a back slot for the worn backpack.

## Features

- **Three tiers** of backpack, each with a bigger, differently shaped interior.
- **Placeable:** put a backpack down so others can walk in too.
- **Plots are permanent.** An interior is never deleted, and a copied backpack points at the same interior.
- **Hotspots:** place a hotspot on an interior wall block to use whatever block touches it, as if you stood there and right-clicked it. A wall block can hold a hotspot on each side, and higher tiers allow more.
- **Dyeable:** dye a backpack to tint its interior walls. A dyed backpack in a crafting grid by itself gives an undyed copy that keeps its interior.
- **Decorative wall blocks** for building inside, which don't take hotspots. Craft them with a dye to colour them.

## Controls

| Key | Action |
| --- | --- |
| `Z` (tap) | Use the primary hotspot |
| `Z` (hold) | Open a menu to pick a hotspot |
| `X` | Leave the backpack you're inside (the zipper item also works) |

Both keys can be rebound. To change the primary hotspot, right-click a hotspot with an empty hand.

## Commands

Requires operator permissions.

- `/dbp backpack <plotId>` gives you a backpack linked to that plot, for recovering one that was lost.
- `/dbp new`, `/dbp enter <plotId>` and `/dbp loaded` are debug commands.

## Configuration

`config/dimensional_backpacks-common.toml`:

- `hotspots.tier1` to `tier3`: hotspots per tier (0 to 3). 0 turns hotspots off for that tier.
- `allowed_blocks.placement`: which blocks can be placed inside backpacks.
- `allowed_blocks.hotspot`: which blocks work through hotspots.

Each list has a `mode` (`WHITELIST` or `BLACKLIST`) and a `blocks` list. Entries can be a block (`minecraft:chest`), a whole mod (`minecraft` or `minecraft:*`) or a tag (`#minecraft:logs`).

## Mod compatibility

Hotspots default to a whitelist of vanilla (`minecraft`) and Refined Storage. Many mods open their menus in ways that only work when the block is nearby, and they either fail or crash when used from a hotspot. AE2, Mekanism, Tom's Storage and Sophisticated Storage are known not to work. Add a mod to the hotspot whitelist only after testing it.

### For mod authors

If your block's menu rejects the player because they're in another dimension or out of reach, you can allow hotspot use:

```kotlin
if (HotspotApi.isUsingHotspot(player, pos)) return true
```

`HotspotApi.isUsingHotspot(player, pos)` is true while a hotspot is using the block at `pos`, and while a menu opened that way stays open. Server side only. A cancellable `HotspotUseEvent` is also posted on the NeoForge event bus before each hotspot use.

Note that this doesn't help menus that look the block up on the client, since the client doesn't have the pocket dimension loaded.

## Building

```
.\gradlew.bat build
```

The jar is written to `build\libs`. Use `.\gradlew.bat runClient` to test.

## License

[LGPL-3.0](LICENSE) (with the GPL text in [COPYING](COPYING)).
