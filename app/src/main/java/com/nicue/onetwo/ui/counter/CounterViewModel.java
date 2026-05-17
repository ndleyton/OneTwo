package com.nicue.onetwo.ui.counter;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.nicue.onetwo.data.counter.CounterEntity;
import com.nicue.onetwo.data.counter.CounterRepository;
import java.util.List;

public class CounterViewModel extends ViewModel {
    private final CounterRepository counterRepository;
    private final LiveData<List<CounterEntity>> counters;

    public CounterViewModel(CounterRepository counterRepository) {
        this.counterRepository = counterRepository;
        this.counters = counterRepository.observeCounters();
    }

    public LiveData<List<CounterEntity>> getCounters() {
        return counters;
    }

    public void addCounter(String title, int value) {
        counterRepository.addCounter(title, value);
    }

    public void updateCounterValue(long counterId, int value) {
        counterRepository.updateCounterValue(counterId, value);
    }

    public void deleteCounter(long counterId) {
        counterRepository.deleteCounter(counterId);
    }

    public void reorderCounters(List<Long> orderedCounterIds) {
        counterRepository.reorderCounters(orderedCounterIds);
    }
}
