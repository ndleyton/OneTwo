# RFC: MTG Life Counter Fragment

- Status: Proposed
- Date: 2026-05-18

## Summary

Add a new top-level MTG life counter fragment to OneTwo. The feature starts on a setup screen with `New Game`, `Players`, and `Life` inputs, defaulting to 4 players and 40 life. Starting a game transitions in-place to a shared-screen life board that supports 1 to 6 players, uses OneTwo colors, and updates each player total by exactly `-1` or `+1` per tap.

## Motivation

The existing counter screen works for generic counters but is not optimized for a shared Magic: The Gathering play surface. MTG games need:

- fast game setup
- large readable life totals
- layouts that work when a phone is placed on the table
- fixed `+1` and `-1` interactions without opening dialogs

This feature should feel purpose-built instead of asking users to create and manage multiple generic counters manually.

## Goals

- Add a new fragment dedicated to MTG life tracking.
- Support 1 to 6 players.
- Default setup to 4 players and 40 life.
- Show a setup state first, then a play state after pressing start.
- Use large life totals and single-tap `+` and `-` actions.
- Match OneTwo theming in light and dark mode.
- Preserve in-progress game state across view recreation and configuration changes.

## Non-Goals

- Commander damage, poison, energy, experience, or other side counters
- match history or undo history
- persistent saved games in Room or SharedPreferences
- tablet-specific layouts beyond what naturally scales from the phone-first design
- custom increment values, gestures, or long-press acceleration in v1

## Product Decision

This should be a new top-level drawer destination, not a mode added onto the existing counter feature.

Why:

- the interaction model is substantially different from the generic counter list
- the setup flow is MTG-specific
- the play view is a full-screen shared board, not a scrolling list
- keeping it separate avoids complicating `CounterFragment`, `CounterViewModel`, and Room data

## UX Proposal

### Setup State

The fragment initially shows a centered setup surface inside the existing activity shell.

- Title: `New Game`
- Field 1: `Players`
  - numeric input
  - default `4`
  - validation range `1..6`
- Field 2: `Life`
  - numeric input
  - default `40`
  - validation `> 0`
- Primary CTA: `Start Game`

Implementation notes:

- Use `TextInputLayout` plus `TextInputEditText` to match existing patterns in the app.
- Keep validation inline and non-blocking until `Start Game` is pressed.
- If the user enters invalid data, show field errors instead of silently coercing values.

### Play State

After pressing `Start Game`, the same fragment swaps to a full-board play state.

Each player panel contains:

- a large life total centered in the tile
- a minus affordance that decreases life by 1
- a plus affordance that increases life by 1
- a distinct background color derived from the app palette

Behavior:

- tapping minus decrements by exactly `1`
- tapping plus increments by exactly `1`
- no confirmation dialogs
- no animations beyond standard Material press feedback in v1

The board should fill the fragment content area below the existing toolbar. We should not special-case hiding the app bar in v1; the app currently uses a consistent top-level navigation shell and this feature should fit it.

## Layout Strategy By Player Count

Because the board must work from 1 to 6 players and some counts need asymmetric arrangements, we should use dedicated XML templates per player count rather than a generic `RecyclerView`.

Proposed templates:

| Players | Template | Notes |
| --- | --- | --- |
| 1 | single full-screen panel | upright text |
| 2 | two stacked halves | top panel rotated 180, bottom upright |
| 3 | one top half plus two bottom panels | top rotated 180, lower pair oriented to left/right edges |
| 4 | 2x2 board | closest visual match to the provided reference |
| 5 | two top panels plus three bottom panels | custom template to avoid an awkward empty slot |
| 6 | 2x3 board | top row rotated 180, bottom row upright |

Rotation guideline:

- top-facing seats: `180`
- bottom-facing seats: `0`
- left-facing seats: `270`
- right-facing seats: `90`

This preserves the shared-table feeling for lower player counts while keeping 5- and 6-player support practical on a phone.

## Visual Design

The provided reference is the right interaction model, but the colors should be clearly OneTwo.

Recommended approach:

- define dedicated player tile colors in `values/colors.xml` and `values-night/colors.xml`
- derive them from the existing blue, gold, and red palette rather than importing unrelated neon colors
- pair each tile color with an explicit foreground color for contrast

Suggested palette direction:

- player 1: primary container family
- player 2: secondary container family
- player 3: tertiary container family
- player 4: primary or secondary tonal variant
- player 5: tertiary tonal variant
- player 6: darker supporting neutral or accent variant

The exact values can be tuned during implementation, but the RFC decision is to introduce explicit `lifeCounterPlayerX` and `lifeCounterOnPlayerX` color resources instead of reusing unrelated dice colors.

