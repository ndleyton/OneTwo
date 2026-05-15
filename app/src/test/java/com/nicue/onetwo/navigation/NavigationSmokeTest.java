package com.nicue.onetwo.navigation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.test.core.app.ActivityScenario;

import com.nicue.onetwo.MainActivity;
import com.nicue.onetwo.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class NavigationSmokeTest {
    @Test
    public void topLevelDestinations_areAvailableInNavGraph() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                Fragment fragment = activity.getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);
                assertNotNull(fragment);
                NavController navController = ((NavHostFragment) fragment).getNavController();
                assertEquals(R.id.nav_counter, navController.getCurrentDestination().getId());

                navController.navigate(R.id.nav_dice);
                assertEquals(R.id.nav_dice, navController.getCurrentDestination().getId());

                navController.navigate(R.id.nav_timer);
                assertEquals(R.id.nav_timer, navController.getCurrentDestination().getId());
            });
        }
    }
}
