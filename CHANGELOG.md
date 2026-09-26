# Changelog

All notable changes to Gestorage are documented in this file.

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
