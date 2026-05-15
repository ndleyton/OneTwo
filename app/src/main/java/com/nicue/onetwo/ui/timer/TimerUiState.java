package com.nicue.onetwo.ui.timer;

import java.util.ArrayList;
import java.util.List;

public class TimerUiState {
    private final ArrayList<TimerItemUiModel> timers;
    private final boolean paused;
    private final long configuredDurationMs;

    public TimerUiState(List<TimerItemUiModel> timers, boolean paused, long configuredDurationMs) {
        this.timers = new ArrayList<>(timers);
        this.paused = paused;
        this.configuredDurationMs = configuredDurationMs;
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
}
