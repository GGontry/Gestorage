# Gestorage

[![Mod Loader](https://img.shields.io/badge/Modloader-Fabric-blue)](https://fabricmc.net/)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-ea5b2b)](https://www.minecraft.net/)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)

**Gestorage** is a modular Fabric mod (Minecraft 1.21.11) that improves player storage and inventory management. It is an ecosystem of independent modules that can be enabled and disabled individually, so you only install the mechanics you actually want.

All modules ship **disabled by default** and are activated from the in-game config screen (`/gestorage config`), by keybind, or by editing `config/gestorage/`.

---

## Features

| Module | What it does |
|---|---|
| **Expanded Ender Chest** | Grows the ender chest from 27 to 54 (Large) or 228 (Extra Large) slots, selectable server-wide via a game rule, with per-player overflow persistence and keybind access. |
| **Shulker Restock** | Auto-refill for inventories: bind a hotbar/inventory slot to a shulker box and it is topped up automatically when it runs low. |
| **Stackable Shulkers** | Empty shulker boxes stack up to 64 for players; hopper stacking is a separate server-side option (off by default). Controlled with `/gestorage shulkerstack`. |
| **Storage Overlay** | Informational panel shown next to any open inventory: inventory name, item name, stack counts, item counts and free-slot count. |
| **Inventory Sorting** | Sort button and keybind for containers, shulker boxes, ender chests and the player inventory, with configurable merge, name and direction options. |
| **Slot Lock** | Hold a key to mark inventory or ender chest slots as locked (red border). Locked slots can be skipped by sorting and protected from drop/use. |
| **Careful Break** | Server-authoritative mining suite: careful break to hand, careful drop to inventory, tree capitator, better harvesting (cactus/sugarcane/bamboo/kelp chains) and auto replant for trees and crops. |
| **Tool Wheel** | Radial tool selector bound to a key: a persistent 9-slot wheel that swaps real items between the wheel and your hand, with an optional server-side Auto Tool that instantly swaps the best tool while mining. |

---

## Module details

### Expanded Ender Chest

- Three storage tiers, chosen server-wide with the `gestorage:ender_chest_size` game rule: Normal (27), Large (54) and Extra Large (228 slots).
- Overflow items beyond the vanilla 27 slots are stored per player and persist across sessions (NBT state, versioned with backups).
- A configurable keybind opens your ender chest from anywhere, without needing an item.
- Also accessible with `/gestorage endersize [normal|large|extra_large]` (requires OP level 2).

### Shulker Restock

- Mark two slots (source shulker and target) with the configured marking key to create a persistent per-world link.
- The target slot is refilled from the shulker up to default stack size whenever it falls below the configured threshold, with adaptive back-off.
- **Reverse Order** — choose whether items are pulled from the first shulker slot to the last (default) or from the last to the first. Available in the module settings and as its own assignable keybind.
- Links are saved per world in `config/gestorage/links.json` and survive restarts; they are shared across players.

### Stackable Shulkers

- Empty shulker boxes behave like any other item (stack to 64) for players while non-empty ones keep their usual stack limit of 1. Shulkers carrying a loot table (the ones found in structure chests) count as non-empty and never stack, so placing one cannot duplicate its loot.
- Modded shulkers that already define a different `MAX_STACK_SIZE` keep that limit instead of being forced to 64.
- Hoppers keep the vanilla limit of 1 by default. Enable hopper stacking with `/gestorage shulkerstack hoppers on`; it only works while player stacking is also on, so turning hoppers on also enables the main rule if needed. Use `/gestorage shulkerstack hoppers off` to return hoppers to the vanilla limit.
- While hoppers are off, a hopper only moves an empty shulker into an **empty** slot. A destination with one shulker in every slot is treated as full; once slots are emptied (shift-clicked or taken by hand), the hopper resumes moving items into them. The rule is enforced everywhere: normal transfers, item-entity pickup/collision, hopper minecarts and shift-clicking into a hopper screen all move at most one shulker at a time. Picking up a dropped stack leaves the rest of the stack in the world instead of deleting it.
- This also works with Lithium: Gestorage keeps Lithium's cached full-slot count in sync when hoppers are off, and invalidates that cache when either rule changes, so an emptied destination does not leave the hopper stuck (and toggling the rule does not leave a stale count behind).
- Turning player stacking off splits any existing over-max stacks in the player inventory, cursor and ender chest into singles; whatever does not fit is dropped at the player's feet. The same cleanup runs when joining a world whose rule is already off.
- Server-authoritative and per world, like the ender chest size: turn player stacking on with `/gestorage shulkerstack players on`, off with `/gestorage shulkerstack players off`, and check both states with `/gestorage shulkerstack` (changing them requires OP level 2). It is the `gestorage:stackable_shulkers` game rule, so plain `/gamerule` works too, and every client is told about a change right away (including the hopper rule, so GUI limits match the server).
- There is no config entry for it: it used to be toggled from the config screen, and if you had it enabled your old `shulker_stack.json` is imported into the game rule the first time you start your world.
- Upgrading a 1.21.1 world keeps its settings: the values saved under the old `gestorage:stackableShulkers` / `gestorage:hopperStackableShulkers` names are imported once into the new snake_case rules. Vanilla logs the old keys it has to drop while opening the world; that message is expected.

### Storage Overlay

- Renders a panel adjacent to the open inventory showing inventory name, hovered item name, icon, stack and item counts, and remaining free slots.
- Every element can be shown or hidden individually, each with its own assignable toggle keybind.

### Inventory Sorting

- One-click sort for player inventory (main inventory only; hotbar and armor untouched), chests, ender chests and shulker boxes.
- Options: merge stacks, sort by name, sort order (ascending/descending), and per-inventory-type blocking.
- The server validates the sorted range, so this works consistently regardless of who is hosting.

### Slot Lock

- Enable the module, bind a **lock key**, then hold that key while left-clicking a slot to toggle its lock. Lockable slots are the whole player inventory (main inventory, hotbar, armor and offhand) and the ender chest slots while an ender chest is open.
- Holding the key outlines every lockable slot in **yellow**; locked slots always show a **red** outline. Locking a slot does not stop you from moving the item yourself: it only feeds the sorting, drop and use options below.
- **Sorting Skips Locks** — the sort button ignores locked slots and packs the remaining items around them.
- **Prevent Drop** — blocks Q drops and drag-out drops from locked slots. **Prevent Use** — blocks right-click use and block placement with an item in a locked hand. It does not block attacks, and opening chests or doors still works.
- Locks are stored per player and per world, and are server-authoritative: the client only mirrors the lock set to draw the borders.

### Careful Break

Server-side flags, synced to all clients and toggleable with OP level 2 (or as the singleplayer host):

- **Careful Break** — mined blocks go straight into your inventory (sneak to activate).
- **Careful Drop / Always Careful** — block and entity drops (items, loot tables, worn equipment, minecart contents) are collected into your inventory instead of being scattered.
- **Tree Capitator** — fell whole trees by breaking the base log (with tree/leaf caps).
- **Better Harvesting** — break entire cactus, sugarcane, bamboo and kelp chains in one swing.
- **Auto Replant** — automatically replant trees and crops after harvesting.

### Tool Wheel

- A radial HUD wheel (9 slots) opened with a keybind; click to swap a real item between the wheel and your main hand. The wheel contents are saved per player and follow you across sessions and dimensions.
- **Auto Tool** (server-authoritative per player): on `START_DESTROY_BLOCK` the best tool in the wheel is instantly swapped into your hand, re-evaluated on every block you start mining, and one second after mining stops the swap is undone: whatever the tool slot held goes back into it and the tool goes back into the wheel. Changing hotbar slots while Auto Tool works is never fought over, and turning Auto Tool off mid-vein returns the item immediately.
- An **enchantment preference** (`None` / `Fortune` / `Silk Touch`) makes Auto Tool pick a wheel tool carrying that enchantment whenever it can break the block, even against a faster tool without it; when no preferred tool can break the block it falls back to the fastest one.
- `/gestorage toolslot <1-9|clear>` pins the hotbar slot Auto Tool swaps into.
- Per-player storage is persisted in the overworld (`gestorage_tool_wheel_<uuid>`) and synced to the owner on join and after every change.

---

## Requirements

- Minecraft **1.21.11**
- Fabric Loader **>= 0.20.1**
- Fabric API **0.141.6+1.21.11** (or newer for the same Minecraft version)
- owo-lib **0.13.0+1.21.11** (required — the mod bundles a sentinel that warns you when it is missing)
- Mod Menu **17.0.1** (optional, only adds the config-screen button)

---

## Installation

1. Install **Fabric Loader** for Minecraft 1.21.11.
2. Install the matching **Fabric API** version.
3. Download the latest **Gestorage** release from the [Releases page](../../releases).
4. Drop the `.jar` file into your `.minecraft/mods` directory.
5. Launch the game. Modules are off by default — open the config screen with `/gestorage config` (or Mod Menu) to enable them.

---

## Configuration

All configuration lives in `config/gestorage/`:

| File | Purpose |
|---|---|
| `ender_chest.json5` | Ender chest access keybind and module enabled flag |
| `shulker_refill.json5` | Refill threshold, refill order, marking key and module enabled flag |
| `storage_overlay.json5` | Overlay element visibility and toggle keybinds |
| `inventory_sorting.json5` | Sorting options and toggle keybinds |
| `slot_lock.json5` | Slot Lock key, behaviour switches and module enabled flag |
| `careful_break.json` | Server-authoritative Careful Break flags (written by the server only) |
| `careful_break_keybinds.json` | Client-side Careful Break keybinds |
| `tool_wheel.json5` | Tool Wheel keys and module enabled flag |
| `links.json` | Shulker link definitions |

The config screen draws its window, buttons and checkboxes with real transparency, so it blends with the world behind it without an external translucency mod.

### Commands and game rules

- `/gestorage config` — opens the in-game config screen.
- `/gestorage endersize [normal|large|extra_large]` — sets the `gestorage:ender_chest_size` game rule (OP level 2).
- `/gestorage shulkerstack` — reports the player and hopper stacking states.
- `/gestorage shulkerstack players [on|off]` — turns player/global empty-shulker stacking on or off (OP level 2).
- `/gestorage shulkerstack hoppers [on|off]` — allows hoppers to stack empty shulkers (OP level 2); off by default. Turning them on also enables player stacking if needed; turning them off keeps hoppers at the vanilla limit of 1.
- `/gestorage toolslot [<1-9>|clear]` — pins (or unpins) the hotbar slot Auto Tool swaps into. Personal, no OP level needed.
- Game rule `gestorage:ender_chest_size` — accepts `0` (normal), `1` (large) or `2` (extra large); the game rule is the single authority for the ender chest size on the server.
- Game rule `gestorage:stackable_shulkers` — `true`/`false`, the single authority for Stackable Shulkers on the server.
- Game rule `gestorage:hopper_stackable_shulkers` — `true`/`false`, default `false`; lets hoppers treat empty shulkers as stackable when `gestorage:stackable_shulkers` is also on.

Game rules use snake_case because 1.21.11 requires rule ids to be valid identifiers (no uppercase letters), the same way vanilla renamed all of its own rules. Worlds from the 1.21.1 line keep their values: the old camelCase keys are imported automatically on first load.

---

## Feedback & Contributions

Gestorage is actively developed. If you run into an issue, have a feature idea, or want to contribute to the code, open an **Issue** or submit a **Pull Request**.

---

## License

Released under the [MIT License](LICENSE).
