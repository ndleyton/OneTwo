package com.nicue.onetwo.ui.timer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.SavedStateHandle;

import com.nicue.onetwo.LiveDataTestUtil;
import com.nicue.onetwo.core.TimerScheduler;
import com.nicue.onetwo.data.timer.TimerStateStore;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class TimerViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Test
    public void timerStateTransitions_areOwnedByViewModel() throws Exception {
        FakeTimerScheduler scheduler = new FakeTimerScheduler();
        TimerViewModel viewModel = new TimerViewModel(
                new SavedStateHandle(),
                new TimerStateStore(),
                scheduler
        );

        TimerUiState state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertTrue(state.isPaused());
        assertEquals(2, state.getTimers().size());

        viewModel.editDuration(2_000L);
        viewModel.togglePlayPause();
        state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertFalse(state.isPaused());

        scheduler.advanceBy(1_000L);
        state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertTrue(state.getTimers().get(0).getRemainingTimeMs() <= 1_000L);

        viewModel.advanceTimer();
        state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertTrue(state.getTimers().get(1).isActive());

        scheduler.advanceBy(2_500L);
        state = LiveDataTestUtil.getValue(viewModel.getUiState());
        assertTrue(state.isPaused());
        assertEquals(0L, state.getTimers().get(1).getRemainingTimeMs());
    }

    private static class FakeTimerScheduler implements TimerScheduler {
        private TickListener tickListener;
        private long nowMs;

        @Override
        public void start(TickListener listener) {
            tickListener = listener;
        }

        @Override
        public void stop() {
            tickListener = null;
        }

        @Override
        public long now() {
            return nowMs;
        }

        void advanceBy(long deltaMs) {
            nowMs += deltaMs;
            if (tickListener != null) {
                tickListener.onTick(nowMs);
            }
        }
    }
}
