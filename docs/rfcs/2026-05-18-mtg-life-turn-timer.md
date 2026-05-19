# RFC: MTG Life Turn Timer

- Status: Proposed
- Date: 2026-05-18
- Related:
  - `docs/rfcs/2026-05-18-mtg-life-counter-fragment.md`
  - `docs/rfcs/2026-05-18-mtg-commander-damage.md`

## Summary

Add an optional per-player turn timer to the MTG life counter. When enabled, each player tile in `MtgLifeFragment` shows a compact countdown plus a `Pass` control. Only the active player's timer counts down. Pressing `Pass` moves the active turn to the next player in clockwise order and starts their clock immediately.

The timer is disabled by default, configurable from the existing setup surface, and visually secondary to the life total and commander-damage UI.

## Motivation

The standalone `TimerFragment` already proves the app can run a multi-seat countdown clock, but it is disconnected from the MTG board. Commander and multiplayer MTG play often need both life totals and turn pacing at the same time. Switching between two screens is awkward on a shared phone/table setup, and asking players to remember whose turn is active defeats the purpose of an in-app clock.

This feature should keep the MTG board as the main surface while adding a lightweight chess-clock style turn flow.

## Goals

- Add an optional turn timer to the existing MTG life screen.
- Show one timer per seat in `life_player_cell.xml`.
- Decrement time only for the active seat.
- Add a `Pass` action on the tile so the turn advances clockwise.
- Keep the timer hidden unless enabled from setup.
- Keep the timer visually compact so life total and commander damage remain primary.
- Preserve timer state across view recreation and configuration changes.
- Reuse the existing timer behavior and scheduler concepts where practical.

## Non-Goals

- A second top-level timer screen inside MTG life.
- Per-turn increment or delay in v1.
- Manual reordering of turn order in v1.
- Background persistent games beyond current `SavedStateHandle` behavior.
- Automatic winner detection, elimination rules, or skipping dead players.
- Editing timer values mid-game in v1.

## Product Decisions

### Scope

This is an extension of `MtgLifeFragment`, not a separate fragment and not a new drawer destination.

### Setup

Extend `life_setup_content.xml` with turn-timer controls alongside the existing commander-damage toggle:

- `Turn Timer` switch, default `off`
- `Time` configuration shown only when the switch is enabled

Recommended default when enabled:

- base time: `5:00`

Why:

- it matches the current default in `TimerViewModel`
- it avoids inventing a new default only for MTG life
- it keeps the first implementation simple by omitting increment

### Runtime Behavior

When the game starts with turn timer enabled:

- each player receives the configured starting time
- seat `0` is the initial active player
- the timer begins paused until the first user action

Recommended first actions that start the clock:

- pressing `Pass` from the initial active seat
- pressing a dedicated per-screen or per-tile `Start` control if implementation chooses to add one

The initial implementation should prefer the smallest UI that makes this state obvious. If `Pass` is used as the first action, it should both start the timer system and advance to the next clockwise player.

Pressing `Pass`:

- stops decrementing the current active timer
- advances to the next seat in clockwise order
- starts decrementing the next seat's timer immediately

When a timer reaches `0`:

- that seat remains the active seat
- the turn timer pauses
- all `Pass` buttons become disabled until the user starts a new game

This keeps the failure state explicit and avoids silently continuing after a player times out.

### Pause And Resume Controls

Do not add app-bar pause or resume actions in the initial implementation.

Why:

- the turn timer is secondary to the life-counter board
- adding more top-app-bar actions increases UI complexity immediately
- the first version should prove the core per-seat countdown and clockwise pass flow first

### Clockwise Order

The implementation must not treat clockwise as an implicit `seatIndex + 1` assumption unless the board layout is verified to match it for every player count.

Instead, define turn order explicitly per supported player count inside the life feature. The order should be derived from the board templates already used by `MtgLifeFragment`, then validated visually during implementation.

This matters most for 3-, 4-, and 5-player layouts, where XML placement and rotation are not as trivial as the 2-player case.

### Small Visual Footprint

The timer UI must be present but secondary:

- small text
- compact `Pass` affordance
- no large card treatment
- no displacement of life controls below acceptable touch-target sizes

The life total remains the dominant element in each tile.

## UX Proposal

### Setup State

Add two new rows below `Commander Damage`:

- label: `Turn Timer`
- control: `SwitchMaterial`
- default: unchecked

- label: `Time`
- control: compact configuration affordance for `mm:ss`
- visible only when `Turn Timer` is checked

Implementation recommendation:

- reuse the existing timer-duration editing pattern from `TimerFragment`
- use a small summary value such as `5:00`
- open the same minute/second picker dialog when the user taps the timer-value row

This avoids crowding `life_setup_content.xml` with four more inline number fields.

### Play State

Each `life_player_cell.xml` tile gains a compact turn-timer row inside `inner_player_layout`.

Recommended contents:

- timer text, for example `4:37`
- small active-state indicator
- `Pass` button

Behavior:

- all tiles show their remaining time when timer is enabled
- only the active player's timer is visually emphasized
- only the active player's `Pass` button is enabled
- inactive players still show their stored remaining time
- when timer is disabled, the entire timer row is hidden

### Placement

Recommended placement is at the top edge of the tile, above the life total. The commander-damage strip already occupies the bottom edge, so putting the timer there would create unnecessary competition.

This implies:

