package com.nicue.onetwo.ui.chooser;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

public class ChooserViewModel extends ViewModel {
    private static final String KEY_CHOOSING_ORDER = "choosing_order";
    private final SavedStateHandle savedStateHandle;
    private final LiveData<Boolean> choosingOrder;

    public ChooserViewModel(SavedStateHandle savedStateHandle) {
        this.savedStateHandle = savedStateHandle;
        if (savedStateHandle.get(KEY_CHOOSING_ORDER) == null) {
            savedStateHandle.set(KEY_CHOOSING_ORDER, false);
        }
        choosingOrder = savedStateHandle.getLiveData(KEY_CHOOSING_ORDER);
    }

    public LiveData<Boolean> getChoosingOrder() {
        return choosingOrder;
    }

    public void setChoosingOrder(boolean choosingOrder) {
        savedStateHandle.set(KEY_CHOOSING_ORDER, choosingOrder);
    }
}
