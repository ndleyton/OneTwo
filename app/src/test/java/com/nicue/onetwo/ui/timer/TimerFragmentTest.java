package com.nicue.onetwo.ui.timer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.view.MenuItem;
import android.widget.LinearLayout;
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.core.app.ApplicationProvider;
import com.nicue.onetwo.OneTwoApplication;
import com.nicue.onetwo.R;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.fakes.RoboMenuItem;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class TimerFragmentTest {

    @Rule public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Before
    public void setUp() {
        OneTwoApplication app = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void launchFragment_showsInitialTimers() {
        FragmentScenario<TimerFragment> scenario =
                FragmentScenario.launchInContainer(TimerFragment.class, null, R.style.AppTheme);

        scenario.onFragment(
                fragment -> {
                    LinearLayout timerContainer =
                            fragment.getView().findViewById(R.id.linear_timers);
                    assertTrue(timerContainer.getChildCount() > 0);
                });
    }

    @Test
    public void playPause_menuItemTogglesPlayPause() {
        FragmentScenario<TimerFragment> scenario =
                FragmentScenario.launchInContainer(TimerFragment.class, null, R.style.AppTheme);

        scenario.onFragment(
                fragment -> {
                    TimerViewModel viewModel =
                            new androidx.lifecycle.ViewModelProvider(fragment)
                                    .get(TimerViewModel.class);

                    // Initially paused
                    assertTrue(viewModel.getUiState().getValue().isPaused());

                    // Simulate menu item click for action_play_pause
                    MenuItem playPauseItem = new RoboMenuItem(R.id.action_play_pause);
                    fragment.onMenuItemSelected(playPauseItem);

                    // Should toggle to active (not paused)
                    assertFalse(viewModel.getUiState().getValue().isPaused());

                    // Toggle again
                    fragment.onMenuItemSelected(playPauseItem);
                    assertTrue(viewModel.getUiState().getValue().isPaused());
                });
    }
}
