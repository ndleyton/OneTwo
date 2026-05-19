package com.nicue.onetwo.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TurnTimerEngine {
    private final ArrayList<Long> remainingTimes = new ArrayList<>();
    private int runningIndex;
    private boolean paused = true;
    private long configuredDurationMs;
    private long configuredIncrementMs;
    private long lastTickTimeMs;

    public TurnTimerEngine(long durationMs, long incrementMs) {
        this.configuredDurationMs = durationMs;
        this.configuredIncrementMs = incrementMs;
    }

    public List<Long> getRemainingTimes() {
        return Collections.unmodifiableList(remainingTimes);
    }

    public void setRemainingTimes(List<Long> times) {
        this.remainingTimes.clear();
        this.remainingTimes.addAll(times);
    }

    public int getRunningIndex() {
        return runningIndex;
    }

    public void setRunningIndex(int runningIndex) {
        this.runningIndex = runningIndex;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public long getConfiguredDurationMs() {
        return configuredDurationMs;
    }

    public void setConfiguredDurationMs(long durationMs) {
        this.configuredDurationMs = durationMs;
    }

    public long getConfiguredIncrementMs() {
        return configuredIncrementMs;
    }

    public void setConfiguredIncrementMs(long incrementMs) {
        this.configuredIncrementMs = incrementMs;
    }

    public long getLastTickTimeMs() {
        return lastTickTimeMs;
    }

    public void setLastTickTimeMs(long lastTickTimeMs) {
        this.lastTickTimeMs = lastTickTimeMs;
    }

    public void start(long nowMs) {
        if (!paused || remainingTimes.isEmpty() || remainingTimes.get(runningIndex) <= 0L) {
            return;
        }
        paused = false;
        lastTickTimeMs = nowMs;
    }

    public void pause() {
        paused = true;
    }

    public boolean tick(long nowMs) {
        if (paused || remainingTimes.isEmpty()) {
            return false;
        }
        long delta = Math.max(0L, nowMs - lastTickTimeMs);
        lastTickTimeMs = nowMs;
        long updatedRemaining = remainingTimes.get(runningIndex) - delta;
        if (updatedRemaining <= 0L) {
            remainingTimes.set(runningIndex, 0L);
            paused = true;
            return true;
        } else {
            remainingTimes.set(runningIndex, updatedRemaining);
            return false;
        }
    }

    public void advance(long nowMs) {
        if (paused || remainingTimes.isEmpty()) {
            return;
        }
        remainingTimes.set(runningIndex, remainingTimes.get(runningIndex) + configuredIncrementMs);
        runningIndex = (runningIndex + 1) % remainingTimes.size();
        lastTickTimeMs = nowMs;
    }

    public void advanceTo(int nextIndex, long nowMs) {
        if (paused || remainingTimes.isEmpty()) {
            return;
        }
        remainingTimes.set(runningIndex, remainingTimes.get(runningIndex) + configuredIncrementMs);
        runningIndex = nextIndex;
        lastTickTimeMs = nowMs;
    }

    public void editDuration(long durationMs, long incrementMs) {
        paused = true;
        configuredDurationMs = durationMs;
        configuredIncrementMs = incrementMs;
        for (int i = 0; i < remainingTimes.size(); i++) {
            remainingTimes.set(i, durationMs);
        }
        runningIndex = 0;
    }

    public void addTimer() {
        remainingTimes.add(configuredDurationMs);
    }

    public void removeTimer() {
        if (remainingTimes.size() <= 1) {
            return;
        }
        remainingTimes.remove(remainingTimes.size() - 1);
        if (runningIndex >= remainingTimes.size()) {
            runningIndex = 0;
        }
    }

    public void setMaxTimers(int maxTimers) {
        if (maxTimers < 1) {
            return;
        }
        while (remainingTimes.size() > maxTimers) {
            remainingTimes.remove(remainingTimes.size() - 1);
        }
        if (remainingTimes.isEmpty()) {
            remainingTimes.add(configuredDurationMs);
        }
        if (runningIndex >= remainingTimes.size()) {
            runningIndex = 0;
        }
    }
}
