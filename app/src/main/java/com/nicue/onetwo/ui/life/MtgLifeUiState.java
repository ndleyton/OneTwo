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

    public MtgLifeUiState(
            boolean showingSetup,
            int playerCount,
            int startingLife,
            List<LifePlayerUiModel> players,
            Integer playersErrorResId,
            Integer lifeErrorResId,
            boolean commanderDamageEnabled) {
        this.showingSetup = showingSetup;
        this.playerCount = playerCount;
        this.startingLife = startingLife;
        this.players = players;
        this.playersErrorResId = playersErrorResId;
        this.lifeErrorResId = lifeErrorResId;
        this.commanderDamageEnabled = commanderDamageEnabled;
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
}
