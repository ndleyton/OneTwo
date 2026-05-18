package com.nicue.onetwo.ui.life;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.nicue.onetwo.R;
import java.util.ArrayList;
import java.util.List;

public class MtgLifeViewModel extends ViewModel {
    private static final String KEY_SHOWING_SETUP = "showingSetup";
    private static final String KEY_PLAYER_COUNT = "playerCount";
    private static final String KEY_STARTING_LIFE = "startingLife";
    private static final String KEY_CURRENT_LIVES = "currentLives";
    private static final String KEY_PLAYERS_ERROR_RES_ID = "playersErrorResId";
    private static final String KEY_LIFE_ERROR_RES_ID = "lifeErrorResId";
    private static final String KEY_COMMANDER_DAMAGE_ENABLED = "commanderDamageEnabled";
    private static final String KEY_COMMANDER_DAMAGE_MATRIX = "commanderDamageMatrix";

    private final SavedStateHandle savedStateHandle;
    private final MutableLiveData<MtgLifeUiState> uiState = new MutableLiveData<>();

    public MtgLifeViewModel(SavedStateHandle savedStateHandle) {
        this.savedStateHandle = savedStateHandle;

        if (!savedStateHandle.contains(KEY_SHOWING_SETUP)) {
            savedStateHandle.set(KEY_SHOWING_SETUP, true);
            savedStateHandle.set(KEY_PLAYER_COUNT, 4);
            savedStateHandle.set(KEY_STARTING_LIFE, 40);
            savedStateHandle.set(KEY_CURRENT_LIVES, new ArrayList<Integer>());
            savedStateHandle.set(KEY_PLAYERS_ERROR_RES_ID, null);
            savedStateHandle.set(KEY_LIFE_ERROR_RES_ID, null);
            savedStateHandle.set(KEY_COMMANDER_DAMAGE_ENABLED, true);
            savedStateHandle.set(KEY_COMMANDER_DAMAGE_MATRIX, new ArrayList<ArrayList<Integer>>());
        }

        updateUiState();
    }

    public LiveData<MtgLifeUiState> getUiState() {
        return uiState;
    }

    public void validateAndStartGame(String playersStr, String lifeStr) {
        Boolean lastEnabled = savedStateHandle.get(KEY_COMMANDER_DAMAGE_ENABLED);
        validateAndStartGame(playersStr, lifeStr, lastEnabled == null || lastEnabled);
    }

