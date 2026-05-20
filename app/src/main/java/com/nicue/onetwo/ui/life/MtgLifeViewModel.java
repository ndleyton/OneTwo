package com.nicue.onetwo.ui.life;

import android.os.SystemClock;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.nicue.onetwo.R;
import com.nicue.onetwo.core.HandlerTimerScheduler;
import com.nicue.onetwo.core.TimerScheduler;
import com.nicue.onetwo.utils.TimerBackend;
import com.nicue.onetwo.utils.TurnTimerEngine;
import java.util.ArrayList;
import java.util.List;

public class MtgLifeViewModel extends ViewModel {
    private static final String KEY_SHOWING_SETUP = "showingSetup";
    private static final String KEY_PLAYER_COUNT = "playerCount";
    private static final String KEY_STARTING_LIFE = "startingLife";
    private static final String KEY_CURRENT_LIVES = "currentLives";
    private static final String KEY_RECENT_LIFE_CHANGES = "recentLifeChanges";
    private static final String KEY_RECENT_LIFE_CHANGE_TIMESTAMPS = "recentLifeChangeTimestamps";
    private static final String KEY_PLAYERS_ERROR_RES_ID = "playersErrorResId";
    private static final String KEY_LIFE_ERROR_RES_ID = "lifeErrorResId";
    private static final String KEY_COMMANDER_DAMAGE_ENABLED = "commanderDamageEnabled";
    private static final String KEY_COMMANDER_DAMAGE_MATRIX = "commanderDamageMatrix";
    private static final String KEY_STARTING_PLAYER = "startingPlayer";

    private static final String KEY_TURN_TIMER_ENABLED = "turnTimerEnabled";
    private static final String KEY_TURN_TIMER_DURATION_MS = "turnTimerDurationMs";
    private static final String KEY_TURN_TIMER_REMAINING_TIMES = "turnTimerRemainingTimes";
    private static final String KEY_TURN_TIMER_ACTIVE_SEAT_INDEX = "turnTimerActiveSeatIndex";
    private static final String KEY_TURN_TIMER_PAUSED = "turnTimerPaused";
    private static final String KEY_TURN_TIMER_FINISHED = "turnTimerFinished";
    private static final String KEY_TURN_TIMER_LAST_TICK_TIME_MS = "turnTimerLastTickTimeMs";

    private static final int DEFAULT_PLAYER_COUNT = 4;
    private static final int DEFAULT_STARTING_LIFE = 40;
    private static final int MIN_PLAYER_COUNT = 1;
    private static final int MAX_PLAYER_COUNT = 6;
    private static final int COMMANDER_LETHAL_DAMAGE = 21;
    private static final long DEFAULT_TURN_TIMER_DURATION_MS = 300000L;
    public static final long RECENT_LIFE_CHANGE_WINDOW_MS = 2000L;

    private final SavedStateHandle savedStateHandle;
    private final MutableLiveData<MtgLifeUiState> uiState = new MutableLiveData<>();
    private final NowProvider nowProvider;
    private final TimerScheduler timerScheduler;
    private final TurnTimerEngine timerEngine;

    interface NowProvider {
        long now();
    }

    public MtgLifeViewModel(SavedStateHandle savedStateHandle) {
        this(savedStateHandle, SystemClock::elapsedRealtime, new HandlerTimerScheduler());
    }

    MtgLifeViewModel(SavedStateHandle savedStateHandle, NowProvider nowProvider) {
        this(savedStateHandle, nowProvider, new HandlerTimerScheduler());
    }

    MtgLifeViewModel(
            SavedStateHandle savedStateHandle,
            NowProvider nowProvider,
            TimerScheduler timerScheduler) {
        this.savedStateHandle = savedStateHandle;
        this.nowProvider = nowProvider;
        this.timerScheduler = timerScheduler;
        this.timerEngine = new TurnTimerEngine(DEFAULT_TURN_TIMER_DURATION_MS, 0L);

        if (!savedStateHandle.contains(KEY_SHOWING_SETUP)) {
            initializeDefaultState();
        }
        restoreTurnTimerState();

        // Resume turn timer ticking if it was running before recreation
        if (getTurnTimerEnabled() && !getTurnTimerPaused() && !getTurnTimerFinished()) {
            timerEngine.setLastTickTimeMs(nowProvider.now());
            persistTurnTimerState();
            timerScheduler.start(this::handleTick);
        }

        initStartingPlayer();
        updateUiState();
    }

