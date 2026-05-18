package com.nicue.onetwo.ui.life;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.SavedStateHandle;

import com.nicue.onetwo.LiveDataTestUtil;
import com.nicue.onetwo.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class MtgLifeViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private MtgLifeViewModel viewModel;

    @Before
    public void setUp() {
        viewModel = new MtgLifeViewModel(new SavedStateHandle());
    }

    @Test
    public void testDefaultSetupState() throws Exception {
        MtgLifeUiState state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertTrue(state.isShowingSetup());
        assertEquals(4, state.getPlayerCount());
        assertEquals(40, state.getStartingLife());
        assertNull(state.getPlayersErrorResId());
        assertNull(state.getLifeErrorResId());
        assertTrue(state.getPlayers().isEmpty());
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
        assertEquals(1, state.getPlayers().get(1).getSeatIndex());
        assertEquals(20, state.getPlayers().get(1).getLifeTotal());
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

        // Non-integer player count
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

        // Increment player 0 life
        viewModel.incrementLife(0);
        state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertEquals(21, state.getPlayers().get(0).getLifeTotal());
        assertEquals(20, state.getPlayers().get(1).getLifeTotal());

        // Decrement player 1 life
        viewModel.decrementLife(1);
        state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertEquals(21, state.getPlayers().get(0).getLifeTotal());
        assertEquals(19, state.getPlayers().get(1).getLifeTotal());
    }

    @Test
    public void testRotationsForVaryingPlayerCounts() throws Exception {
        // 3 Players
        viewModel.validateAndStartGame("3", "40");
        MtgLifeUiState state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertEquals(180, state.getPlayers().get(0).getRotationDegrees());
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
}
