package com.nicue.onetwo.ui.timer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.widget.Button;
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
                    LinearLayout timerContainer = fragment.getView().findViewById(R.id.linear_timers);
                    assertTrue(timerContainer.getChildCount() > 0);

                    Button playButton = fragment.getView().findViewById(R.id.play_button);
                    assertEquals(fragment.getString(R.string.play), playButton.getText().toString());
                });
    }

    @Test
    public void playPause_togglesPlayButtonText() {
        FragmentScenario<TimerFragment> scenario =
                FragmentScenario.launchInContainer(TimerFragment.class, null, R.style.AppTheme);

        scenario.onFragment(
                fragment -> {
                    TimerViewModel viewModel =
                            new androidx.lifecycle.ViewModelProvider(fragment)
                                    .get(TimerViewModel.class);
                    Button playButton = fragment.getView().findViewById(R.id.play_button);

                    // Initially Play
                    assertEquals(fragment.getString(R.string.play), playButton.getText().toString());

                    // Toggle
                    viewModel.togglePlayPause();
                    assertEquals(fragment.getString(R.string.pause), playButton.getText().toString());
                });
    }
}
