package com.nicue.onetwo.Utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TimerBackendTest {

    @Test
    public void formatRemainingTimeShowsMinutesAndSecondsAbovePanicThreshold() {
        assertEquals("5:00", TimerBackend.formatRemainingTime(300000, 10000));
        assertEquals("0:10", TimerBackend.formatRemainingTime(10000, 10000));
    }

    @Test
    public void formatRemainingTimeShowsDecisecondsBelowPanicThreshold() {
        assertEquals("0:09:99", TimerBackend.formatRemainingTime(9990, 10000));
        assertEquals("0:01:23", TimerBackend.formatRemainingTime(1234, 10000));
    }
}
