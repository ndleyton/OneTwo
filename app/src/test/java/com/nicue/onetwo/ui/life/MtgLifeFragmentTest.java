package com.nicue.onetwo.ui.life;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Dialog;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.fragment.app.testing.FragmentScenario;
import com.nicue.onetwo.R;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class MtgLifeFragmentTest {

    @Rule public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Test
    public void testSetupStateVisibleOnFirstLaunchAndPrefilled() {
        try (FragmentScenario<MtgLifeFragment> scenario =
                FragmentScenario.launchInContainer(MtgLifeFragment.class, null, R.style.AppTheme)) {

            scenario.onFragment(
                    fragment -> {
                        View view = fragment.getView();
                        assertNotNull(view);

                        View setupContent = view.findViewById(R.id.setup_content);
                        assertNotNull(setupContent);
                        assertEquals(View.VISIBLE, setupContent.getVisibility());

                        View boardContainer = view.findViewById(R.id.board_container);
                        assertNotNull(boardContainer);
                        assertEquals(View.VISIBLE, boardContainer.getVisibility());

                        EditText playersInput = view.findViewById(R.id.players_input);
                        EditText lifeInput = view.findViewById(R.id.life_input);

                        assertNotNull(playersInput);
                        assertNotNull(lifeInput);

                        assertEquals("4", playersInput.getText().toString());
                        assertEquals("40", lifeInput.getText().toString());
                    });
        }
    }

    @Test
    public void testStartGameSwapsToBoardAndTappingPlusMinusUpdatesTotals() {
        try (FragmentScenario<MtgLifeFragment> scenario =
                FragmentScenario.launchInContainer(MtgLifeFragment.class, null, R.style.AppTheme)) {

            // 1. Press Start Game with defaults (4 players, 40 life)
            scenario.onFragment(
                    fragment -> {
                        View view = fragment.getView();
                        assertNotNull(view);
                        Button startButton = view.findViewById(R.id.start_game_button);
                        assertNotNull(startButton);
                        startButton.performClick();
                    });

            // 2. Verify we transitioned to playing board state (4 players board)
            scenario.onFragment(
                    fragment -> {
                        View view = fragment.getView();
                        assertNotNull(view);
                        View setupContent = view.findViewById(R.id.setup_content);
                        assertEquals(View.GONE, setupContent.getVisibility());

                        View boardContainer = view.findViewById(R.id.board_container);
                        assertEquals(View.VISIBLE, boardContainer.getVisibility());

                        // 4-player board is inflated, check that R.id.player_1, player_2, player_3,
                        // player_4 are present
                        View player1 = view.findViewById(R.id.player_1);
                        View player2 = view.findViewById(R.id.player_2);
                        View player3 = view.findViewById(R.id.player_3);
                        View player4 = view.findViewById(R.id.player_4);

                        assertNotNull(player1);
                        assertNotNull(player2);
                        assertNotNull(player3);
                        assertNotNull(player4);

                        // Verify starting life totals are 40
                        TextView life1 = player1.findViewById(R.id.tv_life_count);
                        TextView life2 = player2.findViewById(R.id.tv_life_count);
                        assertEquals("40", life1.getText().toString());
                        assertEquals("40", life2.getText().toString());

                        // Tap right half for player 1
                        View incrementZone1 = player1.findViewById(R.id.life_increment_zone);
                        assertNotNull(incrementZone1);
                        incrementZone1.performClick();

                        // Tap left half for player 2
                        View decrementZone2 = player2.findViewById(R.id.life_decrement_zone);
                        assertNotNull(decrementZone2);
                        decrementZone2.performClick();
                    });

            // 3. Verify life totals are updated accordingly
            scenario.onFragment(
                    fragment -> {
                        View view = fragment.getView();
                        assertNotNull(view);
                        View player1 = view.findViewById(R.id.player_1);
                        View player2 = view.findViewById(R.id.player_2);

                        assertNotNull(player1);
                        assertNotNull(player2);
                        TextView life1 = player1.findViewById(R.id.tv_life_count);
                        TextView life2 = player2.findViewById(R.id.tv_life_count);

                        assertEquals("41", life1.getText().toString());
                        assertEquals("39", life2.getText().toString());
                    });
        }
    }

    @Test
    public void testNewGameResetActionReturnsToSetupWithPrefilledValues() {
        try (FragmentScenario<MtgLifeFragment> scenario =
                FragmentScenario.launchInContainer(MtgLifeFragment.class, null, R.style.AppTheme)) {

            // 1. Enter non-default setup, e.g., 3 players, 30 life, and start game
            scenario.onFragment(
                    fragment -> {
                        View view = fragment.getView();
                        assertNotNull(view);
                        EditText playersInput = view.findViewById(R.id.players_input);
                        EditText lifeInput = view.findViewById(R.id.life_input);
                        playersInput.setText("3");
                        lifeInput.setText("30");

                        Button startButton = view.findViewById(R.id.start_game_button);
                        startButton.performClick();
                    });

            // 2. Verify we are playing 3 players game
            scenario.onFragment(
                    fragment -> {
                        View view = fragment.getView();
                        assertNotNull(view);
                        View setupContent = view.findViewById(R.id.setup_content);
                        assertEquals(View.GONE, setupContent.getVisibility());

                        View boardContainer = view.findViewById(R.id.board_container);
                        assertEquals(View.VISIBLE, boardContainer.getVisibility());

                        View player1 = view.findViewById(R.id.player_1);
                        View player3 = view.findViewById(R.id.player_3);
                        assertNotNull(player1);
                        assertNotNull(player3);

                        TextView life1 = player1.findViewById(R.id.tv_life_count);
                        assertEquals("30", life1.getText().toString());
                    });

            // 3. Trigger app-bar "New Game" menu action (action_new_game)
            scenario.onFragment(
                    fragment -> {
                        org.robolectric.fakes.RoboMenuItem resetMenuItem =
                                new org.robolectric.fakes.RoboMenuItem(R.id.action_new_game);
                        fragment.onMenuItemSelected(resetMenuItem);
                    });

            // 4. Verify we are back on the setup screen and inputs are prefilled with last-used
            // values
            // (3 and 30)
            scenario.onFragment(
                    fragment -> {
                        View view = fragment.getView();
                        assertNotNull(view);
                        View setupContent = view.findViewById(R.id.setup_content);
                        assertEquals(View.VISIBLE, setupContent.getVisibility());

                        View boardContainer = view.findViewById(R.id.board_container);
                        assertEquals(View.VISIBLE, boardContainer.getVisibility());

                        EditText playersInput = view.findViewById(R.id.players_input);
                        EditText lifeInput = view.findViewById(R.id.life_input);
                        assertEquals("3", playersInput.getText().toString());
                        assertEquals("30", lifeInput.getText().toString());
                    });
        }
    }

    @Test
    public void testTappingOutsideSetupCardDismissesOverlayWhenGameIsActive() {
        try (FragmentScenario<MtgLifeFragment> scenario =
                FragmentScenario.launchInContainer(MtgLifeFragment.class, null, R.style.AppTheme)) {

            // 1. Start a game with default settings
            scenario.onFragment(
                    fragment -> {
                        View view = fragment.getView();
                        assertNotNull(view);
                        Button startButton = view.findViewById(R.id.start_game_button);
                        startButton.performClick();
                    });

            // 2. Go back to setup screen via menu action
            scenario.onFragment(
                    fragment -> {
                        org.robolectric.fakes.RoboMenuItem resetMenuItem =
                                new org.robolectric.fakes.RoboMenuItem(R.id.action_new_game);
                        fragment.onMenuItemSelected(resetMenuItem);
                    });

            // 3. Verify setup overlay is visible
            scenario.onFragment(
                    fragment -> {
                        View view = fragment.getView();
                        assertNotNull(view);
                        View setupOverlay = view.findViewById(R.id.setup_overlay);
                        assertEquals(View.VISIBLE, setupOverlay.getVisibility());
                    });

            // 4. Tap the overlay (clicking outside the card)
            scenario.onFragment(
                    fragment -> {
                        View view = fragment.getView();
                        assertNotNull(view);
                        View setupOverlay = view.findViewById(R.id.setup_overlay);
                        setupOverlay.performClick();
                    });

            // 5. Verify setup overlay is now GONE (modal dismissed/escaped)
            scenario.onFragment(
                    fragment -> {
                        View view = fragment.getView();
                        assertNotNull(view);
                        View setupOverlay = view.findViewById(R.id.setup_overlay);
                        assertEquals(View.GONE, setupOverlay.getVisibility());
                    });
        }
    }

    @Test
    public void testCommanderDamageDialogHalfTapZonesUpdateSummary() {
        try (FragmentScenario<MtgLifeFragment> scenario =
                FragmentScenario.launchInContainer(MtgLifeFragment.class, null, R.style.AppTheme)) {

            scenario.onFragment(
                    fragment -> {
                        View view = fragment.getView();
                        assertNotNull(view);
                        Button startButton = view.findViewById(R.id.start_game_button);
                        startButton.performClick();
                    });

            scenario.onFragment(
                    fragment -> {
                        View player1 = fragment.requireView().findViewById(R.id.player_1);
                        assertNotNull(player1);
                        View commanderGrid = player1.findViewById(R.id.commander_damage_grid);
                        assertNotNull(commanderGrid);
                        try {
                            java.lang.reflect.Method showDialogMethod =
                                    MtgLifeFragment.class.getDeclaredMethod(
                                            "showCommanderDamageDialog", int.class);
                            showDialogMethod.setAccessible(true);
                            showDialogMethod.invoke(fragment, 0);
                        } catch (ReflectiveOperationException e) {
                            throw new AssertionError(e);
                        }
                        Shadows.shadowOf(Looper.getMainLooper()).idle();

                        Dialog dialog = ShadowDialog.getLatestDialog();
                        assertNotNull(dialog);
                        assertTrue(dialog.isShowing());
                        assertNotNull(dialog.getWindow());

                        View decorView = dialog.getWindow().getDecorView();
                        View incrementZone =
                                findViewWithContentDescription(
                                        decorView,
                                        fragment.getString(
                                                R.string.mtg_commander_damage_increase_desc, 2, 1));
                        View decrementZone =
                                findViewWithContentDescription(
                                        decorView,
                                        fragment.getString(
                                                R.string.mtg_commander_damage_decrease_desc, 2, 1));

                        assertNotNull(incrementZone);
                        assertNotNull(decrementZone);

                        incrementZone.performClick();
                        incrementZone.performClick();
                        decrementZone.performClick();

                        View updatedSummary =
                                findViewWithContentDescription(
                                        commanderGrid,
                                        fragment.getString(
                                                R.string.mtg_commander_damage_cell_desc, 2, 1, 1));
                        assertNotNull(updatedSummary);
                    });
        }
    }

    @Test
    public void testLongPressLifeZonesAdjustByTen() {
        try (FragmentScenario<MtgLifeFragment> scenario =
                FragmentScenario.launchInContainer(MtgLifeFragment.class, null, R.style.AppTheme)) {

            scenario.onFragment(
                    fragment -> {
                        View view = fragment.getView();
                        assertNotNull(view);
                        Button startButton = view.findViewById(R.id.start_game_button);
                        assertNotNull(startButton);
                        startButton.performClick();
                    });

            scenario.onFragment(
                    fragment -> {
                        View view = fragment.requireView();
                        View player1 = view.findViewById(R.id.player_1);
                        View player2 = view.findViewById(R.id.player_2);
                        assertNotNull(player1);
                        assertNotNull(player2);

                        View incrementZone1 = player1.findViewById(R.id.life_increment_zone);
                        View decrementZone2 = player2.findViewById(R.id.life_decrement_zone);
                        assertNotNull(incrementZone1);
                        assertNotNull(decrementZone2);

                        incrementZone1.performLongClick();
                        decrementZone2.performLongClick();
                    });

            scenario.onFragment(
                    fragment -> {
                        View view = fragment.requireView();
                        View player1 = view.findViewById(R.id.player_1);
                        View player2 = view.findViewById(R.id.player_2);
                        assertNotNull(player1);
                        assertNotNull(player2);

                        TextView life1 = player1.findViewById(R.id.tv_life_count);
                        TextView life2 = player2.findViewById(R.id.tv_life_count);
                        assertEquals("50", life1.getText().toString());
                        assertEquals("30", life2.getText().toString());
                    });
        }
    }

    @Test
    public void testFivePlayerCommanderDamageLayoutForPlayer5MatchesFragment() {
        try (FragmentScenario<MtgLifeFragment> scenario =
                FragmentScenario.launchInContainer(MtgLifeFragment.class, null, R.style.AppTheme)) {

            // 1. Enter 5 players setup and start game
            scenario.onFragment(
                    fragment -> {
                        View view = fragment.getView();
                        assertNotNull(view);
                        EditText playersInput = view.findViewById(R.id.players_input);
                        EditText lifeInput = view.findViewById(R.id.life_input);
                        playersInput.setText("5");
                        lifeInput.setText("40");

                        Button startButton = view.findViewById(R.id.start_game_button);
                        startButton.performClick();
                    });

            // 2. Open commander damage dialog for player 5 (seat 4)
            scenario.onFragment(
                    fragment -> {
                        View player5 = fragment.requireView().findViewById(R.id.player_5);
                        View commanderGrid = player5.findViewById(R.id.commander_damage_grid);
                        assertNotNull(commanderGrid);
                        try {
                            java.lang.reflect.Method showDialogMethod =
                                    MtgLifeFragment.class.getDeclaredMethod(
                                            "showCommanderDamageDialog", int.class);
                            showDialogMethod.setAccessible(true);
                            showDialogMethod.invoke(fragment, 4);
                        } catch (ReflectiveOperationException e) {
                            throw new AssertionError(e);
                        }
                        Shadows.shadowOf(Looper.getMainLooper()).idle();

                        Dialog dialog = ShadowDialog.getLatestDialog();
                        assertNotNull(dialog);
                        assertTrue(dialog.isShowing());
                        assertNotNull(dialog.getWindow());

                        View decorView = dialog.getWindow().getDecorView();
                        android.widget.LinearLayout dialogContent =
                                decorView.findViewWithTag("commander_dialog_content");
                        assertNotNull(dialogContent);

                        // Verify that row 0 has the expected cells:
                        // Col 0: Player 1 (source seat 0) -> visible
                        // Col 1: Player 2 (source seat 1) -> visible
                        // Col 2: Player 5 (source seat 4, self) -> invisible
                        android.widget.LinearLayout row0 =
                                (android.widget.LinearLayout) dialogContent.getChildAt(0);
                        View cell0 = row0.getChildAt(0);
                        View cell1 = row0.getChildAt(1);
                        View cell2 = row0.getChildAt(2);

                        assertEquals(View.VISIBLE, cell0.getVisibility());
                        assertEquals(View.VISIBLE, cell1.getVisibility());
                        assertEquals(View.INVISIBLE, cell2.getVisibility());

                        // Verify increment zones exist in cell0 and cell1
                        View incZone0 =
                                findViewWithContentDescription(
                                        cell0,
                                        fragment.getString(
                                                R.string.mtg_commander_damage_increase_desc, 1, 5));
                        View incZone1 =
                                findViewWithContentDescription(
                                        cell1,
                                        fragment.getString(
                                                R.string.mtg_commander_damage_increase_desc, 2, 5));
                        assertNotNull(incZone0);
                        assertNotNull(incZone1);

                        // Verify that row 1 has the expected cells:
                        // Col 0: Player 3 (source seat 2) -> visible
                        // Col 1: Player 4 (source seat 3) -> visible
                        // Col 2: Spacer (seat 5) -> invisible
                        android.widget.LinearLayout row1 =
                                (android.widget.LinearLayout) dialogContent.getChildAt(1);
                        View cell3 = row1.getChildAt(0);
                        View cell4 = row1.getChildAt(1);
                        View cell5 = row1.getChildAt(2);

                        assertEquals(View.VISIBLE, cell3.getVisibility());
                        assertEquals(View.VISIBLE, cell4.getVisibility());
                        assertEquals(View.INVISIBLE, cell5.getVisibility());

                        View incZone3 =
                                findViewWithContentDescription(
                                        cell3,
                                        fragment.getString(
                                                R.string.mtg_commander_damage_increase_desc, 3, 5));
                        View incZone4 =
                                findViewWithContentDescription(
                                        cell4,
                                        fragment.getString(
                                                R.string.mtg_commander_damage_increase_desc, 4, 5));
                        assertNotNull(incZone3);
                        assertNotNull(incZone4);
                    });
        }
    }

    private static View findViewWithContentDescription(View root, CharSequence contentDescription) {
        if (root == null) {
            return null;
        }
        CharSequence rootDescription = root.getContentDescription();
        if (rootDescription != null
                && rootDescription.toString().contentEquals(contentDescription)) {
            return root;
        }
        if (root instanceof android.view.ViewGroup rootGroup) {
            for (int i = 0; i < rootGroup.getChildCount(); i++) {
                View match =
                        findViewWithContentDescription(rootGroup.getChildAt(i), contentDescription);
                if (match != null) {
                    return match;
                }
            }
        }
        return null;
    }
}
