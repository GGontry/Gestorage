# 6.0.0

## Minecraft 1.21.11

- **Ported to Minecraft 1.21.11** (Fabric Loader >= 0.20.1, Fabric API 0.141.6+, owo-lib 0.13.0, Mod Menu 17.0.1). This release also contains everything that was in development for 5.0.0.
- **Game rules are now snake_case.** `gestorage:enderChestSize`, `gestorage:stackableShulkers` and `gestorage:hopperStackableShulkers` became `gestorage:ender_chest_size`, `gestorage:stackable_shulkers` and `gestorage:hopper_stackable_shulkers`. 1.21.11 stores game rules in a registry keyed by identifiers, and identifier paths cannot contain uppercase letters (vanilla renamed every one of its own rules the same way), so the old names can no longer be registered.
- **Existing worlds keep their settings.** The values saved under the old names in a 1.21.1 world are imported into the new rules the first time the world is opened. The import runs once per world, so a later `/gamerule` change is never overwritten. Vanilla logs the old keys it has to drop while opening the world once; that is expected and harmless.

## Added

- **Slot Lock** module: hold a configurable key and left-click a slot to lock it.
  - Lockable slots: the whole player inventory (main inventory, hotbar, armor and offhand) and the ender chest while it is open.
  - Holding the key outlines lockable slots in yellow; locked slots keep a red outline.
  - Options: sorting skips locked slots, prevent drop, and prevent use (right-click use and block placement; attacks and opening containers/doors are unaffected).
  - Locks are stored per player and per world, are server-authoritative, and do not stop normal item movement.
- **Shulker Restock: reversible refill order** — you can now choose whether items are pulled out of the shulker box from the first slot to the last (the default) or from the last slot to the first. Toggle it with the `Reverse Order` option in the module settings or its own assignable keybind; the server honours your choice in the `refill_request` packet.
- **Config screen: real translucency, no extra mod required.** The settings menu's window, buttons and checkboxes have always shipped with semi-transparent artwork, but the vanilla draw path never enabled alpha blending on them, so an external mod (Translucency Fix) was needed to see through them. Gestorage now renders them blended, so the menu blends with the world behind it out of the box.
- `gestorage:hopper_stackable_shulkers` game rule (default `false`): when enabled, hoppers and hopper minecarts stack empty shulkers while `gestorage:stackable_shulkers` is also enabled. Hopper stacking is independent from player stacking and stays off unless explicitly enabled.
- `/gestorage shulkerstack players [on|off]` and `/gestorage shulkerstack hoppers [on|off]`; the no-argument form reports both states. Turning hoppers on also enables player stacking if it was off, because the main rule is required.

## Changed

- `/gestorage shulkerstack` was simplified to the `players` and `hoppers` subcommands; the old bare `on`/`off` and `true`/`false` aliases were removed.
- With hoppers off, a hopper only moves empty shulkers into empty slots, so a destination chest with one shulker in every slot is treated as full. Enabling hoppers lets them merge into existing stacks instead.
- The shulker state packet now mirrors both the player and hopper rules, so client-side GUI limits match the server instead of predicting a stack the server will reject.

## Fixed

- A hopper picking up a dropped stack of empty shulkers while hopper stacking was off could delete most of the stack: the whole entity stack was copied, the destination capped it to 1, and the entity was discarded. The pickup now moves at most the effective limit and leaves the remainder in the item entity.
- Hopper-rule bypasses through item-entity collision and hopper minecart ticks were closed; both now run inside the hopper context. Shift-clicking or manually inserting into a hopper screen is also capped to one shulker per slot while hopper stacking is off.
- Empty shulkers carrying a loot table (`CONTAINER_LOOT`) were treated as empty and could stack, which risked duplicating structure loot when placed. They are now always treated as non-empty.
- The hopper transfer context is now reentrant and always cleared in a `finally` block, so nested hopper calls or an exception can no longer leave the server thread marked as a hopper.
- Turning player stacking off now splits existing over-max empty-shulker stacks in the player inventory, cursor and ender chest into singles, dropping the overflow at the player's feet. The same normalization runs on join, and the extended ender chest is normalized when it is opened.
- Modded shulkers with a custom `MAX_STACK_SIZE` now keep their own limit instead of being forced to 64.
- Lithium's cached full-slot count is now invalidated when either shulker rule changes, so toggling hopper stacking no longer leaves hoppers stuck (or over-eager) until the inventory changes.
- A legacy over-max destination stack can no longer corrupt a hopper transfer: vanilla's merge calculation subtracts a negative amount when the destination already exceeds the limit, so slots at or over the limit are skipped.
- A hopper chain no longer stops after the destination chest is emptied while hoppers are off. Empty shulkers are max 1 only during the hopper transfer, which made Lithium's cached full-slot count treat each single as full; removing them with the player limit of 64 never decremented that cache, including when shift-clicking or manually taking stacks changed the count in place. The compatibility mixin now applies the same effective limit whenever Lithium updates its cache, so the hopper resumes as soon as a slot is cleared.
- **Shulker Restock: the hotbar count no longer looks like it is being consumed when refilling blocks.** Placing a block is predicted on the client (the stack is decremented locally), so when a refill restored the count to the very number the server had already sent, the correction was never pushed and the client kept counting down while the server stayed full. The server now forces the corrected stack back to the client after a refill, so blocks behave like rockets and every other item.
- **Tool Wheel: Auto Tool now always swaps into the pinned Default Slot.** Previously it only moved your hand when it had to change the tool, so when the tool already in your hand was the right one for the block it left your selection untouched and the Default Slot was ignored. Auto Tool now selects this slot on every mine start, whether or not an item swap is needed.
- Extended ender chest overflow is now saved at most once every two seconds per player instead of on every refill request, which could force repeated world saves. Closing the ender chest still saves immediately, and refill still keeps target stacks topped to 64.
- The ender chest overflow NBT is backed up once per session before it is first read, so a first write can no longer overwrite the only recoverable copy.
- Keybind capture in the config screen now reads the live modifier state. Modifiers held before capture started are included, and a modifier released during capture is no longer stuck in the binding.
- Careful Break now handles nested deaths correctly: an inner death without a player killer no longer clears the outer death context, so later drops from the outer kill are still collected.
- Legacy (version 0) shulker link files with a malformed entry now skip only that entry instead of discarding every link.
- The client-side ender chest size mirror now updates on join and whenever `gestorage:ender_chest_size` changes (including a plain `/gamerule`), not only when an ender chest is opened.
- Tool Wheel sync now also reads state that is still pending in memory, so a change made before the next world save is no longer sent to the client as empty.

## Compatibility

- Client and server must both run 6.0.0: Slot Lock adds new packets, the shulker state packet carries both the player and hopper rules, the refill packet carries the refill-order field, and the game rule names changed.
- Lithium is optional and supported. No Lithium classes are required at runtime; the compatibility mixin is skipped when Lithium is not installed.