    public void validateAndStartGame(String playersStr, String lifeStr, boolean commanderDamageEnabled) {
        boolean valid = true;
        Integer playersError = null;
        Integer lifeError = null;

        int players = 0;
        try {
            players = Integer.parseInt(playersStr);
            if (players < 1 || players > 6) {
                playersError = R.string.mtg_setup_players_error;
                valid = false;
            }
        } catch (NumberFormatException e) {
            playersError = R.string.mtg_setup_players_error;
            valid = false;
        }

        int life = 0;
        try {
            life = Integer.parseInt(lifeStr);
            if (life <= 0) {
                lifeError = R.string.mtg_setup_life_error;
                valid = false;
            }
        } catch (NumberFormatException e) {
            lifeError = R.string.mtg_setup_life_error;
            valid = false;
        }

        savedStateHandle.set(KEY_PLAYERS_ERROR_RES_ID, playersError);
        savedStateHandle.set(KEY_LIFE_ERROR_RES_ID, lifeError);

        if (valid) {
            savedStateHandle.set(KEY_PLAYER_COUNT, players);
            savedStateHandle.set(KEY_STARTING_LIFE, life);

            ArrayList<Integer> lives = new ArrayList<>();
            for (int i = 0; i < players; i++) {
                lives.add(life);
            }
            savedStateHandle.set(KEY_CURRENT_LIVES, lives);
            savedStateHandle.set(KEY_COMMANDER_DAMAGE_ENABLED, commanderDamageEnabled);

            ArrayList<ArrayList<Integer>> matrix = new ArrayList<>();
            for (int d = 0; d < players; d++) {
                ArrayList<Integer> row = new ArrayList<>();
                for (int s = 0; s < players; s++) {
                    row.add(0);
                }
                matrix.add(row);
            }
            savedStateHandle.set(KEY_COMMANDER_DAMAGE_MATRIX, matrix);

            savedStateHandle.set(KEY_SHOWING_SETUP, false);
        }

        updateUiState();
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

    public void incrementCommanderDamage(int defenderSeatIndex, int sourceSeatIndex) {
        ArrayList<ArrayList<Integer>> matrix = savedStateHandle.get(KEY_COMMANDER_DAMAGE_MATRIX);
        if (matrix != null && defenderSeatIndex >= 0 && defenderSeatIndex < matrix.size()) {
            ArrayList<Integer> row = new ArrayList<>(matrix.get(defenderSeatIndex));
            if (sourceSeatIndex >= 0 && sourceSeatIndex < row.size()) {
                if (defenderSeatIndex != sourceSeatIndex) {
                    row.set(sourceSeatIndex, row.get(sourceSeatIndex) + 1);
                    ArrayList<ArrayList<Integer>> newMatrix = new ArrayList<>(matrix);
                    newMatrix.set(defenderSeatIndex, row);
                    savedStateHandle.set(KEY_COMMANDER_DAMAGE_MATRIX, newMatrix);
                    updateUiState();
                }
            }
        }
    }

    public void decrementCommanderDamage(int defenderSeatIndex, int sourceSeatIndex) {
        ArrayList<ArrayList<Integer>> matrix = savedStateHandle.get(KEY_COMMANDER_DAMAGE_MATRIX);
        if (matrix != null && defenderSeatIndex >= 0 && defenderSeatIndex < matrix.size()) {
            ArrayList<Integer> row = new ArrayList<>(matrix.get(defenderSeatIndex));
            if (sourceSeatIndex >= 0 && sourceSeatIndex < row.size()) {
                if (defenderSeatIndex != sourceSeatIndex) {
                    int val = row.get(sourceSeatIndex);
                    if (val > 0) {
                        row.set(sourceSeatIndex, val - 1);
                        ArrayList<ArrayList<Integer>> newMatrix = new ArrayList<>(matrix);
                        newMatrix.set(defenderSeatIndex, row);
                        savedStateHandle.set(KEY_COMMANDER_DAMAGE_MATRIX, newMatrix);
                        updateUiState();
                    }
                }
            }
        }
    }

    public void resetToSetup() {
        savedStateHandle.set(KEY_SHOWING_SETUP, true);
        savedStateHandle.set(KEY_PLAYERS_ERROR_RES_ID, null);
        savedStateHandle.set(KEY_LIFE_ERROR_RES_ID, null);
        updateUiState();
    }

    public void dismissSetup() {
        Boolean showing = savedStateHandle.get(KEY_SHOWING_SETUP);
        List<Integer> currentLives = savedStateHandle.get(KEY_CURRENT_LIVES);
        if (showing != null && showing && currentLives != null && !currentLives.isEmpty()) {
            savedStateHandle.set(KEY_SHOWING_SETUP, false);
            updateUiState();
        }
    }

    private int getRotationForSeat(int seatIndex, int totalPlayers) {
        switch (totalPlayers) {
            case 1:
                return 0;
            case 2:
                return (seatIndex == 0) ? 180 : 0;
            case 3:
                if (seatIndex == 0) return 180;
                if (seatIndex == 1) return 90;
                return 270;
            case 4:
                return (seatIndex % 2 == 0) ? 90 : 270;
            case 5:
                if (seatIndex == 4) return 0;
                return (seatIndex % 2 == 0) ? 90 : 270;
            case 6:
                return (seatIndex % 2 == 0) ? 90 : 270;
            default:
                return 0;
        }
    }

    private int getBackgroundColorResForSeat(int seatIndex) {
        switch (seatIndex) {
            case 0:
                return R.color.lifeCounterPlayer1;
            case 1:
                return R.color.lifeCounterPlayer2;
            case 2:
                return R.color.lifeCounterPlayer3;
            case 3:
                return R.color.lifeCounterPlayer4;
            case 4:
                return R.color.lifeCounterPlayer5;
            case 5:
                return R.color.lifeCounterPlayer6;
            default:
                return R.color.lifeCounterPlayer1;
        }
    }

    private int getForegroundColorResForSeat(int seatIndex) {
        switch (seatIndex) {
            case 0:
                return R.color.lifeCounterOnPlayer1;
            case 1:
                return R.color.lifeCounterOnPlayer2;
            case 2:
                return R.color.lifeCounterOnPlayer3;
            case 3:
                return R.color.lifeCounterOnPlayer4;
            case 4:
                return R.color.lifeCounterOnPlayer5;
            case 5:
                return R.color.lifeCounterOnPlayer6;
            default:
                return R.color.lifeCounterOnPlayer1;
        }
    }

    private List<CommanderDamageUiModel> buildCommanderDamages(int defenderIndex, ArrayList<ArrayList<Integer>> matrix, int totalPlayers) {
        List<CommanderDamageUiModel> list = new ArrayList<>();
        ArrayList<Integer> row = (matrix != null && defenderIndex < matrix.size()) ? matrix.get(defenderIndex) : null;
        for (int sourceIndex = 0; sourceIndex < totalPlayers; sourceIndex++) {
            int amount = 0;
            if (row != null && sourceIndex < row.size()) {
                amount = row.get(sourceIndex);
            }
            boolean self = (sourceIndex == defenderIndex);
            boolean lethal = amount >= 21;

            int bg;
            int fg;
            if (self) {
                bg = android.R.color.transparent;
                fg = R.color.m3_outline;
            } else if (lethal) {
                bg = R.color.secondAccent;
                fg = android.R.color.white;
            } else {
                bg = getBackgroundColorResForSeat(sourceIndex);
                fg = getForegroundColorResForSeat(sourceIndex);
            }

            list.add(new CommanderDamageUiModel(sourceIndex, amount, self, lethal, bg, fg));
        }
        return list;
    }

    private void updateUiState() {
        boolean showingSetup = savedStateHandle.get(KEY_SHOWING_SETUP);
        int playerCount = savedStateHandle.get(KEY_PLAYER_COUNT);
        int startingLife = savedStateHandle.get(KEY_STARTING_LIFE);
        List<Integer> currentLives = savedStateHandle.get(KEY_CURRENT_LIVES);
        Integer playersErrorResId = savedStateHandle.get(KEY_PLAYERS_ERROR_RES_ID);
        Integer lifeErrorResId = savedStateHandle.get(KEY_LIFE_ERROR_RES_ID);

        Boolean enabled = savedStateHandle.get(KEY_COMMANDER_DAMAGE_ENABLED);
        boolean isEnabled = (enabled != null) ? enabled : true;

        ArrayList<ArrayList<Integer>> matrix = savedStateHandle.get(KEY_COMMANDER_DAMAGE_MATRIX);

        List<LifePlayerUiModel> playerModels = new ArrayList<>();
        if (!showingSetup && currentLives != null) {
            int size = currentLives.size();
            for (int i = 0; i < size; i++) {
                int rotation = getRotationForSeat(i, size);
                int bg = getBackgroundColorResForSeat(i);
                int fg = getForegroundColorResForSeat(i);
                boolean isVisible = isEnabled && (size > 1);
                List<CommanderDamageUiModel> damages = buildCommanderDamages(i, matrix, size);
                playerModels.add(new LifePlayerUiModel(i, currentLives.get(i), rotation, bg, fg, isVisible, damages));
            }
        }

        uiState.setValue(
                new MtgLifeUiState(
                        showingSetup,
                        playerCount,
                        startingLife,
                        playerModels,
                        playersErrorResId,
                        lifeErrorResId,
                        isEnabled));
    }
}
