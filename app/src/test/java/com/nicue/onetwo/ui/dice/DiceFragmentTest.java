package com.nicue.onetwo.ui.dice;

import static org.junit.Assert.assertEquals;

import android.view.View;
import android.widget.TextView;
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ApplicationProvider;
import com.nicue.onetwo.OneTwoApplication;
import com.nicue.onetwo.R;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class DiceFragmentTest {

    @Rule public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Before
    public void setUp() {
        OneTwoApplication app = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void launchFragment_showsEmptyStateInitially() {
        FragmentScenario<DiceFragment> scenario =
                FragmentScenario.launchInContainer(DiceFragment.class, null, R.style.AppTheme);

        scenario.onFragment(
                fragment -> {
                    View emptyText = fragment.getView().findViewById(R.id.tv_dice_empty);
                    assertEquals(View.VISIBLE, emptyText.getVisibility());

                    RecyclerView recyclerView =
                            fragment.getView().findViewById(R.id.recyclerview_dice);
                    assertEquals(0, recyclerView.getAdapter().getItemCount());

                    TextView totalText = fragment.getView().findViewById(R.id.tv_dice_total);
                    assertEquals("0", totalText.getText().toString());
                });
    }

    @Test
    public void addDieViaViewModel_updatesUI() {
        FragmentScenario<DiceFragment> scenario =
                FragmentScenario.launchInContainer(DiceFragment.class, null, R.style.AppTheme);

        scenario.onFragment(
                fragment -> {
                    DiceViewModel viewModel =
                            new androidx.lifecycle.ViewModelProvider(fragment)
                                    .get(DiceViewModel.class);
                    // Clear first just in case
                    while (!viewModel.getUiState().getValue().getDice().isEmpty()) {
                        viewModel.removeDie(0);
                    }
                    viewModel.addDie(20);
                });

        scenario.onFragment(
                fragment -> {
                    RecyclerView recyclerView =
                            fragment.getView().findViewById(R.id.recyclerview_dice);
                    assertEquals(1, recyclerView.getAdapter().getItemCount());

                    View emptyText = fragment.getView().findViewById(R.id.tv_dice_empty);
                    assertEquals(View.GONE, emptyText.getVisibility());
                });
    }
}
