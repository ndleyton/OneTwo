package com.nicue.onetwo.ui.life;

public class LifePlayerUiModel {
    private final int seatIndex;
    private final int lifeTotal;
    private final int rotationDegrees;
    private final int backgroundColorRes;
    private final int foregroundColorRes;

    public LifePlayerUiModel(int seatIndex, int lifeTotal, int rotationDegrees, int backgroundColorRes, int foregroundColorRes) {
        this.seatIndex = seatIndex;
        this.lifeTotal = lifeTotal;
        this.rotationDegrees = rotationDegrees;
        this.backgroundColorRes = backgroundColorRes;
        this.foregroundColorRes = foregroundColorRes;
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
}

