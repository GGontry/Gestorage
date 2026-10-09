# Changelog

All notable changes to Gestorage are documented in this file.

## Unreleased

### Added

- **Shulker Restock: reversible refill order** — you can now choose whether items are pulled out of the shulker box from the first slot to the last (the default) or from the last slot to the first. Toggle it with the `Reverse Order` option in the module settings or its own assignable keybind; the server honours your choice in the `refill_request` packet.

### Before you update

- Client and server must run the same version: the refill packet now carries the refill-order field.

## 4.0.0

Released 2026-10-09.

### Fixed

- **Shulker Restock: the hotbar count no longer looks like it is being consumed when refilling blocks.** Placing a block is predicted on your client (the stack is decremented locally), so when a refill restored the count to the very number the server had already sent, the correction was never pushed and your client kept counting down while the server stayed full. The server now forces the corrected stack back to the client after a refill, so blocks behave like rockets and every other item.
- **Tool Wheel: Auto Tool now always swaps into the pinned Default Slot.** Previously it only moved your hand when it had to change the tool, so when the tool already in your hand was the right one for the block it left your selection untouched and the Default Slot was ignored. Auto Tool now selects this slot on every mine start, whether or not an item swap is needed.

### Before you update

- Client and server must run the same version.

## 3.0.0

Released 2026-10-04.

### Stackable Shulkers

- **Now a command, like the ender chest size** — turn it on with `/gestorage shulkerstack on` (or off with `/gestorage shulkerstack off`), and check the current state with `/gestorage shulkerstack`. Changing it requires OP level 2, it is per world and it takes effect immediately. `/gestorage shulkerstack` with no arguments only reports the state.
- **Plain `/gamerule` works too** — `gestorage:stackableShulkers` is a real game rule now, so `/gamerule gestorage:stackableShulkers true` does the same thing and every client is told about the change right away, not only on join.
- The stacking itself is unchanged: empty shulker boxes stack up to 64 everywhere, hoppers and droppers included, and a shulker box that holds items still stacks to 1.

### Tool Wheel

- **Enchantment preference** — a new `Prefer: None / Fortune / Silk Touch` option in the Tool Wheel settings, with its own assignable keybind. A wheel tool carrying the preferred enchantment is used whenever it can break the block, even against a faster tool without it; when no preferred tool can break the block, Auto Tool falls back to the fastest tool. Stored per player, synced to your client, applied to the next block you start breaking.
- **Auto Tool hands back exactly what it took** — one second after you stop mining, whatever was in the tool slot goes back into that slot and the auto tool goes back into the wheel slot it came from. A stack you used, repaired or re-enchanted while it sat in the wheel is still handed back, nothing is ever duplicated, destroyed or shuffled into another slot, and turning Auto Tool off while a tool is swapped in returns it immediately.
- **Reconnecting no longer disables Auto Tool** — your wheel, the Auto Tool flag and the pinned slot are loaded from your save as you join, so Auto Tool works right away instead of only after you open the wheel once. Players who never used the module still get no save file created.
- **Default Slot is command-only** — pin the hotbar slot Auto Tool swaps into with `/gestorage toolslot <1-9>`, or `/gestorage toolslot clear` to follow the selected slot again. The row and its keybind are no longer in the config screen.

### Fixed

- Changing hotbar slots while Auto Tool works no longer makes your hand jump back to the tool slot: returning a tool never moves your selection, so you keep whatever slot you switched to.
- Auto Tool no longer drags an item out of the slot that held it. Whatever the tool slot contained (a bow, a block, a second pickaxe) always goes back to that same slot, instead of being looked for elsewhere and swapped with a mining tool that stayed behind in its place.
- Two Auto Tool swaps in a row no longer leave the two tools in each other's wheel slot: a pending swap is rolled back before the next decision, so mining stone → dirt → stone keeps the pickaxe and the shovel where they belong.
- The Efficiency bonus now only applies to tools that actually break the block, so an Efficiency V helmet, a block or a stick can no longer out-score a pickaxe.
- The one second revert no longer fires in the middle of a vein while the game keeps breaking a block on its own after the button was released.
- Switching the Tool Wheel module off now also switches Auto Tool off on the server, instead of the server keeping to swap tools for a module you had turned off. The module state is reported to the server when you join and on every toggle.
- Auto Tool ignores block positions the player could not actually reach instead of reacting to them.

### Before you update

