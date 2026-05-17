package com.nicue.onetwo.data.counter;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import com.nicue.onetwo.LiveDataTestUtil;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class CounterRepositoryTest {
    @Rule public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private CounterDatabase counterDatabase;
    private CounterRepository counterRepository;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        counterDatabase =
                Room.inMemoryDatabaseBuilder(context, CounterDatabase.class)
                        .allowMainThreadQueries()
                        .build();
        Executor directExecutor =
                new Executor() {
                    @Override
                    public void execute(Runnable command) {
                        command.run();
                    }
                };
        counterRepository = new CounterRepository(counterDatabase.counterDao(), directExecutor);
    }

    @After
    public void tearDown() {
        counterDatabase.close();
    }

    @Test
    public void addUpdateDeleteCounters_roundTripsThroughRoom() throws Exception {
        counterRepository.addCounter("Alpha", 3);
        counterRepository.addCounter("Beta", 7);

        List<CounterEntity> counters =
                LiveDataTestUtil.getValue(counterRepository.observeCounters());
        assertEquals(2, counters.size());
        assertEquals("Alpha", counters.get(0).getTitle());
        assertEquals(3, counters.get(0).getValue());

        counterRepository.updateCounterValue(counters.get(0).getId(), 11);
        counters = LiveDataTestUtil.getValue(counterRepository.observeCounters());
        assertEquals(11, counters.get(0).getValue());

        counterRepository.deleteCounter(counters.get(1).getId());
        counters = LiveDataTestUtil.getValue(counterRepository.observeCounters());
        assertEquals(1, counters.size());
        assertEquals("Alpha", counters.get(0).getTitle());
    }

    @Test
    public void reorderCounters_updatesObservedOrder() throws Exception {
        counterRepository.addCounter("Alpha", 1);
        counterRepository.addCounter("Beta", 2);
        counterRepository.addCounter("Gamma", 3);

        List<CounterEntity> counters =
                LiveDataTestUtil.getValue(counterRepository.observeCounters());

        counterRepository.reorderCounters(
                Arrays.asList(
                        counters.get(2).getId(), counters.get(0).getId(), counters.get(1).getId()));

        counters = LiveDataTestUtil.getValue(counterRepository.observeCounters());
        assertEquals("Gamma", counters.get(0).getTitle());
        assertEquals("Alpha", counters.get(1).getTitle());
        assertEquals("Beta", counters.get(2).getTitle());
    }
}
