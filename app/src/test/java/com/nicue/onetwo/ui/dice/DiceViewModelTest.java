package com.nicue.onetwo.ui.dice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.SavedStateHandle;
import androidx.test.core.app.ApplicationProvider;
import com.nicue.onetwo.LiveDataTestUtil;
import com.nicue.onetwo.data.dice.DicePrefsDataSource;
import com.nicue.onetwo.data.dice.DiceRepository;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class DiceViewModelTest {
    @Rule public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private DiceViewModel viewModel;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences("SHARED_PREFS_FILE", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
        viewModel =
                new DiceViewModel(
                        new DiceRepository(new DicePrefsDataSource(context)),
                        new SavedStateHandle());
    }

    @Test
    public void addRemoveRollAll_updatesDiceState() throws Exception {
        viewModel.addDie(6);
        viewModel.addDie(20);

        DiceUiState state = LiveDataTestUtil.getValue(viewModel.getUiState());
        List<DieUiModel> dice = state.getDice();
        assertEquals(2, dice.size());
        assertEquals(6, dice.get(0).getFaces());
        assertEquals(26, state.getTotal());

        viewModel.rollAllDice();
        state = LiveDataTestUtil.getValue(viewModel.getUiState());
        dice = state.getDice();
        assertTrue(dice.get(0).getValue() >= 1 && dice.get(0).getValue() <= 6);
        assertTrue(dice.get(1).getValue() >= 1 && dice.get(1).getValue() <= 20);
        assertEquals(dice.get(0).getValue() + dice.get(1).getValue(), state.getTotal());

        viewModel.removeDie(0);
        state = LiveDataTestUtil.getValue(viewModel.getUiState());
        dice = state.getDice();
        assertEquals(1, dice.size());
        assertEquals(20, dice.get(0).getFaces());
        assertEquals(dice.get(0).getValue(), state.getTotal());
    }

    @Test
    public void rollAllDice_keepsLockedDieValue() throws Exception {
        viewModel.addDie(20);
        viewModel.addDie(20);
        viewModel.toggleLock(0);

        int lockedValue = dieAt(0).getValue();
        for (int i = 0; i < 50; i++) {
            viewModel.rollAllDice();
            assertEquals(lockedValue, dieAt(0).getValue());
        }
        assertTrue(dieAt(0).isLocked());
        assertFalse(dieAt(1).isLocked());
    }

    @Test
    public void rollDie_ignoresLockedDie() throws Exception {
        viewModel.addDie(20);
        viewModel.toggleLock(0);

        int lockedValue = dieAt(0).getValue();
        for (int i = 0; i < 50; i++) {
            viewModel.rollDie(0);
            assertEquals(lockedValue, dieAt(0).getValue());
        }
    }

    @Test
    public void toggleLock_unlocksAndAllowsRollingAgain() throws Exception {
        viewModel.addDie(6);
        viewModel.toggleLock(0);
        assertTrue(dieAt(0).isLocked());

        viewModel.toggleLock(0);
        assertFalse(dieAt(0).isLocked());
    }

    @Test
    public void addDie_startsUnlocked() throws Exception {
        viewModel.addDie(6);
        viewModel.toggleLock(0);
        viewModel.addDie(6);

        assertTrue(dieAt(0).isLocked());
        assertFalse(dieAt(1).isLocked());
    }

    @Test
    public void removeDie_shiftsLockFlagsWithRemainingDice() throws Exception {
        viewModel.addDie(4);
        viewModel.addDie(6);
        viewModel.addDie(20);
        viewModel.toggleLock(2);

        viewModel.removeDie(0);

        DiceUiState state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertEquals(2, state.getDice().size());
        assertEquals(6, dieAt(0).getFaces());
        assertFalse(dieAt(0).isLocked());
        assertEquals(20, dieAt(1).getFaces());
        assertTrue(dieAt(1).isLocked());
    }

    @Test
    public void lockState_survivesSavedStateHandleRoundTrip() throws Exception {
        SavedStateHandle savedStateHandle = new SavedStateHandle();
        DiceRepository repository =
                new DiceRepository(
                        new DicePrefsDataSource(
                                ApplicationProvider.<Context>getApplicationContext()));
        DiceViewModel first = new DiceViewModel(repository, savedStateHandle);
        first.addDie(6);
        first.addDie(20);
        first.toggleLock(1);

        DiceViewModel restored = new DiceViewModel(repository, savedStateHandle);

        List<DieUiModel> dice = LiveDataTestUtil.getValue(restored.getUiState()).getDice();
        assertEquals(2, dice.size());
        assertFalse(dice.get(0).isLocked());
        assertTrue(dice.get(1).isLocked());
    }

    @Test
    public void unlockAllDice_clearsEveryLock() throws Exception {
        viewModel.addDie(6);
        viewModel.addDie(20);
        viewModel.toggleLock(0);
        viewModel.toggleLock(1);

        viewModel.unlockAllDice();

        assertFalse(dieAt(0).isLocked());
        assertFalse(dieAt(1).isLocked());
    }

    @Test
    public void uiState_reportsLockedAndRollableDice() throws Exception {
        DiceUiState state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertFalse(state.hasLockedDice());
        assertFalse(state.hasRollableDice());

        viewModel.addDie(6);
        state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertFalse(state.hasLockedDice());
        assertTrue(state.hasRollableDice());

        viewModel.toggleLock(0);
        state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertTrue(state.hasLockedDice());
        assertFalse(state.hasRollableDice());
    }

    @Test
    public void lockedDie_stillCountsTowardTotal() throws Exception {
        viewModel.addDie(6);
        viewModel.addDie(6);
        viewModel.toggleLock(0);

        DiceUiState state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertEquals(dieAt(0).getValue() + dieAt(1).getValue(), state.getTotal());
    }

    private DieUiModel dieAt(int position) throws Exception {
        return LiveDataTestUtil.getValue(viewModel.getUiState()).getDice().get(position);
    }
}
