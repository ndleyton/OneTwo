package com.nicue.onetwo.ui.life;

import java.util.List;

public class MtgLifeUiState {
    private final boolean showingSetup;
    private final int playerCount;
    private final int startingLife;
    private final List<LifePlayerUiModel> players;

    public MtgLifeUiState(boolean showingSetup, int playerCount, int startingLife, List<LifePlayerUiModel> players) {
        this.showingSetup = showingSetup;
        this.playerCount = playerCount;
        this.startingLife = startingLife;
        this.players = players;
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
}
