package com.nicue.onetwo.data.timer;

import java.util.ArrayList;
import java.util.List;

public class TimerSnapshot {
    private final ArrayList<Long> remainingTimes;
    private final int runningIndex;
    private final boolean paused;
    private final long configuredDurationMs;
    private final long configuredIncrementMs;

    public TimerSnapshot(
            List<Long> remainingTimes,
            int runningIndex,
            boolean paused,
            long configuredDurationMs,
            long configuredIncrementMs) {
        this.remainingTimes = new ArrayList<>(remainingTimes);
        this.runningIndex = runningIndex;
        this.paused = paused;
        this.configuredDurationMs = configuredDurationMs;
        this.configuredIncrementMs = configuredIncrementMs;
    }

    public ArrayList<Long> getRemainingTimes() {
        return new ArrayList<>(remainingTimes);
    }

    public int getRunningIndex() {
        return runningIndex;
    }

    public boolean isPaused() {
        return paused;
    }

    public long getConfiguredDurationMs() {
        return configuredDurationMs;
    }

    public long getConfiguredIncrementMs() {
        return configuredIncrementMs;
    }
}
