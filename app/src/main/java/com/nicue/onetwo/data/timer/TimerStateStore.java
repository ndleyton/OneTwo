package com.nicue.onetwo.data.timer;

public class TimerStateStore {
    private TimerSnapshot snapshot;

    public synchronized void save(TimerSnapshot timerSnapshot) {
        snapshot = timerSnapshot;
    }

    public synchronized TimerSnapshot getSnapshot() {
        return snapshot;
    }
}
