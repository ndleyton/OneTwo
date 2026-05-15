package com.nicue.onetwo.ui.counter;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.nicue.onetwo.LiveDataTestUtil;
import com.nicue.onetwo.data.counter.CounterDatabase;
import com.nicue.onetwo.data.counter.CounterEntity;
import com.nicue.onetwo.data.counter.CounterRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;
import java.util.concurrent.Executor;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class CounterViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private CounterDatabase counterDatabase;
    private CounterViewModel viewModel;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        counterDatabase = Room.inMemoryDatabaseBuilder(context, CounterDatabase.class)
                .allowMainThreadQueries()
                .build();
        Executor directExecutor = new Executor() {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        };
        viewModel = new CounterViewModel(
                new CounterRepository(counterDatabase.counterDao(), directExecutor)
        );
    }

    @After
    public void tearDown() {
        counterDatabase.close();
    }

    @Test
    public void addUpdateDeleteCounter_updatesObservedState() throws Exception {
        viewModel.addCounter("Player 1", 5);

        List<CounterEntity> counters = LiveDataTestUtil.getValue(viewModel.getCounters());
        assertEquals(1, counters.size());
        assertEquals(5, counters.get(0).getValue());

        viewModel.updateCounterValue(counters.get(0).getId(), 9);
        counters = LiveDataTestUtil.getValue(viewModel.getCounters());
        assertEquals(9, counters.get(0).getValue());

        viewModel.deleteCounter(counters.get(0).getId());
        counters = LiveDataTestUtil.getValue(viewModel.getCounters());
        assertEquals(0, counters.size());
    }
}
