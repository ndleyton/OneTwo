# RFC: MTG Commander Damage

- Status: Proposed
- Date: 2026-05-18
- Related: `docs/rfcs/2026-05-18-mtg-life-counter-fragment.md`

## Summary

Add commander-damage tracking to the existing MTG life counter. Each `LifePlayerUiModel` tile should render a compact commander-damage strip at the bottom, matching the placement in the provided reference. The strip acts as a summary for damage dealt to that player by each seat's commander, and tapping it opens a modal editor where the values can be adjusted. Commander-damage state persists alongside life totals in the existing `MtgLifeViewModel` state.

## Motivation

The life counter now supports shared-screen MTG play, but Commander games still require players to track a second loss condition outside the app. Keeping commander damage inside the same tile avoids paper tracking, while using a modal editor avoids overloading the base board with dense micro-controls.

This is a natural follow-up to the original life-counter RFC, which explicitly left commander damage out of v1.

## Goals

- Add per-player commander-damage tracking to the MTG life screen.
- Render commander damage inside each player tile, anchored at the bottom of the tile.
- Keep the life total visually dominant and the commander-damage UI clearly secondary.
- Preserve commander-damage state across view recreation and configuration changes.
- Support the same 1 to 6 player range as the existing MTG life feature.
- Make the 21-damage Commander loss threshold easy to notice.

## Non-Goals

- Poison, energy, experience, or other side counters.
- Persistent saved games outside the current in-memory plus `SavedStateHandle` session model.
- Automatic winner detection, player elimination flows, or lockout behavior after 21 damage.
- Partner/background/multiple-commander tracking per player in v1.
- Renaming players or adding deck metadata in this RFC.

## Product Decisions

### Scope

This is an extension of the existing MTG life fragment, not a separate top-level feature.

### Visibility

Add a `Commander Damage` toggle to the setup state, defaulting to `on`. When enabled, the play board shows a commander-damage summary strip in every tile. When disabled, the board keeps the current life-only layout.

Why:

- the MTG life feature is useful for both Commander and non-Commander games
- always-on commander UI would add noise for formats that do not use it
- a setup toggle keeps the play experience explicit instead of inferring format from starting life

### Tile Placement

Commander damage belongs inside `LifePlayerUiModel` as bottom-of-tile summary content, below the large life total and below the primary `+` and `-` life interactions. The tile should open a modal editor for changes rather than making the base grid itself directly editable.

## UX Proposal

### Setup State

Extend the current setup form with:

- label: `Commander Damage`
- control: `MaterialSwitch`
- default: `on`

The rest of the setup flow stays unchanged.

### Play State

Each player tile continues to show:

- large centered life total
- tap targets for `-1` and `+1` life

When commander damage is enabled, the tile also shows a compact damage grid pinned to the bottom of the tile.

Each commander-damage cell represents:

- defender: the tile's player
- source: one seat in the current game
- value: total commander damage dealt by that source to that defender

To keep the layout stable, render one cell per seat, including self:

- self cell: disabled, labeled `me`
- opponent cells: non-editable summary values, starting at `0`

This mirrors the provided reference while avoiding gaps in the grid.

### Interaction Model

Life interactions stay exactly as they are today.

Commander-damage interactions should be modal:

- tapping the commander-damage strip opens a dialog or modal bottom sheet for that defender
- the base grid on the tile is a summary only and does not directly modify values
- the modal shows one editable row per source seat, including a disabled `me` row
- each opponent row has explicit increment and decrement controls
- decrement floors at `0`
- reaching `21` does not lock the value; users can decrement back to `20` or lower to correct mistakes
- dismissing the modal returns to the main board with updated summary values

Why this interaction:

- it preserves the clean board-level layout
- it reduces accidental edits from stray taps on a crowded shared screen
- it gives the editor enough room for explicit `+` and `-` controls without shrinking the life UI

### Modal Editor

Recommended editor behavior:

- title: `Commander Damage`
- subtitle or supporting text identifies the defending player, for example `Damage dealt to Player 2`
- one row per source seat in stable seat order
- each row shows source identity, current value, decrement, and increment
- the self row stays visible but disabled so seat mapping remains consistent
- values update immediately in the shared ViewModel as the user edits
- modal dismissal uses normal back, outside-tap, or close affordances; no extra save step is required in v1

### Visual Treatment

- The commander-damage strip should be clearly smaller than the life controls.
- Use a rounded container or small card treatment similar to the reference.
- Opponent cells should use the source player's seat color family so the source mapping is consistent across the board.
- The self cell should use a neutral outlined or muted treatment rather than a player color.
- At `21` or more, the relevant commander-damage cell should switch to an error or warning treatment.
- The base-strip summary should look tappable as a whole, but the individual cells should not look like standalone buttons.

## Layout Strategy

Update `app/src/main/res/layout/life_player_cell.xml` instead of introducing a second player-cell layout.

Recommended structure:

- keep the current centered life total and left/right life buttons
- add a bottom-aligned commander-damage container inside `inner_player_layout`
- constrain the life-total area above that container so the new strip has reserved space

Grid recommendation by player count:

| Players | Commander grid |
| --- | --- |
| 1 | hidden |
| 2 | 1 x 2 |
| 3 | 2 x 2 |
| 4 | 2 x 2 |
| 5 | 2 x 3 |
| 6 | 2 x 3 |

Implementation note:

