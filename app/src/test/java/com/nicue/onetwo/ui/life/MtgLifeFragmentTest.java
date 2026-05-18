package com.nicue.onetwo.ui.life;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class MtgLifeFragmentTest {

    @Rule public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Test
    public void testSetupStateVisibleOnFirstLaunchAndPrefilled() {
        FragmentScenario<MtgLifeFragment> scenario =
                FragmentScenario.launchInContainer(MtgLifeFragment.class, null, R.style.AppTheme);

        scenario.onFragment(
                fragment -> {
                    View view = fragment.getView();
                    assertNotNull(view);

                    View setupContent = view.findViewById(R.id.setup_content);
                    assertNotNull(setupContent);
                    assertEquals(View.VISIBLE, setupContent.getVisibility());

                    View boardContainer = view.findViewById(R.id.board_container);
                    assertNotNull(boardContainer);
                    assertEquals(View.GONE, boardContainer.getVisibility());

                    EditText playersInput = view.findViewById(R.id.players_input);
                    EditText lifeInput = view.findViewById(R.id.life_input);

                    assertNotNull(playersInput);
                    assertNotNull(lifeInput);

                    assertEquals("4", playersInput.getText().toString());
                    assertEquals("40", lifeInput.getText().toString());
                });
    }

    @Test
    public void testStartGameSwapsToBoardAndTappingPlusMinusUpdatesTotals() {
        FragmentScenario<MtgLifeFragment> scenario =
                FragmentScenario.launchInContainer(MtgLifeFragment.class, null, R.style.AppTheme);

        // 1. Press Start Game with defaults (4 players, 40 life)
        scenario.onFragment(
                fragment -> {
                    View view = fragment.getView();
                    Button startButton = view.findViewById(R.id.start_game_button);
                    assertNotNull(startButton);
                    startButton.performClick();
                });

        // 2. Verify we transitioned to playing board state (4 players board)
        scenario.onFragment(
                fragment -> {
                    View view = fragment.getView();
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

                    // Tap PLUS button for player 1
                    View btnPlus1 = player1.findViewById(R.id.btn_plus);
                    assertNotNull(btnPlus1);
                    btnPlus1.performClick();

                    // Tap MINUS button for player 2
                    View btnMinus2 = player2.findViewById(R.id.btn_minus);
                    assertNotNull(btnMinus2);
                    btnMinus2.performClick();
                });

        // 3. Verify life totals are updated accordingly
        scenario.onFragment(
                fragment -> {
                    View view = fragment.getView();
                    View player1 = view.findViewById(R.id.player_1);
                    View player2 = view.findViewById(R.id.player_2);

                    TextView life1 = player1.findViewById(R.id.tv_life_count);
                    TextView life2 = player2.findViewById(R.id.tv_life_count);

                    assertEquals("41", life1.getText().toString());
                    assertEquals("39", life2.getText().toString());
                });
    }

    @Test
    public void testNewGameResetActionReturnsToSetupWithPrefilledValues() {
        FragmentScenario<MtgLifeFragment> scenario =
                FragmentScenario.launchInContainer(MtgLifeFragment.class, null, R.style.AppTheme);

        // 1. Enter non-default setup, e.g., 3 players, 30 life, and start game
        scenario.onFragment(
                fragment -> {
                    View view = fragment.getView();
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

        // 4. Verify we are back on the setup screen and inputs are prefilled with last-used values
        // (3 and 30)
        scenario.onFragment(
                fragment -> {
                    View view = fragment.getView();
                    View setupContent = view.findViewById(R.id.setup_content);
                    assertEquals(View.VISIBLE, setupContent.getVisibility());

                    View boardContainer = view.findViewById(R.id.board_container);
                    assertEquals(View.GONE, boardContainer.getVisibility());

                    EditText playersInput = view.findViewById(R.id.players_input);
                    EditText lifeInput = view.findViewById(R.id.life_input);
                    assertEquals("3", playersInput.getText().toString());
                    assertEquals("30", lifeInput.getText().toString());
                });
    }
}
