package com.nicue.onetwo.ui.timer;

import androidx.annotation.NonNull;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.SavedStateHandleSupport;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.nicue.onetwo.core.TimerScheduler;
import com.nicue.onetwo.data.timer.TimerStateStore;

public class TimerViewModelFactory implements ViewModelProvider.Factory {
    private final TimerStateStore timerStateStore;
    private final TimerScheduler timerScheduler;

    public TimerViewModelFactory(TimerStateStore timerStateStore, TimerScheduler timerScheduler) {
        this.timerStateStore = timerStateStore;
        this.timerScheduler = timerScheduler;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass,
                                          @NonNull androidx.lifecycle.viewmodel.CreationExtras extras) {
        if (modelClass.isAssignableFrom(TimerViewModel.class)) {
            SavedStateHandle savedStateHandle = SavedStateHandleSupport.createSavedStateHandle(extras);
            return (T) new TimerViewModel(savedStateHandle, timerStateStore, timerScheduler);
        }
        throw new IllegalArgumentException("Unknown ViewModel class " + modelClass.getName());
    }
}