- Stackable Shulkers moved out of the config screen and into the `gestorage:stackableShulkers` game rule. If you had it enabled, the old `shulker_stack.json` is imported into the rule the first time you start your world, so nothing changes; both old files are then renamed with a `.migrated` suffix and are no longer read. If you had it off, just run `/gestorage shulkerstack on` when you want it.
- The Stackable Shulkers toggle keybind is gone, along with the `shulker_stack_keybinds.json` file that stored it. Use the command instead.
- `/gestorage tooldefault` no longer exists and the persistent `Default Tool` setting is gone. Auto Tool now remembers each swap it makes and returns the items themselves, so nothing has to be held when you turn it on.
- The `Default Slot` keybind is gone from the config screen; use `/gestorage toolslot <1-9>` to re-pin the slot.
- Client and server must run the same version.

## 3.0.0-beta.1

A modular storage and inventory overhaul for Fabric 1.21.1. This is a **pre-release**: every module still ships **disabled by default**, so you only activate the mechanics you actually want.

### New module: Tool Wheel

A radial tool selector built on real item swaps — no virtual hand, no duplicated items.

- **Persistent 9-slot wheel**, stored per player in the world. Open it as a vanilla 9x1 chest, or press a key to bring up a radial HUD wheel (9 sectors) and click the sector you want.
- Every selection is a **real swap** between the wheel slot and your hand, so nothing is ever duplicated or lost.
- While the wheel is open, attack/use/pick input is blocked, so a click can never fall through and accidentally mine or place a block.
- **Auto Tool** (server-side, per player): on every block-break start it compares your hand against every wheel tool and instantly swaps in a strictly faster one — re-evaluated on every mine start, so mining stone then dirt does not get stuck on the pickaxe. One second after you stop mining, it puts your tool back. Guards make sure a manual change is never clobbered and a mid-vein revert can never fire.

#### Auto Tool: choose your default tool and your tool slot

- **Default Tool** — the tool Auto Tool always returns to. Set it with `/gestorage tooldefault set [<slot 1-9>]` (or the `Default Tool` row in the config screen, which takes the item in your hand) and remove it with `/gestorage tooldefault clear`. On revert it is looked for in the wheel first and then across your whole inventory, and swapped back, so a restocked or moved tool still matches. If it cannot be found at all, the previous session behaviour takes over, so the auto tool is never stranded in your hand.
- **Default Slot** — pin the hotbar slot Auto Tool swaps into. When Auto Tool fires, your hand moves to that slot and the swap always happens there; the client is notified immediately, so what you see is what you click. Set it with `/gestorage toolslot <1-9>`, with the new assignable keybind, or by clicking the `Default Slot` row (cycles `Selected → 1..9 → Selected`).

Both settings are server-authoritative, stored per player in the wheel state file and synced to your client. Existing wheel saves load untouched: the new fields are written additively, so no wheel data is wiped.

### Careful Break

- **Auto Replant is now two separate options**: `Auto Replant Trees` (used by Tree Capitator) and `Auto Replant Crops` (used by Better Harvesting), each with its own toggle, its own assignable keybind and its own state flag.
- Existing configs migrate automatically: an old `Auto Replant` value enables both new options, and your old keybind moves to the trees option. A backup is written before the config is read.
- **Entity drops are now collected through a single funnel**, so mob loot (with Looting), worn armour and held items all land in your inventory instead of scattering on the ground. Item frames and vehicles (minecarts) are covered as well.
- Toggle state is broadcast on join and after every change, so the config screen always shows the live server value.

### Fixes and technical changes

- Auto Tool mining speed now accounts for Efficiency (`level² + 1`), matching vanilla.
- Reading Tool Wheel data no longer creates a save file for players who never used the module, and unknown or corrupt state versions are handled instead of failing.
- Shulker Restock: one slot map built per tick instead of per-link lookups, with precomputed item names in the sorter.
- Inventory Sorting: the container is marked dirty after a sort so the result is saved immediately.
- Ender chest overflow: session backups are reset on server stop.
- Network payloads: bounded string reads/writes on the refill packet.
- Every module config now defaults to disabled, applied consistently across the mod.

### Commands

- `/gestorage config` — opens the config screen.
- `/gestorage endersize [normal|large|extra_large]` — ender chest size (OP level 2 required to change it).
- `/gestorage tooldefault [set [<slot 1-9>]]` / `/gestorage tooldefault clear` — Auto Tool default tool.
- `/gestorage toolslot [<1-9>|clear]` — Auto Tool hotbar slot.

`tooldefault` and `toolslot` are personal per-player state and require no OP level.

### Before you update

- **Client and server must run the same version.** This release changes the Careful Break state payload, so a 2.0.1 client cannot join a 3.0.0 server (or the other way round).
- Extra Large ender chest (228 slots) is still experimental and prints a warning when enabled.
- Backup your world if you rely on the ender chest overflow data; it is versioned, but this is a pre-release.

### Requirements

- Minecraft 1.21.1
- Fabric Loader 0.16.10+
- Fabric API
- Java 21
- Mod Menu (optional, for the config screen entry)
