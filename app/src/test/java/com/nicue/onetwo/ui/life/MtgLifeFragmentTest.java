package com.nicue.onetwo.ui.life;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Dialog;
import android.graphics.Rect;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.test.core.app.ActivityScenario;
import com.nicue.onetwo.MainActivity;
import com.nicue.onetwo.R;
import com.nicue.onetwo.databinding.LifePlayerCellBinding;
import java.util.concurrent.TimeUnit;
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
    public void testSetupStateHiddenOnFirstLaunchAndPrefilledOnNewGame() {
        try (FragmentScenario<MtgLifeFragment> scenario =
                FragmentScenario.launchInContainer(MtgLifeFragment.class, null, R.style.AppTheme)) {

            scenario.onFragment(
                    fragment -> {
                        View view = fragment.getView();
                        assertNotNull(view);

                        View setupContent = view.findViewById(R.id.setup_content);
                        assertNotNull(setupContent);
                        assertEquals(View.GONE, setupContent.getVisibility());

                        View boardContainer = view.findViewById(R.id.board_container);
                        assertNotNull(boardContainer);
                        assertEquals(View.VISIBLE, boardContainer.getVisibility());
                    });

            scenario.onFragment(
                    fragment -> {
                        org.robolectric.fakes.RoboMenuItem resetMenuItem =
                                new org.robolectric.fakes.RoboMenuItem(R.id.action_new_game);
                        fragment.onMenuItemSelected(resetMenuItem);
                    });

            scenario.onFragment(
                    fragment -> {
                        View view = fragment.getView();
                        assertNotNull(view);

                        View setupContent = view.findViewById(R.id.setup_content);
                        assertEquals(View.VISIBLE, setupContent.getVisibility());

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

            scenario.onFragment(
                    fragment -> {
                        org.robolectric.fakes.RoboMenuItem resetMenuItem =
                                new org.robolectric.fakes.RoboMenuItem(R.id.action_new_game);
                        fragment.onMenuItemSelected(resetMenuItem);

                        View view = fragment.getView();
                        assertNotNull(view);
                        Button startButton = view.findViewById(R.id.start_game_button);
                        assertNotNull(startButton);
                        startButton.performClick();
                    });

            scenario.onFragment(
                    fragment -> {
                        View view = fragment.getView();
                        assertNotNull(view);
                        View setupContent = view.findViewById(R.id.setup_content);
                        assertEquals(View.GONE, setupContent.getVisibility());

                        View boardContainer = view.findViewById(R.id.board_container);
                        assertEquals(View.VISIBLE, boardContainer.getVisibility());

                        View player1 = view.findViewById(R.id.player_1);
                        View player2 = view.findViewById(R.id.player_2);
                        View player3 = view.findViewById(R.id.player_3);
                        View player4 = view.findViewById(R.id.player_4);

                        assertNotNull(player1);
                        assertNotNull(player2);
                        assertNotNull(player3);
                        assertNotNull(player4);

                        TextView life1 = player1.findViewById(R.id.tv_life_count);
                        TextView life2 = player2.findViewById(R.id.tv_life_count);
                        TextView negativeChange1 =
                                player1.findViewById(R.id.tv_recent_life_change_negative);
                        TextView positiveChange1 =
                                player1.findViewById(R.id.tv_recent_life_change_positive);
                        TextView negativeChange2 =
                                player2.findViewById(R.id.tv_recent_life_change_negative);
                        TextView positiveChange2 =
                                player2.findViewById(R.id.tv_recent_life_change_positive);
                        assertEquals("40", life1.getText().toString());
                        assertEquals("40", life2.getText().toString());
                        assertEquals(View.GONE, negativeChange1.getVisibility());
                        assertEquals(View.GONE, positiveChange1.getVisibility());
                        assertEquals(View.GONE, negativeChange2.getVisibility());
                        assertEquals(View.GONE, positiveChange2.getVisibility());

                        View incrementZone1 = player1.findViewById(R.id.life_increment_zone);
                        assertNotNull(incrementZone1);
                        incrementZone1.performClick();

                        View decrementZone2 = player2.findViewById(R.id.life_decrement_zone);
                        assertNotNull(decrementZone2);
                        decrementZone2.performClick();
                    });

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
                        TextView negativeChange1 =
                                player1.findViewById(R.id.tv_recent_life_change_negative);
                        TextView positiveChange1 =
                                player1.findViewById(R.id.tv_recent_life_change_positive);
                        TextView negativeChange2 =
                                player2.findViewById(R.id.tv_recent_life_change_negative);
                        TextView positiveChange2 =
                                player2.findViewById(R.id.tv_recent_life_change_positive);

                        assertEquals("41", life1.getText().toString());
                        assertEquals("39", life2.getText().toString());
                        assertEquals(View.GONE, negativeChange1.getVisibility());
                        assertEquals(View.VISIBLE, positiveChange1.getVisibility());
                        assertEquals("+1", positiveChange1.getText().toString());
                        assertEquals(View.VISIBLE, negativeChange2.getVisibility());
                        assertEquals("-1", negativeChange2.getText().toString());
                        assertEquals(View.GONE, positiveChange2.getVisibility());
                    });
        }
    }

    @Test
    public void testNewGameResetActionReturnsToSetupWithPrefilledValues() {
        try (FragmentScenario<MtgLifeFragment> scenario =
                FragmentScenario.launchInContainer(MtgLifeFragment.class, null, R.style.AppTheme)) {

            scenario.onFragment(
                    fragment -> {
                        org.robolectric.fakes.RoboMenuItem resetMenuItem =
                                new org.robolectric.fakes.RoboMenuItem(R.id.action_new_game);
                        fragment.onMenuItemSelected(resetMenuItem);

                        View view = fragment.getView();
                        assertNotNull(view);
                        EditText playersInput = view.findViewById(R.id.players_input);
                        EditText lifeInput = view.findViewById(R.id.life_input);
                        playersInput.setText("3");
                        lifeInput.setText("30");

                        Button startButton = view.findViewById(R.id.start_game_button);
                        startButton.performClick();
                    });

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

            scenario.onFragment(
                    fragment -> {
                        org.robolectric.fakes.RoboMenuItem resetMenuItem =
                                new org.robolectric.fakes.RoboMenuItem(R.id.action_new_game);
                        fragment.onMenuItemSelected(resetMenuItem);
                    });

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

            scenario.onFragment(
                    fragment -> {
                        org.robolectric.fakes.RoboMenuItem resetMenuItem =
                                new org.robolectric.fakes.RoboMenuItem(R.id.action_new_game);
                        fragment.onMenuItemSelected(resetMenuItem);
                    });

            scenario.onFragment(
                    fragment -> {
                        View view = fragment.getView();
                        assertNotNull(view);
                        View setupOverlay = view.findViewById(R.id.setup_overlay);
                        assertEquals(View.VISIBLE, setupOverlay.getVisibility());
                    });

            scenario.onFragment(
                    fragment -> {
                        View view = fragment.getView();
                        assertNotNull(view);
                        View setupOverlay = view.findViewById(R.id.setup_overlay);
                        setupOverlay.performClick();
                    });

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
    public void testTwoPlayerCommanderDamageLayoutForBottomPlayerFollowsVerticalBoardOrder() {
        try (FragmentScenario<MtgLifeFragment> scenario =
                FragmentScenario.launchInContainer(MtgLifeFragment.class, null, R.style.AppTheme)) {

            scenario.onFragment(
                    fragment -> {
                        startLifeGame(fragment, "2", "40");
                    });

            scenario.onFragment(
                    fragment -> {
                        android.widget.LinearLayout dialogContent =
                                showCommanderDamageDialog(fragment, 1);

                        assertEquals(2, dialogContent.getChildCount());

                        android.widget.LinearLayout row0 =
                                (android.widget.LinearLayout) dialogContent.getChildAt(0);
                        android.widget.LinearLayout row1 =
                                (android.widget.LinearLayout) dialogContent.getChildAt(1);

                        assertEquals(1, row0.getChildCount());
                        assertEquals(1, row1.getChildCount());
                        assertCommanderCellContainsSource(fragment, row0.getChildAt(0), 1, 2);
                        assertEquals(View.INVISIBLE, row1.getChildAt(0).getVisibility());
                    });
        }
    }

    @Test
    public void testThreePlayerCommanderDamageLayoutForPlayer1FollowsBoardOrder() {
        try (FragmentScenario<MtgLifeFragment> scenario =
                FragmentScenario.launchInContainer(MtgLifeFragment.class, null, R.style.AppTheme)) {

            scenario.onFragment(
                    fragment -> {
                        startLifeGame(fragment, "3", "40");
                    });

            scenario.onFragment(
                    fragment -> {
                        android.widget.LinearLayout dialogContent =
                                showCommanderDamageDialog(fragment, 0);

                        android.widget.LinearLayout row0 =
                                (android.widget.LinearLayout) dialogContent.getChildAt(0);
                        android.widget.LinearLayout row1 =
                                (android.widget.LinearLayout) dialogContent.getChildAt(1);
                        View topLeftCell = row0.getChildAt(0);
                        View topRightCell = row0.getChildAt(1);

                        assertEquals(2, dialogContent.getChildCount());
                        assertCommanderCellContainsSource(fragment, topLeftCell, 2, 1);
                        assertCommanderCellContainsSource(fragment, topRightCell, 3, 1);
                        assertEquals(View.INVISIBLE, row1.getChildAt(0).getVisibility());
                        assertEquals(View.INVISIBLE, row1.getChildAt(1).getVisibility());
                    });
        }
    }

    @Test
    public void testLongPressLifeZonesAdjustByTen() {
        try (FragmentScenario<MtgLifeFragment> scenario =
                FragmentScenario.launchInContainer(MtgLifeFragment.class, null, R.style.AppTheme)) {

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
                        TextView negativeChange1 =
                                player1.findViewById(R.id.tv_recent_life_change_negative);
                        TextView positiveChange1 =
                                player1.findViewById(R.id.tv_recent_life_change_positive);
                        TextView negativeChange2 =
                                player2.findViewById(R.id.tv_recent_life_change_negative);
                        TextView positiveChange2 =
                                player2.findViewById(R.id.tv_recent_life_change_positive);
                        assertEquals("50", life1.getText().toString());
                        assertEquals("30", life2.getText().toString());
                        assertEquals(View.GONE, negativeChange1.getVisibility());
                        assertEquals("+10", positiveChange1.getText().toString());
                        assertEquals("-10", negativeChange2.getText().toString());
                        assertEquals(View.GONE, positiveChange2.getVisibility());
                    });
        }
    }

    @Test
    public void testRecentLifeChangeDoesNotRestartAnimationForSameTimestamp() {
        try (FragmentScenario<MtgLifeFragment> scenario =
                FragmentScenario.launchInContainer(MtgLifeFragment.class, null, R.style.AppTheme)) {

            scenario.onFragment(
                    fragment -> {
                        View player1 = fragment.requireView().findViewById(R.id.player_1);
                        assertNotNull(player1);

                        View incrementZone = player1.findViewById(R.id.life_increment_zone);
                        TextView recentChange =
                                player1.findViewById(R.id.tv_recent_life_change_positive);
                        assertNotNull(incrementZone);
                        assertNotNull(recentChange);

                        incrementZone.performClick();

                        Object tag = recentChange.getTag();
                        assertTrue(tag instanceof Long);
                        long timestamp = (Long) tag;

                        recentChange.setAlpha(0.75f);
                        recentChange.setTranslationX(5f);
                        recentChange.setTranslationY(7f);

                        LifePlayerCellBinding cellBinding = LifePlayerCellBinding.bind(player1);
                        try {
                            java.lang.reflect.Method bindMethod =
                                    MtgLifeFragment.class.getDeclaredMethod(
                                            "bindRecentLifeChange",
                                            LifePlayerCellBinding.class,
                                            int.class,
                                            long.class,
                                            int.class);
                            bindMethod.setAccessible(true);
                            bindMethod.invoke(
                                    fragment,
                                    cellBinding,
                                    1,
                                    timestamp,
                                    recentChange.getCurrentTextColor());
                        } catch (ReflectiveOperationException e) {
                            throw new AssertionError(e);
                        }

                        assertEquals(0.75f, recentChange.getAlpha(), 0f);
                        assertEquals(5f, recentChange.getTranslationX(), 0f);
                        assertEquals(7f, recentChange.getTranslationY(), 0f);
                    });
        }
    }

    @Test
    public void testFivePlayerCommanderDamageLayoutForPlayer5KeepsBoardSeatSequence() {
        try (FragmentScenario<MtgLifeFragment> scenario =
                FragmentScenario.launchInContainer(MtgLifeFragment.class, null, R.style.AppTheme)) {

            scenario.onFragment(
                    fragment -> {
                        startLifeGame(fragment, "5", "40");
                    });

            scenario.onFragment(
                    fragment -> {
                        android.widget.LinearLayout dialogContent =
                                showCommanderDamageDialog(fragment, 4);

                        assertEquals(3, dialogContent.getChildCount());

                        android.widget.LinearLayout row0 =
                                (android.widget.LinearLayout) dialogContent.getChildAt(0);
                        View cell0 = row0.getChildAt(0);
                        View cell1 = row0.getChildAt(1);

                        assertEquals(View.VISIBLE, cell0.getVisibility());
                        assertEquals(View.VISIBLE, cell1.getVisibility());
                        assertEquals(2, row0.getChildCount());

                        assertCommanderCellContainsSource(fragment, cell0, 1, 5);
                        assertCommanderCellContainsSource(fragment, cell1, 2, 5);

                        android.widget.LinearLayout row1 =
                                (android.widget.LinearLayout) dialogContent.getChildAt(1);
                        View cell3 = row1.getChildAt(0);
                        View cell4 = row1.getChildAt(1);

                        assertEquals(View.VISIBLE, cell3.getVisibility());
                        assertEquals(View.VISIBLE, cell4.getVisibility());
                        assertEquals(2, row1.getChildCount());

                        assertCommanderCellContainsSource(fragment, cell3, 3, 5);
                        assertCommanderCellContainsSource(fragment, cell4, 4, 5);

                        android.widget.LinearLayout row2 =
                                (android.widget.LinearLayout) dialogContent.getChildAt(2);
                        assertEquals(2, row2.getChildCount());
                        assertEquals(View.INVISIBLE, row2.getChildAt(0).getVisibility());
                        assertEquals(View.INVISIBLE, row2.getChildAt(1).getVisibility());
                    });
        }
    }

    @Test
    public void testHoldingLifeZoneRepeatsTenPointChangeEveryOnePointFiveSeconds() {
        try (FragmentScenario<MtgLifeFragment> scenario =
                FragmentScenario.launchInContainer(MtgLifeFragment.class, null, R.style.AppTheme)) {

            scenario.onFragment(
                    fragment -> {
                        View player1 = fragment.requireView().findViewById(R.id.player_1);
                        assertNotNull(player1);

                        View incrementZone = player1.findViewById(R.id.life_increment_zone);
                        assertNotNull(incrementZone);

                        incrementZone.setPressed(true);
                        incrementZone.performLongClick();
                        Shadows.shadowOf(Looper.getMainLooper())
                                .idleFor(1500, TimeUnit.MILLISECONDS);
                        incrementZone.setPressed(false);

                        MotionEvent cancelEvent =
                                MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_CANCEL, 0f, 0f, 0);
                        incrementZone.dispatchTouchEvent(cancelEvent);
                        cancelEvent.recycle();
                    });

            scenario.onFragment(
                    fragment -> {
                        View player1 = fragment.requireView().findViewById(R.id.player_1);
                        assertNotNull(player1);

                        TextView life1 = player1.findViewById(R.id.tv_life_count);
                        TextView recentChange1 =
                                player1.findViewById(R.id.tv_recent_life_change_positive);
                        assertEquals("60", life1.getText().toString());
                        assertEquals("+20", recentChange1.getText().toString());
                    });
        }
    }

    @Test
    public void testThreeDigitLifeDisplayWorks() {
        try (FragmentScenario<MtgLifeFragment> scenario =
                FragmentScenario.launchInContainer(MtgLifeFragment.class, null, R.style.AppTheme)) {

            scenario.onFragment(
                    fragment -> {
                        org.robolectric.fakes.RoboMenuItem resetMenuItem =
                                new org.robolectric.fakes.RoboMenuItem(R.id.action_new_game);
                        fragment.onMenuItemSelected(resetMenuItem);

                        View view = fragment.getView();
                        assertNotNull(view);
                        EditText playersInput = view.findViewById(R.id.players_input);
                        EditText lifeInput = view.findViewById(R.id.life_input);
                        playersInput.setText("4");
                        lifeInput.setText("100");

                        Button startButton = view.findViewById(R.id.start_game_button);
                        startButton.performClick();
                    });

            scenario.onFragment(
                    fragment -> {
                        View view = fragment.getView();
                        assertNotNull(view);
                        View player1 = view.findViewById(R.id.player_1);
                        assertNotNull(player1);

                        TextView life1 = player1.findViewById(R.id.tv_life_count);
                        assertEquals("100", life1.getText().toString());
                    });
        }
    }

    @Test
    public void testSixPlayerLayoutIncrementShowsVisiblePositiveDelta() {
        try (FragmentScenario<MtgLifeFragment> scenario =
                FragmentScenario.launchInContainer(MtgLifeFragment.class, null, R.style.AppTheme)) {

            scenario.onFragment(
                    fragment -> {
                        org.robolectric.fakes.RoboMenuItem resetMenuItem =
                                new org.robolectric.fakes.RoboMenuItem(R.id.action_new_game);
                        fragment.onMenuItemSelected(resetMenuItem);

                        View view = fragment.getView();
                        assertNotNull(view);
                        EditText playersInput = view.findViewById(R.id.players_input);
                        EditText lifeInput = view.findViewById(R.id.life_input);
                        assertNotNull(playersInput);
                        assertNotNull(lifeInput);
                        playersInput.setText("6");
                        lifeInput.setText("40");

                        Button startButton = view.findViewById(R.id.start_game_button);
                        assertNotNull(startButton);
                        startButton.performClick();
                    });

            scenario.onFragment(
                    fragment -> {
                        View player6 = fragment.requireView().findViewById(R.id.player_6);
                        assertNotNull(player6);

                        View incrementZone = player6.findViewById(R.id.life_increment_zone);
                        assertNotNull(incrementZone);
                        incrementZone.performClick();
                    });

            scenario.onFragment(
                    fragment -> {
                        View player6 = fragment.requireView().findViewById(R.id.player_6);
                        assertNotNull(player6);

                        TextView life6 = player6.findViewById(R.id.tv_life_count);
                        TextView positiveDelta =
                                player6.findViewById(R.id.tv_recent_life_change_positive);
                        assertNotNull(life6);
                        assertNotNull(positiveDelta);
                        assertEquals("41", life6.getText().toString());
                        assertEquals(View.VISIBLE, positiveDelta.getVisibility());
                        assertEquals("+1", positiveDelta.getText().toString());
                        Rect visibleRect = new Rect();
                        assertTrue(positiveDelta.getGlobalVisibleRect(visibleRect));
                        assertTrue(visibleRect.width() > 0);
                        assertTrue(visibleRect.height() > 0);
                    });
        }
    }

    @Test
    public void testTurnTimerResetInPreviewBoardOnReturnToSetup() {
        try (FragmentScenario<MtgLifeFragment> scenario =
                FragmentScenario.launchInContainer(MtgLifeFragment.class, null, R.style.AppTheme)) {

            scenario.onFragment(
                    fragment -> {
                        org.robolectric.fakes.RoboMenuItem resetMenuItem =
                                new org.robolectric.fakes.RoboMenuItem(R.id.action_new_game);
                        fragment.onMenuItemSelected(resetMenuItem);

                        View view = fragment.getView();
                        assertNotNull(view);

                        android.widget.CompoundButton timerSwitch =
                                view.findViewById(R.id.turn_timer_switch);
                        assertNotNull(timerSwitch);
                        timerSwitch.setChecked(true);

                        Button startButton = view.findViewById(R.id.start_game_button);
                        assertNotNull(startButton);
                        startButton.performClick();
                    });

            scenario.onFragment(
                    fragment -> {
                        View view = fragment.getView();
                        assertNotNull(view);

                        View player1 = view.findViewById(R.id.player_1);
                        assertNotNull(player1);
                        View timerContainer = player1.findViewById(R.id.timer_container);
                        assertNotNull(timerContainer);
                        assertEquals(View.VISIBLE, timerContainer.getVisibility());
                    });

            scenario.onFragment(
                    fragment -> {
                        org.robolectric.fakes.RoboMenuItem resetMenuItem =
                                new org.robolectric.fakes.RoboMenuItem(R.id.action_new_game);
                        fragment.onMenuItemSelected(resetMenuItem);
                    });

            scenario.onFragment(
                    fragment -> {
                        View view = fragment.getView();
                        assertNotNull(view);

                        View player1 = view.findViewById(R.id.player_1);
                        assertNotNull(player1);
                        View timerContainer = player1.findViewById(R.id.timer_container);
                        assertNotNull(timerContainer);
                        assertEquals(View.GONE, timerContainer.getVisibility());
                    });
        }
    }

    @Test
    public void testActivePlayerTimerDistinguisher() {
        try (FragmentScenario<MtgLifeFragment> scenario =
                FragmentScenario.launchInContainer(MtgLifeFragment.class, null, R.style.AppTheme)) {

            scenario.onFragment(
                    fragment -> {
                        org.robolectric.fakes.RoboMenuItem resetMenuItem =
                                new org.robolectric.fakes.RoboMenuItem(R.id.action_new_game);
                        fragment.onMenuItemSelected(resetMenuItem);

                        View view = fragment.getView();
                        assertNotNull(view);

                        android.widget.CompoundButton timerSwitch =
                                view.findViewById(R.id.turn_timer_switch);
                        assertNotNull(timerSwitch);
                        timerSwitch.setChecked(true);

                        Button startButton = view.findViewById(R.id.start_game_button);
                        assertNotNull(startButton);
                        startButton.performClick();
                    });

            scenario.onFragment(
                    fragment -> {
                        View view = fragment.getView();
                        assertNotNull(view);

                        View player1 = view.findViewById(R.id.player_1);
                        assertNotNull(player1);
                        View timerContainer1 = player1.findViewById(R.id.timer_container);
                        assertNotNull(timerContainer1);
                        float elevation1 = timerContainer1.getElevation();

                        View player2 = view.findViewById(R.id.player_2);
                        assertNotNull(player2);
                        View timerContainer2 = player2.findViewById(R.id.timer_container);
                        assertNotNull(timerContainer2);
                        float elevation2 = timerContainer2.getElevation();

                        assertTrue(
                                "Active player's timer container should have higher elevation than inactive",
                                elevation1 > elevation2);
                    });
        }
    }

    @Test
    public void testChooseAndStartButtonNavigatesToChooser() {
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(
                    activity -> {
                        Fragment fragment =
                                activity.getSupportFragmentManager()
                                        .findFragmentById(R.id.nav_host_fragment);
                        assertNotNull(fragment);
                        NavController navController =
                                ((NavHostFragment) fragment).getNavController();

                        // Navigate to setup content first
                        Fragment currentFragment =
                                fragment.getChildFragmentManager().getFragments().get(0);
                        assertTrue(currentFragment instanceof MtgLifeFragment);
                        MtgLifeFragment mtgLifeFragment = (MtgLifeFragment) currentFragment;

                        org.robolectric.fakes.RoboMenuItem newGameItem =
                                new org.robolectric.fakes.RoboMenuItem(R.id.action_new_game);
                        mtgLifeFragment.onMenuItemSelected(newGameItem);

                        View view = mtgLifeFragment.requireView();
                        View setupContent = view.findViewById(R.id.setup_content);
                        assertNotNull(setupContent);
                        assertEquals(View.VISIBLE, setupContent.getVisibility());

                        // Fill details
                        EditText playersInput = view.findViewById(R.id.players_input);
                        EditText lifeInput = view.findViewById(R.id.life_input);
                        playersInput.setText("4");
                        lifeInput.setText("40");

                        // Press Choose 1st & Start button
                        Button chooseAndStartButton =
                                view.findViewById(R.id.choose_and_start_button);
                        assertNotNull(chooseAndStartButton);
                        chooseAndStartButton.performClick();

                        // Assert we navigated to nav_chooser
                        assertEquals(
                                R.id.nav_chooser, navController.getCurrentDestination().getId());
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

    private static void startLifeGame(MtgLifeFragment fragment, String players, String life) {
        org.robolectric.fakes.RoboMenuItem resetMenuItem =
                new org.robolectric.fakes.RoboMenuItem(R.id.action_new_game);
        fragment.onMenuItemSelected(resetMenuItem);

        View view = fragment.getView();
        assertNotNull(view);
        EditText playersInput = view.findViewById(R.id.players_input);
        EditText lifeInput = view.findViewById(R.id.life_input);
        assertNotNull(playersInput);
        assertNotNull(lifeInput);
        playersInput.setText(players);
        lifeInput.setText(life);

        Button startButton = view.findViewById(R.id.start_game_button);
        assertNotNull(startButton);
        startButton.performClick();
    }

    private static android.widget.LinearLayout showCommanderDamageDialog(
            MtgLifeFragment fragment, int defenderSeatIndex) {
        try {
            java.lang.reflect.Method showDialogMethod =
                    MtgLifeFragment.class.getDeclaredMethod("showCommanderDamageDialog", int.class);
            showDialogMethod.setAccessible(true);
            showDialogMethod.invoke(fragment, defenderSeatIndex);
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
        return dialogContent;
    }

    private static void assertCommanderCellContainsSource(
            MtgLifeFragment fragment, View cell, int sourcePlayerNumber, int defenderPlayerNumber) {
        View incrementZone =
                findViewWithContentDescription(
                        cell,
                        fragment.getString(
                                R.string.mtg_commander_damage_increase_desc,
                                sourcePlayerNumber,
                                defenderPlayerNumber));
        assertNotNull(incrementZone);
    }

    @Test
    public void testCoachMarkDismissedOnDestroyView() {
        try (FragmentScenario<MtgLifeFragment> scenario =
                FragmentScenario.launchInContainer(MtgLifeFragment.class, null, R.style.AppTheme)) {
            scenario.onFragment(
                    fragment -> {
                        View anchor = new View(fragment.requireContext());
                        try {
                            java.lang.reflect.Method showCoachMarkMethod =
                                    MtgLifeFragment.class.getDeclaredMethod("showCoachMark", View.class);
                            showCoachMarkMethod.setAccessible(true);
                            showCoachMarkMethod.invoke(fragment, anchor);

                            java.lang.reflect.Field coachMarkField =
                                    MtgLifeFragment.class.getDeclaredField("coachMarkPopup");
                            coachMarkField.setAccessible(true);
                            android.widget.PopupWindow popup = (android.widget.PopupWindow) coachMarkField.get(fragment);

                            assertNotNull(popup);
                            assertTrue(popup.isShowing());

                            fragment.onDestroyView();

                            org.junit.Assert.assertNull(coachMarkField.get(fragment));
                            org.junit.Assert.assertFalse(popup.isShowing());
                        } catch (ReflectiveOperationException e) {
                            throw new AssertionError(e);
                        }
                    });
        }
    }

    @Test
    public void testCoachMarkAccessibilityAndInteraction() {
        try (FragmentScenario<MtgLifeFragment> scenario =
                FragmentScenario.launchInContainer(MtgLifeFragment.class, null, R.style.AppTheme)) {
            scenario.onFragment(
                    fragment -> {
                        View anchor = new View(fragment.requireContext());
                        boolean[] anchorClicked = {false};
                        anchor.setOnClickListener(v -> anchorClicked[0] = true);

                        try {
                            java.lang.reflect.Method showCoachMarkMethod =
                                    MtgLifeFragment.class.getDeclaredMethod("showCoachMark", View.class);
                            showCoachMarkMethod.setAccessible(true);
                            showCoachMarkMethod.invoke(fragment, anchor);

                            java.lang.reflect.Field coachMarkField =
                                    MtgLifeFragment.class.getDeclaredField("coachMarkPopup");
                            coachMarkField.setAccessible(true);
                            android.widget.PopupWindow popup = (android.widget.PopupWindow) coachMarkField.get(fragment);

                            assertNotNull(popup);
                            assertTrue(popup.isShowing());
                            assertTrue(popup.isOutsideTouchable()); // Accessibility: can be dismissed by touching outside

                            View popupContentView = popup.getContentView();
                            assertNotNull(popupContentView);

                            // Simulate click on the coach mark
                            popupContentView.performClick();

                            // Verify anchor was clicked as a result
                            assertTrue(anchorClicked[0]);

                            // Verify popup was dismissed
                            org.junit.Assert.assertFalse(popup.isShowing());

                            // Verify ViewModel received the dismiss signal
                            java.lang.reflect.Field viewModelField =
                                    MtgLifeFragment.class.getDeclaredField("viewModel");
                            viewModelField.setAccessible(true);
                            MtgLifeViewModel viewModel = (MtgLifeViewModel) viewModelField.get(fragment);
                            assertTrue(viewModel.isSetupCoachMarkDismissed());

                            // Verify reference is cleared via dismiss listener
                            Shadows.shadowOf(Looper.getMainLooper()).idle();
                            org.junit.Assert.assertNull(coachMarkField.get(fragment));

                        } catch (ReflectiveOperationException e) {
                            throw new AssertionError(e);
                        }
                    });
        }
    }
}
