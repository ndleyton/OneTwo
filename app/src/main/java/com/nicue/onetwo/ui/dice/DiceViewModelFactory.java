package com.nicue.onetwo.ui.dice;

import androidx.annotation.NonNull;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.SavedStateHandleSupport;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.nicue.onetwo.data.dice.DiceRepository;

public class DiceViewModelFactory implements ViewModelProvider.Factory {
    private final DiceRepository diceRepository;

    public DiceViewModelFactory(DiceRepository diceRepository) {
        this.diceRepository = diceRepository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass,
                                          @NonNull androidx.lifecycle.viewmodel.CreationExtras extras) {
        if (modelClass.isAssignableFrom(DiceViewModel.class)) {
            SavedStateHandle savedStateHandle = SavedStateHandleSupport.createSavedStateHandle(extras);
            return (T) new DiceViewModel(diceRepository, savedStateHandle);
        }
        throw new IllegalArgumentException("Unknown ViewModel class " + modelClass.getName());
    }
}
