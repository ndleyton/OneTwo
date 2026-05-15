package com.nicue.onetwo.ui.timer;

public class TimerItemUiModel {
    private final long remainingTimeMs;
    private final String displayTime;
    private final boolean active;
    private final boolean enabled;
    private final boolean finished;

    public TimerItemUiModel(long remainingTimeMs, String displayTime, boolean active,
                            boolean enabled, boolean finished) {
        this.remainingTimeMs = remainingTimeMs;
        this.displayTime = displayTime;
        this.active = active;
        this.enabled = enabled;
        this.finished = finished;
    }

    public long getRemainingTimeMs() {
        return remainingTimeMs;
    }

    public String getDisplayTime() {
        return displayTime;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isFinished() {
        return finished;
    }
}
