package com.nicue.onetwo.ui.dice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.SavedStateHandle;
import androidx.test.core.app.ApplicationProvider;

import com.nicue.onetwo.LiveDataTestUtil;
import com.nicue.onetwo.data.dice.DicePrefsDataSource;
import com.nicue.onetwo.data.dice.DiceRepository;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

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
}