    public LiveData<MtgLifeUiState> getUiState() {
        return uiState;
    }

    public void setStartingPlayer(Integer seatIndex) {
        savedStateHandle.set(KEY_STARTING_PLAYER, seatIndex);
        if (seatIndex != null) {
            timerEngine.setRunningIndex(seatIndex);
            savedStateHandle.set(KEY_TURN_TIMER_ACTIVE_SEAT_INDEX, seatIndex);
        }
        updateUiState();
    }

    public Integer getStartingPlayer() {
        return savedStateHandle.get(KEY_STARTING_PLAYER);
    }

    public void startTimer() {
        startTimer(nowProvider.now());
    }

    public void togglePlayPause() {
        if (!getTurnTimerEnabled() || getTurnTimerFinished()) {
            return;
        }
        if (getTurnTimerPaused()) {
            startTimer();
        } else {
            pauseTimer();
        }
    }

    public boolean getTurnTimerEnabled() {
        Boolean enabled = savedStateHandle.get(KEY_TURN_TIMER_ENABLED);
        return enabled != null && enabled;
    }

    public long getTurnTimerDurationMs() {
        return timerEngine.getConfiguredDurationMs();
    }

    public void setTurnTimerEnabled(boolean enabled) {
        savedStateHandle.set(KEY_TURN_TIMER_ENABLED, enabled);
        if (!enabled) {
            timerScheduler.stop();
            timerEngine.setPaused(true);
            savedStateHandle.set(KEY_TURN_TIMER_FINISHED, false);
            savedStateHandle.set(KEY_TURN_TIMER_REMAINING_TIMES, null);
            savedStateHandle.set(KEY_TURN_TIMER_ACTIVE_SEAT_INDEX, 0);
            savedStateHandle.set(KEY_TURN_TIMER_PAUSED, true);
            savedStateHandle.set(KEY_TURN_TIMER_LAST_TICK_TIME_MS, 0L);
        }
        updateUiState();
    }

    public void setTurnTimerDurationMs(long durationMs) {
        timerEngine.setConfiguredDurationMs(durationMs);
        savedStateHandle.set(KEY_TURN_TIMER_DURATION_MS, durationMs);
        updateUiState();
    }

    public ArrayList<Long> getTurnTimerRemainingTimes() {
        return new ArrayList<>(timerEngine.getRemainingTimes());
    }

    public int getTurnTimerActiveSeatIndex() {
        return timerEngine.getRunningIndex();
    }

    public boolean getTurnTimerPaused() {
        return timerEngine.isPaused();
    }

    public boolean getTurnTimerFinished() {
        Boolean finished = savedStateHandle.get(KEY_TURN_TIMER_FINISHED);
        return finished != null && finished;
    }

    private long getTurnTimerLastTickTimeMs() {
        return timerEngine.getLastTickTimeMs();
    }

    private void setTurnTimerLastTickTimeMs(long timeMs) {
        timerEngine.setLastTickTimeMs(timeMs);
        savedStateHandle.set(KEY_TURN_TIMER_LAST_TICK_TIME_MS, timeMs);
    }

    public boolean validateAndStartGame(String playersStr, String lifeStr) {
        return validateAndStartGame(
                playersStr, lifeStr, getCommanderDamageEnabled(), getTurnTimerEnabled());
    }

    public boolean validateAndStartGame(
            String playersStr, String lifeStr, boolean commanderDamageEnabled) {
        return validateAndStartGame(playersStr, lifeStr, commanderDamageEnabled, getTurnTimerEnabled());
    }