- Use a lightweight child container that can be rebuilt from binding code, such as `GridLayout` or a small included layout set.
- Keep the grid within the already rotated `inner_player_layout` so the strip follows the tile orientation automatically.

## Architecture

### State Model

Extend `MtgLifeViewModel` saved state with:

- `KEY_COMMANDER_DAMAGE_ENABLED`
- `KEY_COMMANDER_DAMAGE_MATRIX`

Recommended persisted structure:

- `boolean commanderDamageEnabled`
- `List<ArrayList<Integer>> commanderDamageMatrix`

Matrix semantics:

- outer index: defender seat
- inner index: source seat
- `matrix[defender][source]` is commander damage dealt by `source` to `defender`
- self entries always remain `0`

Initialize the matrix when a new game starts:

- size `N x N`
- all values `0`

Reset behavior:

- `New Game` returns to setup and preserves the last-used commander-damage toggle just as the screen already preserves player count and starting life

### UI Models

Extend `LifePlayerUiModel` rather than pushing commander-damage logic into the fragment.

Recommended shape:

- `LifePlayerUiModel`
  - existing fields
  - `boolean commanderDamageVisible`
  - `List<CommanderDamageUiModel> commanderDamages`

- `CommanderDamageUiModel`
  - `sourceSeatIndex`
  - `amount`
  - `boolean self`
  - `boolean lethal`
  - `int backgroundColorRes`
  - `int foregroundColorRes`

This keeps the fragment focused on rendering and click wiring, matching the existing architecture where seat colors and rotations are already derived in `MtgLifeViewModel`.

Recommended additional UI state:

- `CommanderDamageEditorUiModel`
  - `defenderSeatIndex`
  - `defenderLabel`
  - `List<CommanderDamageUiModel> commanderDamages`

This can be exposed either as part of the main screen state or as transient dialog state, depending on whether the implementation uses a `DialogFragment` or an in-fragment modal surface.

### Fragment Responsibilities

`MtgLifeFragment` should:

- keep binding the current board template per player count
- bind the existing life buttons and life total exactly as today
- show or hide the commander-damage container per player
- inflate or bind commander-damage cells from `LifePlayerUiModel`
- open the commander-damage editor when a player tile summary strip is tapped
- route modal increment and decrement events to `MtgLifeViewModel`
- restore the modal cleanly across configuration changes if it is open

### ViewModel Responsibilities

`MtgLifeViewModel` should:

- validate the new setup toggle input
- create the commander-damage matrix on game start when enabled
- expose commander-damage UI state inside each `LifePlayerUiModel`
- expose the currently edited defender, if the implementation stores editor state in the ViewModel
- increment and decrement commander damage for a specific defender/source pair
- clamp commander damage at `0`
- derive `lethal` once a source reaches `21` or more

## Accessibility and Usability

- The summary strip needs a content description in the form `Commander damage for player 1. Double tap to edit.`
- Each summary value should still expose source-aware spoken text such as `Commander damage from player 3 to player 1: 7`.
- The self cell should expose a description such as `Your own commander damage slot`.
- The modal editor must provide accessible increment and decrement controls without relying on long-press.
- Do not rely on color alone to identify source seats; the order must remain stable and the content description must include player numbers.
- The commander strip must not reduce life-button touch targets below 48dp.

## Testing Plan

Add or update local JVM and Robolectric coverage for:

- `MtgLifeViewModelTest`
  - new games initialize a zeroed commander-damage matrix when enabled
  - disabling commander damage leaves player life state unchanged and hides commander UI
  - incrementing from the modal updates only the targeted defender/source entry
  - decrement never goes below `0`
  - self entries remain `0`
  - `21` commander damage marks the entry as lethal
  - `New Game` preserves the last-used commander-damage toggle

- `MtgLifeFragmentTest`
  - setup shows the commander-damage toggle with default `on`
  - starting a commander-enabled game shows the bottom commander strip
  - tapping a commander summary strip opens the modal editor for that defender
  - modal increment and decrement controls update the visible summary values
  - self rows are disabled in the modal
  - dismissing and reopening the modal shows the current values
  - commander-disabled games do not render the strip

## Implementation Plan

1. Extend setup state and strings with the commander-damage toggle.
2. Add commander-damage saved-state keys and matrix logic to `MtgLifeViewModel`.
3. Extend `LifePlayerUiModel` with commander-damage child state.
4. Update `life_player_cell.xml` to reserve bottom space for the commander strip.
5. Add a commander-damage dialog or modal surface and its row layout.
6. Bind the tile summary strip in `MtgLifeFragment` and wire modal increment and decrement handlers.
7. Add lethal-state styling and accessibility actions.
8. Add focused ViewModel and fragment tests.

## Risks and Mitigations

- The bottom strip could crowd small 5- and 6-player layouts.
  - Mitigation: keep the commander UI compact, reserve a fixed bottom band, and auto-size the life total more aggressively when commander damage is enabled.
- The extra modal step could slow common edits.
  - Mitigation: keep the summary strip easy to hit, open the editor with a single tap, and make modal controls explicit and fast.
- Matrix indexing bugs are easy to introduce.
  - Mitigation: keep defender/source semantics explicit in method names and cover them with focused unit tests.

## Future Extensions

- Optional per-player commander names.
- Partner or multi-commander support.
- Poison counters using the same bottom-of-tile secondary-state pattern.
- Explicit player-dead highlighting when life reaches `0` or commander damage reaches `21`.
