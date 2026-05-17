package com.nicue.onetwo.core;

import android.app.Application;

import androidx.room.Room;

import com.nicue.onetwo.data.counter.CounterDatabase;
import com.nicue.onetwo.data.counter.CounterRepository;
import com.nicue.onetwo.data.dice.DicePrefsDataSource;
import com.nicue.onetwo.data.dice.DiceRepository;
import com.nicue.onetwo.data.settings.SettingsPrefsDataSource;
import com.nicue.onetwo.data.settings.SettingsRepository;
import com.nicue.onetwo.data.timer.TimerStateStore;
import com.nicue.onetwo.db.TaskContract;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AppContainer {
    private final CounterRepository counterRepository;
    private final DiceRepository diceRepository;
    private final SettingsRepository settingsRepository;
    private final TimerStateStore timerStateStore;

    public AppContainer(Application application) {
        this(application,
                Room.databaseBuilder(application, CounterDatabase.class, TaskContract.DB_NAME)
                        .addMigrations(CounterDatabase.MIGRATION_2_3)
                        .build(),
                Executors.newSingleThreadExecutor());
    }

    public AppContainer(Application application, CounterDatabase counterDatabase, Executor executor) {
        this.counterRepository = new CounterRepository(counterDatabase.counterDao(), executor);
        this.diceRepository = new DiceRepository(
                new DicePrefsDataSource(application.getApplicationContext())
        );
        this.settingsRepository = new SettingsRepository(
                new SettingsPrefsDataSource(application.getApplicationContext())
        );
        this.timerStateStore = new TimerStateStore();
    }

    public CounterRepository getCounterRepository() {
        return counterRepository;
    }

    public DiceRepository getDiceRepository() {
        return diceRepository;
    }

    public SettingsRepository getSettingsRepository() {
        return settingsRepository;
    }

    public TimerStateStore getTimerStateStore() {
        return timerStateStore;
    }
}