## Architecture

### Navigation

Add a new top-level destination:

- `nav_mtg_life` in `app/src/main/res/navigation/main_nav_graph.xml`
- matching drawer item in `app/src/main/res/menu/drawer_view.xml`
- matching entry in `MainActivity` `AppBarConfiguration`
- new localized strings in `values/strings.xml` and `values-es/strings.xml`

Menu label recommendation: `Life Counter`

### Package and Files

Recommended package:

- `app/src/main/java/com/nicue/onetwo/ui/life/`

Initial file set:

- `MtgLifeFragment.java`
- `MtgLifeViewModel.java`
- `MtgLifeUiState.java`
- `LifePlayerUiModel.java`
- `MtgLifeViewModelFactory.java` only if needed by the chosen state approach

Initial resource set:

- `app/src/main/res/layout/life_fragment.xml`
- `app/src/main/res/layout/life_setup_content.xml`
- `app/src/main/res/layout/life_board_1.xml`
- `app/src/main/res/layout/life_board_2.xml`
- `app/src/main/res/layout/life_board_3.xml`
- `app/src/main/res/layout/life_board_4.xml`
- `app/src/main/res/layout/life_board_5.xml`
- `app/src/main/res/layout/life_board_6.xml`
- `app/src/main/res/menu/life_actions.xml` for an app-bar `New Game` action

### State Management

This feature does not need Room or SharedPreferences in v1.

Use a `ViewModel` with in-memory state backed by `SavedStateHandle` so that:

- the game survives configuration changes
- the fragment can recreate its view without losing scores
- process recreation has a straightforward restoration path

Recommended state model:

- `boolean showingSetup`
- `int playerCount`
- `int startingLife`
- `List<Integer> currentLives`

Recommended UI model:

- `MtgLifeUiState`
  - `showingSetup`
  - `playerCount`
  - `startingLife`
  - `List<LifePlayerUiModel> players`

- `LifePlayerUiModel`
  - `seatIndex`
  - `lifeTotal`
  - `rotationDegrees`
  - `backgroundColorRes`
  - `foregroundColorRes`

This keeps all game rules in the `ViewModel` and lets the fragment stay focused on binding clicks and rendering the active template.

### Interaction Model

Fragment responsibilities:

- inflate binding
- render setup or play state
- route button taps to the `ViewModel`
- inflate the correct board template for the current player count

ViewModel responsibilities:

- validate setup input
- create a new game with the chosen player count and starting life
- increment and decrement life totals
- expose UI state as `LiveData`
- reset back to setup on `New Game`

Recommended reset behavior:

- an app-bar `New Game` action returns to setup
- setup fields stay prefilled with the last used values, not hard-reset to 4 and 40 every time

That makes rematches faster while still honoring the required defaults for first launch.

## Accessibility and Usability

- plus and minus controls must keep at least 48dp touch targets
- life totals should use auto-sizing text or per-template text sizes so 3-digit values remain readable
- content descriptions should identify player number and action, for example `Decrease life for player 2`
- color choices must meet contrast requirements in both `values` and `values-night`
- layout should avoid relying on color alone for affordance; the `+` and `-` controls must remain explicit

## Testing Plan

Local JVM and Robolectric tests are sufficient for v1.

Add:

- `MtgLifeViewModelTest`
  - defaults are 4 players and 40 life
  - validation rejects players outside `1..6`
  - validation rejects non-positive starting life
  - starting a game creates the expected number of players
  - increment and decrement only change the targeted player by 1
  - `New Game` returns to setup and preserves last-used configuration

- `MtgLifeFragmentTest`
  - setup state is visible on first launch
  - setup fields are prefilled with `4` and `40`
  - starting a game swaps to the 4-player board
  - tapping plus and minus updates visible totals

- update `NavigationSmokeTest`
  - verify `nav_mtg_life` is present and navigable

## Implementation Plan

1. Add navigation, drawer, strings, and the fragment shell.
2. Implement the setup state and `MtgLifeViewModel`.
3. Add the 1-6 player board templates and per-seat rendering.
4. Add the app-bar `New Game` reset action.
5. Add tests and polish colors for light and dark mode.

## Risks and Mitigations

- Small-screen density for 5 and 6 players
  - Mitigation: dedicated templates, tight padding, and auto-sized life text
- Dark mode contrast regressions
  - Mitigation: explicit foreground color resources per tile, not implicit theme lookups
- Overengineering state persistence
  - Mitigation: keep v1 session-only and avoid database work unless a save/resume requirement appears later

## Future Extensions

Likely follow-ups if the feature lands well:

- commander damage tracking
- poison counters
- random first player / turn order integration
- per-player names
- larger-table or tablet-specific layouts
