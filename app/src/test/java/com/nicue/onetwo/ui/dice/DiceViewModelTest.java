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
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private DiceViewModel viewModel;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences("SHARED_PREFS_FILE", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
        viewModel = new DiceViewModel(
                new DiceRepository(new DicePrefsDataSource(context)),
                new SavedStateHandle()
        );
    }

    @Test
    public void addRemoveRollAll_updatesDiceState() throws Exception {
        viewModel.addDie(6);
        viewModel.addDie(20);

        List<DieUiModel> dice = LiveDataTestUtil.getValue(viewModel.getUiState()).getDice();
        assertEquals(2, dice.size());
        assertEquals(6, dice.get(0).getFaces());

        viewModel.rollAllDice();
        dice = LiveDataTestUtil.getValue(viewModel.getUiState()).getDice();
        assertTrue(dice.get(0).getValue() >= 1 && dice.get(0).getValue() <= 6);
        assertTrue(dice.get(1).getValue() >= 1 && dice.get(1).getValue() <= 20);

        viewModel.removeDie(0);
        dice = LiveDataTestUtil.getValue(viewModel.getUiState()).getDice();
        assertEquals(1, dice.size());
        assertEquals(20, dice.get(0).getFaces());
    }
}
