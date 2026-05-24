package com.nicue.onetwo.ui.life;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AbstractSavedStateViewModelFactory;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import androidx.savedstate.SavedStateRegistryOwner;
import com.nicue.onetwo.data.settings.SettingsRepository;

public class MtgLifeViewModelFactory extends AbstractSavedStateViewModelFactory {

    private final SettingsRepository settingsRepository;

    public MtgLifeViewModelFactory(
            @NonNull SavedStateRegistryOwner owner,
            @Nullable Bundle defaultArgs,
            @NonNull SettingsRepository settingsRepository) {
        super(owner, defaultArgs);
        this.settingsRepository = settingsRepository;
    }

    @NonNull @Override
    protected <T extends ViewModel> T create(
            @NonNull String key, @NonNull Class<T> modelClass, @NonNull SavedStateHandle handle) {
        if (modelClass.isAssignableFrom(MtgLifeViewModel.class)) {
            // noinspection unchecked
            return (T) new MtgLifeViewModel(handle, settingsRepository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
