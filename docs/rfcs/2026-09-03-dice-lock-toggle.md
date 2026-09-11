# RFC: Dice Lock Toggle

- Status: Proposed
- Date: 2026-09-03

## Summary

Add a per-die lock toggle to the dice screen. Each die tile gains a small lock button in its top-end corner. A locked die is frozen: it keeps its current value and is skipped by both `Roll All` and the tap-to-roll gesture on its own tile. Lock state lives in `SavedStateHandle` for the current session only and is not persisted to `SharedPreferences`.

## Motivation

The dice screen currently rolls all-or-one. Many tabletop games need the "hold some, reroll the rest" flow: Yahtzee-style keeps, re-rolling a single bad d20 while the damage dice stand, or holding a result while deciding what to do next.

Today the only way to preserve a value is to not press `Roll All`, then reroll each unwanted die individually by tapping it. That is tedious and error prone, because a stray tap on a tile silently rerolls it with no way to recover the previous value.

## Goals

- Let the user mark any die as locked and unlocked from the tile itself.
- Exclude locked dice from `Roll All`.
- Exclude locked dice from the single-die tap-to-roll gesture.
- Make lock state legible at a glance across a grid of tiles.
- Preserve lock state across configuration changes.
- Keep the existing roll, add, and remove interactions unchanged.

## Non-Goals

- Persisting lock state, or die values, across process death.
- Protecting locked dice from deletion.
- A `lock all` action.
- Locked/unlocked breakdowns in the results summary or total.
- Any change to the persisted dice format in `DicePrefsDataSource`.

## Product Decisions

### Lock Semantics

A lock blocks every reroll of that die, not just `Roll All`.

The tile root is one large roll target (`DiceAdapter` binds a click listener to the card root), so a lock that only guarded the toolbar action would be defeated by any stray tap on the tile. A lock the user can lose by accident is not worth having.

Tapping a locked tile is not silent. The card keeps `?attr/selectableItemBackground` as its foreground, so it ripples on touch whether or not anything happens; a ripple with no consequence reads as a bug. A locked tile answers a tap with a short shake instead of the roll spin.

### Lock Does Not Block Delete

Long-press still removes a locked die immediately.

Lock means "this value is held", not "this tile is protected from everything". Overloading it into general write protection makes the icon mean two things at once, and long-press is deliberate enough that accidental deletion of a locked die is unlikely.

Note that long-press delete has no confirmation and no undo today. That is a pre-existing rough edge and is out of scope for this RFC.

### State Lifetime

Lock state is held in `SavedStateHandle` alongside faces, values, and ids. It survives rotation and view recreation. It does not survive process death.

Locking is a within-a-session action: hold these, reroll those. Persisting it to `SharedPreferences` would be actively wrong today, because only faces are persisted and values are not — a die restored as "locked" would display a face-count placeholder rather than the value the user locked, which is incoherent. Persisting values as well, purely to support locks across app restarts, is a larger change serving a case nobody has asked for.

### Roll All With Nothing Rollable

If every die is locked, `Roll All` suppresses its haptic burst and the summary-card bounce entirely.

The default path fires an eight-pulse vibration and animates the total before rolling. Playing that when no value can change tells the user something happened when nothing did.

The same suppression applies when the board is empty, which is a small behaviour change beyond locking: `Roll All` on a board with no dice previously buzzed and bounced the total too. Both cases are the same statement — nothing on this board can change — and should read the same way.

### Tile Treatment

The lock control is a small always-present `ImageButton` pinned to the tile's top-end corner, consuming its own clicks so it never triggers a roll. Top-start houses the die-type badge, the centre holds the value, and bottom-end holds the die glyph, so top-end is the one free corner.

The icon is recessive when unlocked (alpha ~0.5, matching the die glyph's existing treatment on the same tile) and full opacity when locked. It stays visible in both states: a control that only appears once used is undiscoverable.

Icon alone is not enough. On a two-column grid with up to ten tile colours, a 20dp corner glyph is easy to miss when scanning which dice are held, so a locked tile also takes a 2dp stroke.

That stroke uses the luminance-derived contrast colour `DiceAdapter` already computes for tile text, not `?attr/colorPrimary`. Tile backgrounds are assigned at runtime from `diceColors`, and a fixed theme colour will disappear against whichever entries sit near it. The same computed colour tints the lock icon.

### Unlock All

A second app-bar item appears beside `Roll All`, but only when at least one die is locked.

With several dice locked, clearing them means several precise taps on small corner targets. There is no matching `lock all`: its only outcome is a state where `Roll All` does nothing, which is a control whose success case is a dead button.

### Summary And Total

The results summary and total are unchanged. A locked die contributes its held value to the total and renders an ordinary chip.

The total answers "what is on the table", and a locked die is still on the table. The chips are already tight at `minWidth=56dp` and a 28dp height, with no room for a state glyph.

## Implementation Notes

- `DiceViewModel` gains a fourth parallel array, `locked`, kept in sync with faces, values, and ids across `addDie`, `removeDie`, `rollDie`, and `rollAllDice`. Removal at a lower index must shift lock flags with the rest of the state.
- `DieUiModel` gains `isLocked()`. `DiceAdapter`'s `DiffUtil.areContentsTheSame` must compare it, or lock icons will not refresh when nothing else about the die changed.
- `DiceAdapter.Listener` gains `onToggleLock(int position)`.
- `animateAllVisibleItems` skips locked holders. Its completion counter must still fire the end action when zero tiles animate, otherwise an all-locked `Roll All` would hang the callback.
- Two new vector drawables, `ic_lock_24` and `ic_lock_open_24`.
- Toggling a lock fires a single short vibration, matching the tap convention used elsewhere in the app. `DiceFragment` currently exposes only a `long[]` pattern overload and needs the scalar form.

## Testing

- `DiceViewModelTest`: locked dice keep their value through `rollAllDice` and `rollDie`; lock state survives a `SavedStateHandle` round trip; lock flags shift correctly when a die at a lower index is removed.
- `DiceFragmentTest`: the lock button toggles the die's state and the tile reflects it.
