package com.nicue.onetwo.ui.life;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

public class MtgLifeViewModel extends ViewModel {
    private static final String KEY_SHOWING_SETUP = "showingSetup";
    private static final String KEY_PLAYER_COUNT = "playerCount";
    private static final String KEY_STARTING_LIFE = "startingLife";
    private static final String KEY_CURRENT_LIVES = "currentLives";

    private final SavedStateHandle savedStateHandle;
    private final MutableLiveData<MtgLifeUiState> uiState = new MutableLiveData<>();

    public MtgLifeViewModel(SavedStateHandle savedStateHandle) {
        this.savedStateHandle = savedStateHandle;

        if (!savedStateHandle.contains(KEY_SHOWING_SETUP)) {
            savedStateHandle.set(KEY_SHOWING_SETUP, true);
            savedStateHandle.set(KEY_PLAYER_COUNT, 4);
            savedStateHandle.set(KEY_STARTING_LIFE, 40);
            savedStateHandle.set(KEY_CURRENT_LIVES, new ArrayList<Integer>());
        }

        updateUiState();
    }

    public LiveData<MtgLifeUiState> getUiState() {
        return uiState;
    }

    public void validateAndStartGame(String playersStr, String lifeStr) {
        try {
            int players = Integer.parseInt(playersStr);
            int life = Integer.parseInt(lifeStr);

            if (players < 1 || players > 6) {
                return;
            }
            if (life <= 0) {
                return;
            }

            savedStateHandle.set(KEY_PLAYER_COUNT, players);
            savedStateHandle.set(KEY_STARTING_LIFE, life);
            
            ArrayList<Integer> lives = new ArrayList<>();
            for (int i = 0; i < players; i++) {
                lives.add(life);
            }
            savedStateHandle.set(KEY_CURRENT_LIVES, lives);
            savedStateHandle.set(KEY_SHOWING_SETUP, false);
            
            updateUiState();
        } catch (NumberFormatException e) {
            // Invalid input
        }
    }

    public void incrementLife(int seatIndex) {
        List<Integer> currentLives = new ArrayList<>(savedStateHandle.get(KEY_CURRENT_LIVES));
        if (seatIndex >= 0 && seatIndex < currentLives.size()) {
            currentLives.set(seatIndex, currentLives.get(seatIndex) + 1);
            savedStateHandle.set(KEY_CURRENT_LIVES, currentLives);
            updateUiState();
        }
    }

    public void decrementLife(int seatIndex) {
        List<Integer> currentLives = new ArrayList<>(savedStateHandle.get(KEY_CURRENT_LIVES));
        if (seatIndex >= 0 && seatIndex < currentLives.size()) {
            currentLives.set(seatIndex, currentLives.get(seatIndex) - 1);
            savedStateHandle.set(KEY_CURRENT_LIVES, currentLives);
            updateUiState();
        }
    }

    public void resetToSetup() {
        savedStateHandle.set(KEY_SHOWING_SETUP, true);
        updateUiState();
    }

    private void updateUiState() {
        boolean showingSetup = savedStateHandle.get(KEY_SHOWING_SETUP);
        int playerCount = savedStateHandle.get(KEY_PLAYER_COUNT);
        int startingLife = savedStateHandle.get(KEY_STARTING_LIFE);
        List<Integer> currentLives = savedStateHandle.get(KEY_CURRENT_LIVES);

        List<LifePlayerUiModel> playerModels = new ArrayList<>();
        if (!showingSetup && currentLives != null) {
            for (int i = 0; i < currentLives.size(); i++) {
                playerModels.add(new LifePlayerUiModel(i, currentLives.get(i), 0, 0, 0));
            }
        }

        uiState.setValue(new MtgLifeUiState(showingSetup, playerCount, startingLife, playerModels));
    }
}