    public boolean validateAndStartGame(
            String playersStr,
            String lifeStr,
            boolean commanderDamageEnabled,
            boolean turnTimerEnabled) {
        Integer parsedPlayerCount = parsePlayerCount(playersStr);
        Integer parsedStartingLife = parseStartingLife(lifeStr);

        savedStateHandle.set(
                KEY_PLAYERS_ERROR_RES_ID,
                parsedPlayerCount == null ? R.string.mtg_setup_players_error : null);
        savedStateHandle.set(
                KEY_LIFE_ERROR_RES_ID,
                parsedStartingLife == null ? R.string.mtg_setup_life_error : null);

        if (parsedPlayerCount == null || parsedStartingLife == null) {
            savedStateHandle.set(KEY_SHOWING_SETUP, true);
            updateUiState();
            return false;
        }

        savedStateHandle.set(KEY_PLAYER_COUNT, parsedPlayerCount);
        savedStateHandle.set(KEY_STARTING_LIFE, parsedStartingLife);
        savedStateHandle.set(
                KEY_CURRENT_LIVES, createInitialLives(parsedPlayerCount, parsedStartingLife));
        savedStateHandle.set(
                KEY_RECENT_LIFE_CHANGES, createInitialRecentLifeChanges(parsedPlayerCount));
        savedStateHandle.set(
                KEY_RECENT_LIFE_CHANGE_TIMESTAMPS,
                createInitialRecentLifeChangeTimestamps(parsedPlayerCount));
        savedStateHandle.set(KEY_COMMANDER_DAMAGE_ENABLED, commanderDamageEnabled);
        savedStateHandle.set(
                KEY_COMMANDER_DAMAGE_MATRIX, createCommanderDamageMatrix(parsedPlayerCount));

        savedStateHandle.set(KEY_TURN_TIMER_ENABLED, turnTimerEnabled);
        Integer startingPlayer = getStartingPlayer();
        int initialActiveSeat = startingPlayer == null ? 0 : startingPlayer;
        if (turnTimerEnabled) {
            long duration = getTurnTimerDurationMs();
            ArrayList<Long> initialRemaining = new ArrayList<>();
            for (int i = 0; i < parsedPlayerCount; i++) {
                initialRemaining.add(duration);
            }
            timerEngine.setRemainingTimes(initialRemaining);
            timerEngine.setRunningIndex(initialActiveSeat);
            timerEngine.setPaused(true);
            timerEngine.setLastTickTimeMs(0L);
            savedStateHandle.set(KEY_TURN_TIMER_FINISHED, duration <= 0L);
            persistTurnTimerState();
        } else {
            timerScheduler.stop();
            timerEngine.setRemainingTimes(new ArrayList<>());
            timerEngine.setRunningIndex(initialActiveSeat);
            timerEngine.setPaused(true);
            timerEngine.setLastTickTimeMs(0L);
            savedStateHandle.set(KEY_TURN_TIMER_REMAINING_TIMES, null);
            savedStateHandle.set(KEY_TURN_TIMER_ACTIVE_SEAT_INDEX, initialActiveSeat);
            savedStateHandle.set(KEY_TURN_TIMER_PAUSED, true);
            savedStateHandle.set(KEY_TURN_TIMER_FINISHED, false);
            savedStateHandle.set(KEY_TURN_TIMER_LAST_TICK_TIME_MS, 0L);
        }

        savedStateHandle.set(KEY_SHOWING_SETUP, false);

        updateUiState();
        return true;
    }

    public void incrementLife(int seatIndex) {
        updateLifeTotal(seatIndex, 1);
    }

    public void decrementLife(int seatIndex) {
        updateLifeTotal(seatIndex, -1);
    }

    public void incrementLifeBy(int seatIndex, int amount) {
        updateLifeTotal(seatIndex, amount);
    }

    public void decrementLifeBy(int seatIndex, int amount) {
        updateLifeTotal(seatIndex, -amount);
    }

    public void incrementCommanderDamage(int defenderSeatIndex, int sourceSeatIndex) {
        updateCommanderDamage(defenderSeatIndex, sourceSeatIndex, 1);
    }

    public void decrementCommanderDamage(int defenderSeatIndex, int sourceSeatIndex) {
        updateCommanderDamage(defenderSeatIndex, sourceSeatIndex, -1);
    }

    public void passTurn(int seatIndex) {
        if (!getTurnTimerEnabled()
                || getTurnTimerFinished()
                || seatIndex != getTurnTimerActiveSeatIndex()) {
            return;
        }

        long nowMs = nowProvider.now();
        int totalPlayers = getPlayerCount();
        int nextSeat = getNextSeatClockwise(seatIndex, totalPlayers);

        if (getTurnTimerPaused()) {
            timerEngine.setRunningIndex(nextSeat);
            startTimer(nowMs);
        } else {
            boolean expired = timerEngine.tick(nowMs);
            if (expired) {
                finishTurnTimer();
                return;
            }
            timerEngine.advanceTo(nextSeat, nowMs);
            persistTurnTimerState();
            updateUiState();
        }
    }

    public void pauseForBackground() {
        pauseTimer();
    }

