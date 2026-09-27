# Changelog

All notable changes to Gestorage are documented in this file.

## 3.0.0-beta.1

A modular storage and inventory overhaul for Fabric 1.21.1. This is a **pre-release**: every module still ships **disabled by default**, so you only activate the mechanics you actually want.

### New module: Tool Wheel

A radial tool selector built on real item swaps — no virtual hand, no duplicated items.

- **Persistent 9-slot wheel**, stored per player in the world. Open it as a vanilla 9x1 chest, or press a key to bring up a radial HUD wheel (9 sectors) and click the sector you want.
- Every selection is a **real swap** between the wheel slot and your hand, so nothing is ever duplicated or lost.
- While the wheel is open, attack/use/pick input is blocked, so a click can never fall through and accidentally mine or place a block.
- **Auto Tool** (server-side, per player): on every block-break start it compares your hand against every wheel tool and instantly swaps in a strictly faster one — re-evaluated on every mine start, so mining stone then dirt does not get stuck on the pickaxe. One second after you stop mining, it puts your tool back. Guards make sure a manual change is never clobbered and a mid-vein revert can never fire. The swap is also re-checked a few times per second while a block is really being broken, so it no longer misses when the game reports the previous block position on the first click.

#### Auto Tool: the tool it returns to and the slot it uses

- **Session tool** — Auto Tool always returns to the tool you were holding when you enabled it, so the hand ends up exactly where you started. The tool is looked for in the wheel first and then across your whole inventory, matching by exact stack, then by item and enchantments, then by item alone — so a tool you used, repaired or re-enchanted while it was parked still comes back instead of being destroyed. Reverting never destroys an item and never leaves you empty handed: if the tool really is gone, the auto tool only goes back to the wheel when that slot is free, otherwise your hand is left untouched. Turning Auto Tool off while a tool is still swapped in returns it immediately instead of waiting for the timer. The old persistent `Default Tool` setting and `/gestorage tooldefault` are gone.
- **Reconnect no longer disables Auto Tool** — your wheel, the Auto Tool flag and the pinned slot are loaded from your save as soon as you join again, so Auto Tool works immediately without having to open the wheel first. Players with no saved wheel still get no file created.
- **Default Slot** — pin the hotbar slot Auto Tool swaps into. When Auto Tool fires, your hand moves to that slot and the swap always happens there; the client is notified immediately, so what you see is what you click. It is command-only: `/gestorage toolslot <1-9>` pins it, `/gestorage toolslot clear` follows the selected slot again.
- **Enchantment preference** — choose which enchantment Auto Tool prefers with a new `Prefer: None / Fortune / Silk Touch` option in the Tool Wheel settings (with its own assignable keybind). A wheel tool carrying the preferred enchantment wins as long as it can mine the block at all, so a Silk Touch pickaxe is used even against a faster Fortune one; when no preferred tool can mine the block Auto Tool falls back to the fastest tool. The preference is stored per player, synced to your client and applies to the next block you start breaking.

The slot is server-authoritative, stored per player in the wheel state file and synced to your client. Existing wheel saves load untouched: the new fields are written additively, so no wheel data is wiped.

### Careful Break

- **Auto Replant is now two separate options**: `Auto Replant Trees` (used by Tree Capitator) and `Auto Replant Crops` (used by Better Harvesting), each with its own toggle, its own assignable keybind and its own state flag.
- Existing configs migrate automatically: an old `Auto Replant` value enables both new options, and your old keybind moves to the trees option. A backup is written before the config is read.
- **Entity drops are now collected through a single funnel**, so mob loot (with Looting), worn armour and held items all land in your inventory instead of scattering on the ground. Item frames and vehicles (minecarts) are covered as well.
- Toggle state is broadcast on join and after every change, so the config screen always shows the live server value.

### Fixes and technical changes

- Auto Tool mining speed now accounts for Efficiency (`level² + 1`), matching vanilla — and only for tools that actually break the block, so an Efficiency helmet, a block or a stick can no longer out-score a pickaxe.
- Two Auto Tools in a row no longer end up in each other's wheel slot: a swap that is still pending is rolled back before the next decision, so mining stone → dirt → stone keeps the pickaxe and the shovel where they belong.
- The one second revert no longer fires in the middle of a vein when the game finishes a block on its own after the button was released.
- Switching the whole Tool Wheel module off now switches Auto Tool off on the server too (previously the server kept swapping tools for a disabled module). The module state is reported on join and on every toggle.
- Enabling Auto Tool with an empty tool slot no longer claims the tool is missing.
- Reading Tool Wheel data no longer creates a save file for players who never used the module, and unknown or corrupt state versions are handled instead of failing.
- Shulker Restock: one slot map built per tick instead of per-link lookups, with precomputed item names in the sorter.
- Inventory Sorting: the container is marked dirty after a sort so the result is saved immediately.
- Ender chest overflow: session backups are reset on server stop.
- Network payloads: bounded string reads/writes on the refill packet.
- Every module config now defaults to disabled, applied consistently across the mod.

### Commands

- `/gestorage config` — opens the config screen.
- `/gestorage endersize [normal|large|extra_large]` — ender chest size (OP level 2 required to change it).
- `/gestorage toolslot [<1-9>|clear]` — Auto Tool hotbar slot.

`toolslot` is personal per-player state and requires no OP level.

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
