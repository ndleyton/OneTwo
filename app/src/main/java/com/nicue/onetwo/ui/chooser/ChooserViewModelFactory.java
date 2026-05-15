package com.nicue.onetwo.ui.chooser;

import androidx.annotation.NonNull;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.SavedStateHandleSupport;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class ChooserViewModelFactory implements ViewModelProvider.Factory {
    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass,
                                          @NonNull androidx.lifecycle.viewmodel.CreationExtras extras) {
        if (modelClass.isAssignableFrom(ChooserViewModel.class)) {
            SavedStateHandle savedStateHandle = SavedStateHandleSupport.createSavedStateHandle(extras);
            return (T) new ChooserViewModel(savedStateHandle);
        }
        throw new IllegalArgumentException("Unknown ViewModel class " + modelClass.getName());
    }
}
