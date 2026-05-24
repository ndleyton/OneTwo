package com.nicue.onetwo.data.settings;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsPrefsDataSource {
    private static final String PREF_FILE = "SETTINGS_PREFS";
    private static final String KEY_ALWAYS_ON = "always_on";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_CHOOSER_ORDER = "chooser_order";
    private static final String KEY_LIFE_COUNTER_HAPTIC_FEEDBACK = "life_counter_haptic_feedback";
    private final SharedPreferences sharedPreferences;

    public SettingsPrefsDataSource(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
    }

    public boolean isAlwaysOnEnabled() {
        return sharedPreferences.getBoolean(KEY_ALWAYS_ON, true);
    }

    public void setAlwaysOnEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_ALWAYS_ON, enabled).apply();
    }

    public boolean isDarkModeEnabled() {
        return sharedPreferences.getBoolean(KEY_DARK_MODE, false);
    }

    public void setDarkModeEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_DARK_MODE, enabled).apply();
    }

    public boolean isChooserOrderEnabled() {
        return sharedPreferences.getBoolean(KEY_CHOOSER_ORDER, false);
    }

    public void setChooserOrderEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_CHOOSER_ORDER, enabled).apply();
    }

    public boolean isLifeCounterHapticFeedbackEnabled() {
        return sharedPreferences.getBoolean(KEY_LIFE_COUNTER_HAPTIC_FEEDBACK, true);
    }

    public void setLifeCounterHapticFeedbackEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_LIFE_COUNTER_HAPTIC_FEEDBACK, enabled).apply();
    }
}
