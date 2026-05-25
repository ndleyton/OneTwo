package com.nicue.onetwo.ui.timer;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.nicue.onetwo.core.TimerScheduler;
import com.nicue.onetwo.data.timer.TimerSnapshot;
import com.nicue.onetwo.data.timer.TimerStateStore;
import com.nicue.onetwo.utils.TimerBackend;
import com.nicue.onetwo.utils.TurnTimerEngine;
import java.util.ArrayList;
import java.util.List;

public class TimerViewModel extends ViewModel {
    private static final String KEY_REMAINING_TIMES = "timer_remaining_times";
    private static final String KEY_RUNNING_INDEX = "timer_running_index";
    private static final String KEY_IS_PAUSED = "timer_is_paused";
    private static final String KEY_CONFIGURED_DURATION = "timer_configured_duration";
    private static final String KEY_CONFIGURED_INCREMENT = "timer_configured_increment";
    private static final long DEFAULT_DURATION_MS = 1500000L;
    private static final long DEFAULT_INCREMENT_MS = 0L;

    private final SavedStateHandle savedStateHandle;
    private final TimerStateStore timerStateStore;
    private final TimerScheduler timerScheduler;
    private final MutableLiveData<TimerUiState> uiState = new MutableLiveData<>();
    private final MutableLiveData<Integer> finishEvents = new MutableLiveData<>(0);

    private final TurnTimerEngine timerEngine;

    public TimerViewModel(
            SavedStateHandle savedStateHandle,
            TimerStateStore timerStateStore,
            TimerScheduler timerScheduler) {
        this.savedStateHandle = savedStateHandle;
        this.timerStateStore = timerStateStore;
        this.timerScheduler = timerScheduler;
        this.timerEngine = new TurnTimerEngine(DEFAULT_DURATION_MS, DEFAULT_INCREMENT_MS);
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
        if (timerEngine.isPaused()) {
            startTimer();
        } else {
            pauseTimer();
        }
    }

    public void advanceTimer() {
        if (timerEngine.isPaused() || timerEngine.getRemainingTimes().isEmpty()) {
            return;
        }
        timerEngine.advance(timerScheduler.now());
        persistState();
        emitState();
    }

    public void editDuration(long durationMs) {
        editDuration(durationMs, timerEngine.getConfiguredIncrementMs());
    }

    public void editDuration(long durationMs, long incrementMs) {
        pauseTimer();
        timerEngine.editDuration(durationMs, incrementMs);
        persistState();
        emitState();
    }

    public void addTimer(int maxTimers) {
        if (timerEngine.getRemainingTimes().size() >= maxTimers) {
            return;
        }
        timerEngine.addTimer();
        persistState();
        emitState();
    }

    public void removeTimer() {
        if (timerEngine.getRemainingTimes().size() <= 1) {
            return;
        }
        timerEngine.removeTimer();
        persistState();
        emitState();
    }

    public void setTimerCount(int count, int maxTimers) {
        int currentSize = timerEngine.getRemainingTimes().size();
        if (count < 1) count = 1;
        if (count > maxTimers) count = maxTimers;
        
        while (currentSize < count) {
            timerEngine.addTimer();
            currentSize++;
        }
        while (currentSize > count) {
            timerEngine.removeTimer();
            currentSize--;
        }
        persistState();
        emitState();
    }

    public void setMaxTimers(int maxTimers) {
        timerEngine.setMaxTimers(maxTimers);
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
        if (!timerEngine.isPaused()
                || timerEngine.getRemainingTimes().isEmpty()
                || timerEngine.getRemainingTimes().get(timerEngine.getRunningIndex()) <= 0L) {
            return;
        }
        timerEngine.start(timerScheduler.now());
        timerScheduler.start(
                new TimerScheduler.TickListener() {
                    @Override
                    public void onTick(long nowMs) {
                        handleTick(nowMs);
                    }
                });
        persistState();
        emitState();
    }

    private void pauseTimer() {
        if (timerEngine.isPaused()) {
            return;
        }
        timerScheduler.stop();
        timerEngine.pause();
        persistState();
        emitState();
    }

    private void handleTick(long nowMs) {
        if (timerEngine.isPaused() || timerEngine.getRemainingTimes().isEmpty()) {
            return;
        }
        boolean expired = timerEngine.tick(nowMs);
        if (expired) {
            timerScheduler.stop();
            Integer currentValue = finishEvents.getValue();
            finishEvents.setValue(currentValue == null ? 1 : currentValue + 1);
        }
        persistState();
        emitState();
    }

