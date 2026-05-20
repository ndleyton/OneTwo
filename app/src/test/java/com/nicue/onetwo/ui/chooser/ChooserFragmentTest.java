package com.nicue.onetwo.ui.chooser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.os.Bundle;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.test.core.app.ActivityScenario;
import com.nicue.onetwo.MainActivity;
import com.nicue.onetwo.R;
import com.nicue.onetwo.utils.TouchDisplayView;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class ChooserFragmentTest {

    @Test
    public void testChooserFragmentReturnOnSelectionBehavior() {
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(
                    activity -> {
                        Fragment fragment =
                                activity.getSupportFragmentManager()
                                        .findFragmentById(R.id.nav_host_fragment);
                        assertNotNull(fragment);
                        NavController navController =
                                ((NavHostFragment) fragment).getNavController();

                        // Navigate to Chooser with argument
                        Bundle args = new Bundle();
                        args.putBoolean("return_on_selection", true);
                        navController.navigate(R.id.nav_chooser, args);
                        activity.getSupportFragmentManager().executePendingTransactions();
                        fragment.getChildFragmentManager().executePendingTransactions();
                        assertEquals(
                                R.id.nav_chooser, navController.getCurrentDestination().getId());

                        // Retrieve the fragment view and invoke TouchDisplayView's callback
                        Fragment currentFragment =
                                fragment.getChildFragmentManager().getPrimaryNavigationFragment();
                        assertNotNull(currentFragment);
                        assertTrue(currentFragment instanceof ChooserFragment);
                        ChooserFragment chooserFragment = (ChooserFragment) currentFragment;

                        View view = chooserFragment.getView();
                        assertNotNull(view);
                        TouchDisplayView chooserView = view.findViewById(R.id.chooser_view);
                        assertNotNull(chooserView);

                        // Add a touch so mTouches is not empty
                        MotionEvent eventDown =
                                MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, 100f, 100f, 0);
                        chooserView.dispatchTouchEvent(eventDown);
                        eventDown.recycle();

                        // Set fingersDown = true via reflection
                        try {
                            java.lang.reflect.Field fingersDownField =
                                    TouchDisplayView.class.getDeclaredField("fingersDown");
                            fingersDownField.setAccessible(true);
                            fingersDownField.set(chooserView, true);
                        } catch (Exception e) {
                            throw new AssertionError(e);
                        }

                        // Trigger choice selection
                        chooserView.checkGlobalVariable();

                        // Idle main looper for delay
                        Shadows.shadowOf(Looper.getMainLooper())
                                .idleFor(
                                        TouchDisplayView.SELECTION_REVEAL_DURATION_MS + 1500L,
                                        TimeUnit.MILLISECONDS);

                        // Assert we popped back to nav_mtg_life
                        assertEquals(
                                R.id.nav_mtg_life, navController.getCurrentDestination().getId());
                    });
        }
    }

    @Test
    public void testClosestSeatIndexCalculation() {
        float width = 400f;
        float height = 400f;

        // Player count 4: Top-Left (0), Top-Right (1), Bottom-Left (2), Bottom-Right (3)
        assertEquals(0, ChooserFragment.getClosestSeatIndex(50f, 50f, width, height, 4));
        assertEquals(0, ChooserFragment.getClosestSeatIndex(150f, 150f, width, height, 4));
        assertEquals(1, ChooserFragment.getClosestSeatIndex(350f, 50f, width, height, 4));
        assertEquals(1, ChooserFragment.getClosestSeatIndex(250f, 150f, width, height, 4));
        assertEquals(2, ChooserFragment.getClosestSeatIndex(50f, 350f, width, height, 4));
        assertEquals(3, ChooserFragment.getClosestSeatIndex(350f, 350f, width, height, 4));

        // Player count 3: player_1 is bottom (0), player_2 top left (1), player_3 top right (2)
        assertEquals(0, ChooserFragment.getClosestSeatIndex(200f, 350f, width, height, 3));
        assertEquals(1, ChooserFragment.getClosestSeatIndex(50f, 50f, width, height, 3));
        assertEquals(2, ChooserFragment.getClosestSeatIndex(350f, 50f, width, height, 3));
    }
}
