package com.nicue.onetwo.data.settings;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class SettingsRepositoryTest {
    private static final String PREF_FILE = "SETTINGS_PREFS";

    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE).edit().clear().commit();
    }

    @Test
    public void lifeCounterHapticFeedbackDefaultsToEnabled() {
        SettingsRepository repository =
                new SettingsRepository(new SettingsPrefsDataSource(context));

        assertTrue(repository.isLifeCounterHapticFeedbackEnabled());
    }

    @Test
    public void setLifeCounterHapticFeedbackEnabledPersistsValue() {
        SettingsRepository repository =
                new SettingsRepository(new SettingsPrefsDataSource(context));

        repository.setLifeCounterHapticFeedbackEnabled(false);

        SettingsRepository reloadedRepository =
                new SettingsRepository(new SettingsPrefsDataSource(context));
        assertFalse(reloadedRepository.isLifeCounterHapticFeedbackEnabled());
    }

    @Test
    public void mtgSetupCoachMarkDefaultsToNotDismissed() {
        SettingsRepository repository =
                new SettingsRepository(new SettingsPrefsDataSource(context));

        assertFalse(repository.isMtgSetupCoachMarkDismissed());
    }

    @Test
    public void setMtgSetupCoachMarkDismissedPersistsValue() {
        SettingsRepository repository =
                new SettingsRepository(new SettingsPrefsDataSource(context));

        repository.setMtgSetupCoachMarkDismissed(true);

        SettingsRepository reloadedRepository =
                new SettingsRepository(new SettingsPrefsDataSource(context));
        assertTrue(reloadedRepository.isMtgSetupCoachMarkDismissed());
    }
}
