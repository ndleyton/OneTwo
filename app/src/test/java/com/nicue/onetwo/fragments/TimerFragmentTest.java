package com.nicue.onetwo.fragments;

import com.nicue.onetwo.ui.timer.TimerFragment;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TimerFragmentTest {

    @Test
    public void calculateMaxTimersUsesAvailableScreenHeight() {
        assertEquals(2, TimerFragment.calculateMaxTimers(200));
        assertEquals(8, TimerFragment.calculateMaxTimers(700));
    }
}
