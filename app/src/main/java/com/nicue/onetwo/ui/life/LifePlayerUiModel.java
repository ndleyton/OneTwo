package com.nicue.onetwo.ui.life;

import java.util.List;

public class LifePlayerUiModel {
    private final int seatIndex;
    private final int lifeTotal;
    private final int rotationDegrees;
    private final int backgroundColorRes;
    private final int foregroundColorRes;
    private final int recentLifeChange;
    private final long recentLifeChangeTimestampMs;
    private final boolean commanderDamageVisible;
    private final List<CommanderDamageUiModel> commanderDamages;

    public LifePlayerUiModel(
            int seatIndex,
            int lifeTotal,
            int rotationDegrees,
            int backgroundColorRes,
            int foregroundColorRes,
            int recentLifeChange,
            long recentLifeChangeTimestampMs,
            boolean commanderDamageVisible,
            List<CommanderDamageUiModel> commanderDamages) {
        this.seatIndex = seatIndex;
        this.lifeTotal = lifeTotal;
        this.rotationDegrees = rotationDegrees;
        this.backgroundColorRes = backgroundColorRes;
        this.foregroundColorRes = foregroundColorRes;
        this.recentLifeChange = recentLifeChange;
        this.recentLifeChangeTimestampMs = recentLifeChangeTimestampMs;
        this.commanderDamageVisible = commanderDamageVisible;
        this.commanderDamages = commanderDamages;
    }

    public int getSeatIndex() {
        return seatIndex;
    }

    public int getLifeTotal() {
        return lifeTotal;
    }

    public int getRotationDegrees() {
        return rotationDegrees;
    }

    public int getBackgroundColorRes() {
        return backgroundColorRes;
    }

    public int getForegroundColorRes() {
        return foregroundColorRes;
    }

    public int getRecentLifeChange() {
        return recentLifeChange;
    }

    public long getRecentLifeChangeTimestampMs() {
        return recentLifeChangeTimestampMs;
    }

    public boolean isCommanderDamageVisible() {
        return commanderDamageVisible;
    }

    public List<CommanderDamageUiModel> getCommanderDamages() {
        return commanderDamages;
    }
}