    @Override
    protected void onCleared() {
        timerScheduler.stop();
        super.onCleared();
    }

    private void startTimer(long nowMs) {
        if (!getTurnTimerEnabled() || getTurnTimerFinished()) {
            return;
        }
        timerEngine.start(nowMs);
        if (!timerEngine.isPaused()) {
            timerScheduler.start(this::handleTick);
        }
        persistTurnTimerState();
        updateUiState();
    }

    private void pauseTimer() {
        if (!getTurnTimerEnabled() || getTurnTimerPaused()) {
            return;
        }
        timerScheduler.stop();
        timerEngine.pause();
        persistTurnTimerState();
        updateUiState();
    }

    private void handleTick(long nowMs) {
        if (!getTurnTimerEnabled() || getTurnTimerPaused() || getTurnTimerFinished()) {
            return;
        }
        boolean expired = timerEngine.tick(nowMs);
        if (expired) {
            finishTurnTimer();
            return;
        }
        persistTurnTimerState();
        updateUiState();
    }

    public void resetToSetup() {
        pauseTimer();
        savedStateHandle.set(KEY_SHOWING_SETUP, true);
        savedStateHandle.set(KEY_PLAYERS_ERROR_RES_ID, null);
        savedStateHandle.set(KEY_LIFE_ERROR_RES_ID, null);
        updateUiState();
    }

    public void dismissSetup() {
        if (!isShowingSetup()) {
            return;
        }

        List<Integer> currentLives = getCurrentLives();
        if (currentLives != null && !currentLives.isEmpty()) {
            savedStateHandle.set(KEY_SHOWING_SETUP, false);
            updateUiState();
        }
    }

    private void initializeDefaultState() {
        savedStateHandle.set(KEY_SHOWING_SETUP, false);
        savedStateHandle.set(KEY_PLAYER_COUNT, DEFAULT_PLAYER_COUNT);
        savedStateHandle.set(KEY_STARTING_LIFE, DEFAULT_STARTING_LIFE);
        savedStateHandle.set(
                KEY_CURRENT_LIVES, createInitialLives(DEFAULT_PLAYER_COUNT, DEFAULT_STARTING_LIFE));
        savedStateHandle.set(
                KEY_RECENT_LIFE_CHANGES, createInitialRecentLifeChanges(DEFAULT_PLAYER_COUNT));
        savedStateHandle.set(
                KEY_RECENT_LIFE_CHANGE_TIMESTAMPS,
                createInitialRecentLifeChangeTimestamps(DEFAULT_PLAYER_COUNT));
        savedStateHandle.set(KEY_PLAYERS_ERROR_RES_ID, null);
        savedStateHandle.set(KEY_LIFE_ERROR_RES_ID, null);
        savedStateHandle.set(KEY_COMMANDER_DAMAGE_ENABLED, true);
        savedStateHandle.set(
                KEY_COMMANDER_DAMAGE_MATRIX, createCommanderDamageMatrix(DEFAULT_PLAYER_COUNT));

        savedStateHandle.set(KEY_TURN_TIMER_ENABLED, false);
        savedStateHandle.set(KEY_TURN_TIMER_DURATION_MS, DEFAULT_TURN_TIMER_DURATION_MS);
        timerEngine.setConfiguredDurationMs(DEFAULT_TURN_TIMER_DURATION_MS);
        timerEngine.setRemainingTimes(new ArrayList<>());
        timerEngine.setRunningIndex(0);
        timerEngine.setPaused(true);
        timerEngine.setLastTickTimeMs(0L);
        savedStateHandle.set(KEY_TURN_TIMER_REMAINING_TIMES, null);
        savedStateHandle.set(KEY_TURN_TIMER_ACTIVE_SEAT_INDEX, 0);
        savedStateHandle.set(KEY_TURN_TIMER_PAUSED, true);
        savedStateHandle.set(KEY_TURN_TIMER_FINISHED, false);
        savedStateHandle.set(KEY_TURN_TIMER_LAST_TICK_TIME_MS, 0L);
    }

