package com.nicue.onetwo.fragments;

import static org.junit.Assert.assertEquals;

import com.nicue.onetwo.ui.timer.TimerFragment;
import org.junit.Test;

public class TimerFragmentTest {

    @Test
    public void calculateMaxTimersUsesAvailableTimerHeight() {
        assertEquals(2, TimerFragment.calculateMaxTimers(232));
        assertEquals(6, TimerFragment.calculateMaxTimers(700));
    }

    @Test
    public void calculateMaxTimersRequiresFullTimerRows() {
        assertEquals(1, TimerFragment.calculateMaxTimers(131));
        assertEquals(1, TimerFragment.calculateMaxTimers(132));
        assertEquals(2, TimerFragment.calculateMaxTimers(232));
    }
}
