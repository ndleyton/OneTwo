package com.nicue.onetwo.fragments;

import static org.junit.Assert.assertEquals;

import com.nicue.onetwo.ui.timer.TimerFragment;
import org.junit.Test;

public class TimerFragmentTest {

    @Test
    public void calculateMaxTimersUsesAvailableTimerHeight() {
        assertEquals(2, TimerFragment.calculateMaxTimers(200));
        assertEquals(8, TimerFragment.calculateMaxTimers(700));
    }

    @Test
    public void calculateMaxTimersRequiresFullTimerRows() {
        assertEquals(1, TimerFragment.calculateMaxTimers(77));
        assertEquals(1, TimerFragment.calculateMaxTimers(78));
        assertEquals(2, TimerFragment.calculateMaxTimers(156));
    }
}
