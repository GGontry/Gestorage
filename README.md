# Gestorage

[![Mod Loader](https://img.shields.io/badge/Modloader-Fabric-blue)](https://fabricmc.net/)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-ea5b2b)](https://www.minecraft.net/)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)

**Gestorage** is a modular Fabric mod (Minecraft 1.21.1) that improves player storage and inventory management. It is an ecosystem of independent modules that can be enabled and disabled individually, so you only install the mechanics you actually want.

All modules ship **disabled by default** and are activated from the in-game config screen (`/gestorage config`), by keybind, or by editing `config/gestorage/`.

---

## Features

| Module | What it does |
|---|---|
| **Expanded Ender Chest** | Grows the ender chest from 27 to 54 (Large) or 228 (Extra Large) slots, selectable server-wide via a game rule, with per-player overflow persistence and keybind access. |
| **Shulker Restock** | Auto-refill for inventories: bind a hotbar/inventory slot to a shulker box and it is topped up automatically when it runs low. |
| **Stackable Shulkers** | Empty shulker boxes stack up to 64, in the player inventory and in every container context (hoppers, droppers, etc.). Server-authoritative, toggleable. |
| **Storage Overlay** | Informational panel shown next to any open inventory: inventory name, item name, stack counts, item counts and free-slot count. |
| **Inventory Sorting** | Sort button and keybind for containers, shulker boxes, ender chests and the player inventory, with configurable merge, name and direction options. |
| **Careful Break** | Server-authoritative mining suite: careful break to hand, careful drop to inventory, tree capitator, better harvesting (cactus/sugarcane/bamboo/kelp chains) and auto replant for trees and crops. |
| **Tool Wheel** | Radial tool selector bound to a key: a persistent 9-slot wheel that swaps real items between the wheel and your hand, with an optional server-side Auto Tool that instantly swaps the best tool while mining. |

---

## Module details

### Expanded Ender Chest

- Three storage tiers, chosen server-wide with the `gestorage:enderChestSize` game rule: Normal (27), Large (54) and Extra Large (228 slots).
- Overflow items beyond the vanilla 27 slots are stored per player and persist across sessions (NBT state, versioned with backups).
- A configurable keybind opens your ender chest from anywhere, without needing an item.
- Also accessible with `/gestorage endersize [normal|large|extra_large]` (requires OP level 2).

### Shulker Restock

- Mark two slots (source shulker and target) with the configured marking key to create a persistent per-world link.
- The target slot is refilled from the shulker up to default stack size whenever it falls below the configured threshold, with adaptive back-off.
- Links are saved per world in `config/gestorage/links.json` and survive restarts; they are shared across players.

### Stackable Shulkers

- Empty shulker boxes behave like any other item (stack to 64) while non-empty ones keep their usual stack limit of 1.
- Server-authoritative: the toggle in the config screen writes the server config and is disabled while connected to a remote server.

### Storage Overlay

- Renders a panel adjacent to the open inventory showing inventory name, hovered item name, icon, stack and item counts, and remaining free slots.
- Every element can be shown or hidden individually, each with its own assignable toggle keybind.

### Inventory Sorting

- One-click sort for player inventory (main inventory only; hotbar and armor untouched), chests, ender chests and shulker boxes.
- Options: merge stacks, sort by name, sort order (ascending/descending), and per-inventory-type blocking.
- The server validates the sorted range, so this works consistently regardless of who is hosting.

### Careful Break

Server-side flags, synced to all clients and toggleable with OP level 2 (or as the singleplayer host):

- **Careful Break** — mined blocks go straight into your inventory (sneak to activate).
- **Careful Drop / Always Careful** — block and entity drops (items, loot tables, worn equipment, minecart contents) are collected into your inventory instead of being scattered.
- **Tree Capitator** — fell whole trees by breaking the base log (with tree/leaf caps).
- **Better Harvesting** — break entire cactus, sugarcane, bamboo and kelp chains in one swing.
- **Auto Replant** — automatically replant trees and crops after harvesting.

### Tool Wheel

- A radial HUD wheel (9 slots) opened with a keybind; click to swap a real item between the wheel and your main hand. The wheel contents are saved per player and follow you across sessions and dimensions.
- **Auto Tool** (server-authoritative per player): on `START_DESTROY_BLOCK` the best tool in the wheel is instantly swapped into your hand, re-evaluated on every block you start mining, and reverted to your original tool one second after mining stops.
- Per-player storage is persisted in the overworld (`gestorage_tool_wheel_<uuid>`) and synced to the owner on join and after every change.

---

## Requirements

- Minecraft **1.21.1**
- Fabric Loader **>= 0.16.10**
- Fabric API
- owo-lib is bundled with the mod (warns you if it is missing)

---

## Installation

1. Install **Fabric Loader** for Minecraft 1.21.1.
2. Install the matching **Fabric API** version.
3. Download the latest **Gestorage** release from the [Releases page](../../releases).
4. Drop the `.jar` file into your `.minecraft/mods` directory.
5. Launch the game. Modules are off by default — open the config screen with `/gestorage config` (or Mod Menu) to enable them.

---

## Configuration

All configuration lives in `config/gestorage/`:

| File | Purpose |
|---|---|
| `ender_chest.json` | Ender chest access keybind and module enabled flag |
| `shulker_refill.json` | Refill threshold, marking key and module enabled flag |
| `shulker_stack.json` | Server-authoritative Stackable Shulkers toggle |
| `storage_overlay.json` | Overlay element visibility and toggle keybinds |
| `inventory_sorting.json` | Sorting options and toggle keybinds |
| `careful_break.json` | Server-authoritative Careful Break flags (written by the server only) |
| `careful_break_keybinds.json` | Client-side Careful Break keybinds |
| `tool_wheel.json` | Tool Wheel keys and module enabled flag |
| `links.json` | Shulker link definitions |

### Commands and game rules

- `/gestorage config` — opens the in-game config screen.
- `/gestorage endersize [normal|large|extra_large]` — sets the `gestorage:enderChestSize` game rule (OP level 2).
- Game rule `gestorage:enderChestSize` — accepts `0` (normal), `1` (large) or `2` (extra large); the game rule is the single authority for the ender chest size on the server.

---

## Feedback & Contributions

Gestorage is actively developed. If you run into an issue, have a feature idea, or want to contribute to the code, open an **Issue** or submit a **Pull Request**.

---

## License

Released under the [MIT License](LICENSE).