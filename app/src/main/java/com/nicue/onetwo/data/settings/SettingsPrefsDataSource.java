package com.nicue.onetwo.data.settings;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsPrefsDataSource {
    private static final String PREF_FILE = "SETTINGS_PREFS";
    private static final String KEY_ALWAYS_ON = "always_on";
    private static final String KEY_DARK_MODE = "dark_mode";
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
}