    private Integer parsePlayerCount(String playersStr) {
        try {
            int players = Integer.parseInt(playersStr);
            return players >= MIN_PLAYER_COUNT && players <= MAX_PLAYER_COUNT ? players : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseStartingLife(String lifeStr) {
        try {
            int life = Integer.parseInt(lifeStr);
            return life > 0 ? life : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private ArrayList<Integer> createInitialLives(int playerCount, int startingLife) {
        ArrayList<Integer> lives = new ArrayList<>();
        for (int i = 0; i < playerCount; i++) {
            lives.add(startingLife);
        }
        return lives;
    }

    private ArrayList<ArrayList<Integer>> createCommanderDamageMatrix(int playerCount) {
        ArrayList<ArrayList<Integer>> matrix = new ArrayList<>();
        for (int defenderIndex = 0; defenderIndex < playerCount; defenderIndex++) {
            ArrayList<Integer> row = new ArrayList<>();
            for (int sourceIndex = 0; sourceIndex < playerCount; sourceIndex++) {
                row.add(0);
            }
            matrix.add(row);
        }
        return matrix;
    }

    private ArrayList<Integer> createInitialRecentLifeChanges(int playerCount) {
        ArrayList<Integer> recentChanges = new ArrayList<>();
        for (int i = 0; i < playerCount; i++) {
            recentChanges.add(0);
        }
        return recentChanges;
    }

    private ArrayList<Long> createInitialRecentLifeChangeTimestamps(int playerCount) {
        ArrayList<Long> timestamps = new ArrayList<>();
        for (int i = 0; i < playerCount; i++) {
            timestamps.add(0L);
        }
        return timestamps;
    }

    private void updateLifeTotal(int seatIndex, int delta) {
        List<Integer> currentLives = getCurrentLives();
        if (!isValidSeatIndex(currentLives, seatIndex)) {
            return;
        }

        ArrayList<Integer> updatedLives = new ArrayList<>(currentLives);
        updatedLives.set(seatIndex, updatedLives.get(seatIndex) + delta);
        savedStateHandle.set(KEY_CURRENT_LIVES, updatedLives);
        updateRecentLifeChange(seatIndex, delta);
        updateUiState();
    }

    private void updateCommanderDamage(int defenderSeatIndex, int sourceSeatIndex, int delta) {
        ArrayList<ArrayList<Integer>> matrix = getCommanderDamageMatrix();
        if (!isValidSeatIndex(matrix, defenderSeatIndex) || defenderSeatIndex == sourceSeatIndex) {
            return;
        }

        ArrayList<Integer> defenderRow = new ArrayList<>(matrix.get(defenderSeatIndex));
        if (!isValidSeatIndex(defenderRow, sourceSeatIndex)) {
            return;
        }

        int currentAmount = defenderRow.get(sourceSeatIndex);
        int updatedAmount = currentAmount + delta;
        if (updatedAmount < 0) {
            return;
        }

        defenderRow.set(sourceSeatIndex, updatedAmount);
        ArrayList<ArrayList<Integer>> updatedMatrix = new ArrayList<>(matrix);
        updatedMatrix.set(defenderSeatIndex, defenderRow);
        savedStateHandle.set(KEY_COMMANDER_DAMAGE_MATRIX, updatedMatrix);

        updateLifeTotalForCommanderDamage(defenderSeatIndex, -delta);
        updateUiState();
    }

    private void updateLifeTotalForCommanderDamage(int defenderSeatIndex, int delta) {
        List<Integer> currentLives = getCurrentLives();
        if (!isValidSeatIndex(currentLives, defenderSeatIndex)) {
            return;
        }

        ArrayList<Integer> updatedLives = new ArrayList<>(currentLives);
        updatedLives.set(defenderSeatIndex, updatedLives.get(defenderSeatIndex) + delta);
        savedStateHandle.set(KEY_CURRENT_LIVES, updatedLives);
        updateRecentLifeChange(defenderSeatIndex, delta);
    }

    private void updateRecentLifeChange(int seatIndex, int delta) {
        List<Integer> recentLifeChanges = getRecentLifeChanges();
        List<Long> recentLifeChangeTimestamps = getRecentLifeChangeTimestamps();
        if (!isValidSeatIndex(recentLifeChanges, seatIndex)
                || !isValidSeatIndex(recentLifeChangeTimestamps, seatIndex)) {
            return;
        }

        long nowMs = nowProvider.now();
        int previousChange = recentLifeChanges.get(seatIndex);
        long previousTimestamp = recentLifeChangeTimestamps.get(seatIndex);
        boolean withinAggregationWindow =
                previousTimestamp > 0 && nowMs - previousTimestamp <= RECENT_LIFE_CHANGE_WINDOW_MS;

        ArrayList<Integer> updatedRecentLifeChanges = new ArrayList<>(recentLifeChanges);
        updatedRecentLifeChanges.set(
                seatIndex, withinAggregationWindow ? previousChange + delta : delta);
        savedStateHandle.set(KEY_RECENT_LIFE_CHANGES, updatedRecentLifeChanges);

        ArrayList<Long> updatedRecentLifeChangeTimestamps =
                new ArrayList<>(recentLifeChangeTimestamps);
        updatedRecentLifeChangeTimestamps.set(seatIndex, nowMs);
        savedStateHandle.set(KEY_RECENT_LIFE_CHANGE_TIMESTAMPS, updatedRecentLifeChangeTimestamps);
    }

    private boolean isShowingSetup() {
        return Boolean.TRUE.equals(savedStateHandle.get(KEY_SHOWING_SETUP));
    }

    private boolean getCommanderDamageEnabled() {
        Boolean enabled = savedStateHandle.get(KEY_COMMANDER_DAMAGE_ENABLED);
        return enabled == null || enabled;
    }

    private int getPlayerCount() {
        Integer playerCount = savedStateHandle.get(KEY_PLAYER_COUNT);
        return playerCount != null ? playerCount : DEFAULT_PLAYER_COUNT;
    }

    private int getStartingLife() {
        Integer startingLife = savedStateHandle.get(KEY_STARTING_LIFE);
        return startingLife != null ? startingLife : DEFAULT_STARTING_LIFE;
    }

    private List<Integer> getCurrentLives() {
        return savedStateHandle.get(KEY_CURRENT_LIVES);
    }

    private ArrayList<ArrayList<Integer>> getCommanderDamageMatrix() {
        return savedStateHandle.get(KEY_COMMANDER_DAMAGE_MATRIX);
    }

    private List<Integer> getRecentLifeChanges() {
        return savedStateHandle.get(KEY_RECENT_LIFE_CHANGES);
    }

    private List<Long> getRecentLifeChangeTimestamps() {
        return savedStateHandle.get(KEY_RECENT_LIFE_CHANGE_TIMESTAMPS);
    }

    private boolean isValidSeatIndex(List<?> list, int seatIndex) {
        return list != null && seatIndex >= 0 && seatIndex < list.size();
    }

    private int getRotationForSeat(int seatIndex, int totalPlayers) {
        return switch (totalPlayers) {
            case 1 -> 0;
            case 2 -> seatIndex == 0 ? 180 : 0;
            case 3 ->
                    switch (seatIndex) {
                        case 0 -> 0;
                        case 1 -> 90;
                        default -> 270;
                    };
            case 4 -> seatIndex % 2 == 0 ? 90 : 270;
            case 5 -> seatIndex == 4 ? 0 : (seatIndex % 2 == 0 ? 90 : 270);
            case 6 -> seatIndex % 2 == 0 ? 90 : 270;
            default -> 0;
        };
    }

    private int getBackgroundColorResForSeat(int seatIndex) {
        return switch (seatIndex) {
            case 0 -> R.color.lifeCounterPlayer1;
            case 1 -> R.color.lifeCounterPlayer2;
            case 2 -> R.color.lifeCounterPlayer3;
            case 3 -> R.color.lifeCounterPlayer4;
            case 4 -> R.color.lifeCounterPlayer5;
            case 5 -> R.color.lifeCounterPlayer6;
            default -> R.color.lifeCounterPlayer1;
        };
    }

    private int getForegroundColorResForSeat(int seatIndex) {
        return switch (seatIndex) {
            case 0 -> R.color.lifeCounterOnPlayer1;
            case 1 -> R.color.lifeCounterOnPlayer2;
            case 2 -> R.color.lifeCounterOnPlayer3;
            case 3 -> R.color.lifeCounterOnPlayer4;
            case 4 -> R.color.lifeCounterOnPlayer5;
            case 5 -> R.color.lifeCounterOnPlayer6;
            default -> R.color.lifeCounterOnPlayer1;
        };
    }

    private List<CommanderDamageUiModel> buildCommanderDamages(
            int defenderSeatIndex, ArrayList<ArrayList<Integer>> matrix, int totalPlayers) {
        List<CommanderDamageUiModel> damages = new ArrayList<>();
        ArrayList<Integer> defenderRow =
                matrix != null && defenderSeatIndex < matrix.size()
                        ? matrix.get(defenderSeatIndex)
                        : null;

        for (int sourceSeatIndex = 0; sourceSeatIndex < totalPlayers; sourceSeatIndex++) {
            int amount =
                    defenderRow != null && sourceSeatIndex < defenderRow.size()
                            ? defenderRow.get(sourceSeatIndex)
                            : 0;
            boolean self = sourceSeatIndex == defenderSeatIndex;
            boolean lethal = amount >= COMMANDER_LETHAL_DAMAGE;

            int backgroundColorRes =
                    self
                            ? android.R.color.transparent
                            : lethal
                                    ? R.color.secondAccent
                                    : getBackgroundColorResForSeat(sourceSeatIndex);
            int foregroundColorRes =
                    self
                            ? R.color.m3_outline
                            : lethal
                                    ? android.R.color.white
                                    : getForegroundColorResForSeat(sourceSeatIndex);

            damages.add(
                    new CommanderDamageUiModel(
                            sourceSeatIndex,
                            amount,
                            self,
                            lethal,
                            backgroundColorRes,
                            foregroundColorRes));
        }

        return damages;
    }

    private int getNextSeatClockwise(int activeSeat, int totalPlayers) {
        return switch (totalPlayers) {
            case 1 -> 0;
            case 2 -> activeSeat == 0 ? 1 : 0;
            case 3 ->
                    switch (activeSeat) {
                        case 0 -> 1;
                        case 1 -> 2;
                        default -> 0;
                    };
            case 4 ->
                    switch (activeSeat) {
                        case 0 -> 1;
                        case 1 -> 3;
                        case 3 -> 2;
                        default -> 0;
                    };
            case 5 ->
                    switch (activeSeat) {
                        case 0 -> 1;
                        case 1 -> 3;
                        case 3 -> 4;
                        case 4 -> 2;
                        default -> 0;
                    };
            case 6 ->
                    switch (activeSeat) {
                        case 0 -> 1;
                        case 1 -> 3;
                        case 3 -> 5;
                        case 5 -> 4;
                        case 4 -> 2;
                        default -> 0;
                    };
            default -> 0;
        };
    }

    private void updateUiState() {
        boolean showingSetup = isShowingSetup();
        int playerCount = getPlayerCount();
        int startingLife = getStartingLife();
        List<Integer> currentLives = getCurrentLives();
        Integer playersErrorResId = savedStateHandle.get(KEY_PLAYERS_ERROR_RES_ID);
        Integer lifeErrorResId = savedStateHandle.get(KEY_LIFE_ERROR_RES_ID);
        boolean commanderDamageEnabled = getCommanderDamageEnabled();
        ArrayList<ArrayList<Integer>> commanderDamageMatrix = getCommanderDamageMatrix();
        List<Integer> recentLifeChanges = getRecentLifeChanges();
        List<Long> recentLifeChangeTimestamps = getRecentLifeChangeTimestamps();

        boolean turnTimerEnabled = getTurnTimerEnabled();
        boolean turnTimerPaused = getTurnTimerPaused();
        boolean turnTimerFinished = getTurnTimerFinished();
        int activeSeatIndex = getTurnTimerActiveSeatIndex();
        ArrayList<Long> remainingTimes = getTurnTimerRemainingTimes();

        List<LifePlayerUiModel> players = new ArrayList<>();
        if (!showingSetup && currentLives != null) {
            int totalPlayers = currentLives.size();
            for (int seatIndex = 0; seatIndex < totalPlayers; seatIndex++) {
                int recentLifeChange =
                        recentLifeChanges != null && seatIndex < recentLifeChanges.size()
                                ? recentLifeChanges.get(seatIndex)
                                : 0;
                long recentLifeChangeTimestampMs =
                        recentLifeChangeTimestamps != null
                                        && seatIndex < recentLifeChangeTimestamps.size()
                                ? recentLifeChangeTimestamps.get(seatIndex)
                                : 0L;

                boolean timerVisible =
                        turnTimerEnabled
                                && remainingTimes != null
                                && seatIndex < remainingTimes.size();
                String timerDisplay = "";
                boolean timerActive = false;
                boolean timerExpired = false;
                boolean passEnabled = false;
                boolean startTimerVisible = false;

                if (timerVisible) {
                    long remainingTime = remainingTimes.get(seatIndex);
                    timerDisplay = TimerBackend.formatRemainingTime(remainingTime, 10000L);
                    timerActive = seatIndex == activeSeatIndex;
                    timerExpired = remainingTime <= 0L;
                    passEnabled = timerActive && !turnTimerFinished && remainingTime > 0L;
                    startTimerVisible = timerActive && turnTimerPaused && !turnTimerFinished;
                }

                players.add(
                        new LifePlayerUiModel(
                                seatIndex,
                                currentLives.get(seatIndex),
                                getRotationForSeat(seatIndex, totalPlayers),
                                getBackgroundColorResForSeat(seatIndex),
                                getForegroundColorResForSeat(seatIndex),
                                recentLifeChange,
                                recentLifeChangeTimestampMs,
                                commanderDamageEnabled && totalPlayers > 1,
                                buildCommanderDamages(
                                        seatIndex, commanderDamageMatrix, totalPlayers),
                                timerVisible,
                                timerDisplay,
                                timerActive,
                                timerExpired,
                                passEnabled,
                                startTimerVisible));
            }
        }

        uiState.setValue(
                new MtgLifeUiState(
                        showingSetup,
                        playerCount,
                        startingLife,
                        players,
                        playersErrorResId,
                        lifeErrorResId,
                        commanderDamageEnabled,
                        turnTimerEnabled,
                        turnTimerPaused,
                        turnTimerFinished));
    }

    private void restoreTurnTimerState() {
        Long savedConfiguredDuration = savedStateHandle.get(KEY_TURN_TIMER_DURATION_MS);
        ArrayList<Long> savedRemainingTimes = savedStateHandle.get(KEY_TURN_TIMER_REMAINING_TIMES);
        Integer savedActiveSeat = savedStateHandle.get(KEY_TURN_TIMER_ACTIVE_SEAT_INDEX);
        Boolean savedPaused = savedStateHandle.get(KEY_TURN_TIMER_PAUSED);
        Long savedLastTick = savedStateHandle.get(KEY_TURN_TIMER_LAST_TICK_TIME_MS);

        timerEngine.setConfiguredDurationMs(
                savedConfiguredDuration == null
                        ? DEFAULT_TURN_TIMER_DURATION_MS
                        : savedConfiguredDuration);
        timerEngine.setRemainingTimes(
                savedRemainingTimes == null ? new ArrayList<>() : savedRemainingTimes);
        timerEngine.setRunningIndex(savedActiveSeat == null ? 0 : savedActiveSeat);
        timerEngine.setPaused(savedPaused == null || savedPaused);
        timerEngine.setLastTickTimeMs(savedLastTick == null ? 0L : savedLastTick);
    }

    private void persistTurnTimerState() {
        savedStateHandle.set(KEY_TURN_TIMER_DURATION_MS, timerEngine.getConfiguredDurationMs());
        savedStateHandle.set(
                KEY_TURN_TIMER_REMAINING_TIMES, new ArrayList<>(timerEngine.getRemainingTimes()));
        savedStateHandle.set(KEY_TURN_TIMER_ACTIVE_SEAT_INDEX, timerEngine.getRunningIndex());
        savedStateHandle.set(KEY_TURN_TIMER_PAUSED, timerEngine.isPaused());
        savedStateHandle.set(KEY_TURN_TIMER_LAST_TICK_TIME_MS, timerEngine.getLastTickTimeMs());
    }

    private void finishTurnTimer() {
        timerScheduler.stop();
        savedStateHandle.set(KEY_TURN_TIMER_FINISHED, true);
        persistTurnTimerState();
        updateUiState();
    }

    private void initStartingPlayer() {
        if (savedStateHandle.contains("starting_player")) {
            Object val = savedStateHandle.get("starting_player");
            Integer parsed = parseInteger(val);
            if (parsed != null) {
                setStartingPlayer(parsed);
            }
        } else if (savedStateHandle.contains(KEY_STARTING_PLAYER)) {
            Object val = savedStateHandle.get(KEY_STARTING_PLAYER);
            Integer parsed = parseInteger(val);
            if (parsed != null) {
                setStartingPlayer(parsed);
            }
        }
    }

    private Integer parseInteger(Object val) {
        if (val instanceof Integer) {
            return (Integer) val;
        } else if (val instanceof String) {
            try {
                return Integer.parseInt((String) val);
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }
}
