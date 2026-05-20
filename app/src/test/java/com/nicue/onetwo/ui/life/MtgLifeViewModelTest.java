package com.nicue.onetwo.ui.life;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.SavedStateHandle;
import com.nicue.onetwo.LiveDataTestUtil;
import com.nicue.onetwo.R;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class MtgLifeViewModelTest {
    private static final class FakeNowProvider implements MtgLifeViewModel.NowProvider {
        private long nowMs = 1L;

        @Override
        public long now() {
            return nowMs;
        }

        void advanceBy(long deltaMs) {
            nowMs += deltaMs;
        }
    }

    @Rule public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private MtgLifeViewModel viewModel;
    private FakeNowProvider nowProvider;

    @Before
    public void setUp() {
        nowProvider = new FakeNowProvider();
        viewModel = new MtgLifeViewModel(new SavedStateHandle(), nowProvider);
    }

    @Test
    public void testDefaultSetupState() throws Exception {
        MtgLifeUiState state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertFalse(state.isShowingSetup());
        assertEquals(4, state.getPlayerCount());
        assertEquals(40, state.getStartingLife());
        assertNull(state.getPlayersErrorResId());
        assertNull(state.getLifeErrorResId());
        assertEquals(4, state.getPlayers().size());
    }

    @Test
    public void testSuccessfulValidationAndGameStart() throws Exception {
        viewModel.validateAndStartGame("2", "20");
        MtgLifeUiState state = LiveDataTestUtil.getValue(viewModel.getUiState());

        assertFalse(state.isShowingSetup());
        assertEquals(2, state.getPlayerCount());
        assertEquals(20, state.getStartingLife());
        assertNull(state.getPlayersErrorResId());
        assertNull(state.getLifeErrorResId());

        assertEquals(2, state.getPlayers().size());
        assertEquals(0, state.getPlayers().get(0).getSeatIndex());
        assertEquals(20, state.getPlayers().get(0).getLifeTotal());
        assertEquals(0, state.getPlayers().get(0).getRecentLifeChange());
        assertEquals(1, state.getPlayers().get(1).getSeatIndex());
        assertEquals(20, state.getPlayers().get(1).getLifeTotal());
        assertEquals(0, state.getPlayers().get(1).getRecentLifeChange());
    }

    @Test
    public void testPlayerCountValidationError() throws Exception {
        // Less than 1 player
        viewModel.validateAndStartGame("0", "20");
        MtgLifeUiState state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertTrue(state.isShowingSetup());
        assertEquals(R.string.mtg_setup_players_error, (int) state.getPlayersErrorResId());
        assertNull(state.getLifeErrorResId());

        // More than 6 players
        viewModel.validateAndStartGame("7", "20");
        state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertTrue(state.isShowingSetup());
        assertEquals(R.string.mtg_setup_players_error, (int) state.getPlayersErrorResId());
        assertNull(state.getLifeErrorResId());

        // Noninteger player count
        viewModel.validateAndStartGame("abc", "20");
        state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertTrue(state.isShowingSetup());
        assertEquals(R.string.mtg_setup_players_error, (int) state.getPlayersErrorResId());
        assertNull(state.getLifeErrorResId());
    }

    @Test
    public void testLifeTotalValidationError() throws Exception {
        // Less than or equal to 0 life
        viewModel.validateAndStartGame("4", "0");
        MtgLifeUiState state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertTrue(state.isShowingSetup());
        assertNull(state.getPlayersErrorResId());
        assertEquals(R.string.mtg_setup_life_error, (int) state.getLifeErrorResId());

        // Negative life
        viewModel.validateAndStartGame("4", "-5");
        state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertTrue(state.isShowingSetup());
        assertNull(state.getPlayersErrorResId());
        assertEquals(R.string.mtg_setup_life_error, (int) state.getLifeErrorResId());

        // Non-integer life
        viewModel.validateAndStartGame("4", "xyz");
        state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertTrue(state.isShowingSetup());
        assertNull(state.getPlayersErrorResId());
        assertEquals(R.string.mtg_setup_life_error, (int) state.getLifeErrorResId());
    }

    @Test
    public void testBothFieldsValidationError() throws Exception {
        viewModel.validateAndStartGame("10", "0");
        MtgLifeUiState state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertTrue(state.isShowingSetup());
        assertEquals(R.string.mtg_setup_players_error, (int) state.getPlayersErrorResId());
        assertEquals(R.string.mtg_setup_life_error, (int) state.getLifeErrorResId());
    }

    @Test
    public void testResetClearsErrorsAndShowsSetup() throws Exception {
        // Generate validation errors first
        viewModel.validateAndStartGame("10", "0");
        MtgLifeUiState state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertEquals(R.string.mtg_setup_players_error, (int) state.getPlayersErrorResId());
        assertEquals(R.string.mtg_setup_life_error, (int) state.getLifeErrorResId());

        // Start game successfully
        viewModel.validateAndStartGame("2", "20");
        state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertFalse(state.isShowingSetup());

        // Reset to setup
        viewModel.resetToSetup();
        state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertTrue(state.isShowingSetup());
        assertNull(state.getPlayersErrorResId());
        assertNull(state.getLifeErrorResId());
    }

    @Test
    public void testIncrementAndDecrementLife() throws Exception {
        viewModel.validateAndStartGame("2", "20");
        MtgLifeUiState state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertEquals(20, state.getPlayers().get(0).getLifeTotal());
        assertEquals(20, state.getPlayers().get(1).getLifeTotal());

        // Increment player 0 life
        viewModel.incrementLife(0);
        state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertEquals(21, state.getPlayers().get(0).getLifeTotal());
        assertEquals(20, state.getPlayers().get(1).getLifeTotal());
        assertEquals(1, state.getPlayers().get(0).getRecentLifeChange());

        // Decrement player 1 life
        viewModel.decrementLife(1);
        state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertEquals(21, state.getPlayers().get(0).getLifeTotal());
        assertEquals(19, state.getPlayers().get(1).getLifeTotal());
        assertEquals(-1, state.getPlayers().get(1).getRecentLifeChange());
    }

    @Test
    public void testRecentLifeChangeTracksLongPressDelta() throws Exception {
        viewModel.validateAndStartGame("2", "20");

        viewModel.incrementLifeBy(0, 10);
        MtgLifeUiState state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertEquals(30, state.getPlayers().get(0).getLifeTotal());
        assertEquals(10, state.getPlayers().get(0).getRecentLifeChange());

        viewModel.decrementLifeBy(1, 10);
        state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertEquals(10, state.getPlayers().get(1).getLifeTotal());
        assertEquals(-10, state.getPlayers().get(1).getRecentLifeChange());
    }

    @Test
    public void testRecentLifeChangeAggregatesWithinWindowAndResetsAfterTimeout() throws Exception {
        viewModel.validateAndStartGame("2", "20");

        viewModel.decrementLife(0);
        MtgLifeUiState state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertEquals(-1, state.getPlayers().get(0).getRecentLifeChange());

        nowProvider.advanceBy(MtgLifeViewModel.RECENT_LIFE_CHANGE_WINDOW_MS - 1);
        viewModel.decrementLife(0);
        state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertEquals(-2, state.getPlayers().get(0).getRecentLifeChange());

        nowProvider.advanceBy(MtgLifeViewModel.RECENT_LIFE_CHANGE_WINDOW_MS + 1);
        viewModel.decrementLife(0);
        state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertEquals(-1, state.getPlayers().get(0).getRecentLifeChange());
    }

    @Test
    public void testRotationsForVaryingPlayerCounts() throws Exception {
        // 3 Players
        viewModel.validateAndStartGame("3", "40");
        MtgLifeUiState state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertEquals(0, state.getPlayers().get(0).getRotationDegrees());
        assertEquals(90, state.getPlayers().get(1).getRotationDegrees());
        assertEquals(270, state.getPlayers().get(2).getRotationDegrees());

        // 4 Players (Lateral orientation matching the user specifications)
        viewModel.validateAndStartGame("4", "40");
        state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertEquals(90, state.getPlayers().get(0).getRotationDegrees());
        assertEquals(270, state.getPlayers().get(1).getRotationDegrees());
        assertEquals(90, state.getPlayers().get(2).getRotationDegrees());
        assertEquals(270, state.getPlayers().get(3).getRotationDegrees());

        // 5 Players (4-player lateral grid + full width bottom 0-degree player)
        viewModel.validateAndStartGame("5", "40");
        state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertEquals(90, state.getPlayers().get(0).getRotationDegrees());
        assertEquals(270, state.getPlayers().get(1).getRotationDegrees());
        assertEquals(90, state.getPlayers().get(2).getRotationDegrees());
        assertEquals(270, state.getPlayers().get(3).getRotationDegrees());
        assertEquals(0, state.getPlayers().get(4).getRotationDegrees());

        // 6 Players (2 wide, 3 tall lateral split grid)
        viewModel.validateAndStartGame("6", "40");
        state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertEquals(90, state.getPlayers().get(0).getRotationDegrees());
        assertEquals(270, state.getPlayers().get(1).getRotationDegrees());
        assertEquals(90, state.getPlayers().get(2).getRotationDegrees());
        assertEquals(270, state.getPlayers().get(3).getRotationDegrees());
        assertEquals(90, state.getPlayers().get(4).getRotationDegrees());
        assertEquals(270, state.getPlayers().get(5).getRotationDegrees());
    }

    @Test
    public void testDefaultCommanderDamageState() throws Exception {
        MtgLifeUiState state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertTrue(state.isCommanderDamageEnabled());

        viewModel.validateAndStartGame("2", "40", true);
        state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertTrue(state.isCommanderDamageEnabled());

        // Check that player 0 has 2 commander damage items (for player 0 and player 1)
        List<CommanderDamageUiModel> damages = state.getPlayers().getFirst().getCommanderDamages();
        assertEquals(2, damages.size());

        // Self damage should be self = true, amount = 0
        assertTrue(damages.get(0).isSelf());
        assertEquals(0, damages.get(0).getAmount());

        // Opponent damage should be self = false, amount = 0
        assertFalse(damages.get(1).isSelf());
        assertEquals(0, damages.get(1).getAmount());
    }

    @Test
    public void testCommanderDamageIncrementAndDecrement() throws Exception {
        viewModel.validateAndStartGame("2", "40", true);

        // Increment player 0 damage from source player 1
        viewModel.incrementCommanderDamage(0, 1);
        MtgLifeUiState state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertEquals(1, state.getPlayers().getFirst().getCommanderDamages().get(1).getAmount());
        assertEquals(39, state.getPlayers().getFirst().getLifeTotal());
        assertEquals(40, state.getPlayers().get(1).getLifeTotal());
        assertEquals(-1, state.getPlayers().getFirst().getRecentLifeChange());

        // Self damage changes should be ignored
        viewModel.incrementCommanderDamage(0, 0);
        state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertEquals(0, state.getPlayers().getFirst().getCommanderDamages().getFirst().getAmount());
        assertEquals(39, state.getPlayers().getFirst().getLifeTotal());
        assertEquals(-1, state.getPlayers().getFirst().getRecentLifeChange());

        // Decrement player 0 damage from source player 1
        viewModel.decrementCommanderDamage(0, 1);
        state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertEquals(0, state.getPlayers().getFirst().getCommanderDamages().get(1).getAmount());
        assertEquals(40, state.getPlayers().getFirst().getLifeTotal());
        assertEquals(0, state.getPlayers().getFirst().getRecentLifeChange());

        // Decrement below 0 floor should be ignored
        viewModel.decrementCommanderDamage(0, 1);
        state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertEquals(0, state.getPlayers().getFirst().getCommanderDamages().get(1).getAmount());
        assertEquals(40, state.getPlayers().getFirst().getLifeTotal());
        assertEquals(0, state.getPlayers().getFirst().getRecentLifeChange());
    }

    @Test
    public void testCommanderDamageVisibilityByPlayerCount() throws Exception {
        // Player count = 1: strip must be hidden even if enabled
        viewModel.validateAndStartGame("1", "40", true);
        MtgLifeUiState state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertFalse(state.getPlayers().getFirst().isCommanderDamageVisible());

        // Player count = 2: strip should be visible
        viewModel.validateAndStartGame("2", "40", true);
        state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertTrue(state.getPlayers().getFirst().isCommanderDamageVisible());

        // If explicitly disabled: strip should be hidden
        viewModel.validateAndStartGame("2", "40", false);
        state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertFalse(state.getPlayers().getFirst().isCommanderDamageVisible());
    }

    @Test
    public void testLethalCommanderDamageTreatment() throws Exception {
        viewModel.validateAndStartGame("2", "40", true);

        // Increment to 20
        for (int i = 0; i < 20; i++) {
            viewModel.incrementCommanderDamage(0, 1);
        }
        MtgLifeUiState state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertFalse(state.getPlayers().getFirst().getCommanderDamages().get(1).isLethal());

        // 21st point -> lethal
        viewModel.incrementCommanderDamage(0, 1);
        state = LiveDataTestUtil.getValue(viewModel.getUiState());
        CommanderDamageUiModel model = state.getPlayers().getFirst().getCommanderDamages().get(1);
        assertTrue(model.isLethal());
        assertEquals(R.color.secondAccent, model.getBackgroundColorRes());
        assertEquals(android.R.color.white, model.getForegroundColorRes());
    }

    @Test
    public void testNewGamePreservesToggle() throws Exception {
        // Disable commander damage and start game
        viewModel.validateAndStartGame("3", "40", false);
        MtgLifeUiState state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertFalse(state.isCommanderDamageEnabled());

        // Reset to setup
        viewModel.resetToSetup();
        state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertTrue(state.isShowingSetup());
        assertFalse(state.isCommanderDamageEnabled());
    }

    private static final class FakeTimerScheduler implements com.nicue.onetwo.core.TimerScheduler {
        private TickListener listener;
        private final FakeNowProvider nowProvider;

        FakeTimerScheduler(FakeNowProvider nowProvider) {
            this.nowProvider = nowProvider;
        }

        @Override
        public void start(TickListener listener) {
            this.listener = listener;
        }

        @Override
        public void stop() {
            this.listener = null;
        }

        @Override
        public long now() {
            return nowProvider.now();
        }

        void triggerTick() {
            if (listener != null) {
                listener.onTick(nowProvider.now());
            }
        }
    }

    @Test
    public void testDefaultTurnTimerState() throws Exception {
        MtgLifeUiState state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertFalse(viewModel.getTurnTimerEnabled());
        assertEquals(300000L, viewModel.getTurnTimerDurationMs());
        assertFalse(state.isTurnTimerEnabled());
        assertTrue(state.isTurnTimerPaused());
        assertFalse(state.isTurnTimerFinished());
    }

    @Test
    public void testTurnTimerSettingsState() throws Exception {
        viewModel.setTurnTimerEnabled(true);
        viewModel.setTurnTimerDurationMs(180000L);
        MtgLifeUiState state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertTrue(viewModel.getTurnTimerEnabled());
        assertEquals(180000L, viewModel.getTurnTimerDurationMs());
        assertTrue(state.isTurnTimerEnabled());
    }

    @Test
    public void testZeroDurationTurnTimerStartsFinishedAndDisablesPass() throws Exception {
        viewModel.setTurnTimerDurationMs(0L);
        viewModel.validateAndStartGame("2", "40", true, true);

        MtgLifeUiState state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertTrue(state.isTurnTimerEnabled());
        assertTrue(state.isTurnTimerPaused());
        assertTrue(state.isTurnTimerFinished());

        List<LifePlayerUiModel> players = state.getPlayers();
        assertEquals("0:00:00", players.get(0).getTimerDisplay());
        assertTrue(players.get(0).isTimerExpired());
        assertFalse(players.get(0).isPassEnabled());
    }

    @Test
    public void testStartGameWithTurnTimerEnabled() throws Exception {
        viewModel.setTurnTimerDurationMs(180000L);
        viewModel.validateAndStartGame("3", "40", true, true);
        MtgLifeUiState state = LiveDataTestUtil.getValue(viewModel.getUiState());

        assertFalse(state.isShowingSetup());
        assertTrue(state.isTurnTimerEnabled());
        assertTrue(state.isTurnTimerPaused());
        assertFalse(state.isTurnTimerFinished());

        List<LifePlayerUiModel> players = state.getPlayers();
        assertEquals(3, players.size());
        for (int i = 0; i < 3; i++) {
            LifePlayerUiModel p = players.get(i);
            assertTrue(p.isTimerVisible());
            assertEquals("3:00", p.getTimerDisplay());
            assertEquals(i == 0, p.isTimerActive());
            assertFalse(p.isTimerExpired());
            assertEquals(i == 0, p.isPassEnabled());
        }
    }

    @Test
    public void testPassTurnStartsTimerAndAdvancesClockwise() throws Exception {
        FakeTimerScheduler fakeScheduler = new FakeTimerScheduler(nowProvider);
        viewModel = new MtgLifeViewModel(new SavedStateHandle(), nowProvider, fakeScheduler);

        viewModel.setTurnTimerDurationMs(300000L);
        viewModel.validateAndStartGame("4", "40", true, true);

        assertEquals(0, viewModel.getTurnTimerActiveSeatIndex());
        assertTrue(viewModel.getTurnTimerPaused());

        viewModel.passTurn(0);

        assertEquals(1, viewModel.getTurnTimerActiveSeatIndex());
        assertFalse(viewModel.getTurnTimerPaused());
        assertTrue(fakeScheduler.listener != null);

        viewModel.passTurn(1);
        assertEquals(3, viewModel.getTurnTimerActiveSeatIndex());

        viewModel.passTurn(3);
        assertEquals(2, viewModel.getTurnTimerActiveSeatIndex());

        viewModel.passTurn(2);
        assertEquals(0, viewModel.getTurnTimerActiveSeatIndex());
    }

    @Test
    public void testPassFromNonActiveSeatDoesNothing() throws Exception {
        viewModel.validateAndStartGame("3", "40", true, true);
        assertEquals(0, viewModel.getTurnTimerActiveSeatIndex());

        viewModel.passTurn(1);
        assertEquals(0, viewModel.getTurnTimerActiveSeatIndex());
    }

    @Test
    public void testClockwiseOrderForVariousPlayerCounts() throws Exception {
        FakeTimerScheduler fakeScheduler = new FakeTimerScheduler(nowProvider);

        // 2 players: 0 -> 1 -> 0
        viewModel = new MtgLifeViewModel(new SavedStateHandle(), nowProvider, fakeScheduler);
        viewModel.validateAndStartGame("2", "40", true, true);
        assertEquals(0, viewModel.getTurnTimerActiveSeatIndex());
        viewModel.passTurn(0);
        assertEquals(1, viewModel.getTurnTimerActiveSeatIndex());
        viewModel.passTurn(1);
        assertEquals(0, viewModel.getTurnTimerActiveSeatIndex());

        // 3 players: 0 -> 1 -> 2 -> 0
        viewModel = new MtgLifeViewModel(new SavedStateHandle(), nowProvider, fakeScheduler);
        viewModel.validateAndStartGame("3", "40", true, true);
        assertEquals(0, viewModel.getTurnTimerActiveSeatIndex());
        viewModel.passTurn(0);
        assertEquals(1, viewModel.getTurnTimerActiveSeatIndex());
        viewModel.passTurn(1);
        assertEquals(2, viewModel.getTurnTimerActiveSeatIndex());
        viewModel.passTurn(2);
        assertEquals(0, viewModel.getTurnTimerActiveSeatIndex());

        // 5 players: 0 -> 1 -> 3 -> 4 -> 2 -> 0
        viewModel = new MtgLifeViewModel(new SavedStateHandle(), nowProvider, fakeScheduler);
        viewModel.validateAndStartGame("5", "40", true, true);
        assertEquals(0, viewModel.getTurnTimerActiveSeatIndex());
        viewModel.passTurn(0);
        assertEquals(1, viewModel.getTurnTimerActiveSeatIndex());
        viewModel.passTurn(1);
        assertEquals(3, viewModel.getTurnTimerActiveSeatIndex());
        viewModel.passTurn(3);
        assertEquals(4, viewModel.getTurnTimerActiveSeatIndex());
        viewModel.passTurn(4);
        assertEquals(2, viewModel.getTurnTimerActiveSeatIndex());
        viewModel.passTurn(2);
        assertEquals(0, viewModel.getTurnTimerActiveSeatIndex());

        // 6 players: 0 -> 1 -> 3 -> 5 -> 4 -> 2 -> 0
        viewModel = new MtgLifeViewModel(new SavedStateHandle(), nowProvider, fakeScheduler);
        viewModel.validateAndStartGame("6", "40", true, true);
        assertEquals(0, viewModel.getTurnTimerActiveSeatIndex());
        viewModel.passTurn(0);
        assertEquals(1, viewModel.getTurnTimerActiveSeatIndex());
        viewModel.passTurn(1);
        assertEquals(3, viewModel.getTurnTimerActiveSeatIndex());
        viewModel.passTurn(3);
        assertEquals(5, viewModel.getTurnTimerActiveSeatIndex());
        viewModel.passTurn(5);
        assertEquals(4, viewModel.getTurnTimerActiveSeatIndex());
        viewModel.passTurn(4);
        assertEquals(2, viewModel.getTurnTimerActiveSeatIndex());
        viewModel.passTurn(2);
        assertEquals(0, viewModel.getTurnTimerActiveSeatIndex());
    }

    @Test
    public void testTickingReducesActivePlayerRemainingTime() throws Exception {
        FakeTimerScheduler fakeScheduler = new FakeTimerScheduler(nowProvider);
        viewModel = new MtgLifeViewModel(new SavedStateHandle(), nowProvider, fakeScheduler);

        viewModel.setTurnTimerDurationMs(300000L);
        viewModel.validateAndStartGame("2", "40", true, true);

        viewModel.passTurn(0);
        assertEquals(1, viewModel.getTurnTimerActiveSeatIndex());

        nowProvider.advanceBy(5000L);
        fakeScheduler.triggerTick();

        MtgLifeUiState state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertEquals("4:55", state.getPlayers().get(1).getTimerDisplay());
        assertEquals("5:00", state.getPlayers().get(0).getTimerDisplay());
    }

    @Test
    public void testTimerExpirationFlow() throws Exception {
        FakeTimerScheduler fakeScheduler = new FakeTimerScheduler(nowProvider);
        viewModel = new MtgLifeViewModel(new SavedStateHandle(), nowProvider, fakeScheduler);

        viewModel.setTurnTimerDurationMs(10000L);
        viewModel.validateAndStartGame("2", "40", true, true);

        viewModel.passTurn(0);
        assertFalse(viewModel.getTurnTimerPaused());

        nowProvider.advanceBy(12000L);
        fakeScheduler.triggerTick();

        MtgLifeUiState state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertTrue(viewModel.getTurnTimerPaused());
        assertTrue(viewModel.getTurnTimerFinished());
        assertEquals(1, viewModel.getTurnTimerActiveSeatIndex());

        LifePlayerUiModel player1 = state.getPlayers().get(1);
        assertEquals("0:00:00", player1.getTimerDisplay());
        assertTrue(player1.isTimerExpired());
        assertFalse(player1.isPassEnabled());
    }

    @Test
    public void testPassTurnChargesCorrectPlayer() throws Exception {
        FakeTimerScheduler fakeScheduler = new FakeTimerScheduler(nowProvider);
        viewModel = new MtgLifeViewModel(new SavedStateHandle(), nowProvider, fakeScheduler);

        viewModel.setTurnTimerDurationMs(300000L);
        viewModel.validateAndStartGame("2", "40", true, true);

        // Turn timer starts paused for player 0.
        // Pass player 0 -> switches active seat to 1 and starts the timer.
        viewModel.passTurn(0);
        assertEquals(1, viewModel.getTurnTimerActiveSeatIndex());

        // Player 1's turn runs for 5 seconds.
        nowProvider.advanceBy(5000L);

        // Player 1 passes turn -> charges player 1 and switches active seat to 0.
        viewModel.passTurn(1);
        assertEquals(0, viewModel.getTurnTimerActiveSeatIndex());

        MtgLifeUiState state = LiveDataTestUtil.getValue(viewModel.getUiState());
        LifePlayerUiModel player0 = state.getPlayers().get(0);
        LifePlayerUiModel player1 = state.getPlayers().get(1);

        // Player 1 should have been charged 5 seconds (displaying "4:55").
        assertEquals("4:55", player1.getTimerDisplay());
        // Player 0 should not have been charged (displaying "5:00").
        assertEquals("5:00", player0.getTimerDisplay());
    }

    @Test
    public void testValidateAndStartGameReturnsCorrectBoolean() {
        assertTrue(viewModel.validateAndStartGame("4", "40"));
        assertFalse(viewModel.validateAndStartGame("0", "40"));
        assertFalse(viewModel.validateAndStartGame("4", "0"));
        assertFalse(viewModel.validateAndStartGame("abc", "xyz"));
    }

    @Test
    public void testStartingPlayerProgrammatic() {
        assertNull(viewModel.getStartingPlayer());
        viewModel.setStartingPlayer(2);
        assertEquals(Integer.valueOf(2), viewModel.getStartingPlayer());
        viewModel.setStartingPlayer(null);
        assertNull(viewModel.getStartingPlayer());
    }

    @Test
    public void testStartingPlayerUpdatesTurnTimerActiveSeat() {
        viewModel.validateAndStartGame("4", "40", true, true);
        assertEquals(0, viewModel.getTurnTimerActiveSeatIndex());

        viewModel.setStartingPlayer(2);
        assertEquals(Integer.valueOf(2), viewModel.getStartingPlayer());
        assertEquals(2, viewModel.getTurnTimerActiveSeatIndex());
    }

    @Test
    public void testStartingPlayerFromIntentExtraInteger() {
        SavedStateHandle handle = new SavedStateHandle();
        handle.set("starting_player", 1);
        MtgLifeViewModel vm = new MtgLifeViewModel(handle, nowProvider);
        assertEquals(Integer.valueOf(1), vm.getStartingPlayer());
    }

    @Test
    public void testStartingPlayerFromIntentExtraString() {
        SavedStateHandle handle = new SavedStateHandle();
        handle.set("starting_player", "3");
        MtgLifeViewModel vm = new MtgLifeViewModel(handle, nowProvider);
        assertEquals(Integer.valueOf(3), vm.getStartingPlayer());
    }

    @Test
    public void testStartingPlayerFromIntentExtraCamelCase() {
        SavedStateHandle handle = new SavedStateHandle();
        handle.set("startingPlayer", 4);
        MtgLifeViewModel vm = new MtgLifeViewModel(handle, nowProvider);
        assertEquals(Integer.valueOf(4), vm.getStartingPlayer());
    }

    @Test
    public void testStartingPlayerFromIntentExtraInvalid() {
        SavedStateHandle handle = new SavedStateHandle();
        handle.set("starting_player", "not_an_int");
        MtgLifeViewModel vm = new MtgLifeViewModel(handle, nowProvider);
        assertNull(vm.getStartingPlayer());
    }

    @Test
    public void testStartTimerVisibilityFlow() throws Exception {
        FakeTimerScheduler fakeScheduler = new FakeTimerScheduler(nowProvider);
        viewModel = new MtgLifeViewModel(new SavedStateHandle(), nowProvider, fakeScheduler);
        viewModel.setTurnTimerDurationMs(180000L);
        viewModel.validateAndStartGame("2", "40", true, true);

        // Before starting: active player (0) should have start button visible, inactive player (1) should not
        MtgLifeUiState state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertTrue(state.getPlayers().get(0).isStartTimerVisible());
        assertFalse(state.getPlayers().get(1).isStartTimerVisible());

        // Start the timer
        viewModel.startTimer();

        // After starting: active player's start button should be gone
        state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertFalse(state.getPlayers().get(0).isStartTimerVisible());
        assertFalse(state.getPlayers().get(1).isStartTimerVisible());
        assertFalse(state.isTurnTimerPaused());
    }

    @Test
    public void testTogglePlayPause() throws Exception {
        FakeTimerScheduler fakeScheduler = new FakeTimerScheduler(nowProvider);
        viewModel = new MtgLifeViewModel(new SavedStateHandle(), nowProvider, fakeScheduler);
        viewModel.setTurnTimerDurationMs(180000L);
        viewModel.validateAndStartGame("2", "40", true, true);

        MtgLifeUiState state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertTrue(state.isTurnTimerPaused());

        // Toggle to play (start)
        viewModel.togglePlayPause();
        state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertFalse(state.isTurnTimerPaused());

        // Toggle to pause
        viewModel.togglePlayPause();
        state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertTrue(state.isTurnTimerPaused());
    }
}
