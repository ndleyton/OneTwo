package com.nicue.onetwo.data.settings;

import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatDelegate;

public class SettingsRepository {
    private final SettingsPrefsDataSource dataSource;

    public SettingsRepository(SettingsPrefsDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public boolean isAlwaysOnEnabled() {
        return dataSource.isAlwaysOnEnabled();
    }

    public void setAlwaysOnEnabled(boolean enabled) {
        dataSource.setAlwaysOnEnabled(enabled);
    }

    public boolean isDarkModeEnabled() {
        return dataSource.isDarkModeEnabled();
    }

    public void setDarkModeEnabled(boolean enabled) {
        dataSource.setDarkModeEnabled(enabled);
    }

    public boolean isChooserOrderEnabled() {
        return dataSource.isChooserOrderEnabled();
    }

    public void setChooserOrderEnabled(boolean enabled) {
        dataSource.setChooserOrderEnabled(enabled);
    }

    public void applyNightMode() {
        boolean darkModeEnabled = isDarkModeEnabled();
        AppCompatDelegate.setDefaultNightMode(
                darkModeEnabled
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    public void applyKeepScreenOn(Window window) {
        if (isAlwaysOnEnabled()) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }
}