    private void restoreState() {
        ArrayList<Long> savedRemainingTimes = savedStateHandle.get(KEY_REMAINING_TIMES);
        Integer savedRunningIndex = savedStateHandle.get(KEY_RUNNING_INDEX);
        Boolean savedPaused = savedStateHandle.get(KEY_IS_PAUSED);
        Long savedConfiguredDuration = savedStateHandle.get(KEY_CONFIGURED_DURATION);
        Long savedConfiguredIncrement = savedStateHandle.get(KEY_CONFIGURED_INCREMENT);

        if (savedRemainingTimes != null) {
            timerEngine.setRemainingTimes(savedRemainingTimes);
            timerEngine.setRunningIndex(savedRunningIndex == null ? 0 : savedRunningIndex);
            timerEngine.setPaused(savedPaused == null || savedPaused);
            timerEngine.setConfiguredDurationMs(
                    savedConfiguredDuration == null
                            ? DEFAULT_DURATION_MS
                            : savedConfiguredDuration);
            timerEngine.setConfiguredIncrementMs(
                    savedConfiguredIncrement == null
                            ? DEFAULT_INCREMENT_MS
                            : savedConfiguredIncrement);
            return;
        }

        TimerSnapshot snapshot = timerStateStore.getSnapshot();
        if (snapshot != null) {
            timerEngine.setRemainingTimes(snapshot.getRemainingTimes());
            timerEngine.setRunningIndex(snapshot.getRunningIndex());
            timerEngine.setPaused(snapshot.isPaused());
            timerEngine.setConfiguredDurationMs(snapshot.getConfiguredDurationMs());
            timerEngine.setConfiguredIncrementMs(snapshot.getConfiguredIncrementMs());
            persistState();
            return;
        }

        timerEngine.setConfiguredDurationMs(DEFAULT_DURATION_MS);
        timerEngine.setConfiguredIncrementMs(DEFAULT_INCREMENT_MS);
        ArrayList<Long> defaultTimes = new ArrayList<>();
        defaultTimes.add(DEFAULT_DURATION_MS);
        defaultTimes.add(DEFAULT_DURATION_MS);
        timerEngine.setRemainingTimes(defaultTimes);
        timerEngine.setRunningIndex(0);
        timerEngine.setPaused(true);
        persistState();
    }

    private void persistState() {
        savedStateHandle.set(KEY_REMAINING_TIMES, new ArrayList<>(timerEngine.getRemainingTimes()));
        savedStateHandle.set(KEY_RUNNING_INDEX, timerEngine.getRunningIndex());
        savedStateHandle.set(KEY_IS_PAUSED, timerEngine.isPaused());
        savedStateHandle.set(KEY_CONFIGURED_DURATION, timerEngine.getConfiguredDurationMs());
        savedStateHandle.set(KEY_CONFIGURED_INCREMENT, timerEngine.getConfiguredIncrementMs());
    }

    private void emitState() {
        List<TimerItemUiModel> timers = new ArrayList<>();
        List<Long> remainingTimes = timerEngine.getRemainingTimes();
        for (int i = 0; i < remainingTimes.size(); i++) {
            long remainingTime = remainingTimes.get(i);
            boolean active = i == timerEngine.getRunningIndex();
            timers.add(
                    new TimerItemUiModel(
                            remainingTime,
                            formatTime(remainingTime),
                            active,
                            active && !timerEngine.isPaused() && remainingTime > 0L,
                            remainingTime <= 0L));
        }
        uiState.setValue(
                new TimerUiState(
                        timers,
                        timerEngine.isPaused(),
                        timerEngine.getConfiguredDurationMs(),
                        timerEngine.getConfiguredIncrementMs()));
    }

    private String formatTime(long milliseconds) {
        return TimerBackend.formatRemainingTime(milliseconds, 10000L);
    }

    private TimerSnapshot createSnapshot() {
        return new TimerSnapshot(
                timerEngine.getRemainingTimes(),
                timerEngine.getRunningIndex(),
                timerEngine.isPaused(),
                timerEngine.getConfiguredDurationMs(),
                timerEngine.getConfiguredIncrementMs());
    }
}
