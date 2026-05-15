package com.nicue.onetwo.ui.timer;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.nicue.onetwo.core.TimerScheduler;
import com.nicue.onetwo.data.timer.TimerSnapshot;
import com.nicue.onetwo.data.timer.TimerStateStore;

import com.nicue.onetwo.utils.TimerBackend;

import java.util.ArrayList;
import java.util.List;

public class TimerViewModel extends ViewModel {
    private static final String KEY_REMAINING_TIMES = "timer_remaining_times";
    private static final String KEY_RUNNING_INDEX = "timer_running_index";
    private static final String KEY_IS_PAUSED = "timer_is_paused";
    private static final String KEY_CONFIGURED_DURATION = "timer_configured_duration";
    private static final long DEFAULT_DURATION_MS = 300000L;

    private final SavedStateHandle savedStateHandle;
    private final TimerStateStore timerStateStore;
    private final TimerScheduler timerScheduler;
    private final MutableLiveData<TimerUiState> uiState = new MutableLiveData<>();
    private final MutableLiveData<Integer> finishEvents = new MutableLiveData<>(0);

    private ArrayList<Long> remainingTimes;
    private int runningIndex;
    private boolean paused;
    private long configuredDurationMs;
    private long lastTickTimeMs;

    public TimerViewModel(SavedStateHandle savedStateHandle, TimerStateStore timerStateStore,
                          TimerScheduler timerScheduler) {
        this.savedStateHandle = savedStateHandle;
        this.timerStateStore = timerStateStore;
        this.timerScheduler = timerScheduler;
        restoreState();
        emitState();
    }

    public LiveData<TimerUiState> getUiState() {
        return uiState;
    }

    public LiveData<Integer> getFinishEvents() {
        return finishEvents;
    }

    public void togglePlayPause() {
        if (paused) {
            startTimer();
        } else {
            pauseTimer();
        }
    }

    public void advanceTimer() {
        if (paused || remainingTimes.isEmpty()) {
            return;
        }
        runningIndex = (runningIndex + 1) % remainingTimes.size();
        lastTickTimeMs = timerScheduler.now();
        persistState();
        emitState();
    }

    public void editDuration(long durationMs) {
        pauseTimer();
        configuredDurationMs = durationMs;
        for (int i = 0; i < remainingTimes.size(); i++) {
            remainingTimes.set(i, durationMs);
        }
        runningIndex = 0;
        persistState();
        emitState();
    }

    public void addTimer(int maxTimers) {
        if (remainingTimes.size() >= maxTimers) {
            return;
        }
        remainingTimes.add(configuredDurationMs);
        persistState();
        emitState();
    }

    public void removeTimer() {
        if (remainingTimes.size() <= 1) {
            return;
        }
        remainingTimes.remove(remainingTimes.size() - 1);
        if (runningIndex >= remainingTimes.size()) {
            runningIndex = 0;
        }
        persistState();
        emitState();
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
        persistState();
        emitState();
    }

    public void pauseForBackground() {
        pauseTimer();
        timerStateStore.save(createSnapshot());
    }

    @Override
    protected void onCleared() {
        timerScheduler.stop();
        timerStateStore.save(createSnapshot());
        super.onCleared();
    }

    private void startTimer() {
        if (!paused || remainingTimes.isEmpty() || remainingTimes.get(runningIndex) <= 0L) {
            return;
        }
        paused = false;
        lastTickTimeMs = timerScheduler.now();
        timerScheduler.start(new TimerScheduler.TickListener() {
            @Override
            public void onTick(long nowMs) {
                handleTick(nowMs);
            }
        });
        persistState();
        emitState();
    }

    private void pauseTimer() {
        if (paused) {
            return;
        }
        timerScheduler.stop();
        paused = true;
        persistState();
        emitState();
    }

    private void handleTick(long nowMs) {
        if (paused || remainingTimes.isEmpty()) {
            return;
        }
        long delta = Math.max(0L, nowMs - lastTickTimeMs);
        lastTickTimeMs = nowMs;
        long updatedRemaining = remainingTimes.get(runningIndex) - delta;
        if (updatedRemaining <= 0L) {
            remainingTimes.set(runningIndex, 0L);
            timerScheduler.stop();
            paused = true;
            Integer currentValue = finishEvents.getValue();
            finishEvents.setValue(currentValue == null ? 1 : currentValue + 1);
        } else {
            remainingTimes.set(runningIndex, updatedRemaining);
        }
        persistState();
        emitState();
    }

    private void restoreState() {
        ArrayList<Long> savedRemainingTimes = savedStateHandle.get(KEY_REMAINING_TIMES);
        Integer savedRunningIndex = savedStateHandle.get(KEY_RUNNING_INDEX);
        Boolean savedPaused = savedStateHandle.get(KEY_IS_PAUSED);
        Long savedConfiguredDuration = savedStateHandle.get(KEY_CONFIGURED_DURATION);

        if (savedRemainingTimes != null) {
            remainingTimes = new ArrayList<>(savedRemainingTimes);
            runningIndex = savedRunningIndex == null ? 0 : savedRunningIndex;
            paused = savedPaused == null || savedPaused;
            configuredDurationMs = savedConfiguredDuration == null
                    ? DEFAULT_DURATION_MS
                    : savedConfiguredDuration;
            return;
        }

        TimerSnapshot snapshot = timerStateStore.getSnapshot();
        if (snapshot != null) {
            remainingTimes = snapshot.getRemainingTimes();
            runningIndex = snapshot.getRunningIndex();
            paused = snapshot.isPaused();
            configuredDurationMs = snapshot.getConfiguredDurationMs();
            persistState();
            return;
        }

        configuredDurationMs = DEFAULT_DURATION_MS;
        remainingTimes = new ArrayList<>();
        remainingTimes.add(DEFAULT_DURATION_MS);
        remainingTimes.add(DEFAULT_DURATION_MS);
        runningIndex = 0;
        paused = true;
        persistState();
    }

    private void persistState() {
        savedStateHandle.set(KEY_REMAINING_TIMES, new ArrayList<>(remainingTimes));
        savedStateHandle.set(KEY_RUNNING_INDEX, runningIndex);
        savedStateHandle.set(KEY_IS_PAUSED, paused);
        savedStateHandle.set(KEY_CONFIGURED_DURATION, configuredDurationMs);
    }

    private void emitState() {
        List<TimerItemUiModel> timers = new ArrayList<>();
        for (int i = 0; i < remainingTimes.size(); i++) {
            long remainingTime = remainingTimes.get(i);
            boolean active = i == runningIndex;
            timers.add(new TimerItemUiModel(
                    remainingTime,
                    formatTime(remainingTime),
                    active,
                    active && !paused && remainingTime > 0L,
                    remainingTime <= 0L
            ));
        }
        uiState.setValue(new TimerUiState(timers, paused, configuredDurationMs));
    }

    private String formatTime(long milliseconds) {
        return TimerBackend.formatRemainingTime(milliseconds, 10000L);
    }

    private TimerSnapshot createSnapshot() {
        return new TimerSnapshot(remainingTimes, runningIndex, paused, configuredDurationMs);
    }
}
