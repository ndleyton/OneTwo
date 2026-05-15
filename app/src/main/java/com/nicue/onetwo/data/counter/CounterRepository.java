package com.nicue.onetwo.data.counter;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.Executor;

public class CounterRepository {
    private final CounterDao counterDao;
    private final Executor executor;

    public CounterRepository(CounterDao counterDao, Executor executor) {
        this.counterDao = counterDao;
        this.executor = executor;
    }

    public LiveData<List<CounterEntity>> observeCounters() {
        return counterDao.observeCounters();
    }

    public void addCounter(String title, int value) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                counterDao.insert(new CounterEntity(title, value));
            }
        });
    }

    public void updateCounterValue(long counterId, int value) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                counterDao.updateValue(counterId, value);
            }
        });
    }

    public void deleteCounter(long counterId) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                counterDao.deleteById(counterId);
            }
        });
    }
}
