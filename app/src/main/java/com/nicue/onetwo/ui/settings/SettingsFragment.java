package com.nicue.onetwo.ui.settings;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.nicue.onetwo.OneTwoApplication;
import com.nicue.onetwo.R;
import com.nicue.onetwo.data.settings.SettingsRepository;

public class SettingsFragment extends PreferenceFragmentCompat {
    public interface SettingsApplier {
        void applyKeepScreenOn(boolean keepScreenOn);

        void applyDarkMode(boolean darkModeEnabled);
    }

    private SettingsRepository settingsRepository;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
        settingsRepository = ((OneTwoApplication) requireActivity().getApplication())
                .getAppContainer()
                .getSettingsRepository();

        configureAlwaysOnPreference();
        configureDarkModePreference();
    }

    private void configureAlwaysOnPreference() {
        SwitchPreferenceCompat preference = findPreference("always_on");
        if (preference == null) {
            return;
        }
        preference.setPersistent(false);
        preference.setChecked(settingsRepository.isAlwaysOnEnabled());
        preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                boolean enabled = (Boolean) newValue;
                settingsRepository.setAlwaysOnEnabled(enabled);
                getSettingsApplier().applyKeepScreenOn(enabled);
                return true;
            }
        });
    }

    private void configureDarkModePreference() {
        SwitchPreferenceCompat preference = findPreference("dark_mode");
        if (preference == null) {
            return;
        }
        preference.setPersistent(false);
        preference.setChecked(settingsRepository.isDarkModeEnabled());
        preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                boolean enabled = (Boolean) newValue;
                settingsRepository.setDarkModeEnabled(enabled);
                getSettingsApplier().applyDarkMode(enabled);
                return true;
            }
        });
    }

    private SettingsApplier getSettingsApplier() {
        if (!(requireActivity() instanceof SettingsApplier)) {
            throw new IllegalStateException("Host activity must implement SettingsApplier");
        }
        return (SettingsApplier) requireActivity();
    }
}
