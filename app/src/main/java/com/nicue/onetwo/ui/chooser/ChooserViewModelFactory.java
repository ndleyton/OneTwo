package com.nicue.onetwo.ui.chooser;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.nicue.onetwo.data.settings.SettingsRepository;

public class ChooserViewModelFactory implements ViewModelProvider.Factory {
    private final SettingsRepository settingsRepository;

    public ChooserViewModelFactory(SettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass,
                                          @NonNull androidx.lifecycle.viewmodel.CreationExtras extras) {
        if (modelClass.isAssignableFrom(ChooserViewModel.class)) {
            return (T) new ChooserViewModel(settingsRepository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class " + modelClass.getName());
    }
}
