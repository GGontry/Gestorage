# 5.0.0

## Added

- **Slot Lock** module: hold a configurable key and left-click a slot to lock it.
  - Lockable slots: the whole player inventory (main inventory, hotbar, armor and offhand) and the ender chest while it is open.
  - Holding the key outlines lockable slots in yellow; locked slots keep a red outline.
  - Options: sorting skips locked slots, prevent drop, and prevent use (right-click use and block placement; attacks and opening containers/doors are unaffected).
  - Locks are stored per player and per world, are server-authoritative, and do not stop normal item movement.
- `gestorage:hopperStackableShulkers` game rule (default `false`): when enabled, hoppers and hopper minecarts stack empty shulkers while `gestorage:stackableShulkers` is also enabled. Hopper stacking is independent from player stacking and stays off unless explicitly enabled.
- `/gestorage shulkerstack players [on|off]` and `/gestorage shulkerstack hoppers [on|off]`; the no-argument form reports both states. Turning hoppers on also enables player stacking if it was off, because the main rule is required.

## Changed

- `/gestorage shulkerstack` was simplified to the `players` and `hoppers` subcommands; the old bare `on`/`off` and `true`/`false` aliases were removed.
- With hoppers off, a hopper only moves empty shulkers into empty slots, so a destination chest with one shulker in every slot is treated as full. Enabling hoppers lets them merge into existing stacks instead.

## Fixed

- A hopper chain no longer stops after the destination chest is emptied while hoppers are off. Empty shulkers are max 1 only during the hopper transfer, which made Lithium's cached full-slot count treat each single as full; removing them with the player limit of 64 never decremented that cache, including when shift-clicking or manually taking stacks changed the count in place. The compatibility mixin now applies the same effective limit whenever Lithium updates its cache, so the hopper resumes as soon as a slot is cleared.

## Compatibility

- Client and server must both run 5.0.0: Slot Lock adds new packets and the hopper behaviour adds a new game rule.
- Lithium is optional and supported. No Lithium classes are required at runtime; the compatibility mixin is skipped when Lithium is not installed.
