package com.nicue.onetwo.ui.counter;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.nicue.onetwo.data.counter.CounterRepository;

public class CounterViewModelFactory implements ViewModelProvider.Factory {
    private final CounterRepository counterRepository;

    public CounterViewModelFactory(CounterRepository counterRepository) {
        this.counterRepository = counterRepository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(CounterViewModel.class)) {
            return (T) new CounterViewModel(counterRepository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class " + modelClass.getName());
    }
}
