package com.nicue.onetwo.ui.timer;

import java.util.ArrayList;
import java.util.List;

public class TimerUiState {
    private final ArrayList<TimerItemUiModel> timers;
    private final boolean paused;
    private final long configuredDurationMs;
    private final long configuredIncrementMs;

    public TimerUiState(
            List<TimerItemUiModel> timers,
            boolean paused,
            long configuredDurationMs,
            long configuredIncrementMs) {
        this.timers = new ArrayList<>(timers);
        this.paused = paused;
        this.configuredDurationMs = configuredDurationMs;
        this.configuredIncrementMs = configuredIncrementMs;
    }

    public ArrayList<TimerItemUiModel> getTimers() {
        return new ArrayList<>(timers);
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