- reserve a small top band for timer UI
- keep the life total centered between the timer row and commander-damage strip
- preserve existing left/right life tap zones

### Visual Treatment

- timer text should use a smaller appearance than `tv_life_count`
- active timer text can use a stronger weight or subtle tint
- expired timer should use an error or warning color
- `Pass` should look tappable but compact, such as a small Material button or chip-sized control

The tile should not gain more heavy chrome than necessary.

## Architecture

### Reuse Strategy

Do not embed `TimerViewModel` inside `MtgLifeFragment`.

Recommended approach:

- extract the countdown and turn-advance logic from `TimerViewModel` into a reusable plain-Java state helper
- keep `TimerScheduler` as the ticking abstraction
- let both `TimerViewModel` and `MtgLifeViewModel` use that shared helper

Why:

- the existing timer already solves ticking, pausing, and remaining-time bookkeeping
- `MtgLifeViewModel` should stay the owner of MTG setup and per-seat UI state
- reusing a helper avoids copy-pasting timer math without coupling one screen's `ViewModel` to another

Reasonable helper shapes:

- `TurnTimerEngine`
- `TurnTimerState`
- `TurnTimerSnapshot`

The exact class names can be chosen during implementation, but the RFC decision is to share timer logic below the `ViewModel` layer.

### Saved State

Extend `MtgLifeViewModel` saved state with fields equivalent to:

- `KEY_TURN_TIMER_ENABLED`
- `KEY_TURN_TIMER_DURATION_MS`
- `KEY_TURN_TIMER_REMAINING_TIMES`
- `KEY_TURN_TIMER_ACTIVE_SEAT_INDEX`
- `KEY_TURN_TIMER_PAUSED`
- `KEY_TURN_TIMER_FINISHED`

The life feature does not need to reuse `TimerStateStore` directly. Unlike the standalone timer screen, MTG life already keeps its whole session in one `SavedStateHandle`.

### UI Models

Extend `MtgLifeUiState` with:

- `boolean turnTimerEnabled`
- `boolean turnTimerPaused`
- `boolean turnTimerFinished`

Extend `LifePlayerUiModel` with:

- `boolean timerVisible`
- `String timerDisplay`
- `boolean timerActive`
- `boolean timerExpired`
- `boolean passEnabled`

This keeps seat-specific rendering decisions in the `ViewModel`, consistent with the current life feature architecture.

### ViewModel Responsibilities

`MtgLifeViewModel` should:

- validate and store turn-timer setup input
- initialize timer state when starting a game
- expose the initial paused state until the first timer action occurs
- expose compact per-seat timer UI data
- handle `passTurn(seatIndex)` or equivalent action
- ignore `Pass` taps from inactive seats
- pause the timer when the fragment backgrounds
- react to tick callbacks and stop cleanly at zero

### Fragment Responsibilities

`MtgLifeFragment` should:

- bind the setup toggle and timer-config row
- render the timer row in each `LifePlayerCellBinding`
- route `Pass` clicks to the `ViewModel`
- pause timer progression in `onStop()` using the same background behavior as `TimerFragment`
- keep the timer row small and hidden when disabled

The fragment should not perform timer math.

## Layout Changes

### `life_setup_content.xml`

Add:

- `turn_timer_switch`
- a compact `turn_timer_value` row or button

Behavior:

- `turn_timer_value` is disabled or hidden when the switch is off
- the current configured duration is shown in `mm:ss`

### `life_player_cell.xml`

Add a compact timer container near the top of `inner_player_layout`.

Suggested structure:

- `tv_turn_timer`
- `btn_pass_turn`

Constraints:

- keep the timer container narrow and shallow
- do not shrink the left and right life tap zones below 48dp effective targets
- adjust `tv_life_count` constraints so it still scales correctly with commander damage visible

## Accessibility

- `Pass` must have a clear content description such as `Pass turn from player 2 to next player`
- timer text should expose player-aware spoken context, for example `Player 2 timer: 4 minutes 37 seconds`
- active versus inactive state should not rely on color alone
- expired state should be announced through text and enabled-state changes, not just tint

## Testing Plan

Add focused local tests for:

- `MtgLifeViewModelTest`
  - timer toggle defaults to off
  - enabling timer initializes equal remaining times for all players
  - enabling timer starts in a paused state
  - only the active player's time decreases on tick
  - the first timer action starts the timer from the paused state
  - `Pass` advances to the next configured seat order
  - inactive-seat `Pass` taps are ignored
  - expiration pauses the timer and marks the active seat expired
  - `New Game` preserves last-used timer settings in setup

- shared timer helper tests
  - tick math matches current `TimerViewModel` semantics
  - advancing seats resets tick baseline correctly
  - paused state does not decrement time

- Robolectric fragment coverage
  - setup hides timer controls when switch is off
  - enabling timer reveals config UI
  - play board shows timer row only when enabled
  - only active tile exposes an enabled `Pass` control

## Implementation Notes

- Prefer reusing `TimerBackend.formatRemainingTime(...)` for display formatting.
- Keep the first version incremental-free even if the shared helper internally remains capable of increment in the future.
- Validate the explicit clockwise seat order against each `life_board_X.xml` template during implementation rather than relying on guesswork from current seat indices.

## Resolved Decisions

- The timer should begin paused until the first user action.
- The initial implementation should not add app-bar pause or resume actions.
