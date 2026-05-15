package com.nicue.onetwo.ui.chooser;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.nicue.onetwo.data.settings.SettingsRepository;

public class ChooserViewModel extends ViewModel {
    private final SettingsRepository settingsRepository;
    private final MutableLiveData<Boolean> choosingOrder = new MutableLiveData<>();

    public ChooserViewModel(SettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
        refreshChoosingOrder();
    }

    public LiveData<Boolean> getChoosingOrder() {
        return choosingOrder;
    }

    public void refreshChoosingOrder() {
        choosingOrder.setValue(settingsRepository.isChooserOrderEnabled());
    }
}
