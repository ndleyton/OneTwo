package com.nicue.onetwo.ui.life;

import java.util.List;

public class MtgLifeUiState {
    private final boolean showingSetup;
    private final int playerCount;
    private final int startingLife;
    private final List<LifePlayerUiModel> players;
    private final Integer playersErrorResId;
    private final Integer lifeErrorResId;
    private final boolean commanderDamageEnabled;
    private final boolean turnTimerEnabled;
    private final boolean turnTimerPaused;
    private final boolean turnTimerFinished;

    public MtgLifeUiState(
            boolean showingSetup,
            int playerCount,
            int startingLife,
            List<LifePlayerUiModel> players,
            Integer playersErrorResId,
            Integer lifeErrorResId,
            boolean commanderDamageEnabled,
            boolean turnTimerEnabled,
            boolean turnTimerPaused,
            boolean turnTimerFinished) {
        this.showingSetup = showingSetup;
        this.playerCount = playerCount;
        this.startingLife = startingLife;
        this.players = players;
        this.playersErrorResId = playersErrorResId;
        this.lifeErrorResId = lifeErrorResId;
        this.commanderDamageEnabled = commanderDamageEnabled;
        this.turnTimerEnabled = turnTimerEnabled;
        this.turnTimerPaused = turnTimerPaused;
        this.turnTimerFinished = turnTimerFinished;
    }

    public boolean isShowingSetup() {
        return showingSetup;
    }

    public int getPlayerCount() {
        return playerCount;
    }

    public int getStartingLife() {
        return startingLife;
    }

    public List<LifePlayerUiModel> getPlayers() {
        return players;
    }

    public Integer getPlayersErrorResId() {
        return playersErrorResId;
    }

    public Integer getLifeErrorResId() {
        return lifeErrorResId;
    }

    public boolean isCommanderDamageEnabled() {
        return commanderDamageEnabled;
    }

    public boolean isTurnTimerEnabled() {
        return turnTimerEnabled;
    }

    public boolean isTurnTimerPaused() {
        return turnTimerPaused;
    }

    public boolean isTurnTimerFinished() {
        return turnTimerFinished;
    }
}
