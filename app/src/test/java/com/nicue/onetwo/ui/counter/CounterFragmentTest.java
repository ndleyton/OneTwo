package com.nicue.onetwo.ui.counter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.view.View;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ApplicationProvider;

import com.nicue.onetwo.OneTwoApplication;
import com.nicue.onetwo.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class CounterFragmentTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Before
    public void setUp() {
        // We ensure the application container is ready
        // In a real project, we might use a TestAppContainer or Hilt
        OneTwoApplication app = ApplicationProvider.getApplicationContext();
        assertNotNull(app.getAppContainer());
    }

    @Test
    public void launchFragment_showsEmptyStateInitially() {
        FragmentScenario<CounterFragment> scenario = FragmentScenario.launchInContainer(CounterFragment.class, null, R.style.AppTheme);
        
        scenario.onFragment(fragment -> {
            View instructionText = fragment.getView().findViewById(R.id.tv_instruction_counter);
            assertEquals(View.VISIBLE, instructionText.getVisibility());
            
            RecyclerView recyclerView = fragment.getView().findViewById(R.id.recyclerview_counters);
            assertEquals(0, recyclerView.getAdapter().getItemCount());
        });
    }

    @Test
    public void addCounterViaViewModel_updatesUI() {
        FragmentScenario<CounterFragment> scenario = FragmentScenario.launchInContainer(CounterFragment.class, null, R.style.AppTheme);

        scenario.onFragment(fragment -> {
            // Simulate adding a counter directly to the ViewModel
            // which the Fragment is observing
            CounterViewModel viewModel = new androidx.lifecycle.ViewModelProvider(fragment).get(CounterViewModel.class);
            viewModel.addCounter("Test Player", 10);
            
            // Note: Since we use an Executor in the Repository, 
            // we might need to ensure the work is finished.
            // In your existing tests, you use a direct executor.
        });

        // Re-check UI state
        scenario.onFragment(fragment -> {
            RecyclerView recyclerView = fragment.getView().findViewById(R.id.recyclerview_counters);
            
            // Robolectric + LiveData + Room (in-memory) usually syncs fast
            // but in a production test, you might use a "Shadow" or "IdlingResource"
            assertEquals(1, recyclerView.getAdapter().getItemCount());
            
            View instructionText = fragment.getView().findViewById(R.id.tv_instruction_counter);
            assertEquals(View.INVISIBLE, instructionText.getVisibility());
        });
    }
}
